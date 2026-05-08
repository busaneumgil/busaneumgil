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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/hazard-reports")
@RequiredArgsConstructor
public class HazardReportController {

	private final HazardReportService hazardReportService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<HazardReportIdResponse> createHazardReport(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		CreateHazardReportRequest request) {
		return ApiResponse.created(hazardReportService.createHazardReport(principal.userId(), request));
	}

	@GetMapping("/me")
	public ApiResponse<HazardReportListResponse> getMyHazardReports(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@RequestParam(required = false) @Positive
		Long cursor,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100)
		int size) {
		return ApiResponse.success(hazardReportService.getMyHazardReports(principal.userId(), cursor, size));
	}

	@GetMapping("/me/{reportId}")
	public ApiResponse<HazardReportDetailResponse> getMyHazardReportDetail(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable @Positive
		Long reportId) {
		return ApiResponse.success(hazardReportService.getMyHazardReportDetail(principal.userId(), reportId));
	}
}
