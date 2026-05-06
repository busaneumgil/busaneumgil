package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.e102.domain.route.dto.request.CreateFavoriteRouteRequest;
import com.ssafy.e102.domain.route.dto.request.UpdateFavoriteRouteRequest;
import com.ssafy.e102.domain.route.dto.response.FavoriteRouteIdResponse;
import com.ssafy.e102.domain.route.dto.response.FavoriteRouteListResponse;
import com.ssafy.e102.domain.route.entity.FavoriteRoute;
import com.ssafy.e102.domain.route.exception.FavoriteRouteErrorCode;
import com.ssafy.e102.domain.route.exception.FavoriteRouteException;
import com.ssafy.e102.domain.route.repository.FavoriteRouteRepository;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class FavoriteRouteServiceTest {

	@Mock
	private FavoriteRouteRepository favoriteRouteRepository;

	@Mock
	private UserRepository userRepository;

	private FavoriteRouteService favoriteRouteService;
	private GeoPointConverter geoPointConverter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		geoPointConverter = new GeoPointConverter();
		favoriteRouteService = new FavoriteRouteService(favoriteRouteRepository, userRepository, geoPointConverter);
	}

	@Test
	@DisplayName("경로 북마크 목록은 최신 저장순 cursor 기반으로 조회한다")
	void getFavoriteRoutes() {
		UUID userId = UUID.randomUUID();
		FavoriteRoute favoriteRoute = favoriteRoute(user(userId), 1L);
		PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "favRouteId"));
		when(favoriteRouteRepository.findAllByUser_UserId(userId, pageable))
			.thenReturn(new SliceImpl<>(List.of(favoriteRoute), pageable, false));

		FavoriteRouteListResponse response = favoriteRouteService.getFavoriteRoutes(userId, null, 10);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).favRouteId()).isEqualTo(1L);
		assertThat(response.content().get(0).routeName()).isEqualTo("부산시민공원-부산역");
		assertThat(response.size()).isEqualTo(10);
		assertThat(response.nextCursor()).isNull();
		assertThat(response.hasNext()).isFalse();
	}

	@Test
	@DisplayName("경로 북마크 목록은 마지막 경로 ID 이후 cursor로 조회한다")
	void getFavoriteRoutesWithCursor() {
		UUID userId = UUID.randomUUID();
		FavoriteRoute favoriteRoute = favoriteRoute(user(userId), 3L);
		PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "favRouteId"));
		when(favoriteRouteRepository.findAllByUser_UserIdAndFavRouteIdLessThan(userId, 10L, pageable))
			.thenReturn(new SliceImpl<>(List.of(favoriteRoute), pageable, true));

		FavoriteRouteListResponse response = favoriteRouteService.getFavoriteRoutes(userId, 10L, 2);

		assertThat(response.content()).hasSize(1);
		assertThat(response.nextCursor()).isEqualTo(3L);
		assertThat(response.hasNext()).isTrue();
	}

	@Test
	@DisplayName("경로 북마크 저장은 현재 사용자와 요청 좌표를 저장한다")
	void createFavoriteRoute() {
		UUID userId = UUID.randomUUID();
		User user = user(userId);
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(favoriteRouteRepository.save(any(FavoriteRoute.class))).thenAnswer(invocation -> {
			FavoriteRoute favoriteRoute = invocation.getArgument(0);
			ReflectionTestUtils.setField(favoriteRoute, "favRouteId", 1L);
			return favoriteRoute;
		});

		FavoriteRouteIdResponse response = favoriteRouteService.createFavoriteRoute(
			userId,
			createRequest());

		assertThat(response.favRouteId()).isEqualTo(1L);
		verify(favoriteRouteRepository).save(any(FavoriteRoute.class));
	}

	@Test
	@DisplayName("경로 북마크 수정은 현재 사용자 소유 경로만 변경한다")
	void updateFavoriteRoute() {
		UUID userId = UUID.randomUUID();
		FavoriteRoute favoriteRoute = favoriteRoute(user(userId), 1L);
		when(favoriteRouteRepository.findById(1L)).thenReturn(Optional.of(favoriteRoute));

		FavoriteRouteIdResponse response = favoriteRouteService.updateFavoriteRoute(
			userId,
			1L,
			new UpdateFavoriteRouteRequest("서면역", null, null, null, RouteOption.SHORTEST));

		assertThat(response.favRouteId()).isEqualTo(1L);
		assertThat(favoriteRoute.getRouteName()).isEqualTo("서면역-부산역");
		assertThat(favoriteRoute.getRouteOption()).isEqualTo(RouteOption.SHORTEST);
	}

	@Test
	@DisplayName("수정 요청에 변경 필드가 없으면 거부한다")
	void rejectEmptyUpdateRequest() {
		assertThatThrownBy(() -> favoriteRouteService.updateFavoriteRoute(
			UUID.randomUUID(),
			1L,
			new UpdateFavoriteRouteRequest(null, null, null, null, null)))
			.isInstanceOf(FavoriteRouteException.class)
			.extracting("errorCode")
			.isEqualTo(FavoriteRouteErrorCode.INVALID_FAVORITE_ROUTE_UPDATE_REQUEST);

		verify(favoriteRouteRepository, never()).findById(1L);
	}

	@Test
	@DisplayName("다른 사용자의 경로 북마크 수정은 거부한다")
	void rejectUpdateOtherUserFavoriteRoute() {
		FavoriteRoute favoriteRoute = favoriteRoute(user(UUID.randomUUID()), 1L);
		when(favoriteRouteRepository.findById(1L)).thenReturn(Optional.of(favoriteRoute));

		assertThatThrownBy(() -> favoriteRouteService.updateFavoriteRoute(
			UUID.randomUUID(),
			1L,
			new UpdateFavoriteRouteRequest("서면역", null, null, null, null)))
			.isInstanceOf(FavoriteRouteException.class)
			.extracting("errorCode")
			.isEqualTo(FavoriteRouteErrorCode.FAVORITE_ROUTE_FORBIDDEN);
	}

	@Test
	@DisplayName("경로 북마크 삭제는 현재 사용자 소유 경로만 삭제한다")
	void deleteFavoriteRoute() {
		UUID userId = UUID.randomUUID();
		FavoriteRoute favoriteRoute = favoriteRoute(user(userId), 1L);
		when(favoriteRouteRepository.findById(1L)).thenReturn(Optional.of(favoriteRoute));

		favoriteRouteService.deleteFavoriteRoute(userId, 1L);

		verify(favoriteRouteRepository).delete(favoriteRoute);
	}

	private CreateFavoriteRouteRequest createRequest() {
		return new CreateFavoriteRouteRequest(
			"부산시민공원",
			"부산역",
			new GeoPointRequest(35.1686, 129.0576),
			new GeoPointRequest(35.1152, 129.0422),
			RouteOption.SAFE);
	}

	private FavoriteRoute favoriteRoute(User user, Long favRouteId) {
		FavoriteRoute favoriteRoute = FavoriteRoute.create(
			user,
			"부산시민공원",
			"부산역",
			geoPointConverter.toPoint(new GeoPointRequest(35.1686, 129.0576)),
			geoPointConverter.toPoint(new GeoPointRequest(35.1152, 129.0422)),
			RouteOption.SAFE);
		ReflectionTestUtils.setField(favoriteRoute, "favRouteId", favRouteId);
		return favoriteRoute;
	}

	private User user(UUID userId) {
		User user = User.create(SocialProvider.KAKAO, "kakao-user-id", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}
}
