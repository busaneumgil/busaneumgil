package com.ssafy.e102.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e102.E102Application;
import com.ssafy.e102.global.response.ApiResponse;

@SpringBootTest(classes = {E102Application.class, SecurityConfigTest.SecurityTestController.class})
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("소셜 로그인은 인증 없이 접근할 수 있다")
	void socialLoginIsPublic() throws Exception {
		mockMvc.perform(post("/api/auth/social-login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"));
	}

	@Test
	@DisplayName("토큰 재발급은 인증 없이 접근할 수 있다")
	void reissueIsPublic() throws Exception {
		mockMvc.perform(post("/api/auth/reissue")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("S2000"));
	}

	@Test
	@DisplayName("내 정보 조회는 인증이 필요하다")
	void usersMeRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("로그아웃은 인증이 필요하다")
	void logoutRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status").value("A4010"))
			.andExpect(jsonPath("$.message").value("인증이 필요합니다."));
	}

	@Test
	@DisplayName("유효한 액세스 토큰은 인증 주체를 만든다")
	void validAccessTokenCreatesPrincipal() throws Exception {
		UUID userId = UUID.randomUUID();
		String accessToken = jwtTokenProvider.createAccessToken(userId);

		mockMvc.perform(get("/api/users/me")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").value(userId.toString()));
	}

	@RestController
	static class SecurityTestController {

		@PostMapping("/api/auth/social-login")
		ApiResponse<String> socialLogin() {
			return ApiResponse.success("ok");
		}

		@PostMapping("/api/auth/reissue")
		ApiResponse<String> reissue() {
			return ApiResponse.success("ok");
		}

		@PostMapping("/api/auth/logout")
		ApiResponse<String> logout() {
			return ApiResponse.success("ok");
		}

		@GetMapping("/api/users/me")
		ApiResponse<String> usersMe(
			@AuthenticationPrincipal
			AuthPrincipal principal) {
			return ApiResponse.success(principal.userId().toString());
		}
	}
}
