package com.ssafy.e102.domain.route.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.route.dto.request.RerouteRequest;
import com.ssafy.e102.domain.route.dto.response.RerouteResponse;
import com.ssafy.e102.domain.route.dto.response.RerouteType;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

@Service
@Transactional(readOnly = true)
public class RerouteService {

	private static final double BUSAN_MIN_LAT = 34.85;
	private static final double BUSAN_MAX_LAT = 35.45;
	private static final double BUSAN_MIN_LNG = 128.70;
	private static final double BUSAN_MAX_LNG = 129.40;

	public RerouteResponse reroute(UUID userId, RerouteRequest request) {
		validateRequest(request);
		validateCurrentPoint(request.currentPoint());
		return new RerouteResponse(RerouteType.NO_REROUTE_NEEDED, null);
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
