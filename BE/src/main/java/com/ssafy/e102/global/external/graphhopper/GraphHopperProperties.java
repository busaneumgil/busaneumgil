package com.ssafy.e102.global.external.graphhopper;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "graphhopper")
public record GraphHopperProperties(
	String baseUrl,
	Duration connectTimeout,
	Duration readTimeout) {

	public GraphHopperProperties {
		Assert.hasText(baseUrl, "GraphHopper base URL은 필수입니다.");
		connectTimeout = defaultIfNull(connectTimeout, Duration.ofSeconds(5));
		readTimeout = defaultIfNull(readTimeout, Duration.ofSeconds(5));
	}

	private static Duration defaultIfNull(Duration value, Duration defaultValue) {
		return value == null ? defaultValue : value;
	}
}
