package com.ssafy.e102.global.external.ai;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ssafy.e102.domain.place.exception.PlaceErrorCode;
import com.ssafy.e102.domain.place.exception.PlaceException;

@Component
public class RestTemplateAiVoiceAnalysisClient implements AiVoiceAnalysisClient {

	private final RestTemplate restTemplate;
	private final AiVoiceAnalysisProperties properties;

	@Autowired
	public RestTemplateAiVoiceAnalysisClient(RestTemplateBuilder builder, AiVoiceAnalysisProperties properties) {
		this(builder.connectTimeout(Duration.ofSeconds(3))
			.readTimeout(Duration.ofSeconds(5))
			.build(), properties);
	}

	RestTemplateAiVoiceAnalysisClient(RestTemplate restTemplate, AiVoiceAnalysisProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	@Override
	public AiVoiceAnalyzeResult analyze(AiVoiceAnalyzeCommand command) {
		try {
			return restTemplate.postForObject(properties.voiceAnalyzeUri(), command, AiVoiceAnalyzeResult.class);
		} catch (HttpStatusCodeException exception) {
			throw new PlaceException(PlaceErrorCode.VOICE_ANALYSIS_AI_FAILED,
				"음성 분석 AI 호출에 실패했습니다.", exception);
		} catch (RestClientException exception) {
			throw new PlaceException(PlaceErrorCode.VOICE_ANALYSIS_AI_FAILED,
				"음성 분석 AI 호출에 실패했습니다.", exception);
		}
	}
}
