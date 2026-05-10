package com.ssafy.e102.domain.route.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.route.dto.request.RouteRatingRequest;
import com.ssafy.e102.domain.route.dto.response.RouteRatingResponse;
import com.ssafy.e102.domain.route.service.RouteRatingService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/route-ratings")
@RequiredArgsConstructor
public class RouteRatingController {

	private final RouteRatingService routeRatingService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RouteRatingResponse> rateRoute(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		RouteRatingRequest request) {
		return ApiResponse.created(routeRatingService.rate(principal.userId(), request));
	}
}
