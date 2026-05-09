package com.ssafy.e102.domain.route.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.request.RerouteRequest;
import com.ssafy.e102.domain.route.dto.response.RerouteResponse;
import com.ssafy.e102.domain.route.dto.response.RerouteType;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.entity.RouteSession;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.repository.RouteSessionRepository;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@Service
@Transactional(readOnly = true)
public class RerouteService {

	private static final double BUSAN_MIN_LAT = 34.85;
	private static final double BUSAN_MAX_LAT = 35.45;
	private static final double BUSAN_MIN_LNG = 128.70;
	private static final double BUSAN_MAX_LNG = 129.40;
	private static final double EARTH_RADIUS_METER = 6_371_000.0;
	private static final double NO_REROUTE_DISTANCE_METER = 10.0;

	private final RouteSessionRepository routeSessionRepository;
	private final ObjectMapper objectMapper;
	private final WKTReader wktReader = new WKTReader();

	public RerouteService(RouteSessionRepository routeSessionRepository, ObjectMapper objectMapper) {
		this.routeSessionRepository = routeSessionRepository;
		this.objectMapper = objectMapper;
	}

	public RerouteResponse reroute(UUID userId, RerouteRequest request) {
		validateRequest(request);
		validateCurrentPoint(request.currentPoint());
		RouteSession routeSession = getOwnedRouteSession(userId, request.routeId());
		RouteSummaryResponse route = restoreRouteSnapshot(routeSession);
		RouteProjection projection = projectCurrentPoint(route, request.currentPoint());
		if (projection.distanceMeter() <= NO_REROUTE_DISTANCE_METER) {
			return new RerouteResponse(RerouteType.NO_REROUTE_NEEDED, null);
		}
		return new RerouteResponse(RerouteType.NO_REROUTE_NEEDED, null);
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
				routeSession.getRouteSnapshotJson(),
				RouteSummaryResponse.class);
			validateRestoredRoute(route);
			return route;
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.", exception);
		}
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
			return new RouteProjection(distanceToGeometryMeter(currentPoint, route.geometry()), null);
		}
		RouteProjection nearestProjection = null;
		for (LegGeometry legGeometry : legGeometries) {
			double distanceMeter = distanceToGeometryMeter(currentPoint, legGeometry.geometry());
			if (nearestProjection == null || distanceMeter < nearestProjection.distanceMeter()) {
				nearestProjection = new RouteProjection(distanceMeter, legGeometry.sequence());
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

	private double distanceToGeometryMeter(GeoPointRequest point, String wkt) {
		Geometry geometry = parseGeometry(wkt);
		Coordinate[] coordinates = geometry.getCoordinates();
		if (coordinates.length == 0) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.");
		}
		if (coordinates.length == 1) {
			return distanceMeter(point.lat(), point.lng(), coordinates[0].y, coordinates[0].x);
		}
		double minDistanceMeter = Double.MAX_VALUE;
		for (int index = 0; index < coordinates.length - 1; index++) {
			minDistanceMeter = Math.min(
				minDistanceMeter,
				distanceToSegmentMeter(point, coordinates[index], coordinates[index + 1]));
		}
		return minDistanceMeter;
	}

	private Geometry parseGeometry(String wkt) {
		try {
			return wktReader.read(wkt);
		} catch (ParseException | IllegalArgumentException exception) {
			throw new RouteException(RouteErrorCode.ROUTE_SESSION_NOT_FOUND, "선택한 경로 정보를 복구할 수 없습니다.", exception);
		}
	}

	private double distanceToSegmentMeter(GeoPointRequest point, Coordinate start, Coordinate end) {
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
			return distanceMeter(point.lat(), point.lng(), start.y, start.x);
		}
		double t = ((pointX - startX) * dx + (pointY - startY) * dy) / segmentLengthSquared;
		double clampedT = Math.max(0.0, Math.min(1.0, t));
		double projectedX = startX + clampedT * dx;
		double projectedY = startY + clampedT * dy;
		return Math.hypot(pointX - projectedX, pointY - projectedY);
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
		Integer legSequence) {
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
