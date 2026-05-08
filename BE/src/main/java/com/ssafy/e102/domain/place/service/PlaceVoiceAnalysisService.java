package com.ssafy.e102.domain.place.service;

import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.place.dto.request.VoiceAnalyzeRequest;
import com.ssafy.e102.domain.place.dto.response.VoiceAnalyzeResponse;
import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.type.VoiceAnalysisMode;
import com.ssafy.e102.domain.place.type.VoiceIntent;
import com.ssafy.e102.global.external.ai.AiVoiceAnalysisClient;
import com.ssafy.e102.global.external.ai.AiVoiceAnalyzeCommand;
import com.ssafy.e102.global.external.ai.AiVoiceAnalyzeResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceVoiceAnalysisService {

	private final AiVoiceAnalysisClient aiVoiceAnalysisClient;

	public VoiceAnalyzeResponse analyze(VoiceAnalyzeRequest request) {
		AiVoiceAnalyzeResult result = aiVoiceAnalysisClient.analyze(AiVoiceAnalyzeCommand.from(request));
		validateResult(result);
		if (request.mode() == VoiceAnalysisMode.MOBILITY_IMPAIRED) {
			return VoiceAnalyzeResponse.of(result, null, null);
		}
		return VoiceAnalyzeResponse.of(result, result.confirmed(), result.confirmationMessage());
	}

	private void validateResult(AiVoiceAnalyzeResult result) {
		if (result == null || result.intent() == null) {
			throw new PlaceException(PlaceErrorCode.VOICE_ANALYSIS_AI_FAILED);
		}
		if (result.intent() == VoiceIntent.PLACE_SEARCH
			&& (result.placeName() == null || result.placeName().isBlank())) {
			throw new PlaceException(PlaceErrorCode.VOICE_ANALYSIS_AI_FAILED);
		}
	}
}
