package com.ssafy.e102.domain.route.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.e102.domain.route.exception.FavoriteRouteErrorCode;
import com.ssafy.e102.domain.route.exception.FavoriteRouteException;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class FavoriteRouteTest {

	private final GeoPointConverter geoPointConverter = new GeoPointConverter();

	@Test
	@DisplayName("경로 북마크는 출발지와 도착지 기준으로 경로명을 자동 생성한다")
	void createFavoriteRoute() {
		User user = user(UUID.randomUUID());
		Point startPoint = point(35.1686, 129.0576);
		Point endPoint = point(35.1152, 129.0422);

		FavoriteRoute favoriteRoute = FavoriteRoute.create(
			user,
			" 부산시민공원 ",
			" 부산역 ",
			startPoint,
			endPoint,
			RouteOption.SAFE);

		assertThat(favoriteRoute.getUser()).isEqualTo(user);
		assertThat(favoriteRoute.getStartLabel()).isEqualTo("부산시민공원");
		assertThat(favoriteRoute.getEndLabel()).isEqualTo("부산역");
		assertThat(favoriteRoute.getRouteName()).isEqualTo("부산시민공원-부산역");
		assertThat(favoriteRoute.getStartPoint()).isEqualTo(startPoint);
		assertThat(favoriteRoute.getEndPoint()).isEqualTo(endPoint);
		assertThat(favoriteRoute.getRouteOption()).isEqualTo(RouteOption.SAFE);
	}

	@Test
	@DisplayName("경로 북마크 수정은 경로명을 다시 계산한다")
	void changeRoute() {
		FavoriteRoute favoriteRoute = FavoriteRoute.create(
			user(UUID.randomUUID()),
			"부산시민공원",
			"부산역",
			point(35.1686, 129.0576),
			point(35.1152, 129.0422),
			RouteOption.SAFE);

		favoriteRoute.changeRoute(
			"서면역",
			"광안리",
			point(35.1577, 129.0590),
			point(35.1532, 129.1187),
			RouteOption.SHORTEST);

		assertThat(favoriteRoute.getRouteName()).isEqualTo("서면역-광안리");
		assertThat(favoriteRoute.getRouteOption()).isEqualTo(RouteOption.SHORTEST);
	}

	@Test
	@DisplayName("경로 북마크 생성 시 필수값이 없으면 생성 요청 오류로 거부한다")
	void rejectInvalidCreateRequest() {
		assertThatThrownBy(() -> FavoriteRoute.create(
			user(UUID.randomUUID()),
			" ",
			"부산역",
			point(35.1686, 129.0576),
			point(35.1152, 129.0422),
			RouteOption.SAFE))
			.isInstanceOf(FavoriteRouteException.class)
			.extracting("errorCode")
			.isEqualTo(FavoriteRouteErrorCode.INVALID_FAVORITE_ROUTE_REQUEST);
	}

	@Test
	@DisplayName("경로 북마크 수정 시 필수값이 없으면 수정 요청 오류로 거부한다")
	void rejectInvalidUpdateRequest() {
		FavoriteRoute favoriteRoute = FavoriteRoute.create(
			user(UUID.randomUUID()),
			"부산시민공원",
			"부산역",
			point(35.1686, 129.0576),
			point(35.1152, 129.0422),
			RouteOption.SAFE);

		assertThatThrownBy(() -> favoriteRoute.changeRoute(
			"",
			"부산역",
			point(35.1686, 129.0576),
			point(35.1152, 129.0422),
			RouteOption.SAFE))
			.isInstanceOf(FavoriteRouteException.class)
			.extracting("errorCode")
			.isEqualTo(FavoriteRouteErrorCode.INVALID_FAVORITE_ROUTE_UPDATE_REQUEST);
	}

	@Test
	@DisplayName("경로 북마크 소유자를 판별한다")
	void isOwner() {
		UUID userId = UUID.randomUUID();
		FavoriteRoute favoriteRoute = FavoriteRoute.create(
			user(userId),
			"부산시민공원",
			"부산역",
			point(35.1686, 129.0576),
			point(35.1152, 129.0422),
			RouteOption.SAFE);

		assertThat(favoriteRoute.isOwner(userId)).isTrue();
		assertThat(favoriteRoute.isOwner(UUID.randomUUID())).isFalse();
	}

	private Point point(double lat, double lng) {
		return geoPointConverter.toPoint(new GeoPointRequest(lat, lng));
	}

	private User user(UUID userId) {
		User user = User.create(SocialProvider.KAKAO, "kakao-user-id", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}
}
