package com.ssafy.e102.domain.route.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.RouteLegResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStepAlertResponse;
import com.ssafy.e102.domain.route.dto.response.RouteStepAlertType;
import com.ssafy.e102.domain.route.dto.response.RouteStepResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSummaryResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.exception.RouteErrorCode;
import com.ssafy.e102.domain.route.exception.RouteException;
import com.ssafy.e102.domain.route.exception.RouteExceptionHandler;
import com.ssafy.e102.domain.route.service.WalkRouteSearchService;
import com.ssafy.e102.domain.route.type.RouteBadge;
import com.ssafy.e102.domain.route.type.RouteLegRole;
import com.ssafy.e102.domain.route.type.RouteOption;
import com.ssafy.e102.domain.route.type.TransportMode;
import com.ssafy.e102.global.exception.GlobalExceptionHandler;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

class RouteControllerTest {

	private WalkRouteSearchService walkRouteSearchService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		walkRouteSearchService = Mockito.mock(WalkRouteSearchService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new RouteController(walkRouteSearchService))
			.setCustomArgumentResolvers(new AuthPrincipalArgumentResolver())
			.setControllerAdvice(new RouteExceptionHandler(), new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("도보 search 요청은 인증 사용자와 start/end body만 service로 넘긴다")
	void searchWalkRoutesUsesAuthenticatedUser() throws Exception {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		when(walkRouteSearchService.search(eq(userId), any(WalkRouteSearchRequest.class)))
			.thenReturn(new WalkRouteSearchResponse("rs_walk_test", List.of()));
		UsernamePasswordAuthenticationToken authentication = authentication(userId);

		mockMvc.perform(post("/routes/search/walk")
			.principal(authentication)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "startPoint": {"lat": 35.12, "lng": 128.936},
				  "endPoint": {"lat": 35.1315, "lng": 128.8823}
				}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"))
			.andExpect(jsonPath("$.data.searchId").value("rs_walk_test"));

		verify(walkRouteSearchService).search(eq(userId), any(WalkRouteSearchRequest.class));
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("도보 search 성공 응답은 경로 API 명세의 핵심 필드를 반환한다")
	void searchWalkRoutesReturnsApiContractFields() throws Exception {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		when(walkRouteSearchService.search(eq(userId), any(WalkRouteSearchRequest.class)))
			.thenReturn(walkRouteSearchResponse());
		UsernamePasswordAuthenticationToken authentication = authentication(userId);

		mockMvc.perform(post("/routes/search/walk")
			.principal(authentication)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "startPoint": {"lat": 35.12, "lng": 128.936},
				  "endPoint": {"lat": 35.1315, "lng": 128.8823}
				}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"))
			.andExpect(jsonPath("$.data.searchId").value("rs_walk_test"))
			.andExpect(jsonPath("$.data.routes[0].routeId").value("rs_walk_test_safe"))
			.andExpect(jsonPath("$.data.routes[0].transportMode").value("WALK"))
			.andExpect(jsonPath("$.data.routes[0].routeOption").value("SAFE"))
			.andExpect(jsonPath("$.data.routes[0].estimatedTimeMinute").value(16))
			.andExpect(jsonPath("$.data.routes[0].badges[0]").value("CROSSWALK"))
			.andExpect(jsonPath("$.data.routes[0].legs[0].steps[0].instruction").value("직진하세요."))
			.andExpect(jsonPath("$.data.routes[0].legs[0].steps[0].alert.type").value("CROSSWALK_AUDIO"))
			.andExpect(jsonPath("$.data.routes[0].legs[0].steps[0].badges").doesNotExist())
			.andExpect(jsonPath("$.data.routes[0].legs[0].steps[0].slopePercent").doesNotExist())
			.andExpect(jsonPath("$.data.routes[0].legs[0].steps[0].widthState").doesNotExist());

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("추천 경로 없음은 RT4040 에러 응답으로 매핑한다")
	void searchWalkRoutesMapsRouteNotFoundError() throws Exception {
		assertRouteError(RouteErrorCode.ROUTE_NOT_FOUND, 404, "RT4040", "탐색 가능한 경로가 없습니다.");
	}

	@Test
	@DisplayName("GraphHopper 실패는 EX5020 에러 응답으로 매핑한다")
	void searchWalkRoutesMapsExternalRouteApiFailed() throws Exception {
		assertRouteError(RouteErrorCode.EXTERNAL_ROUTE_API_FAILED, 502, "EX5020", "외부 경로 정보를 불러오지 못했습니다.");
	}

	@Test
	@DisplayName("GraphHopper timeout은 EX5040 에러 응답으로 매핑한다")
	void searchWalkRoutesMapsExternalRouteApiTimeout() throws Exception {
		assertRouteError(RouteErrorCode.EXTERNAL_ROUTE_API_TIMEOUT, 504, "EX5040", "외부 경로 정보 응답이 지연되고 있습니다.");
	}

	@Test
	@DisplayName("도보 search 좌표 누락 validation 실패는 RT4000으로 반환한다")
	void searchWalkRoutesMapsValidationFailureToRouteError() throws Exception {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

		mockMvc.perform(post("/routes/search/walk")
			.principal(authentication(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "endPoint": {"lat": 35.1315, "lng": 128.8823}
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("RT4000"))
			.andExpect(jsonPath("$.message").value("경로 요청값이 올바르지 않습니다."));

		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("도보 search 좌표 형식 오류는 RT4000으로 반환한다")
	void searchWalkRoutesMapsMalformedBodyToRouteError() throws Exception {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

		mockMvc.perform(post("/routes/search/walk")
			.principal(authentication(userId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "startPoint": {"lat": "wrong", "lng": 128.936},
				  "endPoint": {"lat": 35.1315, "lng": 128.8823}
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("RT4000"))
			.andExpect(jsonPath("$.message").value("경로 요청값이 올바르지 않습니다."));

		SecurityContextHolder.clearContext();
	}

	private void assertRouteError(RouteErrorCode errorCode, int httpStatus, String status, String message)
		throws Exception {
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		when(walkRouteSearchService.search(eq(userId), any(WalkRouteSearchRequest.class)))
			.thenThrow(new RouteException(errorCode));
		UsernamePasswordAuthenticationToken authentication = authentication(userId);

		mockMvc.perform(post("/routes/search/walk")
			.principal(authentication)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "startPoint": {"lat": 35.12, "lng": 128.936},
				  "endPoint": {"lat": 35.1315, "lng": 128.8823}
				}
				"""))
			.andExpect(status().is(httpStatus))
			.andExpect(jsonPath("$.status").value(status))
			.andExpect(jsonPath("$.message").value(message))
			.andExpect(jsonPath("$.data").doesNotExist());

		SecurityContextHolder.clearContext();
	}

	private WalkRouteSearchResponse walkRouteSearchResponse() {
		return new WalkRouteSearchResponse(
			"rs_walk_test",
			List.of(new RouteSummaryResponse(
				"rs_walk_test_safe",
				TransportMode.WALK,
				RouteOption.SAFE,
				"안전 경로",
				BigDecimal.valueOf(950),
				960,
				16,
				List.of(RouteBadge.CROSSWALK),
				"LINESTRING(128.9360 35.1200, 128.8823 35.1315)",
				List.of(new RouteLegResponse(
					1,
					TransportMode.WALK,
					RouteLegRole.WALK_ONLY,
					"목적지까지 도보로 이동하세요.",
					BigDecimal.valueOf(950),
					960,
					16,
					"LINESTRING(128.9360 35.1200, 128.8823 35.1315)",
					List.of(new RouteStepResponse(
						1,
						"직진하세요.",
						BigDecimal.valueOf(30),
						35,
						"LINESTRING(128.9360 35.1200, 128.9361 35.1201)",
						new RouteStepAlertResponse(RouteStepAlertType.CROSSWALK_AUDIO, BigDecimal.ZERO))))))));
	}

	private UsernamePasswordAuthenticationToken authentication(UUID userId) {
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new AuthPrincipal(userId, "access-token"), null);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		return authentication;
	}

	private static class AuthPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().equals(AuthPrincipal.class);
		}

		@Override
		public Object resolveArgument(
			MethodParameter parameter,
			ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory) {
			return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		}
	}
}
