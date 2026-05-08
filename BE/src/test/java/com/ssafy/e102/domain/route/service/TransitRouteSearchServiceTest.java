package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.RouteGuidanceEventResponse;
import com.ssafy.e102.domain.route.dto.response.RouteGuidanceEventType;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.entity.SubwayStationElevator;
import com.ssafy.e102.domain.route.entity.SubwayTimetable;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.SubwayStationElevatorRepository;
import com.ssafy.e102.domain.route.repository.SubwayStationRepository;
import com.ssafy.e102.domain.route.repository.SubwayTimetableRepository;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.SubwayServiceDayType;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.user.type.MobilitySubtype;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.global.external.bims.BusanBimsArrival;
import com.ssafy.e102.global.external.bims.BusanBimsClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperCoordinate;
import com.ssafy.e102.global.external.graphhopper.GraphHopperPathDetail;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteRequest;
import com.ssafy.e102.global.external.odsay.OdsayClient;
import com.ssafy.e102.global.external.odsay.OdsayLaneGeometry;
import com.ssafy.e102.global.external.odsay.OdsayPassStop;
import com.ssafy.e102.global.external.odsay.OdsayTransitLane;
import com.ssafy.e102.global.external.odsay.OdsayTransitLeg;
import com.ssafy.e102.global.external.odsay.OdsayTransitPath;
import com.ssafy.e102.global.external.odsay.OdsayTransitSearchResult;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class TransitRouteSearchServiceTest {

	private static final GeoPointRequest START = new GeoPointRequest(35.1600, 129.0600);
	private static final GeoPointRequest END = new GeoPointRequest(35.1700, 129.0700);
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	@Mock
	private WalkRouteUserProfileQueryService userProfileQueryService;

	@Mock
	private SubwayStationElevatorRepository subwayStationElevatorRepository;

	@Mock
	private SubwayStationRepository subwayStationRepository;

	@Mock
	private SubwayTimetableRepository subwayTimetableRepository;

	@Mock
	private GraphHopperRouteClient graphHopperRouteClient;

	@Mock
	private BusanBimsClient busanBimsClient;

	@Mock
	private OdsayClient odsayClient;

	@Mock
	private RouteSearchCacheService routeSearchCacheService;

	private TransitRouteSearchService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new TransitRouteSearchService(
			userProfileQueryService,
			subwayStationElevatorRepository,
			subwayStationRepository,
			subwayTimetableRepository,
			new WalkRouteProfileService(),
			new WalkRoutePayloadService(new RouteTurnInstructionService()),
			graphHopperRouteClient,
			busanBimsClient,
			odsayClient,
			routeSearchCacheService);
		when(userProfileQueryService.getProfile(any()))
			.thenReturn(new WalkRouteUserProfile(PrimaryUserType.MOBILITY_IMPAIRED, MobilitySubtype.POWER_WHEELCHAIR));
	}

	@Test
	@DisplayName("ODsay 후보가 없으면 RT4040으로 반환한다")
	void throwsRouteNotFoundWhenOdsayReturnsNoCandidate() {
		when(odsayClient.searchPubTransPath(START, END)).thenReturn(new OdsayTransitSearchResult(List.of()));

		assertThatThrownBy(() -> service.search(UUID.randomUUID(), request()))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.ROUTE_NOT_FOUND);
	}

	@Test
	@DisplayName("ODsay timeout은 EX5040으로 전파한다")
	void propagatesOdsayTimeout() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenThrow(new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT));

		assertThatThrownBy(() -> service.search(UUID.randomUUID(), request()))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT);
	}

	@Test
	@DisplayName("GraphHopper 일반 실패는 EX5020으로 전파한다")
	void propagatesGraphHopperFailure() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(busPath("map-1", "100", 20, 300))));
		when(odsayClient.loadLane("map-1"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(busanBimsClient.findArrival("BS1", "BL1", "100"))
			.thenReturn(new BusanBimsArrival("BS1", "BL1", "100", null, null));
		when(graphHopperRouteClient.route(any()))
			.thenThrow(new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED));

		assertThatThrownBy(() -> service.search(UUID.randomUUID(), request()))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED);
	}

	@Test
	@DisplayName("BIMS timeout은 EX5040으로 전파한다")
	void propagatesBimsTimeout() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(busPath("map-1", "100", 20, 300))));
		when(odsayClient.loadLane("map-1"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(graphHopperRouteClient.route(any())).thenAnswer(invocation -> walkPath(invocation.getArgument(0)));
		when(busanBimsClient.findArrival("BS1", "BL1", "100"))
			.thenThrow(new RouteException(RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT));

		assertThatThrownBy(() -> service.search(UUID.randomUUID(), request()))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT);
	}

	@Test
	@DisplayName("WALK 연결 실패 후보는 제외하고 가능한 후보만 반환한다")
	void excludesCandidateWhenWalkConnectionNotFound() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(
				busPath("map-blocked", "100", 20, 500),
				busPath("map-ok", "101", 25, 700))));
		when(odsayClient.loadLane("map-blocked"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(odsayClient.loadLane("map-ok"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(busanBimsClient.findArrival(any(), any(), any()))
			.thenReturn(new BusanBimsArrival("BS1", "BL1", "101", 3, true));
		when(graphHopperRouteClient.route(any()))
			.thenThrow(new RouteException(RouteErrorCode.ROUTE_NOT_FOUND))
			.thenAnswer(invocation -> walkPath(invocation.getArgument(0)))
			.thenAnswer(invocation -> walkPath(invocation.getArgument(0)));

		WalkRouteSearchResponse response = service.search(UUID.randomUUID(), request());

		assertThat(response.routes()).hasSize(1);
		assertThat(response.routes().get(0).title()).contains("101");
	}

	@Test
	@DisplayName("transit WALK leg는 walk payload의 guidanceEvents/badges 구조를 유지하고 route badges로 집계한다")
	void mapsWalkConnectionBadgesIntoTransitRouteAndLegs() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(busPath("map-1", "100", 20, 300))));
		when(odsayClient.loadLane("map-1"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(busanBimsClient.findArrival("BS1", "BL1", "100"))
			.thenReturn(new BusanBimsArrival("BS1", "BL1", "100", 3, true));
		when(graphHopperRouteClient.route(any()))
			.thenAnswer(invocation -> walkPathWithDetails(
				invocation.getArgument(0),
				Map.of(
					"slope_state", List.of(new GraphHopperPathDetail(0, 1, "MODERATE")),
					"segment_type", List.of(new GraphHopperPathDetail(0, 1, "CROSS_WALK")))))
			.thenAnswer(invocation -> walkPathWithDetails(
				invocation.getArgument(0),
				Map.of(
					"stairs_state", List.of(new GraphHopperPathDetail(0, 1, "YES")),
					"surface_state", List.of(new GraphHopperPathDetail(0, 1, "UNPAVED")))));

		WalkRouteSearchResponse response = service.search(UUID.randomUUID(), request());

		assertThat(response.routes()).hasSize(1);
		assertThat(response.routes().get(0).badges())
			.containsExactly(
				RouteBadge.MIDDLE_SLOPE,
				RouteBadge.CROSSWALK,
				RouteBadge.STAIR,
				RouteBadge.UNPAVED);
		assertThat(response.routes().get(0).legs().get(0).role()).isEqualTo(RouteLegRole.WALK_TO_TRANSIT);
		assertThat(response.routes().get(0).legs().get(0).badges())
			.containsExactly(RouteBadge.MIDDLE_SLOPE, RouteBadge.CROSSWALK);
		assertThat(response.routes().get(0).legs().get(0).guidanceEvents())
			.extracting(RouteGuidanceEventResponse::type)
			.containsExactly(RouteGuidanceEventType.CROSSWALK, RouteGuidanceEventType.BUS_STOP);
		assertThat(response.routes().get(0).legs().get(1).type()).isEqualTo(TransportMode.BUS);
		assertThat(response.routes().get(0).legs().get(1).guidanceEvents())
			.extracting(RouteGuidanceEventResponse::type)
			.containsExactly(RouteGuidanceEventType.ARRIVING_POINT);
		RouteGuidanceEventResponse arrivingPoint = response.routes().get(0).legs().get(1).guidanceEvents().get(0);
		assertThat(arrivingPoint.distanceFromLegStartMeter()).isEqualByComparingTo("1500.00");
		assertThat(arrivingPoint.durationFromLegStartSecond()).isEqualTo(600);
		assertThat(arrivingPoint.geometry()).isEqualTo("POINT(129.066 35.166)");
		assertThat(response.routes().get(0).legs().get(2).role()).isEqualTo(RouteLegRole.TRANSIT_TO_WALK);
		assertThat(response.routes().get(0).legs().get(2).badges())
			.containsExactly(RouteBadge.STAIR, RouteBadge.UNPAVED);
		assertThat(response.routes().get(0).legs().get(2).guidanceEvents())
			.extracting(RouteGuidanceEventResponse::type)
			.containsExactly(
				RouteGuidanceEventType.STAIR,
				RouteGuidanceEventType.DESTINATION);
	}

	@Test
	@DisplayName("마지막 WALK leg의 ODSay 시간이 0분이어도 목적지 도보 역할로 반환한다")
	void mapsZeroMinuteFinalWalkToTransitToWalkRole() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(busPathWithFinalWalk(
				"map-1",
				"100",
				20,
				300,
				zeroMinuteWalkLeg()))));
		when(odsayClient.loadLane("map-1"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(busanBimsClient.findArrival("BS1", "BL1", "100"))
			.thenReturn(new BusanBimsArrival("BS1", "BL1", "100", 3, true));
		when(graphHopperRouteClient.route(any())).thenAnswer(invocation -> walkPath(invocation.getArgument(0)));

		WalkRouteSearchResponse response = service.search(UUID.randomUUID(), request());

		assertThat(response.routes()).hasSize(1);
		assertThat(response.routes().get(0).legs()).hasSize(3);
		assertThat(response.routes().get(0).legs().get(2).role()).isEqualTo(RouteLegRole.TRANSIT_TO_WALK);
		assertThat(response.routes().get(0).legs().get(2).instruction()).isEqualTo("목적지까지 이동하세요.");
		assertThat(response.routes().get(0).legs().get(2).guidanceEvents())
			.extracting(RouteGuidanceEventResponse::type)
			.containsExactly(RouteGuidanceEventType.DESTINATION);
	}

	@Test
	@DisplayName("BIMS 도착정보가 없어도 후보를 실패 처리하지 않는다")
	void keepsBusCandidateWhenBimsArrivalIsEmpty() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(busPath("map-1", "100", 20, 300))));
		when(odsayClient.loadLane("map-1"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(busanBimsClient.findArrival("BS1", "BL1", "100"))
			.thenReturn(new BusanBimsArrival("BS1", "BL1", "100", null, null));
		when(graphHopperRouteClient.route(any())).thenAnswer(invocation -> walkPath(invocation.getArgument(0)));

		WalkRouteSearchResponse response = service.search(UUID.randomUUID(), request());

		assertThat(response.routes()).hasSize(1);
		assertThat(response.routes().get(0).legs())
			.filteredOn(leg -> leg.type() == TransportMode.BUS)
			.first()
			.extracting(leg -> leg.laneOptions().get(0).remainingMinute(), leg -> leg.laneOptions().get(0).isLowFloor())
			.containsExactly(null, null);
	}

	@Test
	@DisplayName("SUBWAY 승하차 좌표를 엘리베이터 point로 보정하고 시간표 snapshot을 저장한다")
	@SuppressWarnings("unchecked")
	void adjustsSubwayStopsAndStoresTimetableSnapshot() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(subwayPath("map-subway"))));
		when(odsayClient.loadLane("map-subway"))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.SUBWAY, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(subwayStationElevatorRepository.findByOdsayStationId("S1"))
			.thenReturn(List.of(
				elevator("S1", "서면", "부산 1호선", 35.1580, 129.0580),
				elevator("S1", "서면", "부산 1호선", 35.1590, 129.0590)));
		when(subwayStationElevatorRepository.findByOdsayStationId("S2"))
			.thenReturn(List.of(elevator("S2", "부산역", "부산 1호선", 35.1150, 129.0410)));
		when(subwayTimetableRepository.findNextDepartures(
			eq("S1"),
			any(SubwayServiceDayType.class),
			eq(1),
			anyInt(),
			any(Pageable.class)))
			.thenReturn(List.of(SubwayTimetable.create("S1", SubwayServiceDayType.WEEKDAY, 1, "08:02", 28920, "노포")));
		when(graphHopperRouteClient.route(any())).thenAnswer(invocation -> walkPath(invocation.getArgument(0)));

		WalkRouteSearchResponse response = service.search(UUID.randomUUID(), request());

		assertThat(response.routes()).hasSize(1);
		assertThat(response.routes().get(0).legs())
			.filteredOn(leg -> leg.type() == TransportMode.SUBWAY)
			.first()
			.satisfies(leg -> {
				assertThat(leg.boardingStop().name()).isEqualTo("서면 엘리베이터");
				assertThat(leg.boardingStop().lat()).isEqualByComparingTo("35.159");
				assertThat(leg.boardingStop().lng()).isEqualByComparingTo("129.059");
			});

		assertThat(response.routes().get(0).legs().get(0).guidanceEvents())
			.extracting(RouteGuidanceEventResponse::type)
			.containsExactly(RouteGuidanceEventType.SUBWAY_ELEVATOR);
		assertThat(response.routes().get(0).legs())
			.filteredOn(leg -> leg.type() == TransportMode.SUBWAY)
			.first()
			.satisfies(leg -> assertThat(leg.guidanceEvents())
				.extracting(RouteGuidanceEventResponse::type)
				.containsExactly(RouteGuidanceEventType.ARRIVING_POINT));

		ArgumentCaptor<List<TransitRouteSnapshot>> snapshotCaptor = ArgumentCaptor.forClass(List.class);
		verify(routeSearchCacheService).saveTransitMetadata(any(), snapshotCaptor.capture());
		assertThat(snapshotCaptor.getValue().get(0).legs())
			.filteredOn(snapshot -> snapshot.get("type") == TransportMode.SUBWAY)
			.first()
			.satisfies(snapshot -> {
				assertThat(snapshot.get("odsayStationId")).isEqualTo("S1");
				assertThat(snapshot.get("wayCode")).isEqualTo(1);
				Map<String, Object> nextDeparture = (Map<String, Object>)snapshot.get("nextDeparture");
				assertThat(nextDeparture)
					.containsEntry("departureTimeText", "08:02")
					.containsEntry("endStationName", "노포");
			});
	}

	@Test
	@DisplayName("최종 후보는 최대 3개로 제한하고 실제 중복 경로는 routeOptions로 병합한다")
	void selectsTopThreeAndMergesDuplicateRouteOptions() {
		when(odsayClient.searchPubTransPath(START, END))
			.thenReturn(new OdsayTransitSearchResult(List.of(
				busPath("map-fast", "100", 20, 500),
				busPath("map-min-walk", "100", 25, 100),
				busPath("map-extra-1", "101", 30, 700),
				busPath("map-extra-2", "102", 35, 800))));
		when(odsayClient.loadLane(any()))
			.thenReturn(List.of(new OdsayLaneGeometry(TransportMode.BUS, "LINESTRING(129.061 35.161, 129.066 35.166)")));
		when(busanBimsClient.findArrival(any(), any(), any()))
			.thenReturn(new BusanBimsArrival("BS1", "BL1", "100", 3, true));
		when(graphHopperRouteClient.route(any())).thenAnswer(invocation -> walkPath(invocation.getArgument(0)));

		WalkRouteSearchResponse response = service.search(UUID.randomUUID(), request());

		assertThat(response.routes()).hasSize(3);
		assertThat(response.routes().get(0).routeOptions())
			.contains(RouteOption.RECOMMENDED, RouteOption.MIN_TRANSFER, RouteOption.MIN_WALK);
	}

	private WalkRouteSearchRequest request() {
		return new WalkRouteSearchRequest(START, END);
	}

	private OdsayTransitPath busPath(String mapObj, String busNo, int totalTimeMinute, int totalWalkMeter) {
		return busPathWithFinalWalk(mapObj, busNo, totalTimeMinute, totalWalkMeter, walkLeg());
	}

	private OdsayTransitPath busPathWithFinalWalk(
		String mapObj,
		String busNo,
		int totalTimeMinute,
		int totalWalkMeter,
		OdsayTransitLeg finalWalkLeg) {
		return new OdsayTransitPath(
			BigDecimal.valueOf(3000),
			totalTimeMinute,
			totalWalkMeter,
			1,
			0,
			mapObj,
			List.of(
				walkLeg(),
				busLeg(busNo),
				finalWalkLeg),
			Map.of());
	}

	private OdsayTransitPath subwayPath(String mapObj) {
		return new OdsayTransitPath(
			BigDecimal.valueOf(3500),
			25,
			600,
			0,
			1,
			mapObj,
			List.of(
				walkLeg(),
				subwayLeg(),
				walkLeg()),
			Map.of());
	}

	private OdsayTransitLeg walkLeg() {
		return new OdsayTransitLeg(
			TransportMode.WALK,
			BigDecimal.valueOf(300),
			5,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			List.of(),
			List.of(),
			Map.of());
	}

	private OdsayTransitLeg zeroMinuteWalkLeg() {
		return new OdsayTransitLeg(
			TransportMode.WALK,
			BigDecimal.ZERO,
			0,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			List.of(),
			List.of(),
			Map.of());
	}

	private OdsayTransitLeg busLeg(String busNo) {
		return new OdsayTransitLeg(
			TransportMode.BUS,
			BigDecimal.valueOf(1500),
			10,
			"승차정류장",
			BigDecimal.valueOf(35.1610),
			BigDecimal.valueOf(129.0610),
			"OS1",
			"BS1",
			"ARS1",
			null,
			null,
			"하차정류장",
			BigDecimal.valueOf(35.1660),
			BigDecimal.valueOf(129.0660),
			"OS2",
			"BS2",
			"ARS2",
			null,
			null,
			null,
			null,
			List.of(new OdsayTransitLane(busNo, "BL1", null, busNo)),
			List.of(new OdsayPassStop("OS1", "승차정류장", "BS1", "ARS1", BigDecimal.valueOf(35.1610),
				BigDecimal.valueOf(129.0610))),
			Map.of());
	}

	private OdsayTransitLeg subwayLeg() {
		return new OdsayTransitLeg(
			TransportMode.SUBWAY,
			BigDecimal.valueOf(2000),
			12,
			"서면",
			BigDecimal.valueOf(35.1577),
			BigDecimal.valueOf(129.0578),
			"S1",
			null,
			null,
			BigDecimal.valueOf(35.1570),
			BigDecimal.valueOf(129.0570),
			"부산역",
			BigDecimal.valueOf(35.1152),
			BigDecimal.valueOf(129.0413),
			"S2",
			null,
			null,
			BigDecimal.valueOf(35.1155),
			BigDecimal.valueOf(129.0415),
			1,
			"노포행",
			List.of(new OdsayTransitLane(null, null, "1", "부산 1호선")),
			List.of(),
			Map.of());
	}

	private GraphHopperRoutePath walkPath(GraphHopperRouteRequest request) {
		return walkPathWithDetails(request, Map.of());
	}

	private GraphHopperRoutePath walkPathWithDetails(
		GraphHopperRouteRequest request,
		Map<String, List<GraphHopperPathDetail>> details) {
		return new GraphHopperRoutePath(
			BigDecimal.valueOf(300),
			300_000,
			List.of(
				new GraphHopperCoordinate(
					BigDecimal.valueOf(request.startPoint().lng()),
					BigDecimal.valueOf(request.startPoint().lat())),
				new GraphHopperCoordinate(
					BigDecimal.valueOf(request.endPoint().lng()),
					BigDecimal.valueOf(request.endPoint().lat()))),
			details);
	}

	private SubwayStationElevator elevator(String id, String name, String lineName, double lat, double lng) {
		return SubwayStationElevator.create(
			id,
			name,
			lineName,
			GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat)));
	}
}
