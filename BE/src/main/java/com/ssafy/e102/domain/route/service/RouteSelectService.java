package com.ssafy.e102.domain.route.service;

import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.e102.domain.route.dto.request.SelectRouteRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;

/**
 * 사용자가 검색 후보 중 하나를 실제 안내 route session으로 확정한다.
 */
@Service
public class RouteSelectService {

	private static final int SRID = 4326;

	private final RouteSearchCacheService routeSearchCacheService;
	private final RouteSessionCommandService routeSessionCommandService;
	private final ObjectMapper objectMapper;
	private final WKTReader wktReader = new WKTReader();
	private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);

	public RouteSelectService(
		RouteSearchCacheService routeSearchCacheService,
		RouteSessionCommandService routeSessionCommandService,
		ObjectMapper objectMapper) {
		this.routeSearchCacheService = routeSearchCacheService;
		this.routeSessionCommandService = routeSessionCommandService;
		this.objectMapper = objectMapper;
	}

	public void select(UUID userId, String routeId, SelectRouteRequest request) {
		RouteSummaryResponse route = routeSearchCacheService.getOwnedRouteOrThrow(userId, request.searchId(), routeId);
		Coordinate[] coordinates = routeCoordinates(route);
		try {
			routeSessionCommandService.saveActiveSessionIfAbsent(
				userId,
				route.routeId(),
				toPoint(coordinates[0]),
				toPoint(coordinates[coordinates.length - 1]),
				snapshot(request.searchId(), route));
		} catch (DataIntegrityViolationException exception) {
			if (routeSessionCommandService.hasActiveSession(userId, route.routeId())) {
				return;
			}
			throw exception;
		}
	}

	private JsonNode snapshot(String searchId, RouteSummaryResponse route) {
		ObjectNode snapshot = objectMapper.valueToTree(route);
		removeRemainingMinute(snapshot);
		routeSearchCacheService.findTransitMetadata(searchId, route.routeId())
			.ifPresent(metadata -> snapshot.set("backendMetadata", objectMapper.valueToTree(metadata)));
		return snapshot;
	}

	private void removeRemainingMinute(JsonNode node) {
		if (node == null) {
			return;
		}
		if (node.isObject()) {
			((ObjectNode)node).remove("remainingMinute");
			node.elements().forEachRemaining(this::removeRemainingMinute);
			return;
		}
		if (node.isArray()) {
			node.elements().forEachRemaining(this::removeRemainingMinute);
		}
	}

	private Coordinate[] routeCoordinates(RouteSummaryResponse route) {
		Coordinate[] coordinates = coordinates(route.geometry());
		if (coordinates.length >= 2) {
			return coordinates;
		}
		if (route.legs() == null || route.legs().isEmpty()) {
			throw new RouteException(RouteErrorCode.ROUTE_SELECT_CONFLICT);
		}
		Coordinate first = null;
		Coordinate last = null;
		for (RouteLegResponse leg : route.legs()) {
			Coordinate[] legCoordinates = coordinates(leg.geometry());
			if (legCoordinates.length == 0) {
				continue;
			}
			if (first == null) {
				first = legCoordinates[0];
			}
			last = legCoordinates[legCoordinates.length - 1];
		}
		if (first == null || last == null) {
			throw new RouteException(RouteErrorCode.ROUTE_SELECT_CONFLICT);
		}
		return new Coordinate[] {first, last};
	}

	private Coordinate[] coordinates(String geometryText) {
		if (geometryText == null || geometryText.isBlank()) {
			return new Coordinate[0];
		}
		try {
			Geometry geometry = wktReader.read(geometryText);
			return geometry.getCoordinates();
		} catch (ParseException exception) {
			throw new RouteException(RouteErrorCode.ROUTE_SELECT_CONFLICT, "선택 경로 geometry를 해석할 수 없습니다.", exception);
		}
	}

	private Point toPoint(Coordinate coordinate) {
		Point point = geometryFactory.createPoint(coordinate);
		point.setSRID(SRID);
		return point;
	}
}
