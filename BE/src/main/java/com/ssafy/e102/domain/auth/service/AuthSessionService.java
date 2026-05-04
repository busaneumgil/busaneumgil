package com.ssafy.e102.domain.auth.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ssafy.e102.domain.auth.token.AccessTokenBlacklistStore;
import com.ssafy.e102.domain.auth.token.RefreshTokenStore;
import com.ssafy.e102.global.security.JwtTokenProvider;

@Service
public class AuthSessionService {

	private final RefreshTokenStore refreshTokenStore;
	private final AccessTokenBlacklistStore accessTokenBlacklistStore;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthSessionService(
		RefreshTokenStore refreshTokenStore,
		AccessTokenBlacklistStore accessTokenBlacklistStore,
		JwtTokenProvider jwtTokenProvider) {
		this.refreshTokenStore = refreshTokenStore;
		this.accessTokenBlacklistStore = accessTokenBlacklistStore;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	public void invalidateUserSession(UUID userId, String accessToken) {
		refreshTokenStore.deleteByUserId(userId);
		jwtTokenProvider.getAccessTokenRemainingTtl(accessToken)
			.filter(this::isPositive)
			.ifPresent(remainingTtl -> accessTokenBlacklistStore.save(accessToken, remainingTtl));
	}

	private boolean isPositive(Duration ttl) {
		return !ttl.isNegative() && !ttl.isZero();
	}
}
