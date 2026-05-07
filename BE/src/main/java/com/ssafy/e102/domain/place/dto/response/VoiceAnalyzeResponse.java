package com.ssafy.e102.domain.place.dto.response;

import com.ssafy.e102.domain.place.type.VoiceIntent;
import com.ssafy.e102.global.external.ai.AiVoiceAnalyzeResult;

public record VoiceAnalyzeResponse(
	VoiceIntent intent,
	String placeName,
	Boolean confirmed,
	String confirmationMessage) {

	public static VoiceAnalyzeResponse of(AiVoiceAnalyzeResult result, Boolean confirmed,
		String confirmationMessage) {
		return new VoiceAnalyzeResponse(
			result.intent(),
			result.placeName(),
			confirmed,
			confirmationMessage);
	}
}
