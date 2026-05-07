package com.ssafy.e102.domain.route.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.service.WalkRouteSearchService;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

class RouteControllerTest {

	private WalkRouteSearchService walkRouteSearchService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		walkRouteSearchService = Mockito.mock(WalkRouteSearchService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new RouteController(walkRouteSearchService))
			.setCustomArgumentResolvers(new AuthPrincipalArgumentResolver())
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
