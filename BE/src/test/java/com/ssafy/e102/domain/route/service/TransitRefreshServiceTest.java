package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.request.TransitRefreshRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.TransitArrivalStatus;
import com.ssafy.e102.domain.route.dto.response.TransitRefreshResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;

class TransitRefreshServiceTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private RouteSessionRepository routeSessionRepository;
	private ObjectMapper objectMapper;
	private TransitRefreshService service;

	@BeforeEach
	void setUp() {
		routeSessionRepository = mock(RouteSessionRepository.class);
		objectMapper = new ObjectMapper();
		service = new TransitRefreshService(routeSessionRepository, objectMapper);
	}

	@Test
	@DisplayName("선택된 BUS leg는 route session snapshot에서 복구해 refresh 대상이 된다")
	void refreshRestoresBusLegFromRouteSessionSnapshot() {
		RouteSession routeSession = routeSession(routeSummary(TransportMode.BUS), null);
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.of(routeSession));

		TransitRefreshResponse response = service.refresh(USER_ID, "rt_selected_001", new TransitRefreshRequest(2));

		assertThat(response.type()).isEqualTo(TransportMode.BUS);
		assertThat(response.arrivalStatus()).isEqualTo(TransitArrivalStatus.ARRIVAL_UNKNOWN);
		assertThat(response.transits()).isEmpty();
	}

	@Test
	@DisplayName("다른 사용자의 route session이면 A4030을 반환한다")
	void refreshRejectsRouteSessionOwnedByOtherUser() {
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "other_route"))
			.thenReturn(Optional.empty());
		when(routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc("other_route"))
			.thenReturn(Optional.of(mock(RouteSession.class)));

		assertThatThrownBy(() -> service.refresh(USER_ID, "other_route", new TransitRefreshRequest(2)))
			.isInstanceOf(RouteException.class)
			.extracting("errorCode")
			.isEqualTo(RouteErrorCode.ROUTE_ACCESS_DENIED);
	}

	@Test
	@DisplayName("route session이 없으면 RT4043을 반환한다")
	void refreshRejectsMissingRouteSession() {
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "missing_route"))
			.thenReturn(Optional.empty());
		when(routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc("missing_route"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.refresh(USER_ID, "missing_route", new TransitRefreshRequest(2)))
			.isInstanceOf(RouteException.class)
			.extracting("errorCode")
			.isEqualTo(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
	}

	@Test
	@DisplayName("없는 legSequence는 snapshot 복구 실패로 RT4043을 반환한다")
	void refreshRejectsMissingLegSequence() {
		RouteSession routeSession = routeSession(routeSummary(TransportMode.BUS), null);
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.of(routeSession));

		assertThatThrownBy(() -> service.refresh(USER_ID, "rt_selected_001", new TransitRefreshRequest(99)))
			.isInstanceOf(RouteException.class)
			.extracting("errorCode")
			.isEqualTo(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
	}

	@Test
	@DisplayName("WALK leg에 transit-refresh를 요청하면 PT4090을 반환한다")
	void refreshRejectsWalkLeg() {
		RouteSession routeSession = routeSession(routeSummary(TransportMode.WALK), null);
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.of(routeSession));

		assertThatThrownBy(() -> service.refresh(USER_ID, "rt_selected_001", new TransitRefreshRequest(2)))
			.isInstanceOf(RouteException.class)
			.extracting("errorCode")
			.isEqualTo(RouteErrorCode.NOT_TRANSIT_LEG);
	}

	@Test
	@DisplayName("route session snapshot에 backendMetadata가 있어도 FE route payload를 복구한다")
	void refreshIgnoresBackendMetadataWhenRestoringRoutePayload() {
		RouteSession routeSession = routeSession(routeSummary(TransportMode.BUS),
			objectMapper.createObjectNode().put("mapObj", "map-object"));
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(USER_ID, "rt_selected_001"))
			.thenReturn(Optional.of(routeSession));

		TransitRefreshResponse response = service.refresh(USER_ID, "rt_selected_001", new TransitRefreshRequest(2));

		assertThat(response.type()).isEqualTo(TransportMode.BUS);
	}

	private RouteSession routeSession(RouteSummaryResponse route, JsonNode backendMetadata) {
		JsonNode snapshot = objectMapper.valueToTree(route);
		if (backendMetadata != null && snapshot instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode) {
			objectNode.set("backendMetadata", backendMetadata);
		}
		RouteSession routeSession = mock(RouteSession.class);
		when(routeSession.getRouteSnapshotJson()).thenReturn(snapshot);
		return routeSession;
	}

	private RouteSummaryResponse routeSummary(TransportMode targetLegType) {
		return new RouteSummaryResponse(
			"rt_selected_001",
			TransportMode.PUBLIC_TRANSIT,
			RouteOption.RECOMMENDED,
			List.of(RouteOption.RECOMMENDED),
			"대중교통 경로",
			BigDecimal.valueOf(1200),
			900,
			15,
			0,
			List.of(),
			"LINESTRING(128.936 35.12, 128.956 35.14)",
			List.of(
				leg(1, TransportMode.WALK, RouteLegRole.WALK_TO_TRANSIT),
				leg(2, targetLegType, targetLegType == TransportMode.WALK
					? RouteLegRole.WALK_TO_TRANSIT
					: RouteLegRole.TRANSIT)));
	}

	private RouteLegResponse leg(int sequence, TransportMode type, RouteLegRole role) {
		return new RouteLegResponse(
			sequence,
			type,
			role,
			type == TransportMode.WALK ? "정류장까지 이동하세요." : "버스에 탑승하세요.",
			BigDecimal.valueOf(500),
			300,
			5,
			"LINESTRING(128.936 35.12, 128.956 35.14)",
			List.of(),
			type == TransportMode.BUS ? "100" : null,
			List.of(),
			null,
			null,
			null,
			List.of());
	}
}
