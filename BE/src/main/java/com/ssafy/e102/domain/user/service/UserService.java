package com.ssafy.e102.domain.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e102.domain.auth.service.AuthSessionService;
import com.ssafy.e102.domain.user.exception.UserErrorCode;
import com.ssafy.e102.domain.user.exception.UserException;
import com.ssafy.e102.domain.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final AuthSessionService authSessionService;

	public UserService(UserRepository userRepository, AuthSessionService authSessionService) {
		this.userRepository = userRepository;
		this.authSessionService = authSessionService;
	}

	@Transactional
	public void withdraw(UUID userId, String accessToken) {
		if (!userRepository.existsById(userId)) {
			throw new UserException(UserErrorCode.USER_NOT_FOUND);
		}

		// 계정 삭제 후 남은 refresh token과 현재 access token을 함께 막아 재사용 여지를 줄인다.
		userRepository.deleteById(userId);
		authSessionService.invalidateUserSession(userId, accessToken);
	}
}
