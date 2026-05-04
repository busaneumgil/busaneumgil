package com.ssafy.e102.global.security;

import com.ssafy.e102.domain.user.type.SocialProvider;

public record SignupTokenClaims(
	SocialProvider socialProvider,
	String socialProviderUserId) {
}
