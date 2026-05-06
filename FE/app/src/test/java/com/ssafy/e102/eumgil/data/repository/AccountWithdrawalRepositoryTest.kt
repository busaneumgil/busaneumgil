package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.UserApiException
import com.ssafy.e102.eumgil.data.remote.datasource.UserRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountWithdrawalRepositoryTest {
    @Test
    fun `withdraw success clears local user data and auth session`() =
        runTest {
            val authSessionRepository =
                RecordingWithdrawalAuthSessionRepository(
                    authGateState =
                        AuthGateState(
                            authSession =
                                AuthSession(
                                    accessToken = "access-token",
                                    refreshToken = "refresh-token",
                                    userId = "existing-user-id",
                                ),
                            isProfileCompleted = true,
                        ),
                )
            val localDataCleaner = RecordingAccountWithdrawalLocalDataCleaner()
            val remoteDataSource =
                FakeWithdrawUserRemoteDataSource(
                    withdrawMessage = "회원탈퇴가 완료되었습니다.",
                )
            val repository =
                ServerAccountWithdrawalRepository(
                    userRemoteDataSource = remoteDataSource,
                    authSessionRepository = authSessionRepository,
                    localDataCleaner = localDataCleaner,
                )

            val result = repository.withdraw()

            assertEquals("access-token", remoteDataSource.latestWithdrawAccessToken)
            assertTrue(localDataCleaner.clearCalled)
            assertTrue(authSessionRepository.clearAuthSessionCalled)
            assertEquals(
                AccountWithdrawalResult.Success(message = "회원탈퇴가 완료되었습니다."),
                result,
            )
        }

    @Test
    fun `withdraw failure keeps auth session and local user data intact`() =
        runTest {
            val authSessionRepository =
                RecordingWithdrawalAuthSessionRepository(
                    authGateState =
                        AuthGateState(
                            authSession =
                                AuthSession(
                                    accessToken = "access-token",
                                    refreshToken = "refresh-token",
                                    userId = "existing-user-id",
                                ),
                            isProfileCompleted = true,
                        ),
                )
            val localDataCleaner = RecordingAccountWithdrawalLocalDataCleaner()
            val remoteDataSource =
                FakeWithdrawUserRemoteDataSource(
                    exception =
                        UserApiException(
                            httpStatusCode = 500,
                            status = "U5000",
                            message = "회원탈퇴 처리에 실패했습니다.",
                        ),
                )
            val repository =
                ServerAccountWithdrawalRepository(
                    userRemoteDataSource = remoteDataSource,
                    authSessionRepository = authSessionRepository,
                    localDataCleaner = localDataCleaner,
                )

            val result = repository.withdraw()

            assertEquals(
                AccountWithdrawalResult.Failure(message = "회원탈퇴 처리에 실패했습니다."),
                result,
            )
            assertFalse(localDataCleaner.clearCalled)
            assertFalse(authSessionRepository.clearAuthSessionCalled)
        }

    @Test
    fun `withdraw authentication failure clears session and returns authentication failed`() =
        runTest {
            val authSessionRepository =
                RecordingWithdrawalAuthSessionRepository(
                    authGateState =
                        AuthGateState(
                            authSession =
                                AuthSession(
                                    accessToken = "expired-access-token",
                                    refreshToken = "refresh-token",
                                    userId = "existing-user-id",
                                ),
                            isProfileCompleted = true,
                        ),
                )
            val localDataCleaner = RecordingAccountWithdrawalLocalDataCleaner()
            val remoteDataSource =
                FakeWithdrawUserRemoteDataSource(
                    exception =
                        UserApiException(
                            httpStatusCode = 401,
                            status = "A4010",
                            message = "인증이 필요합니다.",
                        ),
                )
            val repository =
                ServerAccountWithdrawalRepository(
                    userRemoteDataSource = remoteDataSource,
                    authSessionRepository = authSessionRepository,
                    localDataCleaner = localDataCleaner,
                )

            val result = repository.withdraw()

            assertEquals(AccountWithdrawalResult.AuthenticationFailed, result)
            assertTrue(authSessionRepository.clearAuthSessionCalled)
            assertFalse(localDataCleaner.clearCalled)
        }
}

private class FakeWithdrawUserRemoteDataSource(
    private val withdrawMessage: String? = null,
    private val exception: Throwable? = null,
) : UserRemoteDataSource(httpJsonClient = HttpJsonClient(baseUrl = "https://example.com")) {
    var latestWithdrawAccessToken: String? = null
        private set

    override suspend fun withdraw(accessToken: String): String {
        latestWithdrawAccessToken = accessToken
        exception?.let { throw it }
        return checkNotNull(withdrawMessage)
    }
}

private class RecordingWithdrawalAuthSessionRepository(
    private val authGateState: AuthGateState,
) : AuthSessionRepository {
    var clearAuthSessionCalled: Boolean = false
        private set

    override fun observeAuthGateState(): Flow<AuthGateState> = emptyFlow()

    override suspend fun getAuthGateState(): AuthGateState = authGateState

    override suspend fun saveAuthSession(
        authSession: AuthSession,
        isProfileCompleted: Boolean,
    ) = Unit

    override suspend fun saveSignupToken(signupToken: String) = Unit

    override suspend fun clearSignupToken() = Unit

    override suspend fun markProfileCompleted() = Unit

    override suspend fun clearAuthSession() {
        clearAuthSessionCalled = true
    }
}

private class RecordingAccountWithdrawalLocalDataCleaner : AccountWithdrawalLocalDataCleaner {
    var clearCalled: Boolean = false
        private set

    override suspend fun clearAfterWithdrawal() {
        clearCalled = true
    }
}
