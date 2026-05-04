package com.ssafy.e102.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthTestConfigControllerTest {

	@Test
	@DisplayName("소셜 로그인 테스트 페이지용 public 설정을 반환한다")
	void getAuthTestConfig() {
		AuthTestConfigProperties properties = new AuthTestConfigProperties(
			true,
			"kakao-js-key",
			"naver-client-id",
			"google-client-id",
			"http://localhost:8080/auth-test.html");
		AuthTestConfigController controller = new AuthTestConfigController(properties);

		AuthTestConfigResponse response = controller.getConfig();

		assertThat(response.kakaoJavaScriptKey()).isEqualTo("kakao-js-key");
		assertThat(response.naverClientId()).isEqualTo("naver-client-id");
		assertThat(response.googleClientId()).isEqualTo("google-client-id");
		assertThat(response.naverCallbackUrl()).isEqualTo("http://localhost:8080/auth-test.html");
	}
}
