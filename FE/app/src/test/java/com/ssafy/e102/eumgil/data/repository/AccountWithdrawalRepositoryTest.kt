package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.entity.BookmarkEntity
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.AuthRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.UserApiException
import com.ssafy.e102.eumgil.data.remote.datasource.UserRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.ReissueResponseDto
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
                    authRemoteDataSource = FakeWithdrawalAuthRemoteDataSource(),
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
                    authRemoteDataSource = FakeWithdrawalAuthRemoteDataSource(),
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
                    authRemoteDataSource = FakeWithdrawalAuthRemoteDataSource(),
                    authSessionRepository = authSessionRepository,
                    localDataCleaner = localDataCleaner,
                )

            val result = repository.withdraw()

            assertEquals(AccountWithdrawalResult.AuthenticationFailed, result)
            assertTrue(authSessionRepository.clearAuthSessionCalled)
            assertFalse(localDataCleaner.clearCalled)
        }

    @Test
    fun `withdraw retries once after reissue success and completes with refreshed token`() =
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
            val userRemoteDataSource =
                FakeWithdrawUserRemoteDataSource(
                    queuedResults =
                        listOf(
                            Result.failure(
                                UserApiException(
                                    httpStatusCode = 401,
                                    status = "A4010",
                                    message = "expired",
                                ),
                            ),
                            Result.success("회원탈퇴가 완료되었습니다."),
                        ),
                )
            val authRemoteDataSource =
                FakeWithdrawalAuthRemoteDataSource(
                    reissueResponse =
                        ReissueResponseDto(
                            accessToken = "new-access-token",
                            refreshToken = "new-refresh-token",
                        ),
                )
            val repository =
                ServerAccountWithdrawalRepository(
                    userRemoteDataSource = userRemoteDataSource,
                    authRemoteDataSource = authRemoteDataSource,
                    authSessionRepository = authSessionRepository,
                    localDataCleaner = localDataCleaner,
                )

            val result = repository.withdraw()

            assertEquals(
                listOf("expired-access-token", "new-access-token"),
                userRemoteDataSource.requestedAccessTokens,
            )
            assertEquals("refresh-token", authRemoteDataSource.latestRefreshToken)
            assertTrue(localDataCleaner.clearCalled)
            assertTrue(authSessionRepository.clearAuthSessionCalled)
            assertEquals(
                AccountWithdrawalResult.Success(message = "회원탈퇴가 완료되었습니다."),
                result,
            )
        }

    @Test
    fun `default local data cleaner clears bookmarks and onboarding state`() =
        runTest {
            val bookmarkDao = RecordingBookmarkDao()
            val initSettingsRepository = RecordingInitSettingsRepository()
            val cleaner =
                DefaultAccountWithdrawalLocalDataCleaner(
                    bookmarkDao = bookmarkDao,
                    initSettingsRepository = initSettingsRepository,
                )

            cleaner.clearAfterWithdrawal()

            assertTrue(bookmarkDao.clearBookmarksCalled)
            assertTrue(initSettingsRepository.clearInitSettingsCalled)
        }
}

private class FakeWithdrawUserRemoteDataSource(
    private val withdrawMessage: String? = null,
    private val exception: Throwable? = null,
    private val queuedResults: List<Result<String>> = emptyList(),
) : UserRemoteDataSource(httpJsonClient = HttpJsonClient(baseUrl = "https://example.com")) {
    var latestWithdrawAccessToken: String? = null
        private set
    val requestedAccessTokens = mutableListOf<String>()

    override suspend fun withdraw(accessToken: String): String {
        latestWithdrawAccessToken = accessToken
        requestedAccessTokens += accessToken
        if (queuedResults.isNotEmpty()) {
            val result = queuedResults[requestedAccessTokens.lastIndex]
            return result.getOrThrow()
        }
        exception?.let { throw it }
        return checkNotNull(withdrawMessage)
    }
}

private class FakeWithdrawalAuthRemoteDataSource(
    private val reissueResponse: ReissueResponseDto =
        ReissueResponseDto(
            accessToken = "unused-access-token",
            refreshToken = "unused-refresh-token",
        ),
) : AuthRemoteDataSource(httpJsonClient = HttpJsonClient(baseUrl = "https://example.com")) {
    var latestRefreshToken: String? = null
        private set

    override suspend fun reissue(refreshToken: String): ReissueResponseDto {
        latestRefreshToken = refreshToken
        return reissueResponse
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

private class RecordingBookmarkDao : BookmarkDao {
    var clearBookmarksCalled: Boolean = false
        private set

    override fun observeBookmarks(): Flow<List<BookmarkEntity>> = emptyFlow()

    override fun observeBookmark(placeId: String): Flow<BookmarkEntity?> = emptyFlow()

    override suspend fun getBookmark(placeId: String): BookmarkEntity? = null

    override suspend fun getBookmarkByTargetId(bookmarkTargetId: String): BookmarkEntity? = null

    override suspend fun getBookmarkCount(): Int = 0

    override suspend fun upsertBookmark(bookmark: BookmarkEntity) = Unit

    override suspend fun upsertBookmarks(bookmarks: List<BookmarkEntity>) = Unit

    override suspend fun deleteBookmark(placeId: String) = Unit

    override suspend fun deleteBookmarkByTargetId(bookmarkTargetId: String) = Unit

    override suspend fun clearBookmarks() {
        clearBookmarksCalled = true
    }
}

private class RecordingInitSettingsRepository : InitSettingsRepository {
    var clearInitSettingsCalled: Boolean = false
        private set

    override fun observeInitSettings(): Flow<InitSettings> = emptyFlow()

    override suspend fun getInitSettings(): InitSettings = InitSettings()

    override suspend fun savePrimaryUserType(selectedPrimaryUserType: String) = Unit

    override suspend fun saveMobilitySubtype(selectedMobilitySubtype: String) = Unit

    override suspend fun saveLowVisionFollowUpCompleted(isCompleted: Boolean) = Unit

    override suspend fun saveLocationTermsAgreement(
        isLocationTermsAgreed: Boolean,
        isPrivacyPolicyAgreed: Boolean,
    ) = Unit

    override suspend fun clearInitSettings() {
        clearInitSettingsCalled = true
    }
}
