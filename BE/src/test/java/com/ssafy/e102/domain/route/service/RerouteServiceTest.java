package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.request.RerouteRequest;
import com.ssafy.e102.domain.route.dto.response.RerouteResponse;
import com.ssafy.e102.domain.route.dto.response.RerouteType;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class RerouteServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private RouteSessionRepository routeSessionRepository;
	private RerouteService service;

	@BeforeEach
	void setUp() {
		routeSessionRepository = mock(RouteSessionRepository.class);
		service = new RerouteService(routeSessionRepository, objectMapper);
	}

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

	@Test
	@DisplayName("사용자 소유 route session이 없고 같은 routeId가 다른 사용자에게 있으면 A4030으로 차단한다")
	void rejectRouteSessionOwnedByOtherUser() {
		UUID userId = UUID.randomUUID();
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, "rt_other"))
			.thenReturn(Optional.empty());
		when(routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc("rt_other"))
			.thenReturn(Optional.of(mock(RouteSession.class)));

		assertRouteError(
			() -> service.reroute(userId, new RerouteRequest("rt_other", new GeoPointRequest(35.12, 128.936))),
			RouteErrorCode.ROUTE_ACCESS_DENIED);
	}

	@Test
	@DisplayName("사용자 소유 route session과 복구 가능한 snapshot이 없으면 RT4043으로 차단한다")
	void rejectMissingRouteSession() {
		UUID userId = UUID.randomUUID();
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, "rt_missing"))
			.thenReturn(Optional.empty());
		when(routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc("rt_missing"))
			.thenReturn(Optional.empty());

		assertRouteError(
			() -> service.reroute(userId, new RerouteRequest("rt_missing", new GeoPointRequest(35.12, 128.936))),
			RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
	}

	@Test
	@DisplayName("route session snapshot을 복구할 수 없으면 RT4043으로 차단한다")
	void rejectBrokenRouteSnapshot() {
		UUID userId = UUID.randomUUID();
		RouteSession routeSession = mock(RouteSession.class);
		when(routeSession.getRouteSnapshotJson()).thenReturn(objectMapper.createObjectNode().put("routeId", "rt_001"));
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, "rt_001"))
			.thenReturn(Optional.of(routeSession));

		assertRouteError(
			() -> service.reroute(userId, new RerouteRequest("rt_001", new GeoPointRequest(35.12, 128.936))),
			RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
	}

	@Test
	@DisplayName("현재 위치가 route geometry 10m 이하면 NO_REROUTE_NEEDED를 반환한다")
	void returnsNoRerouteNeededWhenCurrentPointIsStillNearRouteGeometry() {
		UUID userId = UUID.randomUUID();
		RouteSession routeSession = routeSession(routeSummary("rt_001"));
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, "rt_001"))
			.thenReturn(Optional.of(routeSession));

		RerouteResponse response = service.reroute(
			userId,
			new RerouteRequest("rt_001", new GeoPointRequest(35.12001, 128.93601)));

		assertThat(response.rerouteType()).isEqualTo(RerouteType.NO_REROUTE_NEEDED);
		assertThat(response.route()).isNull();
	}

	@Test
	@DisplayName("route geometry WKT를 파싱할 수 없으면 RT4043으로 차단한다")
	void rejectBrokenRouteGeometry() {
		UUID userId = UUID.randomUUID();
		RouteSession routeSession = routeSession(new RouteSummaryResponse(
			"rt_001",
			TransportMode.WALK,
			RouteOption.SAFE,
			"안전 경로",
			BigDecimal.valueOf(120),
			90,
			2,
			List.of(RouteBadge.LOW_SLOPE),
			"BROKEN",
			List.of()));
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, "rt_001"))
			.thenReturn(Optional.of(routeSession));

		assertRouteError(
			() -> service.reroute(userId, new RerouteRequest("rt_001", new GeoPointRequest(35.12, 128.936))),
			RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
	}

	private RouteSummaryResponse routeSummary(String routeId) {
		return new RouteSummaryResponse(
			routeId,
			TransportMode.WALK,
			RouteOption.SAFE,
			"안전 경로",
			BigDecimal.valueOf(120),
			90,
			2,
			List.of(RouteBadge.LOW_SLOPE),
			"LINESTRING(128.936 35.12, 128.937 35.121)",
			List.of());
	}

	private RouteSession routeSession(RouteSummaryResponse route) {
		RouteSession routeSession = mock(RouteSession.class);
		when(routeSession.getRouteSnapshotJson()).thenReturn(objectMapper.valueToTree(route));
		return routeSession;
	}

	private void assertRouteError(Runnable action, RouteErrorCode expectedErrorCode) {
		assertThatThrownBy(action::run)
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(expectedErrorCode);
	}
}
