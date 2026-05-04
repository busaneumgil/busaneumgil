package com.ssafy.e102.domain.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "social.provider")
public record SocialProviderProperties(Kakao kakao, Naver naver, Google google) {

	public SocialProviderProperties {
		Assert.notNull(kakao, "social.provider.kakao must not be null");
		Assert.notNull(naver, "social.provider.naver must not be null");
		Assert.notNull(google, "social.provider.google must not be null");
	}

	public record Kakao(String userInfoUri) {

		public Kakao {
			Assert.hasText(userInfoUri, "social.provider.kakao.user-info-uri must not be blank");
		}
	}

	public record Naver(String userInfoUri) {

		public Naver {
			Assert.hasText(userInfoUri, "social.provider.naver.user-info-uri must not be blank");
		}
	}

	public record Google(String userInfoUri) {

		public Google {
			Assert.hasText(userInfoUri, "social.provider.google.user-info-uri must not be blank");
		}
	}
}
