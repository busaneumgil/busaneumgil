package com.ssafy.e102.global.external.ai;

import java.util.List;

import com.ssafy.e102.domain.place.dto.request.VoiceAnalyzeHistoryRequest;
import com.ssafy.e102.domain.place.dto.request.VoiceAnalyzeRequest;
import com.ssafy.e102.domain.place.type.VoiceAnalysisMode;

public record AiVoiceAnalyzeCommand(
	String text,
	VoiceAnalysisMode mode,
	List<AiVoiceAnalyzeHistoryMessage> history) {

	public static AiVoiceAnalyzeCommand from(VoiceAnalyzeRequest request) {
		return new AiVoiceAnalyzeCommand(
			request.text(),
			request.mode(),
			historyFrom(request.mode(), request.history()));
	}

	private static List<AiVoiceAnalyzeHistoryMessage> historyFrom(VoiceAnalysisMode mode,
		List<VoiceAnalyzeHistoryRequest> history) {
		if (mode == VoiceAnalysisMode.MOBILITY_IMPAIRED || history == null) {
			return List.of();
		}
		return history.stream()
			.map(message -> new AiVoiceAnalyzeHistoryMessage(message.role(), message.content()))
			.toList();
	}
}
