package com.ssafy.e102.domain.route.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.route.dto.request.WalkRouteSearchRequest;
import com.ssafy.e102.domain.route.dto.request.RerouteRequest;
import com.ssafy.e102.domain.route.dto.request.SelectRouteRequest;
import com.ssafy.e102.domain.route.dto.response.RerouteResponse;
import com.ssafy.e102.domain.route.dto.response.RouteSessionResponse;
import com.ssafy.e102.domain.route.dto.response.WalkRouteSearchResponse;
import com.ssafy.e102.domain.route.service.RerouteService;
import com.ssafy.e102.domain.route.service.RouteSessionCommandService;
import com.ssafy.e102.domain.route.service.RouteSelectService;
import com.ssafy.e102.domain.route.service.TransitRouteSearchService;
import com.ssafy.e102.domain.route.service.WalkRouteSearchService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 경로 API의 `/routes/**` 진입점이다.
 *
 * <p>Controller는 인증 principal과 API 요청 DTO만 받고, 사용자 profile 조회, 좌표 검증,
 * GraphHopper 후보 조회, 응답 조립은 {@link WalkRouteSearchService}로 넘긴다.
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController {

	private final WalkRouteSearchService walkRouteSearchService;
	private final TransitRouteSearchService transitRouteSearchService;
	private final RerouteService rerouteService;
	private final RouteSelectService routeSelectService;
	private final RouteSessionCommandService routeSessionCommandService;

	@PostMapping("/search/walk")
	public ApiResponse<WalkRouteSearchResponse> searchWalkRoutes(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		WalkRouteSearchRequest request) {
		return ApiResponse.success(walkRouteSearchService.search(principal.userId(), request));
	}

	@PostMapping("/search/transit")
	public ApiResponse<WalkRouteSearchResponse> searchTransitRoutes(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		WalkRouteSearchRequest request) {
		return ApiResponse.success(transitRouteSearchService.search(principal.userId(), request));
	}

	@PostMapping("/reroute")
	public ApiResponse<RerouteResponse> reroute(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		RerouteRequest request) {
		return ApiResponse.success(rerouteService.reroute(principal.userId(), request));
	}

	@PostMapping("/{routeId}/select")
	public ApiResponse<RouteSessionResponse> selectRoute(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable
		String routeId,
		@Valid @RequestBody
		SelectRouteRequest request) {
		return new ApiResponse<>(
			"S2000",
			routeSelectService.select(principal.userId(), routeId, request),
			"경로가 선택되었습니다.");
	}

	@PostMapping("/{routeId}/end")
	public ApiResponse<RouteSessionResponse> endRoute(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable
		String routeId) {
		return new ApiResponse<>(
			"S2000",
			routeSessionCommandService.endSession(principal.userId(), routeId),
			"안내가 종료되었습니다.");
	}
}
