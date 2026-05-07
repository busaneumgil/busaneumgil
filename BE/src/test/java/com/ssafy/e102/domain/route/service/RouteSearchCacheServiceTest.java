package com.ssafy.e102.domain.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;

class RouteSearchCacheServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private RouteSearchCacheService cacheService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		cacheService = new RouteSearchCacheService(redisTemplate, new ObjectMapper());
	}

	@Test
	void savesSearchResponseWithTenMinuteTtl() {
		WalkRouteSearchResponse response = response();

		cacheService.save(response);

		verify(valueOperations).set(
			org.mockito.ArgumentMatchers.eq("routeSearch:rs_walk_test"),
			org.mockito.ArgumentMatchers.contains("\"searchId\":\"rs_walk_test\""),
			org.mockito.ArgumentMatchers.eq(600L),
			org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
	}

	@Test
	void findsRouteFromCachedSearchResponse() {
		when(valueOperations.get("routeSearch:rs_walk_test")).thenReturn("""
			{"searchId":"rs_walk_test","routes":[{"routeId":"rs_walk_test_safe","transportMode":"WALK","routeOption":"SAFE","title":"안전 경로","distanceMeter":100.00,"durationSecond":60,"estimatedTimeMinute":1,"badges":[],"geometry":"LINESTRING(0 0, 1 1)","legs":[]}]}
			""");

		Optional<RouteSummaryResponse> route = cacheService.findRoute("rs_walk_test", "rs_walk_test_safe");

		assertThat(route).isPresent();
		assertThat(route.get().routeId()).isEqualTo("rs_walk_test_safe");
	}

	@Test
	void getRouteThrowsSearchExpiredWhenSearchKeyIsMissing() {
		when(valueOperations.get("routeSearch:expired")).thenReturn(null);

		assertThatThrownBy(() -> cacheService.getRouteOrThrow("expired", "route-id"))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.ROUTE_SEARCH_EXPIRED);
	}

	@Test
	void getRouteThrowsCandidateNotFoundWhenRouteIdIsMissing() {
		when(valueOperations.get("routeSearch:rs_walk_test")).thenReturn("""
			{"searchId":"rs_walk_test","routes":[]}
			""");

		assertThatThrownBy(() -> cacheService.getRouteOrThrow("rs_walk_test", "missing-route"))
			.isInstanceOf(RouteException.class)
			.extracting(exception -> ((RouteException)exception).getErrorCode())
			.isEqualTo(RouteErrorCode.ROUTE_CANDIDATE_NOT_FOUND);
	}

	private WalkRouteSearchResponse response() {
		return new WalkRouteSearchResponse(
			"rs_walk_test",
			List.of(new RouteSummaryResponse(
				"rs_walk_test_safe",
				TransportMode.WALK,
				RouteOption.SAFE,
				"안전 경로",
				BigDecimal.valueOf(100),
				60,
				1,
				List.of(),
				"LINESTRING(0 0, 1 1)",
				List.of())));
	}
}
