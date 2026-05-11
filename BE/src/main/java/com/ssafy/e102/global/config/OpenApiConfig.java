package com.ssafy.e102.global.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	private static final String BEARER_AUTH = "bearerAuth";

	private final String serverUrl;

	public OpenApiConfig(@Value("${openapi.server-url:}")
	String serverUrl) {
		this.serverUrl = serverUrl;
	}

	@Bean
	public OpenAPI openApi() {
		OpenAPI openApi = new OpenAPI()
			.components(new Components()
				.addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
			.info(new Info()
				.title("E102 API")
				.description("E102 API 문서")
				.version("v1"));
		if (serverUrl != null && !serverUrl.isBlank()) {
			openApi.servers(List.of(new Server().url(serverUrl)));
		}
		return openApi;
	}
}
