package com.ssafy.e102.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.auth.dto.request.ReissueRequest;
import com.ssafy.e102.domain.auth.dto.request.SignupRequest;
import com.ssafy.e102.domain.auth.dto.request.SocialLoginRequest;
import com.ssafy.e102.domain.auth.dto.response.SignupResponse;
import com.ssafy.e102.domain.auth.dto.response.SocialLoginResponse;
import com.ssafy.e102.domain.auth.dto.response.TokenResponse;
import com.ssafy.e102.domain.auth.service.AuthService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.AuthPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/social-login")
	public ApiResponse<SocialLoginResponse> socialLogin(
		@Valid @RequestBody
		SocialLoginRequest request) {
		return ApiResponse.success(authService.socialLogin(request));
	}

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
		@Valid @RequestBody
		SignupRequest request) {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(ApiResponse.created(authService.signup(request)));
	}

	@PostMapping("/reissue")
	public ApiResponse<TokenResponse> reissue(
		@Valid @RequestBody
		ReissueRequest request) {
		return ApiResponse.success(authService.reissue(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		Authentication authentication,
		@RequestHeader(HttpHeaders.AUTHORIZATION)
		String authorizationHeader) {
		AuthPrincipal principal = (AuthPrincipal)authentication.getPrincipal();
		authService.logout(principal.userId(), extractBearerToken(authorizationHeader));
		return ResponseEntity.noContent().build();
	}

	private String extractBearerToken(String authorizationHeader) {
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
