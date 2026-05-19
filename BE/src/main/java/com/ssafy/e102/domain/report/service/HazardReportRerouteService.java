package com.ssafy.e102.domain.report.service;

import java.util.UUID;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.report.dto.request.HazardReportRerouteRequest;
import com.ssafy.e102.domain.report.dto.response.HazardReportRerouteResponse;
import com.ssafy.e102.domain.report.entity.HazardReport;
import com.ssafy.e102.domain.report.exception.HazardReportErrorCode;
import com.ssafy.e102.domain.report.exception.HazardReportException;
import com.ssafy.e102.domain.report.repository.HazardReportRepository;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.service.RouteProjectionGeometryService;
import com.ssafy.e102.domain.route.service.WalkRouteCandidate;
import com.ssafy.e102.domain.route.service.WalkRoutePayloadService;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.RouteSessionStatus;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.route.type.WalkRouteProfile;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteClient;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRouteRequest;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@Service
public class HazardReportRerouteService {

	private final HazardReportRepository hazardReportRepository;
	private final RouteSessionRepository routeSessionRepository;
	private final RouteProjectionGeometryService routeProjectionGeometryService;
	private final HazardReportAvoidAreaBuilder hazardReportAvoidAreaBuilder;
	private final HazardReportRerouteCustomModelFactory hazardReportRerouteCustomModelFactory;
	private final GraphHopperRouteClient graphHopperRouteClient;
	private final WalkRoutePayloadService walkRoutePayloadService;
	@SuppressWarnings("unused")
	private final ObjectMapper objectMapper;

	public HazardReportRerouteService(
		HazardReportRepository hazardReportRepository,
		RouteSessionRepository routeSessionRepository,
		RouteProjectionGeometryService routeProjectionGeometryService,
		HazardReportAvoidAreaBuilder hazardReportAvoidAreaBuilder,
		HazardReportRerouteCustomModelFactory hazardReportRerouteCustomModelFactory,
		GraphHopperRouteClient graphHopperRouteClient,
		WalkRoutePayloadService walkRoutePayloadService,
		ObjectMapper objectMapper) {
		this.hazardReportRepository = hazardReportRepository;
		this.routeSessionRepository = routeSessionRepository;
		this.routeProjectionGeometryService = routeProjectionGeometryService;
		this.hazardReportAvoidAreaBuilder = hazardReportAvoidAreaBuilder;
		this.hazardReportRerouteCustomModelFactory = hazardReportRerouteCustomModelFactory;
		this.graphHopperRouteClient = graphHopperRouteClient;
		this.walkRoutePayloadService = walkRoutePayloadService;
		this.objectMapper = objectMapper;
	}

	public HazardReportRerouteResponse reroute(UUID userId, Long reportId, HazardReportRerouteRequest request) {
		routeProjectionGeometryService.validateCurrentPoint(request.currentPoint());
		HazardReport hazardReport = hazardReportRepository.findWithImagesByReportId(reportId)
			.orElseThrow(() -> new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_NOT_FOUND));
		if (!hazardReport.isOwner(userId)) {
			throw new HazardReportException(HazardReportErrorCode.HAZARD_REPORT_FORBIDDEN);
		}

		RouteSession routeSession = routeSessionRepository
			.findFirstByUser_UserIdAndStatusOrderByUpdatedAtDesc(userId, RouteSessionStatus.ACTIVE)
			.orElseThrow(() -> new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND));
		if (!request.routeId().equals(routeSession.getRouteId())) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
		}

		RouteSummaryResponse currentRoute = routeProjectionGeometryService.restoreRouteSnapshot(routeSession);
		if (currentRoute.transportMode() != TransportMode.WALK) {
			return new HazardReportRerouteResponse(false, null);
		}

		RouteProjectionGeometryService.ProjectedRoutePoint reportProjection =
			routeProjectionGeometryService.projectRoutePoint(currentRoute, hazardReport.getReportPoint());
		Polygon avoidArea = hazardReportAvoidAreaBuilder.build(
			reportProjection.projectedCoordinate(),
			reportProjection.segmentStart(),
			reportProjection.segmentEnd());
		JsonNode customModel = hazardReportRerouteCustomModelFactory.create(avoidArea);

		try {
			GraphHopperRoutePath reroutedPath = graphHopperRouteClient.routeWithCustomModel(
				new GraphHopperRouteRequest(
					request.currentPoint(),
					endPoint(routeSession),
					profileFor(currentRoute.routeOption())),
				customModel);
			RouteSummaryResponse reroutedRoute = walkRoutePayloadService.toRouteSummary(
				request.routeId(),
				new WalkRouteCandidate(
					currentRoute.routeOption(),
					profileFor(currentRoute.routeOption()),
					reroutedPath));
			return new HazardReportRerouteResponse(true, withRouteId(reroutedRoute, newRouteId(request.routeId())));
		} catch (RouteException exception) {
			if (exception.getErrorCode() == RouteErrorCode.ROUTE_NOT_FOUND) {
				return new HazardReportRerouteResponse(false, null);
			}
			throw exception;
		}
	}

	private GeoPointRequest endPoint(RouteSession routeSession) {
		Point endPoint = routeSession.getEndPoint();
		if (endPoint == null) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
		}
		return new GeoPointRequest(endPoint.getY(), endPoint.getX());
	}

	private WalkRouteProfile profileFor(RouteOption routeOption) {
		return routeOption == RouteOption.SHORTEST ? WalkRouteProfile.PEDESTRIAN_FAST : WalkRouteProfile.PEDESTRIAN_SAFE;
	}

	private RouteSummaryResponse withRouteId(RouteSummaryResponse route, String routeId) {
		return new RouteSummaryResponse(
			routeId,
			route.transportMode(),
			route.routeOption(),
			route.routeOptions(),
			route.title(),
			route.distanceMeter(),
			route.durationSecond(),
			route.estimatedTimeMinute(),
			route.badges(),
			route.warnings(),
			route.geometry(),
			route.legs());
	}

	private String newRouteId(String currentRouteId) {
		return currentRouteId + "_hazard_" + UUID.randomUUID();
	}
}
