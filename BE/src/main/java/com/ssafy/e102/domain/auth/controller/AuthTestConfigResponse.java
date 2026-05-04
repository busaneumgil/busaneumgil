package com.ssafy.e102.domain.auth.controller;

public record AuthTestConfigResponse(
	String kakaoJavaScriptKey,
	String naverClientId,
	String googleClientId,
	String naverCallbackUrl) {
}
