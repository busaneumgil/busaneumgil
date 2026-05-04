package com.ssafy.e102.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.auth.dto.request.ReissueRequest;
import com.ssafy.e102.domain.auth.dto.request.SignupRequest;
import com.ssafy.e102.domain.auth.dto.request.SocialLoginRequest;
import com.ssafy.e102.domain.auth.dto.response.SignupResponse;
import com.ssafy.e102.domain.auth.dto.response.SocialLoginResponse;
import com.ssafy.e102.domain.auth.dto.response.TokenResponse;
import com.ssafy.e102.domain.auth.service.AuthService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/social-login")
	public ApiResponse<SocialLoginResponse> socialLogin(
		@Valid @RequestBody
		SocialLoginRequest request) {
		return ApiResponse.success(authService.socialLogin(request));
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SignupResponse> signup(
		@Valid @RequestBody
		SignupRequest request) {
		return ApiResponse.created(authService.signup(request));
	}

	@PostMapping("/reissue")
	public ApiResponse<TokenResponse> reissue(
		@Valid @RequestBody
		ReissueRequest request) {
		return ApiResponse.success(authService.reissue(request));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
		@AuthenticationPrincipal
		AuthPrincipal principal) {
		authService.logout(principal.userId(), principal.accessToken());
		return ApiResponse.successMessage("로그아웃되었습니다.");
	}
}
