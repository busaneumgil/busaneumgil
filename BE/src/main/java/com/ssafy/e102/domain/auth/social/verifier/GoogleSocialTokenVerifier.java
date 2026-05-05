package com.ssafy.e102.domain.auth.social.verifier;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ssafy.e102.domain.auth.dto.SocialUserInfo;
import com.ssafy.e102.domain.auth.exception.AuthErrorCode;
import com.ssafy.e102.domain.auth.exception.AuthException;
import com.ssafy.e102.domain.auth.social.config.SocialProviderProperties;
import com.ssafy.e102.domain.user.type.SocialProvider;

@Component
public class GoogleSocialTokenVerifier implements SocialTokenVerifier {

	private final GoogleIdTokenSubjectVerifier subjectVerifier;

	public GoogleSocialTokenVerifier(SocialProviderProperties.Google properties) {
		Assert.notNull(properties, "구글 소셜 제공자 설정은 필수입니다.");
		this.subjectVerifier = new GoogleApiClientIdTokenSubjectVerifier(properties.webClientId());
	}

	GoogleSocialTokenVerifier(Function<String, String> subjectVerifier) {
		Assert.notNull(subjectVerifier, "구글 ID 토큰 검증기는 필수입니다.");
		this.subjectVerifier = subjectVerifier::apply;
	}

	@Override
	public SocialProvider getProvider() {
		return SocialProvider.GOOGLE;
	}

	@Override
	public SocialUserInfo verify(String socialAccessToken) {
		Assert.hasText(socialAccessToken, "구글 ID token은 필수입니다.");
		String socialProviderUserId = subjectVerifier.verifyAndGetSubject(socialAccessToken);
		return new SocialUserInfo(SocialProvider.GOOGLE, socialProviderUserId);
	}

	private static final class GoogleApiClientIdTokenSubjectVerifier implements GoogleIdTokenSubjectVerifier {

		private final String webClientId;
		private volatile GoogleIdTokenVerifier verifier;

		private GoogleApiClientIdTokenSubjectVerifier(String webClientId) {
			this.webClientId = webClientId;
		}

		@Override
		public String verifyAndGetSubject(String idTokenString) {
			if (!StringUtils.hasText(webClientId)) {
				throw new AuthException(AuthErrorCode.INVALID_AUTH_REQUEST, "구글 Web Client ID 설정이 필요합니다.");
			}

			try {
				GoogleIdToken idToken = verifier().verify(idTokenString);
				if (idToken == null) {
					throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN, "유효하지 않은 구글 ID token입니다.");
				}

				String subject = idToken.getPayload().getSubject();
				if (!StringUtils.hasText(subject)) {
					throw new AuthException(AuthErrorCode.SOCIAL_PROVIDER_API_FAILED,
						"구글 ID token에서 사용자 식별자를 찾을 수 없습니다.");
				}
				return subject;
			} catch (IllegalArgumentException exception) {
				throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN, "유효하지 않은 구글 ID token입니다.",
					exception);
			} catch (GeneralSecurityException | IOException exception) {
				throw new AuthException(AuthErrorCode.SOCIAL_PROVIDER_API_FAILED,
					"구글 ID token 검증에 실패했습니다.", exception);
			}
		}

		private GoogleIdTokenVerifier verifier() throws GeneralSecurityException, IOException {
			GoogleIdTokenVerifier current = verifier;
			if (current != null) {
				return current;
			}

			synchronized (this) {
				if (verifier == null) {
					verifier = new GoogleIdTokenVerifier.Builder(
						GoogleNetHttpTransport.newTrustedTransport(),
						GsonFactory.getDefaultInstance())
						.setAudience(Collections.singletonList(webClientId))
						.build();
				}
				return verifier;
			}
		}
	}

	@FunctionalInterface
	private interface GoogleIdTokenSubjectVerifier {

		String verifyAndGetSubject(String idTokenString);
	}
}
