package com.ssafy.e102.domain.place.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.place.dto.response.PlaceDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceListResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchResponse;
import com.ssafy.e102.domain.place.service.PlaceService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

	private final PlaceService placeService;

	@GetMapping("/search")
	public ApiResponse<PlaceSearchResponse> searchPlaces(
		@RequestParam(required = false)
		String keyword,
		@RequestParam(required = false)
		String lat,
		@RequestParam(required = false)
		String lng,
		@RequestParam(required = false)
		String radius,
		@RequestParam(required = false)
		String cursor,
		@RequestParam(required = false)
		String size) {
		return ApiResponse.success(placeService.searchPlaces(keyword, lat, lng, radius, cursor, size));
	}

	@GetMapping
	public ApiResponse<PlaceListResponse> getPlaces(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@RequestParam(required = false)
		String lat,
		@RequestParam(required = false)
		String lng,
		@RequestParam(required = false)
		String radius,
		@RequestParam(required = false)
		String category,
		@RequestParam(required = false)
		String featureType) {
		return ApiResponse.success(placeService.getPlaces(principal.userId(), lat, lng, radius, category, featureType));
	}

	@GetMapping("/{placeId}")
	public ApiResponse<PlaceDetailResponse> getPlace(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable
		String placeId) {
		return ApiResponse.success(placeService.getPlace(principal.userId(), placeId));
	}
}
