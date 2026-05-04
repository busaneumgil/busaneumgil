package com.ssafy.e102.domain.auth.controller;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/test-config")
@EnableConfigurationProperties(AuthTestConfigProperties.class)
public class AuthTestConfigController {

	private final AuthTestConfigProperties properties;

	public AuthTestConfigController(AuthTestConfigProperties properties) {
		this.properties = properties;
	}

	@GetMapping
	public AuthTestConfigResponse getConfig() {
		return new AuthTestConfigResponse(
			properties.kakaoJavaScriptKey(),
			properties.naverClientId(),
			properties.googleClientId(),
			properties.naverCallbackUrl());
	}
}
