package com.ssafy.e102.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ssafy.e102.domain.place.dto.request.VoiceAnalyzeHistoryRequest;
import com.ssafy.e102.domain.place.dto.request.VoiceAnalyzeRequest;
import com.ssafy.e102.domain.place.dto.response.VoiceAnalyzeResponse;
import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.type.VoiceAnalysisMode;
import com.ssafy.e102.domain.place.type.VoiceIntent;
import com.ssafy.e102.global.external.ai.AiVoiceAnalysisClient;
import com.ssafy.e102.global.external.ai.AiVoiceAnalyzeCommand;
import com.ssafy.e102.global.external.ai.AiVoiceAnalyzeResult;

class PlaceVoiceAnalysisServiceTest {

	@Mock
	private AiVoiceAnalysisClient aiVoiceAnalysisClient;

	private PlaceVoiceAnalysisService placeVoiceAnalysisService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		placeVoiceAnalysisService = new PlaceVoiceAnalysisService(aiVoiceAnalysisClient);
	}

	@Test
	@DisplayName("보행약자 음성 분석은 추출 장소명을 반환하고 확인 필드는 비운다")
	void analyzeMobilityImpairedVoiceText() {
		when(aiVoiceAnalysisClient.analyze(any(AiVoiceAnalyzeCommand.class)))
			.thenReturn(new AiVoiceAnalyzeResult(VoiceIntent.PLACE_SEARCH, "이재모피자", true, "무시되는 문구"));

		VoiceAnalyzeResponse response = placeVoiceAnalysisService.analyze(new VoiceAnalyzeRequest(
			"마 이재모피자 어디있는지 알려주소",
			VoiceAnalysisMode.MOBILITY_IMPAIRED,
			List.of(new VoiceAnalyzeHistoryRequest("user", "이전 발화"))));

		assertThat(response.intent()).isEqualTo(VoiceIntent.PLACE_SEARCH);
		assertThat(response.placeName()).isEqualTo("이재모피자");
		assertThat(response.confirmed()).isNull();
		assertThat(response.confirmationMessage()).isNull();
		ArgumentCaptor<AiVoiceAnalyzeCommand> captor = ArgumentCaptor.forClass(AiVoiceAnalyzeCommand.class);
		verify(aiVoiceAnalysisClient).analyze(captor.capture());
		assertThat(captor.getValue().history()).isEmpty();
	}

	@Test
	@DisplayName("저시력자 음성 분석은 이전 대화와 확인 문구를 유지한다")
	void analyzeLowVisionVoiceTextWithHistory() {
		when(aiVoiceAnalysisClient.analyze(any(AiVoiceAnalyzeCommand.class)))
			.thenReturn(new AiVoiceAnalyzeResult(
				VoiceIntent.PLACE_SEARCH,
				"부산대학교",
				null,
				"부산대학교를 찾으시나요?"));

		VoiceAnalyzeResponse response = placeVoiceAnalysisService.analyze(new VoiceAnalyzeRequest(
			"아니 부산대학교",
			VoiceAnalysisMode.LOW_VISION,
			List.of(
				new VoiceAnalyzeHistoryRequest("user", "부산역 어디야"),
				new VoiceAnalyzeHistoryRequest("assistant",
					"{\"intent\":\"PLACE_SEARCH\",\"placeName\":\"부산역\"}"))));

		assertThat(response.placeName()).isEqualTo("부산대학교");
		assertThat(response.confirmed()).isNull();
		assertThat(response.confirmationMessage()).isEqualTo("부산대학교를 찾으시나요?");
		ArgumentCaptor<AiVoiceAnalyzeCommand> captor = ArgumentCaptor.forClass(AiVoiceAnalyzeCommand.class);
		verify(aiVoiceAnalysisClient).analyze(captor.capture());
		assertThat(captor.getValue().history()).hasSize(2);
	}

	@Test
	@DisplayName("AI 응답에 장소 검색 의도만 있고 장소명이 없으면 실패로 처리한다")
	void rejectInvalidAiResult() {
		when(aiVoiceAnalysisClient.analyze(any(AiVoiceAnalyzeCommand.class)))
			.thenReturn(new AiVoiceAnalyzeResult(VoiceIntent.PLACE_SEARCH, null, null, null));

		assertThatThrownBy(() -> placeVoiceAnalysisService.analyze(new VoiceAnalyzeRequest(
			"어디야",
			VoiceAnalysisMode.MOBILITY_IMPAIRED,
			null)))
			.isInstanceOf(PlaceException.class)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.VOICE_ANALYSIS_AI_FAILED);
	}
}
