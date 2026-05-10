package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class TestAuthSessionRepository(
    initialState: AuthGateState,
) : AuthSessionRepository {
    private var authGateState: AuthGateState = initialState

    override fun observeAuthGateState(): Flow<AuthGateState> = flowOf(authGateState)

    override suspend fun getAuthGateState(): AuthGateState = authGateState

    override suspend fun saveAuthSession(
        authSession: AuthSession,
        isProfileCompleted: Boolean,
    ) {
        authGateState =
            authGateState.copy(
                authSession = authSession,
                isProfileCompleted = isProfileCompleted,
                signupToken = null,
            )
    }

    override suspend fun saveSignupToken(signupToken: String) {
        authGateState =
            authGateState.copy(
                authSession = null,
                isProfileCompleted = false,
                signupToken = signupToken,
            )
    }

    override suspend fun clearSignupToken() {
        authGateState = authGateState.copy(signupToken = null)
    }

    override suspend fun markProfileCompleted() {
        authGateState = authGateState.copy(isProfileCompleted = true)
    }

    override suspend fun clearAuthSession() {
        authGateState = authGateState.copy(authSession = null, isProfileCompleted = false)
    }
}
