package com.ssafy.e102.domain.admin.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.admin.dto.response.AdminAreaListResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminFacilityPayloadResponse;
import com.ssafy.e102.domain.admin.dto.response.AdminRoadNetworkResponse;
import com.ssafy.e102.domain.admin.service.AdminMapService;
import com.ssafy.e102.global.response.ApiResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자 지도", description = "관리자 보행 네트워크 및 편의시설 DB 조회 API")
@Validated
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminMapController {

	private final AdminMapService adminMapService;

	@Operation(summary = "관리자 검수 구/동 목록 조회", description = "DB에 적재된 admin_areas의 gu/dong 목록을 조회한다.")
	@GetMapping("/areas")
	public ApiResponse<AdminAreaListResponse> getAreas() {
		return ApiResponse.success(adminMapService.getAreas());
	}

	@Operation(summary = "관리자 보행 네트워크 조회", description = "행정동 경계와 교차하는 road_segments를 GeoJSON 형태로 조회한다.")
	@GetMapping("/road-network/segments")
	public ApiResponse<AdminRoadNetworkResponse> getRoadNetwork(
		@Parameter(description = "구") @RequestParam(required = false)
		String gu,
		@Parameter(description = "동") @RequestParam(required = false)
		String dong,
		@Parameter(description = "조회 개수. 허용 범위는 1~20000이다.") @RequestParam(defaultValue = "10000") @Min(1) @Max(20000)
		int limit) {
		return ApiResponse.success(adminMapService.getRoadNetwork(gu, dong, limit));
	}

	@Operation(summary = "관리자 편의시설 조회", description = "DB에 적재된 places를 GeoJSON 형태로 조회한다.")
	@GetMapping("/places/facilities")
	public ApiResponse<AdminFacilityPayloadResponse> getFacilities(
		@Parameter(description = "조회 개수. 허용 범위는 1~20000이다.") @RequestParam(defaultValue = "20000") @Min(1) @Max(20000)
		int limit) {
		return ApiResponse.success(adminMapService.getFacilities(limit));
	}
}
