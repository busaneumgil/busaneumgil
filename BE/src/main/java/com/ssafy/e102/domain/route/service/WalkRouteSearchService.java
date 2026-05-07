package com.ssafy.e102.domain.route.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.user.entity.User;
import com.ssafy.e102.domain.user.exception.UserErrorCode;
import com.ssafy.e102.domain.user.exception.UserException;
import com.ssafy.e102.domain.user.repository.UserRepository;
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

	private final UserRepository userRepository;
	private final WalkRouteGraphHopperSearchService graphHopperSearchService;
	private final WalkRoutePayloadService walkRoutePayloadService;

	public WalkRouteSearchService(
		UserRepository userRepository,
		WalkRouteGraphHopperSearchService graphHopperSearchService,
		WalkRoutePayloadService walkRoutePayloadService) {
		this.userRepository = userRepository;
		this.graphHopperSearchService = graphHopperSearchService;
		this.walkRoutePayloadService = walkRoutePayloadService;
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
			.map(candidate -> walkRoutePayloadService.toRouteSummary(searchId, candidate))
			.toList();
	}
}
