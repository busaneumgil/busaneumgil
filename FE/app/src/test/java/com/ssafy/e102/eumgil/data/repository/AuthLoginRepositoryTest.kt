package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLoginRepositoryTest {
    @Test
    fun `local-only login marks profile complete so auth flow can advance`() =
        runTest {
            val authSessionRepository = RecordingAuthSessionRepository()
            val repository =
                LocalOnlyAuthLoginRepository(
                    authSessionRepository = authSessionRepository,
                    handoffDelayMillis = 0L,
                )

            repository.login(AuthLoginRequest(providerKey = "auth-ui-google"))

            assertNotNull(authSessionRepository.savedAuthSession)
            assertTrue(authSessionRepository.savedIsProfileCompleted)
            assertEquals(
                "local-only-auth-session",
                authSessionRepository.savedAuthSession?.accessToken,
            )
        }
}

private class RecordingAuthSessionRepository : AuthSessionRepository {
    var savedAuthSession: AuthSession? = null
        private set
    var savedIsProfileCompleted: Boolean = false
        private set

    override fun observeAuthGateState(): Flow<AuthGateState> = emptyFlow()

    override suspend fun getAuthGateState(): AuthGateState = AuthGateState()

    override suspend fun saveAuthSession(
        authSession: AuthSession,
        isProfileCompleted: Boolean,
    ) {
        savedAuthSession = authSession
        savedIsProfileCompleted = isProfileCompleted
    }

    override suspend fun markProfileCompleted() = Unit

    override suspend fun clearAuthSession() = Unit
}
