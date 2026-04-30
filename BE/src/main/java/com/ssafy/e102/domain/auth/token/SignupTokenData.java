package com.ssafy.e102.domain.auth.token;

import com.ssafy.e102.domain.user.type.SocialProvider;

public record SignupTokenData(
	SocialProvider socialProvider,
	String socialProviderUserId) {
}
