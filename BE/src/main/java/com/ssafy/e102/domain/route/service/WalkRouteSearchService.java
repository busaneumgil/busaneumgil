package com.ssafy.e102.domain.route.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.exception.UserErrorCode;
import com.ssafy.e102.domain.user.exception.UserException;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.global.external.graphhopper.GraphHopperCoordinate;
import com.ssafy.e102.global.external.graphhopper.GraphHopperRoutePath;
import com.ssafy.e102.global.geo.dto.GeoPointRequest;

/**
 * `POST /routes/search/walk`의 상위 흐름을 담당한다.
 *
 * <p>Controller에서 인증 사용자 ID와 start/end 좌표를 받으면 user table에서 접근성 profile 기준을 조회하고,
 * 부산 서비스 영역/20m 근접 검증을 먼저 수행한 뒤 {@link WalkRouteGraphHopperSearchService}에 후보 조회를 맡긴다.
 */
@Service
@Transactional(readOnly = true)
public class WalkRouteSearchService {

	private static final double BUSAN_MIN_LAT = 34.85;
	private static final double BUSAN_MAX_LAT = 35.45;
	private static final double BUSAN_MIN_LNG = 128.70;
	private static final double BUSAN_MAX_LNG = 129.40;
	private static final double START_END_MIN_DISTANCE_METER = 20.0;
	private static final double EARTH_RADIUS_METER = 6_371_000.0;
	private static final String WALK_LEG_INSTRUCTION = "목적지까지 도보로 이동하세요.";

	private final UserRepository userRepository;
	private final WalkRouteGraphHopperSearchService graphHopperSearchService;

	public WalkRouteSearchService(
		UserRepository userRepository,
		WalkRouteGraphHopperSearchService graphHopperSearchService) {
		this.userRepository = userRepository;
		this.graphHopperSearchService = graphHopperSearchService;
	}

	public WalkRouteSearchResponse search(UUID userId, WalkRouteSearchRequest request) {
		User user = getUser(userId);
		GeoPointRequest startPoint = request.startPoint();
		GeoPointRequest endPoint = request.endPoint();
		validateServiceArea(startPoint, endPoint);
		validateStartEndDistance(startPoint, endPoint);

		List<WalkRouteCandidate> candidates = graphHopperSearchService.searchCandidates(
			startPoint,
			endPoint,
			user.getSelectedPrimaryUserType(),
			user.getSelectedMobilitySubtype());
		String searchId = "rs_walk_" + UUID.randomUUID();
		return new WalkRouteSearchResponse(searchId, toRouteSummaries(searchId, candidates));
	}

	private User getUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
	}

	private void validateServiceArea(GeoPointRequest startPoint, GeoPointRequest endPoint) {
		if (!isInBusan(startPoint) || !isInBusan(endPoint)) {
			throw new RouteException(RouteErrorCode.OUT_OF_SERVICE_AREA);
		}
	}

	private boolean isInBusan(GeoPointRequest point) {
		return point.lat() >= BUSAN_MIN_LAT
			&& point.lat() <= BUSAN_MAX_LAT
			&& point.lng() >= BUSAN_MIN_LNG
			&& point.lng() <= BUSAN_MAX_LNG;
	}

	private void validateStartEndDistance(GeoPointRequest startPoint, GeoPointRequest endPoint) {
		if (distanceMeter(startPoint, endPoint) <= START_END_MIN_DISTANCE_METER) {
			throw new RouteException(RouteErrorCode.START_END_TOO_CLOSE);
		}
	}

	private double distanceMeter(GeoPointRequest startPoint, GeoPointRequest endPoint) {
		double startLat = Math.toRadians(startPoint.lat());
		double endLat = Math.toRadians(endPoint.lat());
		double deltaLat = Math.toRadians(endPoint.lat() - startPoint.lat());
		double deltaLng = Math.toRadians(endPoint.lng() - startPoint.lng());
		double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
			+ Math.cos(startLat) * Math.cos(endLat) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METER * c;
	}

	private List<RouteSummaryResponse> toRouteSummaries(String searchId, List<WalkRouteCandidate> candidates) {
		return candidates.stream()
			.map(candidate -> toRouteSummary(searchId, candidate))
			.toList();
	}

	private RouteSummaryResponse toRouteSummary(String searchId, WalkRouteCandidate candidate) {
		GraphHopperRoutePath path = candidate.path();
		String geometry = toLineString(path.coordinates());
		int durationSecond = durationSecond(path.timeMs());
		int estimatedTimeMinute = estimatedTimeMinute(durationSecond);
		BigDecimal distanceMeter = path.distanceMeter().setScale(2, RoundingMode.HALF_UP);

		return new RouteSummaryResponse(
			routeId(searchId, candidate.routeOption()),
			TransportMode.WALK,
			candidate.routeOption(),
			title(candidate.routeOption()),
			distanceMeter,
			durationSecond,
			estimatedTimeMinute,
			List.of(),
			geometry,
			List.of(toWalkOnlyLeg(distanceMeter, durationSecond, estimatedTimeMinute, geometry)));
	}

	private RouteLegResponse toWalkOnlyLeg(
		BigDecimal distanceMeter,
		int durationSecond,
		int estimatedTimeMinute,
		String geometry) {
		return new RouteLegResponse(
			1,
			TransportMode.WALK,
			RouteLegRole.WALK_ONLY,
			WALK_LEG_INSTRUCTION,
			distanceMeter,
			durationSecond,
			estimatedTimeMinute,
			geometry,
			List.of());
	}

	private String routeId(String searchId, RouteOption routeOption) {
		return searchId + "_" + routeOption.name().toLowerCase();
	}

	private String title(RouteOption routeOption) {
		return switch (routeOption) {
			case SAFE -> "안전 경로";
			case SHORTEST -> "최단 경로";
		};
	}

	private int durationSecond(long timeMs) {
		return Math.max(1, (int)Math.ceil(timeMs / 1000.0));
	}

	private int estimatedTimeMinute(int durationSecond) {
		return Math.max(1, durationSecond / 60);
	}

	private String toLineString(List<GraphHopperCoordinate> coordinates) {
		String points = coordinates.stream()
			.map(coordinate -> coordinate.lng().toPlainString() + " " + coordinate.lat().toPlainString())
			.collect(Collectors.joining(", "));
		return "LINESTRING(" + points + ")";
	}
}
