package com.ssafy.e102.domain.auth.client;

import com.ssafy.e102.domain.user.type.SocialProvider;

public interface SocialTokenVerifier {

	SocialProvider getProvider();

	SocialUserInfo verify(String socialAccessToken);
}
