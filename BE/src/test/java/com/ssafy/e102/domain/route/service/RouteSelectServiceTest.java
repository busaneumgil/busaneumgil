package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.request.SelectRouteRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStopResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.TransitLaneOptionResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;

class RouteSelectServiceTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private RouteSearchCacheService routeSearchCacheService;
	private RouteSessionRepository routeSessionRepository;
	private UserRepository userRepository;
	private RouteSelectService service;

	@BeforeEach
	void setUp() {
		routeSearchCacheService = mock(RouteSearchCacheService.class);
		routeSessionRepository = mock(RouteSessionRepository.class);
		userRepository = mock(UserRepository.class);
		service = new RouteSelectService(routeSearchCacheService, routeSessionRepository, userRepository,
			new ObjectMapper());
	}

	@Test
	@DisplayName("Redis 후보 route를 ACTIVE route session으로 저장한다")
	void selectStoresActiveRouteSession() {
		RouteSummaryResponse route = transitRoute("rt_selected_001");
		User user = user(USER_ID);
		when(routeSearchCacheService.getOwnedRouteOrThrow(USER_ID, "rs_transit_test", "rt_selected_001"))
			.thenReturn(route);
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

		service.select(USER_ID, "rt_selected_001", new SelectRouteRequest("rs_transit_test"));

		ArgumentCaptor<RouteSession> sessionCaptor = ArgumentCaptor.forClass(RouteSession.class);
		verify(routeSessionRepository).save(sessionCaptor.capture());
		RouteSession session = sessionCaptor.getValue();
		assertThat(session.getUser()).isEqualTo(user);
		assertThat(session.getRouteId()).isEqualTo("rt_selected_001");
		assertThat(session.getStatus().name()).isEqualTo("ACTIVE");
		assertThat(session.getStartPoint().getY()).isEqualTo(35.12);
		assertThat(session.getStartPoint().getX()).isEqualTo(128.936);
		assertThat(session.getEndPoint().getY()).isEqualTo(35.14);
		assertThat(session.getEndPoint().getX()).isEqualTo(128.956);
		assertThat(session.getRouteSnapshotJson().get("routeId").asText()).isEqualTo("rt_selected_001");
	}

	@Test
	@DisplayName("선택 snapshot에는 검색 시점 실시간 remainingMinute를 저장하지 않는다")
	void selectRemovesLiveRemainingMinuteFromSnapshot() {
		when(routeSearchCacheService.getOwnedRouteOrThrow(USER_ID, "rs_transit_test", "rt_selected_001"))
			.thenReturn(transitRoute("rt_selected_001"));
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID));

		service.select(USER_ID, "rt_selected_001", new SelectRouteRequest("rs_transit_test"));

		ArgumentCaptor<RouteSession> sessionCaptor = ArgumentCaptor.forClass(RouteSession.class);
		verify(routeSessionRepository).save(sessionCaptor.capture());
		JsonNode laneOption = sessionCaptor.getValue()
			.getRouteSnapshotJson()
			.get("legs")
			.get(0)
			.get("laneOptions")
			.get(0);
		assertThat(laneOption.has("remainingMinute")).isFalse();
		assertThat(laneOption.get("routeNo").asText()).isEqualTo("강서구13");
	}

	@Test
	@DisplayName("대중교통 metadata가 있으면 route snapshot에 backendMetadata로 함께 저장한다")
	void selectStoresTransitBackendMetadata() {
		when(routeSearchCacheService.getOwnedRouteOrThrow(USER_ID, "rs_transit_test", "rt_selected_001"))
			.thenReturn(transitRoute("rt_selected_001"));
		when(routeSearchCacheService.findTransitMetadata("rs_transit_test", "rt_selected_001"))
			.thenReturn(java.util.Optional.of(new TransitRouteSnapshot(
				"rt_selected_001",
				"map-object",
				List.of(Map.of(
					"type", TransportMode.BUS,
					"lanes", List.of(Map.of("busLocalBlID", "BL1")),
					"passStops", List.of(Map.of("localStationID", "BS1")))))));
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID));

		service.select(USER_ID, "rt_selected_001", new SelectRouteRequest("rs_transit_test"));

		ArgumentCaptor<RouteSession> sessionCaptor = ArgumentCaptor.forClass(RouteSession.class);
		verify(routeSessionRepository).save(sessionCaptor.capture());
		JsonNode backendMetadata = sessionCaptor.getValue().getRouteSnapshotJson().get("backendMetadata");
		assertThat(backendMetadata.get("routeId").asText()).isEqualTo("rt_selected_001");
		assertThat(backendMetadata.get("mapObj").asText()).isEqualTo("map-object");
		assertThat(backendMetadata.get("legs").get(0).get("lanes").get(0).get("busLocalBlID").asText())
			.isEqualTo("BL1");
	}

	@Test
	@DisplayName("route geometry가 없으면 leg geometry의 처음과 끝을 session 좌표로 사용한다")
	void selectFallsBackToLegGeometryWhenRouteGeometryIsMissing() {
		RouteSummaryResponse route = new RouteSummaryResponse(
			"rt_selected_001",
			TransportMode.WALK,
			RouteOption.SAFE,
			"안전 경로",
			BigDecimal.valueOf(100),
			60,
			1,
			List.of(RouteBadge.CROSSWALK),
			null,
			List.of(walkLeg("LINESTRING(128.936 35.12, 128.956 35.14)")));
		when(routeSearchCacheService.getOwnedRouteOrThrow(USER_ID, "rs_walk_test", "rt_selected_001"))
			.thenReturn(route);
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID));

		service.select(USER_ID, "rt_selected_001", new SelectRouteRequest("rs_walk_test"));

		ArgumentCaptor<RouteSession> sessionCaptor = ArgumentCaptor.forClass(RouteSession.class);
		verify(routeSessionRepository).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getValue().getStartPoint().getY()).isEqualTo(35.12);
		assertThat(sessionCaptor.getValue().getEndPoint().getY()).isEqualTo(35.14);
	}

	@Test
	@DisplayName("선택 route geometry를 해석할 수 없으면 RT4090으로 차단한다")
	void selectRejectsBrokenRouteGeometry() {
		RouteSummaryResponse route = new RouteSummaryResponse(
			"rt_selected_001",
			TransportMode.WALK,
			RouteOption.SAFE,
			"안전 경로",
			BigDecimal.valueOf(100),
			60,
			1,
			List.of(),
			"BROKEN",
			List.of());
		when(routeSearchCacheService.getOwnedRouteOrThrow(USER_ID, "rs_walk_test", "rt_selected_001"))
			.thenReturn(route);

		assertThatThrownBy(() -> service.select(USER_ID, "rt_selected_001", new SelectRouteRequest("rs_walk_test")))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.ROUTE_SELECT_CONFLICT);
	}

	private RouteSummaryResponse transitRoute(String routeId) {
		return new RouteSummaryResponse(
			routeId,
			TransportMode.PUBLIC_TRANSIT,
			RouteOption.RECOMMENDED,
			List.of(RouteOption.RECOMMENDED),
			"강서구13 경로",
			BigDecimal.valueOf(2500),
			900,
			15,
			0,
			List.of(RouteBadge.CROSSWALK),
			"LINESTRING(128.936 35.12, 128.946 35.13, 128.956 35.14)",
			List.of(new RouteLegResponse(
				1,
				TransportMode.BUS,
				RouteLegRole.TRANSIT,
				"강서구13번 버스에 탑승하세요.",
				BigDecimal.valueOf(2500),
				900,
				15,
				"LINESTRING(128.936 35.12, 128.956 35.14)",
				List.of(),
				"강서구13",
				List.of(new TransitLaneOptionResponse("강서구13", 5, 900, 15, true)),
				new RouteStopResponse("출발 정류장", BigDecimal.valueOf(35.12), BigDecimal.valueOf(128.936)),
				new RouteStopResponse("도착 정류장", BigDecimal.valueOf(35.14), BigDecimal.valueOf(128.956)),
				true,
				List.of())));
	}

	private RouteLegResponse walkLeg(String geometry) {
		return new RouteLegResponse(
			1,
			TransportMode.WALK,
			RouteLegRole.WALK_ONLY,
			"목적지까지 이동하세요.",
			BigDecimal.valueOf(100),
			60,
			1,
			geometry,
			List.of());
	}

	private User user(UUID userId) {
		User user = User.create(SocialProvider.KAKAO, "kakao-user-id", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}
}
