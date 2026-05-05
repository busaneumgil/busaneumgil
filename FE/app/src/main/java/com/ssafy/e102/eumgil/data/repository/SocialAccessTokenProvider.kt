package com.ssafy.e102.eumgil.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.ssafy.e102.eumgil.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface SocialAccessTokenProvider {
    suspend fun getAccessToken(provider: AuthSocialProvider): String
}

class CompositeSocialAccessTokenProvider(
    private val providersBySocialProvider: Map<AuthSocialProvider, SocialAccessTokenProvider>,
) : SocialAccessTokenProvider {
    override suspend fun getAccessToken(provider: AuthSocialProvider): String =
        providersBySocialProvider[provider]?.getAccessToken(provider)
            ?: throw IllegalStateException("${provider.displayName} Android login key and SDK connection are required.")
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

class GoogleSocialAccessTokenProvider(
    private val activityProvider: () -> Context?,
) : SocialAccessTokenProvider {
    override suspend fun getAccessToken(provider: AuthSocialProvider): String {
        if (provider != AuthSocialProvider.GOOGLE) {
            throw IllegalStateException("${provider.displayName} Android login key and SDK connection are required.")
        }
        if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            throw IllegalStateException("Google Web Client ID is required.")
        }
        val activityContext =
            activityProvider()
                ?: throw IllegalStateException("Google login requires a foreground Activity.")

        return requestGoogleIdToken(activityContext)
    }

    private suspend fun requestGoogleIdToken(activityContext: Context): String {
        val signInWithGoogleOption =
            GetSignInWithGoogleOption.Builder(
                serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID,
            ).build()
        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

        val response =
            try {
                CredentialManager.create(activityContext).getCredential(
                    context = activityContext,
                    request = request,
                )
            } catch (exception: GetCredentialException) {
                throw IllegalStateException("Google login failed.", exception)
            }

        val credential = response.credential
        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google login returned an unsupported credential.")
        }

        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (exception: GoogleIdTokenParsingException) {
            throw IllegalStateException("Google login returned an invalid ID token.", exception)
        }
    }
}

class NaverSocialAccessTokenProvider(
    private val activityProvider: () -> Context?,
) : SocialAccessTokenProvider {
    override suspend fun getAccessToken(provider: AuthSocialProvider): String {
        if (provider != AuthSocialProvider.NAVER) {
            throw IllegalStateException("${provider.displayName} Android login key and SDK connection are required.")
        }
        if (!isNaverLoginConfigured()) {
            throw IllegalStateException("Naver Client ID, Client Secret, and Client Name are required.")
        }
        val activityContext =
            activityProvider()
                ?: throw IllegalStateException("Naver login requires a foreground Activity.")

        return suspendCancellableCoroutine { continuation ->
            val callback =
                object : OAuthLoginCallback {
                    override fun onSuccess() {
                        val accessToken = NaverIdLoginSDK.getAccessToken()
                        when {
                            !continuation.isActive -> Unit
                            accessToken.isNullOrBlank() ->
                                continuation.resumeWithException(
                                    IllegalStateException("Naver login did not return an access token."),
                                )
                            else -> continuation.resume(accessToken)
                        }
                    }

                    override fun onFailure(
                        httpStatus: Int,
                        message: String,
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("Naver login failed: $httpStatus $message"),
                            )
                        }
                    }

                    override fun onError(
                        errorCode: Int,
                        message: String,
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("Naver login error: $errorCode $message"),
                            )
                        }
                    }
                }

            NaverIdLoginSDK.authenticate(activityContext, callback)
        }
    }

    private fun isNaverLoginConfigured(): Boolean =
        BuildConfig.NAVER_CLIENT_ID.isNotBlank() &&
            BuildConfig.NAVER_CLIENT_SECRET.isNotBlank() &&
            BuildConfig.NAVER_CLIENT_NAME.isNotBlank()
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
