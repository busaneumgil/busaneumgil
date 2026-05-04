package com.ssafy.e102.domain.user.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ssafy.e102.domain.user.service.UserService;
import com.ssafy.e102.global.security.AuthPrincipal;

class UserControllerTest {

	@Mock
	private UserService userService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
	}

	@Test
	@DisplayName("회원탈퇴 요청은 현재 사용자 계정을 삭제하고 204를 반환한다")
	void withdraw() throws Exception {
		UUID userId = UUID.randomUUID();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			new AuthPrincipal(userId), null);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		mockMvc.perform(delete("/api/users/me")
			.principal(authentication)
			.header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		verify(userService).withdraw(userId, "access-token");
		SecurityContextHolder.clearContext();
	}
}
