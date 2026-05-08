package com.ssafy.e102.domain.place.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.domain.place.dto.request.VoiceAnalyzeRequest;
import com.ssafy.e102.domain.place.dto.response.VoiceAnalyzeResponse;
import com.ssafy.e102.domain.place.service.PlaceVoiceAnalysisService;
import com.ssafy.e102.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class PlaceVoiceAnalysisController {

	private final PlaceVoiceAnalysisService placeVoiceAnalysisService;

	@PostMapping("/analyze")
	public ApiResponse<VoiceAnalyzeResponse> analyze(
		@Valid @RequestBody
		VoiceAnalyzeRequest request) {
		return ApiResponse.success(placeVoiceAnalysisService.analyze(request));
	}
}
