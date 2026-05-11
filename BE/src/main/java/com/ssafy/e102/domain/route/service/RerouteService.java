package com.ssafy.e102.domain.route.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.e102.domain.route.dto.request.RerouteRequest;
import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.RerouteResponse;
import com.ssafy.e102.domain.route.dto.response.RouteGuidanceEventResponse;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@Service
public class RerouteService {

	private static final double BUSAN_MIN_LAT = 34.85;
	private static final double BUSAN_MAX_LAT = 35.45;
	private static final double BUSAN_MIN_LNG = 128.70;
	private static final double BUSAN_MAX_LNG = 129.40;
	private static final double EARTH_RADIUS_METER = 6_371_000.0;
	private static final double NO_REROUTE_DISTANCE_METER = 10.0;
	private static final double WALK_REPAIR_MAX_DISTANCE_METER = 100.0;
	private static final double FULL_REROUTE_MAX_DISTANCE_METER = 500.0;
	private static final int SRID = 4326;

	private final RouteSessionRepository routeSessionRepository;
	private final RouteSessionCommandService routeSessionCommandService;
	private final ObjectMapper objectMapper;
	private final WalkRouteSearchService walkRouteSearchService;
	private final TransitRouteSearchService transitRouteSearchService;
	private final WKTReader wktReader = new WKTReader();
	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

	public RerouteService(
		RouteSessionRepository routeSessionRepository,
		RouteSessionCommandService routeSessionCommandService,
		ObjectMapper objectMapper,
		WalkRouteSearchService walkRouteSearchService,
		TransitRouteSearchService transitRouteSearchService) {
		this.routeSessionRepository = routeSessionRepository;
		this.routeSessionCommandService = routeSessionCommandService;
		this.objectMapper = objectMapper;
		this.walkRouteSearchService = walkRouteSearchService;
		this.transitRouteSearchService = transitRouteSearchService;
	}

	public RerouteResponse reroute(UUID userId, RerouteRequest request) {
		validateRequest(request);
		validateCurrentPoint(request.currentPoint());
		RouteSession routeSession = getOwnedRouteSession(userId, request.routeId());
		RouteSummaryResponse route = restoreRouteSnapshot(routeSession);
		RouteProjection projection = projectCurrentPoint(route, request.currentPoint());
		if (projection.distanceMeter() <= NO_REROUTE_DISTANCE_METER) {
			return new RerouteResponse(null);
		}
		if (projection.distanceMeter() <= WALK_REPAIR_MAX_DISTANCE_METER) {
			RouteSummaryResponse repairedRoute = walkRepair(userId, request.currentPoint(), route, projection);
			saveRerouteSession(userId, routeSession, request.currentPoint(), repairedRoute);
			return new RerouteResponse(repairedRoute);
		}
		if (projection.distanceMeter() <= FULL_REROUTE_MAX_DISTANCE_METER) {
			RouteSummaryResponse reroutedRoute = withNewRouteId(
				fullReroute(userId, request.currentPoint(), routeSession, route),
				newRouteId("rr_full"));
			saveRerouteSession(userId, routeSession, request.currentPoint(), reroutedRoute);
			return new RerouteResponse(reroutedRoute);
		}
		throw new RouteException(RouteErrorCode.ROUTE_TOO_FAR_FOR_REROUTE);
	}

	private void saveRerouteSession(
		UUID userId,
		RouteSession previousSession,
		GeoPointRequest currentPoint,
		RouteSummaryResponse route) {
		routeSessionCommandService.saveActiveSessionIfAbsent(
			userId,
			route.routeId(),
			toPoint(currentPoint),
			previousSession.getEndPoint(),
			objectMapper.valueToTree(route));
	}

	private Point toPoint(GeoPointRequest request) {
		Point point = geometryFactory.createPoint(new Coordinate(request.lng(), request.lat()));
		point.setSRID(SRID);
		return point;
	}

	private RouteSummaryResponse withNewRouteId(RouteSummaryResponse route, String routeId) {
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

	private String newRouteId(String prefix) {
		return prefix + "_" + UUID.randomUUID();
	}

	private RouteSummaryResponse walkRepair(
		UUID userId,
		GeoPointRequest currentPoint,
		RouteSummaryResponse previousRoute,
		RouteProjection projection) {
		if (projection.legSequence() == null || projection.projectedCoordinate() == null) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "복귀 경로를 만들 leg 정보를 찾을 수 없습니다.");
		}
		GeoPointRequest repairPoint = new GeoPointRequest(
			projection.projectedCoordinate().y,
			projection.projectedCoordinate().x);
		RouteSummaryResponse repairRoute = repairRoute(userId, currentPoint, repairPoint, previousRoute);
		RouteLegResponse repairLeg = repairRoute.legs()
			.stream()
			.findFirst()
			.orElseThrow(() -> new RouteException(RouteErrorCode.ROUTE_NOT_FOUND));
		List<RouteLegResponse> remainingLegs = remainingLegs(previousRoute, projection);
		if (remainingLegs.isEmpty()) {
			return withNewRouteId(repairRoute, newRouteId("rr_repair"));
		}
		List<RouteLegResponse> combinedLegs = new ArrayList<>();
		combinedLegs.add(withSequenceAndRole(repairLeg, 1, RouteLegRole.WALK_TO_DESTINATION, "기존 경로까지 이동하세요."));
		for (RouteLegResponse leg : remainingLegs) {
			combinedLegs.add(withSequence(leg, combinedLegs.size() + 1));
		}
		List<RouteLegResponse> offsetLegs = withRouteGuidanceOffsets(combinedLegs);
		BigDecimal distanceMeter = totalDistance(offsetLegs);
		int durationSecond = totalDuration(offsetLegs);
		return new RouteSummaryResponse(
			newRouteId("rr_repair"),
			previousRoute.transportMode(),
			previousRoute.routeOption(),
			previousRoute.routeOptions(),
			previousRoute.title(),
			distanceMeter,
			durationSecond,
			estimatedMinute(durationSecond),
			mergeBadges(repairRoute.badges(), previousRoute.badges(), offsetLegs),
			previousRoute.warnings(),
			mergeGeometry(offsetLegs),
			offsetLegs);
	}

	private RouteSummaryResponse repairRoute(
		UUID userId,
		GeoPointRequest currentPoint,
		GeoPointRequest repairPoint,
		RouteSummaryResponse previousRoute) {
		WalkRouteSearchResponse response = walkRouteSearchService.search(
			userId,
			new WalkRouteSearchRequest(currentPoint, repairPoint));
		return response.routes()
			.stream()
			.filter(route -> route.routeOptions() != null && route.routeOptions().contains(previousRoute.routeOption()))
			.findFirst()
			.or(() -> response.routes().stream().findFirst())
			.orElseThrow(() -> new RouteException(RouteErrorCode.ROUTE_NOT_FOUND));
	}

	private RouteSummaryResponse fullReroute(
		UUID userId,
		GeoPointRequest currentPoint,
		RouteSession routeSession,
		RouteSummaryResponse previousRoute) {
		WalkRouteSearchRequest searchRequest = new WalkRouteSearchRequest(
			currentPoint,
			endPoint(routeSession));
		WalkRouteSearchResponse response = switch (previousRoute.transportMode()) {
			case WALK -> walkRouteSearchService.search(userId, searchRequest);
			case PUBLIC_TRANSIT, BUS, SUBWAY -> transitRouteSearchService.search(userId, searchRequest);
		};
		return response.routes()
			.stream()
			.filter(route -> route.routeOptions() != null && route.routeOptions().contains(previousRoute.routeOption()))
			.findFirst()
			.or(() -> response.routes().stream().findFirst())
			.orElseThrow(() -> new RouteException(RouteErrorCode.ROUTE_NOT_FOUND));
	}

	private GeoPointRequest endPoint(RouteSession routeSession) {
		Point endPoint = routeSession.getEndPoint();
		if (endPoint == null) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
		}
		return new GeoPointRequest(endPoint.getY(), endPoint.getX());
	}

	private RouteSession getOwnedRouteSession(UUID userId, String routeId) {
		return routeSessionRepository.findFirstByUser_UserIdAndRouteIdOrderByUpdatedAtDesc(userId, routeId)
			.orElseGet(() -> {
				if (routeSessionRepository.findFirstByRouteIdOrderByUpdatedAtDesc(routeId).isPresent()) {
					throw new RouteException(RouteErrorCode.ROUTE_ACCESS_DENIED);
				}
				throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
			});
	}

	private RouteSummaryResponse restoreRouteSnapshot(RouteSession routeSession) {
		if (routeSession.getRouteSnapshotJson() == null || routeSession.getRouteSnapshotJson().isNull()) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND);
		}
		try {
			RouteSummaryResponse route = objectMapper.treeToValue(
				routePayloadSnapshot(routeSession.getRouteSnapshotJson()),
				RouteSummaryResponse.class);
			validateRestoredRoute(route);
			return route;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.", exception);
		}
	}

	private JsonNode routePayloadSnapshot(JsonNode snapshot) {
		if (snapshot.has("backendMetadata") && snapshot instanceof ObjectNode objectNode) {
			ObjectNode routePayload = objectNode.deepCopy();
			routePayload.remove("backendMetadata");
			return routePayload;
		}
		return snapshot;
	}

	private void validateRestoredRoute(RouteSummaryResponse route) {
		if (route == null || route.routeId() == null || route.routeId().isBlank()
			|| route.transportMode() == null || route.routeOption() == null
			|| route.geometry() == null || route.geometry().isBlank()) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.");
		}
	}

	private RouteProjection projectCurrentPoint(RouteSummaryResponse route, GeoPointRequest currentPoint) {
		List<LegGeometry> legGeometries = legGeometries(route);
		if (legGeometries.isEmpty()) {
			return nearestProjection(currentPoint, route.geometry(), null);
		}
		RouteProjection nearestProjection = null;
		for (LegGeometry legGeometry : legGeometries) {
			RouteProjection projection = nearestProjection(currentPoint, legGeometry.geometry(),
				legGeometry.sequence());
			double distanceMeter = projection.distanceMeter();
			if (nearestProjection == null || distanceMeter < nearestProjection.distanceMeter()) {
				nearestProjection = projection;
			}
		}
		return nearestProjection;
	}

	private List<LegGeometry> legGeometries(RouteSummaryResponse route) {
		if (route.legs() == null || route.legs().isEmpty()) {
			return List.of();
		}
		List<LegGeometry> legGeometries = new ArrayList<>();
		for (RouteLegResponse leg : route.legs()) {
			if (leg.geometry() != null && !leg.geometry().isBlank()) {
				legGeometries.add(new LegGeometry(leg.sequence(), leg.geometry()));
			}
		}
		return List.copyOf(legGeometries);
	}

	private RouteProjection nearestProjection(GeoPointRequest point, String wkt, Integer legSequence) {
		Geometry geometry = parseGeometry(wkt);
		Coordinate[] coordinates = geometry.getCoordinates();
		if (coordinates.length == 0) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.");
		}
		if (coordinates.length == 1) {
			return new RouteProjection(
				distanceMeter(point.lat(), point.lng(), coordinates[0].y, coordinates[0].x),
				legSequence,
				coordinates[0],
				0,
				0.0);
		}
		RouteProjection nearestProjection = null;
		double distanceFromStartMeter = 0.0;
		for (int index = 0; index < coordinates.length - 1; index++) {
			SegmentProjection projection = projectToSegment(point, coordinates[index], coordinates[index + 1]);
			double projectedDistanceFromStart = distanceFromStartMeter
				+ distanceMeter(
					coordinates[index].y,
					coordinates[index].x,
					projection.coordinate().y,
					projection.coordinate().x);
			RouteProjection routeProjection = new RouteProjection(
				projection.distanceMeter(),
				legSequence,
				projection.coordinate(),
				index,
				projectedDistanceFromStart);
			if (nearestProjection == null || routeProjection.distanceMeter() < nearestProjection.distanceMeter()) {
				nearestProjection = routeProjection;
			}
			distanceFromStartMeter += distanceMeter(
				coordinates[index].y,
				coordinates[index].x,
				coordinates[index + 1].y,
				coordinates[index + 1].x);
		}
		return nearestProjection;
	}

	private Geometry parseGeometry(String wkt) {
		try {
			return wktReader.read(wkt);
		} catch (ParseException | IllegalArgumentException exception) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.", exception);
		}
	}

	private SegmentProjection projectToSegment(GeoPointRequest point, Coordinate start, Coordinate end) {
		double referenceLat = Math.toRadians(point.lat());
		double pointX = toProjectedX(point.lng(), referenceLat);
		double pointY = toProjectedY(point.lat());
		double startX = toProjectedX(start.x, referenceLat);
		double startY = toProjectedY(start.y);
		double endX = toProjectedX(end.x, referenceLat);
		double endY = toProjectedY(end.y);
		double dx = endX - startX;
		double dy = endY - startY;
		double segmentLengthSquared = dx * dx + dy * dy;
		if (segmentLengthSquared == 0.0) {
			return new SegmentProjection(distanceMeter(point.lat(), point.lng(), start.y, start.x), start);
		}
		double t = ((pointX - startX) * dx + (pointY - startY) * dy) / segmentLengthSquared;
		double clampedT = Math.max(0.0, Math.min(1.0, t));
		double projectedX = startX + clampedT * dx;
		double projectedY = startY + clampedT * dy;
		Coordinate projectedCoordinate = new Coordinate(
			start.x + (end.x - start.x) * clampedT,
			start.y + (end.y - start.y) * clampedT);
		return new SegmentProjection(Math.hypot(pointX - projectedX, pointY - projectedY), projectedCoordinate);
	}

	private List<RouteLegResponse> remainingLegs(RouteSummaryResponse previousRoute, RouteProjection projection) {
		if (previousRoute.legs() == null || previousRoute.legs().isEmpty()) {
			return List.of();
		}
		List<RouteLegResponse> remainingLegs = new ArrayList<>();
		for (RouteLegResponse leg : previousRoute.legs()) {
			if (leg.sequence() < projection.legSequence()) {
				continue;
			}
			if (leg.sequence() == projection.legSequence()) {
				remainingLegs.add(truncateLeg(leg, projection));
			} else {
				remainingLegs.add(leg);
			}
		}
		return remainingLegs;
	}

	private RouteLegResponse truncateLeg(RouteLegResponse leg, RouteProjection projection) {
		Coordinate[] coordinates = parseGeometry(leg.geometry()).getCoordinates();
		List<Coordinate> truncatedCoordinates = new ArrayList<>();
		truncatedCoordinates.add(projection.projectedCoordinate());
		for (int index = projection.segmentIndex() + 1; index < coordinates.length; index++) {
			if (!sameCoordinate(truncatedCoordinates.get(truncatedCoordinates.size() - 1), coordinates[index])) {
				truncatedCoordinates.add(coordinates[index]);
			}
		}
		if (truncatedCoordinates.size() < 2) {
			truncatedCoordinates.add(projection.projectedCoordinate());
		}
		double originalDistance = leg.distanceMeter() == null
			? geometryDistanceMeter(coordinates)
			: leg.distanceMeter().doubleValue();
		double remainingDistance = Math.max(0.0, originalDistance - projection.distanceFromLegStartMeter());
		int projectedDuration = projectedDuration(leg.durationSecond(), projection.distanceFromLegStartMeter(),
			originalDistance);
		int remainingDuration = Math.max(0, leg.durationSecond() - projectedDuration);
		return new RouteLegResponse(
			leg.sequence(),
			leg.type(),
			leg.role(),
			leg.instruction(),
			scale(remainingDistance),
			remainingDuration,
			estimatedMinute(remainingDuration),
			toLineString(truncatedCoordinates),
			shiftGuidanceEvents(leg.guidanceEvents(), projection.distanceFromLegStartMeter(), projectedDuration),
			leg.routeNo(),
			leg.laneOptions(),
			leg.boardingStop(),
			leg.arrivingStop(),
			leg.isLowFloor(),
			leg.badges());
	}

	private List<RouteGuidanceEventResponse> shiftGuidanceEvents(
		List<RouteGuidanceEventResponse> guidanceEvents,
		double distanceOffset,
		int durationOffset) {
		if (guidanceEvents == null || guidanceEvents.isEmpty()) {
			return List.of();
		}
		List<RouteGuidanceEventResponse> shiftedEvents = new ArrayList<>();
		for (RouteGuidanceEventResponse event : guidanceEvents) {
			double shiftedDistance = event.distanceFromLegStartMeter().doubleValue() - distanceOffset;
			if (shiftedDistance < 0.0) {
				continue;
			}
			shiftedEvents.add(new RouteGuidanceEventResponse(
				shiftedEvents.size() + 1,
				event.type(),
				scale(shiftedDistance),
				Math.max(0, event.durationFromLegStartSecond() - durationOffset),
				event.geometry()));
		}
		return List.copyOf(shiftedEvents);
	}

	private int projectedDuration(int durationSecond, double distanceFromLegStartMeter, double originalDistanceMeter) {
		if (durationSecond <= 0 || originalDistanceMeter <= 0.0) {
			return 0;
		}
		return (int)Math.round(durationSecond * Math.min(1.0, distanceFromLegStartMeter / originalDistanceMeter));
	}

	private List<RouteLegResponse> withRouteGuidanceOffsets(List<RouteLegResponse> legs) {
		List<RouteLegResponse> offsetLegs = new ArrayList<>();
		BigDecimal distanceOffset = BigDecimal.ZERO.setScale(2);
		int durationOffset = 0;
		for (RouteLegResponse leg : legs) {
			offsetLegs.add(withRouteGuidanceOffsets(leg, distanceOffset, durationOffset));
			if (leg.distanceMeter() != null) {
				distanceOffset = distanceOffset.add(leg.distanceMeter()).setScale(2, RoundingMode.HALF_UP);
			}
			durationOffset += leg.durationSecond();
		}
		return List.copyOf(offsetLegs);
	}

	private RouteLegResponse withRouteGuidanceOffsets(
		RouteLegResponse leg,
		BigDecimal distanceOffset,
		int durationOffset) {
		List<RouteGuidanceEventResponse> guidanceEvents = leg.guidanceEvents() == null
			? List.of()
			: leg.guidanceEvents()
				.stream()
				.map(event -> new RouteGuidanceEventResponse(
					event.sequence(),
					event.type(),
					event.distanceFromLegStartMeter(),
					event.durationFromLegStartSecond(),
					distanceOffset.add(event.distanceFromLegStartMeter()).setScale(2, RoundingMode.HALF_UP),
					durationOffset + event.durationFromLegStartSecond(),
					event.geometry()))
				.toList();
		return new RouteLegResponse(
			leg.sequence(),
			leg.type(),
			leg.role(),
			leg.instruction(),
			leg.distanceMeter(),
			leg.durationSecond(),
			leg.estimatedTimeMinute(),
			leg.geometry(),
			guidanceEvents,
			leg.routeNo(),
			leg.laneOptions(),
			leg.boardingStop(),
			leg.arrivingStop(),
			leg.isLowFloor(),
			leg.badges());
	}

	private RouteLegResponse withSequence(RouteLegResponse leg, int sequence) {
		return withSequenceAndRole(leg, sequence, leg.role(), leg.instruction());
	}

	private RouteLegResponse withSequenceAndRole(
		RouteLegResponse leg,
		int sequence,
		RouteLegRole role,
		String instruction) {
		return new RouteLegResponse(
			sequence,
			leg.type(),
			role,
			instruction,
			leg.distanceMeter(),
			leg.durationSecond(),
			leg.estimatedTimeMinute(),
			leg.geometry(),
			leg.guidanceEvents(),
			leg.routeNo(),
			leg.laneOptions(),
			leg.boardingStop(),
			leg.arrivingStop(),
			leg.isLowFloor(),
			leg.badges());
	}

	private BigDecimal totalDistance(List<RouteLegResponse> legs) {
		return legs.stream()
			.filter(leg -> leg.distanceMeter() != null)
			.map(RouteLegResponse::distanceMeter)
			.reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add)
			.setScale(2, RoundingMode.HALF_UP);
	}

	private int totalDuration(List<RouteLegResponse> legs) {
		return legs.stream().mapToInt(RouteLegResponse::durationSecond).sum();
	}

	private List<RouteBadge> mergeBadges(
		List<RouteBadge> repairBadges,
		List<RouteBadge> previousBadges,
		List<RouteLegResponse> legs) {
		LinkedHashSet<RouteBadge> badges = new LinkedHashSet<>();
		if (previousBadges != null) {
			badges.addAll(previousBadges);
		}
		if (repairBadges != null) {
			badges.addAll(repairBadges);
		}
		for (RouteLegResponse leg : legs) {
			if (leg.badges() != null) {
				badges.addAll(leg.badges());
			}
		}
		return List.copyOf(badges);
	}

	private String mergeGeometry(List<RouteLegResponse> legs) {
		List<Coordinate> coordinates = new ArrayList<>();
		for (RouteLegResponse leg : legs) {
			Coordinate[] legCoordinates = parseGeometry(leg.geometry()).getCoordinates();
			for (Coordinate coordinate : legCoordinates) {
				if (coordinates.isEmpty() || !sameCoordinate(coordinates.get(coordinates.size() - 1), coordinate)) {
					coordinates.add(coordinate);
				}
			}
		}
		if (coordinates.size() < 2) {
			return null;
		}
		return toLineString(coordinates);
	}

	private double geometryDistanceMeter(Coordinate[] coordinates) {
		double distanceMeter = 0.0;
		for (int index = 0; index < coordinates.length - 1; index++) {
			distanceMeter += distanceMeter(
				coordinates[index].y,
				coordinates[index].x,
				coordinates[index + 1].y,
				coordinates[index + 1].x);
		}
		return distanceMeter;
	}

	private boolean sameCoordinate(Coordinate first, Coordinate second) {
		return Double.compare(first.x, second.x) == 0 && Double.compare(first.y, second.y) == 0;
	}

	private String toLineString(List<Coordinate> coordinates) {
		return "LINESTRING(" + coordinates.stream()
			.map(coordinate -> coordinateText(coordinate.x) + " " + coordinateText(coordinate.y))
			.reduce((left, right) -> left + ", " + right)
			.orElse("") + ")";
	}

	private String coordinateText(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}

	private BigDecimal scale(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
	}

	private int estimatedMinute(int durationSecond) {
		if (durationSecond <= 0) {
			return 0;
		}
		return Math.max(1, durationSecond / 60);
	}

	private double toProjectedX(double lng, double referenceLat) {
		return Math.toRadians(lng) * Math.cos(referenceLat) * EARTH_RADIUS_METER;
	}

	private double toProjectedY(double lat) {
		return Math.toRadians(lat) * EARTH_RADIUS_METER;
	}

	private double distanceMeter(double fromLat, double fromLng, double toLat, double toLng) {
		double startLat = Math.toRadians(fromLat);
		double endLat = Math.toRadians(toLat);
		double deltaLat = Math.toRadians(toLat - fromLat);
		double deltaLng = Math.toRadians(toLng - fromLng);
		double haversine = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
			+ Math.cos(startLat) * Math.cos(endLat) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
		double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
		return EARTH_RADIUS_METER * angularDistance;
	}

	private record LegGeometry(
		int sequence,
		String geometry) {
	}

	private record RouteProjection(
		double distanceMeter,
		Integer legSequence,
		Coordinate projectedCoordinate,
		int segmentIndex,
		double distanceFromLegStartMeter) {
	}

	private record SegmentProjection(
		double distanceMeter,
		Coordinate coordinate) {
	}

	private void validateRequest(RerouteRequest request) {
		if (request == null || request.routeId() == null || request.routeId().isBlank()
			|| request.currentPoint() == null) {
			throw new RouteException(RouteErrorCode.INVALID_REROUTE_REQUEST);
		}
	}

	private void validateCurrentPoint(GeoPointRequest point) {
		if (point.lat() == null || point.lng() == null
			|| point.lat() < -90.0 || point.lat() > 90.0
			|| point.lng() < -180.0 || point.lng() > 180.0) {
			throw new RouteException(RouteErrorCode.INVALID_CURRENT_POINT);
		}
		if (!isInBusan(point)) {
			throw new RouteException(RouteErrorCode.OUT_OF_SERVICE_AREA);
		}
	}

	private boolean isInBusan(GeoPointRequest point) {
		return point.lat() >= BUSAN_MIN_LAT
			&& point.lat() <= BUSAN_MAX_LAT
			&& point.lng() >= BUSAN_MIN_LNG
			&& point.lng() <= BUSAN_MAX_LNG;
	}
}
