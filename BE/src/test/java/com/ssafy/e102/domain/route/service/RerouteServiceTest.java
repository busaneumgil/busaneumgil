package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ssafy.e102.domain.route.dto.request.RerouteRequest;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class RerouteServiceTest {

	private final RerouteService service = new RerouteService();

	@Test
	@DisplayName("routeId 또는 currentPoint가 없으면 RT4001로 차단한다")
	void rejectMissingRequiredFields() {
		UUID userId = UUID.randomUUID();

		assertRouteError(
			() -> service.reroute(userId, new RerouteRequest(null, new GeoPointRequest(35.12, 128.936))),
			RouteErrorCode.INVALID_REROUTE_REQUEST);
		assertRouteError(
			() -> service.reroute(userId, new RerouteRequest("rt_001", null)),
			RouteErrorCode.INVALID_REROUTE_REQUEST);
	}

	@Test
	@DisplayName("현재 위치 좌표 형식 오류는 RT4005로 차단한다")
	void rejectInvalidCurrentPointFormat() {
		assertRouteError(
			() -> service.reroute(UUID.randomUUID(), new RerouteRequest("rt_001", new GeoPointRequest(null, 128.936))),
			RouteErrorCode.INVALID_CURRENT_POINT);
		assertRouteError(
			() -> service.reroute(UUID.randomUUID(), new RerouteRequest("rt_001", new GeoPointRequest(91.0, 128.936))),
			RouteErrorCode.INVALID_CURRENT_POINT);
	}

	@Test
	@DisplayName("현재 위치가 부산 서비스 영역 밖이면 RT4003으로 차단한다")
	void rejectOutOfServiceAreaCurrentPoint() {
		assertRouteError(
			() -> service.reroute(UUID.randomUUID(),
				new RerouteRequest("rt_001", new GeoPointRequest(37.5665, 126.9780))),
			RouteErrorCode.OUT_OF_SERVICE_AREA);
	}

	private void assertRouteError(Runnable action, RouteErrorCode expectedErrorCode) {
		assertThatThrownBy(action::run)
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(expectedErrorCode);
	}
}
