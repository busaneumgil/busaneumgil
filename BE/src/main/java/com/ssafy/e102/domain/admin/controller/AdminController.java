package com.ssafy.e102.domain.admin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.admin.dto.response.AdminMeResponse;
import com.ssafy.e102.domain.admin.service.AdminService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "관리자", description = "관리자 principal 및 권한 확인 API")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@Operation(summary = "관리자 principal 조회", description = "현재 로그인한 사용자가 ADMIN role인지 확인하고 관리자 권한 정보를 반환한다.")
	@GetMapping("/me")
	public ApiResponse<AdminMeResponse> getMe(
		@AuthenticationPrincipal
		AuthPrincipal principal) {
		return ApiResponse.success(adminService.getMe(principal.userId()));
	}
}
