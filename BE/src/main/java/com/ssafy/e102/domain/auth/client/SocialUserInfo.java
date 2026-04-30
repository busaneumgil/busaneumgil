package com.ssafy.e102.domain.auth.client;

import org.springframework.util.Assert;

import com.ssafy.e102.domain.user.type.SocialProvider;

public record SocialUserInfo(SocialProvider socialProvider, String socialProviderUserId) {

	public SocialUserInfo {
		Assert.notNull(socialProvider, "socialProvider must not be null");
		Assert.hasText(socialProviderUserId, "socialProviderUserId must not be blank");
	}
}
