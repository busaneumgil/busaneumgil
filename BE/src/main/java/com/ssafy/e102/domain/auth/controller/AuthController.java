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
import com.ssafy.e102.global.response.ErrorResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "인증", description = "소셜 로그인, 회원가입, 토큰 재발급, 로그아웃 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@SecurityRequirements()
	@Operation(summary = "소셜 로그인", description = "소셜 provider 인증 정보로 로그인하고 신규 사용자 여부와 토큰을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "socialProvider, socialAccessToken 누락 또는 형식 오류",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "소셜 액세스 토큰이 유효하지 않음",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "502",
			description = "소셜 제공자 API 호출 실패",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/social-login")
	public ApiResponse<SocialLoginResponse> socialLogin(
		@Valid @RequestBody
		SocialLoginRequest request) {
		return ApiResponse.success(authService.socialLogin(request));
	}

	@SecurityRequirements()
	@Operation(summary = "회원가입", description = "소셜 로그인 후 신규 사용자 정보를 저장하고 가입 완료 토큰을 반환합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "사용자 유형 조합 오류, 필수 약관 미동의",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "signupToken 만료 또는 위조",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "409",
			description = "같은 소셜 계정으로 이미 가입 완료된 경우",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SignupResponse> signup(
		@Valid @RequestBody
		SignupRequest request) {
		return ApiResponse.created(authService.signup(request));
	}

	@SecurityRequirements()
	@Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token과 Refresh Token을 재발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "refreshToken 누락",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "리프레시 토큰 만료 또는 위조",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/reissue")
	public ApiResponse<TokenResponse> reissue(
		@Valid @RequestBody
		ReissueRequest request) {
		return ApiResponse.success(authService.reissue(request));
	}

	@Operation(summary = "로그아웃", description = "현재 Access Token을 로그아웃 처리하고 Refresh Token을 제거합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "토큰 누락/만료",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/logout")
	public ApiResponse<Void> logout(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal) {
		authService.logout(principal.userId(), principal.accessToken());
		return ApiResponse.successMessage("로그아웃되었습니다.");
	}
}
