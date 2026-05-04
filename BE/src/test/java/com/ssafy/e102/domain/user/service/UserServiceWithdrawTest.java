package com.ssafy.e102.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ssafy.e102.domain.auth.service.AuthSessionService;
import com.ssafy.e102.domain.user.exception.UserErrorCode;
import com.ssafy.e102.domain.user.exception.UserException;
import com.ssafy.e102.domain.user.repository.UserRepository;

class UserServiceWithdrawTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AuthSessionService authSessionService;

	private UserService userService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		userService = new UserService(userRepository, authSessionService);
	}

	@Test
	@DisplayName("회원탈퇴는 현재 사용자를 삭제하고 남은 인증 세션을 무효화한다")
	void withdraw() {
		UUID userId = UUID.randomUUID();
		when(userRepository.existsById(userId)).thenReturn(true);

		userService.withdraw(userId, "access-token");

		verify(userRepository).deleteById(userId);
		verify(authSessionService).invalidateUserSession(userId, "access-token");
	}

	@Test
	@DisplayName("현재 사용자가 없으면 회원탈퇴를 거부한다")
	void rejectMissingUser() {
		UUID userId = UUID.randomUUID();
		when(userRepository.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> userService.withdraw(userId, "access-token"))
			.isInstanceOf(UserException.class)
			.extracting("errorCode")
			.isEqualTo(UserErrorCode.USER_NOT_FOUND);

		verify(userRepository, never()).deleteById(userId);
		verify(authSessionService, never()).invalidateUserSession(userId, "access-token");
	}
}
