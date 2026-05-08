package com.ssafy.e102.global.external.graphhopper;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * Backend가 GraphHopper runtime을 호출할 때 쓰는 baseUrl과 timeout 설정이다.
 *
 * <p>application.yml 또는 환경변수에서 값을 받고, 누락된 timeout은 경로 구현 계획의 기본값 5초로 맞춘다.
 */
@ConfigurationProperties(prefix = "graphhopper")
public record GraphHopperProperties(
	String baseUrl,
	Duration connectTimeout,
	Duration readTimeout) {

	public GraphHopperProperties {
		Assert.hasText(baseUrl, "GraphHopper 기본 URL은 필수입니다.");
		// 환경별 설정 누락 시에도 외부 호출이 무제한 대기하지 않도록 5초 기본값을 고정한다.
		connectTimeout = defaultIfNull(connectTimeout, Duration.ofSeconds(5));
		readTimeout = defaultIfNull(readTimeout, Duration.ofSeconds(5));
	}

	private static Duration defaultIfNull(Duration value, Duration defaultValue) {
		return value == null ? defaultValue : value;
	}
}
