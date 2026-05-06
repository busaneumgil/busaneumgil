package com.ssafy.e102.domain.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.user.dto.request.UpdateUserTypeRequest;
import com.ssafy.e102.domain.user.dto.response.UserMeResponse;
import com.ssafy.e102.domain.user.dto.response.UserTypeResponse;
import com.ssafy.e102.domain.user.service.UserService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ApiResponse<UserMeResponse> getMe(
		@AuthenticationPrincipal
		AuthPrincipal principal) {
		return ApiResponse.success(userService.getMe(principal.userId()));
	}

	@PatchMapping("/me/user-type")
	public ApiResponse<UserTypeResponse> updateUserType(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		UpdateUserTypeRequest request) {
		return ApiResponse.success(userService.updateUserType(
			principal.userId(),
			request.selectedPrimaryUserType(),
			request.selectedMobilitySubtype()));
	}

	@DeleteMapping("/me")
	public ApiResponse<Void> withdraw(
		@AuthenticationPrincipal
		AuthPrincipal principal) {
		userService.withdraw(principal.userId(), principal.accessToken());
		return ApiResponse.successMessage("회원탈퇴가 완료되었습니다.");
	}
}
