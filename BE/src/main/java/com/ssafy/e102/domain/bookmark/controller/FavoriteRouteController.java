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
import com.ssafy.e102.domain.bookmark.dto.response.FavoriteRouteIdResponse;
import com.ssafy.e102.domain.bookmark.dto.response.FavoriteRouteListResponse;
import com.ssafy.e102.domain.bookmark.service.FavoriteRouteService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/favorite-routes")
@RequiredArgsConstructor
public class FavoriteRouteController {

	private static final String DELETE_SUCCESS_MESSAGE = "경로 북마크가 삭제되었습니다.";

	private final FavoriteRouteService favoriteRouteService;

	@GetMapping
	public ApiResponse<FavoriteRouteListResponse> getFavoriteRoutes(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@RequestParam(required = false) @Positive
		Long cursor,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100)
		int size) {
		return ApiResponse.success(favoriteRouteService.getFavoriteRoutes(principal.userId(), cursor, size));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<FavoriteRouteIdResponse> createFavoriteRoute(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		CreateFavoriteRouteRequest request) {
		return ApiResponse.created(favoriteRouteService.createFavoriteRoute(principal.userId(), request));
	}

	@PatchMapping("/{favRouteId}")
	public ApiResponse<FavoriteRouteIdResponse> updateFavoriteRoute(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable @Positive
		Long favRouteId,
		@Valid @RequestBody
		UpdateFavoriteRouteRequest request) {
		return ApiResponse.success(favoriteRouteService.updateFavoriteRoute(principal.userId(), favRouteId, request));
	}

	@DeleteMapping("/{favRouteId}")
	public ApiResponse<Void> deleteFavoriteRoute(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable @Positive
		Long favRouteId) {
		favoriteRouteService.deleteFavoriteRoute(principal.userId(), favRouteId);
		return ApiResponse.successMessage(DELETE_SUCCESS_MESSAGE);
	}
}
