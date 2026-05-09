package com.ssafy.e102.domain.bookmark.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.bookmark.dto.request.CreateFavoriteRouteRequest;
import com.ssafy.e102.domain.bookmark.dto.request.UpdateFavoriteRouteRequest;
import com.ssafy.e102.domain.bookmark.dto.response.FavoriteRouteDetailResponse;
import com.ssafy.e102.domain.bookmark.dto.response.FavoriteRouteIdResponse;
import com.ssafy.e102.domain.bookmark.dto.response.FavoriteRouteListResponse;
import com.ssafy.e102.domain.bookmark.service.FavoriteRouteService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.response.ErrorResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "경로 북마크", description = "경로 북마크 등록, 조회, 수정, 삭제 API")
@Validated
@RestController
@RequestMapping("/favorite-routes")
@RequiredArgsConstructor
public class FavoriteRouteController {

	private static final String DELETE_SUCCESS_MESSAGE = "경로 북마크가 삭제되었습니다.";
	private static final String ERROR_C4000 = "{\"status\":\"C4000\",\"message\":\"잘못된 입력입니다.\"}";
	private static final String ERROR_A4010 = "{\"status\":\"A4010\",\"message\":\"인증이 필요합니다.\"}";
	private static final String ERROR_FR4000 = "{\"status\":\"FR4000\",\"message\":\"경로 북마크 요청값이 올바르지 않습니다.\"}";
	private static final String ERROR_FR4001 = "{\"status\":\"FR4001\",\"message\":\"경로 북마크 수정 요청값이 올바르지 않습니다.\"}";
	private static final String ERROR_FR4030 = "{\"status\":\"FR4030\",\"message\":\"경로 북마크에 대한 권한이 없습니다.\"}";
	private static final String ERROR_FR4040 = "{\"status\":\"FR4040\",\"message\":\"경로 북마크를 찾을 수 없습니다.\"}";
	private static final String ERROR_FR4041 = "{\"status\":\"FR4041\",\"message\":\"선택한 경로 정보를 찾을 수 없습니다.\"}";

	private final FavoriteRouteService favoriteRouteService;

	@Operation(summary = "경로 북마크 목록 조회", description = "로그인 사용자의 경로 북마크 목록을 커서 기반으로 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "cursor/size 형식 또는 범위 오류",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_C4000))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "토큰 누락/만료",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_A4010)))
	})
	@GetMapping
	public ApiResponse<FavoriteRouteListResponse> getFavoriteRoutes(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "다음 페이지 조회 기준이 되는 마지막 경로 북마크 ID") @RequestParam(required = false) @Positive
		Long cursor,
		@Parameter(description = "조회 개수") @RequestParam(defaultValue = "10") @Min(1) @Max(100)
		int size) {
		return ApiResponse.success(favoriteRouteService.getFavoriteRoutes(principal.userId(), cursor, size));
	}

	@Operation(summary = "경로 북마크 상세 조회", description = "로그인 사용자의 특정 경로 북마크 상세 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "path variable 형식 오류",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_C4000))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "토큰 누락/만료",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_A4010))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "403",
			description = "다른 사용자의 경로 북마크 조회 시도",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4030))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "경로 북마크가 존재하지 않음",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4040)))
	})
	@GetMapping("/{favRouteId}")
	public ApiResponse<FavoriteRouteDetailResponse> getFavoriteRouteDetail(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "경로 북마크 ID") @PathVariable @Positive
		Long favRouteId) {
		return ApiResponse.success(favoriteRouteService.getFavoriteRouteDetail(principal.userId(), favRouteId));
	}

	@Operation(summary = "경로 북마크 저장", description = "로그인 사용자의 경로 북마크를 저장합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "요청값 검증 실패 또는 경로 북마크 생성 규칙 위반",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = {
					@ExampleObject(name = "C4000", value = ERROR_C4000),
					@ExampleObject(name = "FR4000", value = ERROR_FR4000)
				})),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "토큰 누락/만료",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_A4010))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "403",
			description = "다른 사용자의 route session 저장 시도",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4030))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "route session 없음 또는 snapshot 복구 불가",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4041)))
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FavoriteRouteIdResponse> createFavoriteRoute(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		CreateFavoriteRouteRequest request) {
		return ApiResponse.created(favoriteRouteService.createFavoriteRoute(principal.userId(), request));
	}

	@Operation(summary = "경로 북마크 수정", description = "로그인 사용자의 경로 북마크 정보를 수정합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "요청값 검증 실패 또는 경로 북마크 수정 규칙 위반",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = {
					@ExampleObject(name = "C4000", value = ERROR_C4000),
					@ExampleObject(name = "FR4001", value = ERROR_FR4001)
				})),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "토큰 누락/만료",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_A4010))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "403",
			description = "다른 사용자의 경로 북마크 수정 시도",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4030))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "경로 북마크가 존재하지 않음",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4040)))
	})
	@PatchMapping("/{favRouteId}")
	public ApiResponse<FavoriteRouteIdResponse> updateFavoriteRoute(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "경로 북마크 ID") @PathVariable @Positive
		Long favRouteId,
		@Valid @RequestBody
		UpdateFavoriteRouteRequest request) {
		return ApiResponse.success(favoriteRouteService.updateFavoriteRoute(principal.userId(), favRouteId, request));
	}

	@Operation(summary = "경로 북마크 삭제", description = "로그인 사용자의 경로 북마크를 삭제합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401",
			description = "토큰 누락/만료",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_A4010))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "403",
			description = "다른 사용자의 경로 북마크 삭제 시도",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4030))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "경로 북마크가 존재하지 않음",
			content = @Content(
				schema = @Schema(implementation = ErrorResponse.class),
				examples = @ExampleObject(value = ERROR_FR4040)))
	})
	@DeleteMapping("/{favRouteId}")
	public ApiResponse<Void> deleteFavoriteRoute(
		@Parameter(hidden = true) @AuthenticationPrincipal
		AuthPrincipal principal,
		@Parameter(description = "경로 북마크 ID") @PathVariable @Positive
		Long favRouteId) {
		favoriteRouteService.deleteFavoriteRoute(principal.userId(), favRouteId);
		return ApiResponse.successMessage(DELETE_SUCCESS_MESSAGE);
	}
}
