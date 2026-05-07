package com.ssafy.e102.global.external.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;
import com.ssafy.e102.domain.place.type.VoiceAnalysisMode;
import com.ssafy.e102.domain.place.type.VoiceIntent;

class RestTemplateAiVoiceAnalysisClientTest {

	private static final String AI_BASE_URL = "https://ai.dev.busaneumgil.com";
	private static final String AI_VOICE_ANALYSIS_URI = AI_BASE_URL + "/voice/analyze";

	private MockRestServiceServer server;
	private RestTemplateAiVoiceAnalysisClient client;

	@BeforeEach
	void setUp() {
		RestTemplate restTemplate = new RestTemplate();
		server = MockRestServiceServer.createServer(restTemplate);
		client = new RestTemplateAiVoiceAnalysisClient(restTemplate,
			new AiVoiceAnalysisProperties(AI_BASE_URL));
	}

	@Test
	@DisplayName("AI 음성 분석 서버에 STT 텍스트와 대화 이력을 전달한다")
	void analyzeVoiceText() {
		server.expect(requestTo(AI_VOICE_ANALYSIS_URI))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("{\"text\":\"응\",\"mode\":\"LOW_VISION\","
				+ "\"history\":[{\"role\":\"assistant\",\"content\":\"{\\\"intent\\\":\\\"PLACE_SEARCH\\\"}\"}]}"))
			.andRespond(withSuccess("{\"intent\":\"PLACE_SEARCH\",\"placeName\":\"이재모피자\","
				+ "\"confirmed\":true,\"confirmationMessage\":null}", MediaType.APPLICATION_JSON));

		AiVoiceAnalyzeResult result = client.analyze(new AiVoiceAnalyzeCommand(
			"응",
			VoiceAnalysisMode.LOW_VISION,
			List.of(new AiVoiceAnalyzeHistoryMessage("assistant", "{\"intent\":\"PLACE_SEARCH\"}"))));

		assertThat(result.intent()).isEqualTo(VoiceIntent.PLACE_SEARCH);
		assertThat(result.placeName()).isEqualTo("이재모피자");
		assertThat(result.confirmed()).isTrue();
		server.verify();
	}

	@Test
	@DisplayName("AI 서버 오류는 음성 분석 실패 예외로 매핑한다")
	void mapAiServerError() {
		server.expect(requestTo(AI_VOICE_ANALYSIS_URI))
			.andRespond(withServerError());

		assertThatThrownBy(() -> client.analyze(new AiVoiceAnalyzeCommand(
			"이재모피자 어디야",
			VoiceAnalysisMode.LOW_VISION,
			List.of())))
			.isInstanceOf(PlaceException.class)
			.extracting("errorCode")
			.isEqualTo(PlaceErrorCode.VOICE_ANALYSIS_AI_FAILED);
	}

	@Test
	@DisplayName("AI base URL 끝의 슬래시는 제거하고 음성 분석 경로를 붙인다")
	void buildVoiceAnalyzeUriFromBaseUrl() {
		AiVoiceAnalysisProperties properties = new AiVoiceAnalysisProperties(AI_BASE_URL + "/");

		assertThat(properties.voiceAnalyzeUri()).isEqualTo(AI_VOICE_ANALYSIS_URI);
	}
}
