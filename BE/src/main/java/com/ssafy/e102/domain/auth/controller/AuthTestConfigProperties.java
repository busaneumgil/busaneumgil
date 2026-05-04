package com.ssafy.e102.domain.auth.controller;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.test")
public record AuthTestConfigProperties(
	boolean enabled,
	String kakaoJavaScriptKey,
	String naverClientId,
	String googleClientId,
	String naverCallbackUrl) {
}
