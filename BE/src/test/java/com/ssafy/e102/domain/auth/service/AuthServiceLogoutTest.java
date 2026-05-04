package com.ssafy.e102.domain.auth.service;

import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ssafy.e102.domain.auth.client.CompositeSocialTokenVerifier;
import com.ssafy.e102.domain.auth.token.RefreshTokenStore;
import com.ssafy.e102.domain.auth.token.SignupTokenStore;
import com.ssafy.e102.domain.user.repository.UserRepository;
import com.ssafy.e102.global.security.JwtProperties;
import com.ssafy.e102.global.security.JwtTokenProvider;

class AuthServiceLogoutTest {

	@Mock
	private CompositeSocialTokenVerifier socialTokenVerifier;

	@Mock
	private UserRepository userRepository;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Mock
	private SignupTokenStore signupTokenStore;

	@Mock
	private AuthSessionService authSessionService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		JwtProperties jwtProperties = new JwtProperties(
			"bG9jYWwtand0LXNlY3JldC1mb3ItZTEwMi0zMmJ5dGVzISE=",
			"e102-test",
			Duration.ofMinutes(15),
			Duration.ofDays(14),
			Duration.ofMinutes(10));
		authService = new AuthService(
			socialTokenVerifier,
			userRepository,
			jwtTokenProvider,
			refreshTokenStore,
			signupTokenStore,
			jwtProperties,
			authSessionService);
	}

	@Test
	@DisplayName("로그아웃은 현재 사용자 세션을 무효화한다")
	void logout() {
		UUID userId = UUID.randomUUID();

		authService.logout(userId, "access-token");

		verify(authSessionService).invalidateUserSession(userId, "access-token");
	}
}
