package com.ssafy.e102.eumgil.data.repository

import android.content.Context
import com.ssafy.e102.eumgil.BuildConfig
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface SocialAccessTokenProvider {
    suspend fun getAccessToken(provider: AuthSocialProvider): String
}

class UnavailableSocialAccessTokenProvider : SocialAccessTokenProvider {
    override suspend fun getAccessToken(provider: AuthSocialProvider): String {
        throw IllegalStateException("${provider.displayName} Android login key and SDK connection are required.")
    }
}

class KakaoSocialAccessTokenProvider(
    private val context: Context,
) : SocialAccessTokenProvider {
    override suspend fun getAccessToken(provider: AuthSocialProvider): String {
        if (provider != AuthSocialProvider.KAKAO) {
            throw IllegalStateException("${provider.displayName} Android login key and SDK connection are required.")
        }
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            throw IllegalStateException("Kakao Native App Key is required.")
        }

        return suspendCancellableCoroutine { continuation ->
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                when {
                    error != null && continuation.isActive -> continuation.resumeWithException(error)
                    token != null && continuation.isActive -> continuation.resume(token.accessToken)
                    continuation.isActive -> {
                        continuation.resumeWithException(IllegalStateException("Kakao login did not return an access token."))
                    }
                }
            }

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        callback(null, error)
                    } else if (error != null) {
                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    } else {
                        callback(token, null)
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }
    }
}

enum class AuthSocialProvider(
    val serverValue: String,
    val displayName: String,
) {
    GOOGLE(serverValue = "GOOGLE", displayName = "Google"),
    NAVER(serverValue = "NAVER", displayName = "Naver"),
    KAKAO(serverValue = "KAKAO", displayName = "Kakao"),
    ;

    companion object {
        fun fromProviderKey(providerKey: String): AuthSocialProvider? =
            when (providerKey) {
                "auth-ui-google" -> GOOGLE
                "auth-ui-naver" -> NAVER
                "auth-ui-kakao" -> KAKAO
                else -> null
            }
    }
}
