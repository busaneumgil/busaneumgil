package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.request.RouteRatingRequest;
import com.ssafy.e102.domain.route.dto.response.RouteRatingResponse;
import com.ssafy.e102.domain.route.entity.RouteRating;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteRatingRepository;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;

class RouteRatingServiceTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final ObjectMapper objectMapper = new ObjectMapper();
	private RouteRatingRepository routeRatingRepository;
	private RouteSessionRepository routeSessionRepository;
	private UserRepository userRepository;
	private RouteRatingService service;

	@BeforeEach
	void setUp() {
		routeRatingRepository = mock(RouteRatingRepository.class);
		routeSessionRepository = mock(RouteSessionRepository.class);
		userRepository = mock(UserRepository.class);
		service = new RouteRatingService(routeRatingRepository, routeSessionRepository, userRepository);
	}

	@Test
	@DisplayName("route session snapshot을 route_context_json으로 복사해 rating을 저장한다")
	void rateStoresRouteContextFromSession() {
		JsonNode snapshot = snapshot("rt_selected_001");
		RouteSession routeSession = mock(RouteSession.class);
		when(routeSession.getRouteSnapshotJson()).thenReturn(snapshot);
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.of(routeSession));
		when(routeRatingRepository.findByUser_UserIdAndRouteId(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.empty());
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID));
		when(routeRatingRepository.save(org.mockito.ArgumentMatchers.any(RouteRating.class)))
			.thenAnswer(invocation -> {
				RouteRating rating = invocation.getArgument(0);
				ReflectionTestUtils.setField(rating, "ratingId", 1L);
				return rating;
			});

		RouteRatingResponse response = service.rate(USER_ID, new RouteRatingRequest("rt_selected_001", 5));

		ArgumentCaptor<RouteRating> ratingCaptor = ArgumentCaptor.forClass(RouteRating.class);
		verify(routeRatingRepository).save(ratingCaptor.capture());
		assertThat(response.ratingId()).isEqualTo(1L);
		assertThat(ratingCaptor.getValue().getRouteId()).isEqualTo("rt_selected_001");
		assertThat(ratingCaptor.getValue().getScore()).isEqualTo((short)5);
		assertThat(ratingCaptor.getValue().getRouteContextJson()).isEqualTo(snapshot);
	}

	@Test
	@DisplayName("route session이 없어도 route_context_json=null로 rating을 저장한다")
	void rateStoresRatingWithoutRouteContextWhenSessionIsMissing() {
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "rt_without_session"))
			.thenReturn(Optional.empty());
		when(routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc("rt_without_session"))
			.thenReturn(Optional.empty());
		when(routeRatingRepository.findByUser_UserIdAndRouteId(USER_ID, "rt_without_session"))
			.thenReturn(Optional.empty());
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID));
		when(routeRatingRepository.save(org.mockito.ArgumentMatchers.any(RouteRating.class)))
			.thenAnswer(invocation -> {
				RouteRating rating = invocation.getArgument(0);
				ReflectionTestUtils.setField(rating, "ratingId", 2L);
				return rating;
			});

		RouteRatingResponse response = service.rate(USER_ID, new RouteRatingRequest("rt_without_session", 4));

		ArgumentCaptor<RouteRating> ratingCaptor = ArgumentCaptor.forClass(RouteRating.class);
		verify(routeRatingRepository).save(ratingCaptor.capture());
		assertThat(response.ratingId()).isEqualTo(2L);
		assertThat(ratingCaptor.getValue().getRouteContextJson()).isNull();
	}

	@Test
	@DisplayName("같은 사용자의 같은 routeId 평가는 최신 score로 갱신한다")
	void rateUpdatesExistingRating() {
		RouteRating existingRating = RouteRating.create(user(USER_ID), "rt_selected_001", 3, snapshot("old"));
		ReflectionTestUtils.setField(existingRating, "ratingId", 3L);
		JsonNode snapshot = snapshot("rt_selected_001");
		RouteSession routeSession = mock(RouteSession.class);
		when(routeSession.getRouteSnapshotJson()).thenReturn(snapshot);
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.of(routeSession));
		when(routeRatingRepository.findByUser_UserIdAndRouteId(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.of(existingRating));

		RouteRatingResponse response = service.rate(USER_ID, new RouteRatingRequest("rt_selected_001", 5));

		assertThat(response.ratingId()).isEqualTo(3L);
		assertThat(existingRating.getScore()).isEqualTo((short)5);
		assertThat(existingRating.getRouteContextJson()).isEqualTo(snapshot);
	}

	@Test
	@DisplayName("다른 사용자의 route session은 A4030으로 차단한다")
	void rateRejectsOtherUserRoute() {
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "other_route"))
			.thenReturn(Optional.empty());
		when(routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc("other_route"))
			.thenReturn(Optional.of(mock(RouteSession.class)));

		assertThatThrownBy(() -> service.rate(USER_ID, new RouteRatingRequest("other_route", 5)))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.ROUTE_ACCESS_DENIED);
	}

	private JsonNode snapshot(String routeId) {
		return objectMapper.createObjectNode().put("routeId", routeId);
	}

	private User user(UUID userId) {
		User user = User.create(SocialProvider.KAKAO, "kakao-user-id", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}
}
