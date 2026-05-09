package com.ssafy.e102.domain.report.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.report.dto.request.CreateHazardReportRequest;
import com.ssafy.e102.domain.report.dto.response.HazardReportDetailResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportIdResponse;
import com.ssafy.e102.domain.report.dto.response.HazardReportListResponse;
import com.ssafy.e102.domain.report.service.HazardReportService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "도로 상태 제보", description = "사용자 도로 상태 제보 등록 및 내 제보 조회 API")
@Validated
@RestController
@RequestMapping("/hazard-reports")
@RequiredArgsConstructor
public class HazardReportController {

	private final HazardReportService hazardReportService;

	@Operation(summary = "도로 상태 제보 등록", description = "로그인 사용자가 도로 상태 제보를 등록합니다.")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<HazardReportIdResponse> createHazardReport(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		CreateHazardReportRequest request) {
		return ApiResponse.created(hazardReportService.createHazardReport(principal.userId(), request));
	}

	@Operation(summary = "내 도로 상태 제보 목록 조회", description = "로그인 사용자가 등록한 도로 상태 제보 목록을 커서 기반으로 조회합니다.")
	@GetMapping("/me")
	public ApiResponse<HazardReportListResponse> getMyHazardReports(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "다음 페이지 조회 기준이 되는 마지막 제보 ID") @RequestParam(required = false) @Positive
		Long cursor,
		@Parameter(description = "조회 개수") @RequestParam(defaultValue = "10") @Min(1) @Max(100)
		int size) {
		return ApiResponse.success(hazardReportService.getMyHazardReports(principal.userId(), cursor, size));
	}

	@Operation(summary = "내 도로 상태 제보 상세 조회", description = "로그인 사용자가 등록한 특정 도로 상태 제보 상세 정보를 조회합니다.")
	@GetMapping("/me/{reportId}")
	public ApiResponse<HazardReportDetailResponse> getMyHazardReportDetail(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "도로 상태 제보 ID") @PathVariable @Positive
		Long reportId) {
		return ApiResponse.success(hazardReportService.getMyHazardReportDetail(principal.userId(), reportId));
	}
}
