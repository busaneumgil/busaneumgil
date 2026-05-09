package com.ssafy.e102.domain.bookmark.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.bookmark.dto.request.CreatePlaceBookmarkRequest;
import com.ssafy.e102.domain.bookmark.dto.response.PlaceBookmarkCreateResponse;
import com.ssafy.e102.domain.bookmark.dto.response.PlaceBookmarkListResponse;
import com.ssafy.e102.domain.bookmark.service.PlaceBookmarkService;
import com.ssafy.e102.global.response.ApiResponse;
import com.ssafy.e102.global.security.principal.AuthPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class PlaceBookmarkController {

	private final PlaceBookmarkService placeBookmarkService;

	@GetMapping
	public ApiResponse<PlaceBookmarkListResponse> getBookmarks(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@RequestParam(required = false) @Positive
		Long cursor,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100)
		int size) {
		return ApiResponse.success(placeBookmarkService.getBookmarks(principal.userId(), cursor, size));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PlaceBookmarkCreateResponse> createBookmark(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@Valid @RequestBody
		CreatePlaceBookmarkRequest request) {
		return ApiResponse.created(placeBookmarkService.createBookmark(principal.userId(), request));
	}

	@DeleteMapping("/targets/{bookmarkTargetId}")
	public ResponseEntity<Void> deleteBookmarkByTarget(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable @Pattern(regexp = "tgt_[0-9a-f]{16}")
		String bookmarkTargetId) {
		placeBookmarkService.deleteBookmarkByTarget(principal.userId(), bookmarkTargetId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/places/{placeId}")
	public ResponseEntity<Void> deleteBookmarkByPlaceId(
		@AuthenticationPrincipal
		AuthPrincipal principal,
		@PathVariable @Positive
		Long placeId) {
		placeBookmarkService.deleteBookmarkByPlaceId(principal.userId(), placeId);
		return ResponseEntity.noContent().build();
	}
}
