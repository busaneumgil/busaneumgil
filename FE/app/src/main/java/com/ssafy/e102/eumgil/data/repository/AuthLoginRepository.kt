package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthSession
import kotlinx.coroutines.delay

data class AuthLoginRequest(
    val providerKey: String,
)

interface AuthLoginRepository {
    suspend fun login(request: AuthLoginRequest)
}

class LocalOnlyAuthLoginRepository(
    private val authSessionRepository: AuthSessionRepository,
    private val handoffDelayMillis: Long = LOCAL_ONLY_LOGIN_DELAY_MILLIS,
) : AuthLoginRepository {
    override suspend fun login(request: AuthLoginRequest) {
        require(request.providerKey.isNotBlank()) { "로그인 방식을 다시 선택해주세요." }

        delay(handoffDelayMillis)
        // Social login is not wired yet, so any supported provider click advances the
        // user through the app's next gate with a local mock session.
        authSessionRepository.saveAuthSession(
            authSession = AuthSession(accessToken = LOCAL_ONLY_AUTH_SESSION_MARKER),
            isProfileCompleted = true,
        )
    }

    private companion object {
        private const val LOCAL_ONLY_LOGIN_DELAY_MILLIS = 450L
        private const val LOCAL_ONLY_AUTH_SESSION_MARKER = "local-only-auth-session"
    }
}
