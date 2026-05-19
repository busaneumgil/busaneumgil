package com.ssafy.e102.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ssafy.e102.domain.report.dto.request.HazardReportRerouteRequest;
import com.ssafy.e102.domain.report.dto.response.HazardReportRerouteResponse;
import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.repository.HazardReportRepository;
import com.ssafy.e102.domain.report.type.ReportStatus;
import com.ssafy.e102.domain.report.type.ReportType;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.service.RouteProjectionGeometryService;
import com.ssafy.e102.domain.route.service.WalkRouteProfileService;
import com.ssafy.e102.domain.route.service.WalkRouteUserProfile;
import com.ssafy.e102.domain.route.service.WalkRouteUserProfileQueryService;
import com.ssafy.e102.domain.route.service.WalkRoutePayloadService;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.RouteSessionStatus;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.type.MobilitySubtype;
import com.ssafy.e102.domain.user.type.PrimaryUserType;
import com.ssafy.e102.domain.user.type.SocialProvider;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteRequest;
import com.ssafy.e102.global.geo.GeoPointConverter;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

class HazardReportRerouteServiceTest {

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

	@Mock
	private HazardReportRepository hazardReportRepository;

	@Mock
	private RouteSessionRepository routeSessionRepository;

	@Mock
	private RouteProjectionGeometryService routeProjectionGeometryService;

	@Mock
	private HazardReportAvoidAreaBuilder hazardReportAvoidAreaBuilder;

	@Mock
	private HazardReportRerouteCustomModelFactory hazardReportRerouteCustomModelFactory;

	@Mock
	private GraphHopperRouteClient graphHopperRouteClient;

	@Mock
	private WalkRoutePayloadService walkRoutePayloadService;

	@Mock
	private WalkRouteUserProfileQueryService walkRouteUserProfileQueryService;

	@Mock
	private WalkRouteProfileService walkRouteProfileService;

	private HazardReportRerouteService service;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final GeoPointConverter geoPointConverter = new GeoPointConverter();

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		service = new HazardReportRerouteService(
			hazardReportRepository,
			routeSessionRepository,
			routeProjectionGeometryService,
			hazardReportAvoidAreaBuilder,
			hazardReportRerouteCustomModelFactory,
			graphHopperRouteClient,
			walkRoutePayloadService,
			walkRouteUserProfileQueryService,
			walkRouteProfileService,
			objectMapper);
	}

	@Test
	@DisplayName("reroute is rejected when the report is not owned by the authenticated user")
	void rejectOtherUsersReport() {
		UUID userId = UUID.randomUUID();
		when(hazardReportRepository.findWithImagesByReportId(12L))
			.thenReturn(Optional.of(report(UUID.randomUUID(), 12L, 35.1, 129.1)));

		assertThatThrownBy(() -> service.reroute(
			userId,
			12L,
			new HazardReportRerouteRequest("rr_active_123", new GeoPointRequest(35.1, 129.1))))
			.isInstanceOf(HazardReportException.class)
			.extracting("errorCode")
			.isEqualTo(HazardReportErrorCode.HAZARD_REPORT_FORBIDDEN);
	}

	@Test
	@DisplayName("reroute is rejected when the active route session routeId does not match the request")
	void rejectRouteIdMismatch() {
		UUID userId = UUID.randomUUID();
		when(hazardReportRepository.findWithImagesByReportId(12L))
			.thenReturn(Optional.of(report(userId, 12L, 35.1, 129.1)));
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
			eq(userId),
			eq("rr_active_123"),
			eq(RouteSessionStatus.ACTIVE)))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.reroute(
			userId,
			12L,
			new HazardReportRerouteRequest("rr_active_123", new GeoPointRequest(35.1, 129.1))))
			.isInstanceOf(RouteException.class)
			.extracting("errorCode")
			.isEqualTo(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);

		verify(routeSessionRepository).findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
			eq(userId),
			eq("rr_active_123"),
			eq(RouteSessionStatus.ACTIVE));
		verify(routeSessionRepository, never()).findFirstByUser_UserIdAndStatusOrderByUpdatedAtDesc(eq(userId), any());
		verify(graphHopperRouteClient, never()).routeWithCustomModel(any(), any());
	}

	@Test
	@DisplayName("reroute preserves the user's walk profile when requesting the alternate route")
	void rerouteUsesUserWalkProfile() {
		UUID userId = UUID.randomUUID();
		HazardReport report = report(userId, 12L, 35.1200, 129.0000);
		RouteSession routeSession = routeSession("rr_active_123");
		GraphHopperRoutePath reroutedPath = new GraphHopperRoutePath(
			BigDecimal.valueOf(80),
			60_000L,
			List.of(),
			java.util.Map.of());
		RouteSummaryResponse reroutedRoute = routeSummary("rerouted-route-id", RouteOption.SHORTEST);
		when(hazardReportRepository.findWithImagesByReportId(12L)).thenReturn(Optional.of(report));
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
			eq(userId),
			eq("rr_active_123"),
			eq(RouteSessionStatus.ACTIVE)))
			.thenReturn(Optional.of(routeSession));
		when(routeProjectionGeometryService.restoreRouteSnapshot(routeSession))
			.thenReturn(routeSummary("rr_active_123", RouteOption.SHORTEST));
		when(routeProjectionGeometryService.projectRoutePoint(any(RouteSummaryResponse.class), any(Point.class)))
			.thenReturn(new RouteProjectionGeometryService.ProjectedRoutePoint(
				new Coordinate(129.0000, 35.1200),
				0,
				5.0,
				new Coordinate(128.9999, 35.1200),
				new Coordinate(129.0001, 35.1200)));
		when(hazardReportAvoidAreaBuilder.build(any(), any(), any()))
			.thenReturn(GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(128.9999, 35.1199),
				new Coordinate(129.0001, 35.1199),
				new Coordinate(129.0001, 35.1201),
				new Coordinate(128.9999, 35.1201),
				new Coordinate(128.9999, 35.1199)
			}));
		when(hazardReportRerouteCustomModelFactory.create(any()))
			.thenReturn(JsonNodeFactory.instance.objectNode());
		when(walkRouteUserProfileQueryService.getProfile(userId))
			.thenReturn(new WalkRouteUserProfile(PrimaryUserType.MOBILITY_IMPAIRED, MobilitySubtype.MANUAL_WHEELCHAIR));
		when(walkRouteProfileService.resolve(
			PrimaryUserType.MOBILITY_IMPAIRED,
			MobilitySubtype.MANUAL_WHEELCHAIR,
			RouteOption.SHORTEST))
			.thenReturn(WalkRouteProfile.WHEELCHAIR_MANUAL_FAST);
		when(graphHopperRouteClient.routeWithCustomModel(any(GraphHopperRouteRequest.class), any()))
			.thenReturn(reroutedPath);
		when(walkRoutePayloadService.toRouteSummary(any(), any()))
			.thenReturn(reroutedRoute);

		HazardReportRerouteResponse response = service.reroute(
			userId,
			12L,
			new HazardReportRerouteRequest("rr_active_123", new GeoPointRequest(35.1, 129.1)));

		assertThat(response.rerouted()).isTrue();
		verify(graphHopperRouteClient).routeWithCustomModel(
			argThat(request -> request.profile() == WalkRouteProfile.WHEELCHAIR_MANUAL_FAST),
			any());
		verify(walkRoutePayloadService).toRouteSummary(
			eq("rr_active_123"),
			argThat(candidate -> candidate.profile() == WalkRouteProfile.WHEELCHAIR_MANUAL_FAST));
	}

	@Test
	@DisplayName("reroute returns rerouted false when the request-local avoid area yields no route")
	void returnsNoAlternateRouteWhenGraphHopperCannotRoute() {
		UUID userId = UUID.randomUUID();
		HazardReport report = report(userId, 12L, 35.1200, 129.0000);
		RouteSession routeSession = routeSession("rr_active_123");
		when(hazardReportRepository.findWithImagesByReportId(12L)).thenReturn(Optional.of(report));
		when(routeSessionRepository.findFirstByUser_UserIdAndRouteIdAndStatusOrderByUpdatedAtDesc(
			eq(userId),
			eq("rr_active_123"),
			eq(RouteSessionStatus.ACTIVE)))
			.thenReturn(Optional.of(routeSession));
		when(routeProjectionGeometryService.restoreRouteSnapshot(routeSession)).thenReturn(routeSummary("rr_active_123"));
		when(routeProjectionGeometryService.projectRoutePoint(any(RouteSummaryResponse.class), any(Point.class)))
			.thenReturn(new RouteProjectionGeometryService.ProjectedRoutePoint(
				new Coordinate(129.0000, 35.1200),
				0,
				5.0,
				new Coordinate(128.9999, 35.1200),
				new Coordinate(129.0001, 35.1200)));
		when(hazardReportAvoidAreaBuilder.build(any(), any(), any()))
			.thenReturn(GEOMETRY_FACTORY.createPolygon(new Coordinate[] {
				new Coordinate(128.9999, 35.1199),
				new Coordinate(129.0001, 35.1199),
				new Coordinate(129.0001, 35.1201),
				new Coordinate(128.9999, 35.1201),
				new Coordinate(128.9999, 35.1199)
			}));
		when(hazardReportRerouteCustomModelFactory.create(any()))
			.thenReturn(JsonNodeFactory.instance.objectNode());
		when(walkRouteUserProfileQueryService.getProfile(userId))
			.thenReturn(new WalkRouteUserProfile(PrimaryUserType.LOW_VISION, null));
		when(walkRouteProfileService.resolve(PrimaryUserType.LOW_VISION, null, RouteOption.SAFE))
			.thenReturn(WalkRouteProfile.VISUAL_SAFE);
		when(graphHopperRouteClient.routeWithCustomModel(any(), any()))
			.thenThrow(new RouteException(RouteErrorCode.ROUTE_NOT_FOUND));

		HazardReportRerouteResponse response = service.reroute(
			userId,
			12L,
			new HazardReportRerouteRequest("rr_active_123", new GeoPointRequest(35.1, 129.1)));

		assertThat(response.rerouted()).isFalse();
		assertThat(response.route()).isNull();
	}

	private HazardReport report(UUID userId, Long reportId, double lat, double lng) {
		User user = User.create(SocialProvider.KAKAO, "kakao", PrimaryUserType.LOW_VISION, null);
		ReflectionTestUtils.setField(user, "userId", userId);
		HazardReport report = HazardReport.create(
			user,
			ReportType.RAMP,
			"report",
			geoPointConverter.toPoint(new GeoPointRequest(lat, lng)),
			List.of());
		ReflectionTestUtils.setField(report, "reportId", reportId);
		ReflectionTestUtils.setField(report, "status", ReportStatus.PENDING);
		return report;
	}

	private RouteSession routeSession(String routeId) {
		User user = User.create(SocialProvider.KAKAO, "kakao", PrimaryUserType.LOW_VISION, null);
		return RouteSession.create(
			user,
			routeId,
			GEOMETRY_FACTORY.createPoint(new Coordinate(128.9999, 35.1200)),
			GEOMETRY_FACTORY.createPoint(new Coordinate(129.0100, 35.1200)),
			objectMapper.valueToTree(routeSummary(routeId)));
	}

	private RouteSummaryResponse routeSummary(String routeId) {
		return routeSummary(routeId, RouteOption.SAFE);
	}

	private RouteSummaryResponse routeSummary(String routeId, RouteOption routeOption) {
		return new RouteSummaryResponse(
			routeId,
			TransportMode.WALK,
			routeOption,
			List.of(routeOption),
			"safe route",
			BigDecimal.valueOf(120),
			90,
			2,
			List.of(),
			List.of(),
			"LINESTRING(128.9999 35.1200, 129.0001 35.1200, 129.0100 35.1200)",
			List.of(new RouteLegResponse(
				1,
				TransportMode.WALK,
				RouteLegRole.WALK_ONLY,
				"Walk",
				BigDecimal.valueOf(120),
				90,
				2,
				"LINESTRING(128.9999 35.1200, 129.0001 35.1200, 129.0100 35.1200)",
				List.of())));
	}
}
