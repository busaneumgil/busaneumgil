package com.ssafy.e102.domain.route.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;

/**
 * 검색 직후 선택 전 상태의 route 후보 묶음을 Redis에 임시 저장한다.
 *
 * <p>검색 API는 DB session을 만들지 않고 이 cache만 생성한다. 이후 select API는 같은 {@code searchId}와
 * {@code routeId}를 이 cache에서 확인하고, TTL이 지나 cache가 없으면 검색 만료로 처리한다.
 */
@Service
public class RouteSearchCacheService {

	private static final String ROUTE_SEARCH_KEY_PREFIX = "routeSearch:";
	private static final String ROUTE_SEARCH_METADATA_KEY_PREFIX = "routeSearchMeta:";
	private static final Duration ROUTE_SEARCH_TTL = Duration.ofMinutes(10);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public RouteSearchCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	public void save(WalkRouteSearchResponse response) {
		redisTemplate.opsForValue().set(
			key(response.searchId()),
			serialize(response),
			ROUTE_SEARCH_TTL.toSeconds(),
			TimeUnit.SECONDS);
	}

	public void saveTransitMetadata(String searchId, List<TransitRouteSnapshot> snapshots) {
		redisTemplate.opsForValue().set(
			metadataKey(searchId),
			serialize(snapshots),
			ROUTE_SEARCH_TTL.toSeconds(),
			TimeUnit.SECONDS);
	}

	public Optional<WalkRouteSearchResponse> findSearch(String searchId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(searchId)))
			.map(this::deserialize);
	}

	public Optional<RouteSummaryResponse> findRoute(String searchId, String routeId) {
		return findSearch(searchId)
			.flatMap(response -> response.routes()
				.stream()
				.filter(route -> route.routeId().equals(routeId))
				.findFirst());
	}

	public RouteSummaryResponse getRouteOrThrow(String searchId, String routeId) {
		WalkRouteSearchResponse response = findSearch(searchId)
			.orElseThrow(() -> new RouteException(RouteErrorCode.ROUTE_SEARCH_EXPIRED));
		return response.routes()
			.stream()
			.filter(route -> route.routeId().equals(routeId))
			.findFirst()
			.orElseThrow(() -> new RouteException(RouteErrorCode.ROUTE_CANDIDATE_NOT_FOUND));
	}

	private String key(String searchId) {
		return ROUTE_SEARCH_KEY_PREFIX + searchId;
	}

	private String metadataKey(String searchId) {
		return ROUTE_SEARCH_METADATA_KEY_PREFIX + searchId;
	}

	private String serialize(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("경로 검색 후보를 직렬화할 수 없습니다.", exception);
		}
	}

	private WalkRouteSearchResponse deserialize(String value) {
		try {
			return objectMapper.readValue(value, WalkRouteSearchResponse.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("경로 검색 후보를 역직렬화할 수 없습니다.", exception);
		}
	}
}
