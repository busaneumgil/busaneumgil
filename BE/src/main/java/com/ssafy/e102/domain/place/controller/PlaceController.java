package com.ssafy.e102.domain.place.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.place.dto.response.PlaceDetailResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceListResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceReverseGeocodeResponse;
import com.ssafy.e102.domain.place.dto.response.PlaceSearchResponse;
import com.ssafy.e102.domain.place.service.PlaceService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "장소", description = "장소 검색, 목록 조회, 상세 조회 API")
@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

	private final PlaceService placeService;

	@Operation(summary = "텍스트 장소 검색", description = "키워드와 위치 조건으로 외부 장소 검색 결과를 커서 기반으로 조회합니다.")
	@GetMapping("/search")
	public ApiResponse<PlaceSearchResponse> searchPlaces(
		@Parameter(description = "검색 키워드") @RequestParam(required = false)
		String keyword,
		@Parameter(description = "중심 위도") @RequestParam(required = false)
		String lat,
		@Parameter(description = "중심 경도") @RequestParam(required = false)
		String lng,
		@Parameter(description = "검색 반경") @RequestParam(required = false)
		String radius,
		@Parameter(description = "다음 페이지 조회 커서") @RequestParam(required = false)
		String cursor,
		@Parameter(description = "조회 개수") @RequestParam(required = false)
		String size) {
		return ApiResponse.success(placeService.searchPlaces(keyword, lat, lng, radius, cursor, size));
	}

	@Operation(summary = "좌표 주소 변환", description = "위도와 경도를 기준으로 주소 정보를 조회합니다.")
	@GetMapping("/reverse-geocode")
	public ApiResponse<PlaceReverseGeocodeResponse> reverseGeocode(
		@Parameter(description = "위도") @RequestParam(required = false)
		String lat,
		@Parameter(description = "경도") @RequestParam(required = false)
		String lng) {
		return ApiResponse.success(placeService.reverseGeocode(lat, lng));
	}

	@Operation(summary = "장소 목록 조회", description = "위치, 카테고리, 접근성 속성 조건으로 내부 장소 목록을 조회합니다.")
	@GetMapping
	public ApiResponse<PlaceListResponse> getPlaces(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "중심 위도") @RequestParam(required = false)
		String lat,
		@Parameter(description = "중심 경도") @RequestParam(required = false)
		String lng,
		@Parameter(description = "검색 반경") @RequestParam(required = false)
		String radius,
		@Parameter(description = "장소 카테고리") @RequestParam(required = false)
		String category,
		@Parameter(description = "접근성 속성 유형") @RequestParam(required = false)
		String featureType) {
		return ApiResponse.success(placeService.getPlaces(principal.userId(), lat, lng, radius, category, featureType));
	}

	@Operation(summary = "시설 상세 조회", description = "내부 장소 ID로 장소 상세 정보와 접근성 정보를 조회합니다.")
	@GetMapping("/{placeId}")
	public ApiResponse<PlaceDetailResponse> getPlace(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "장소 ID") @PathVariable
		String placeId) {
		return ApiResponse.success(placeService.getPlace(principal.userId(), placeId));
	}
}
