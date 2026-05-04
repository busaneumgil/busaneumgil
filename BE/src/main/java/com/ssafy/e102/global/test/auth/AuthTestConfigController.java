package com.ssafy.e102.global.test.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/test-config")
@EnableConfigurationProperties(AuthTestConfigProperties.class)
@ConditionalOnProperty(prefix = "auth.test", name = "enabled", havingValue = "true")
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
			properties.googleClientId());
	}
}
