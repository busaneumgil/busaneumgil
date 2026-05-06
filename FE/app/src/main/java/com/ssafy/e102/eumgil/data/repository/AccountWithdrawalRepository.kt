package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.UserApiException
import com.ssafy.e102.eumgil.data.remote.datasource.UserRemoteDataSource

sealed interface AccountWithdrawalResult {
    data class Success(
        val message: String,
    ) : AccountWithdrawalResult

    data object MissingSession : AccountWithdrawalResult

    data object AuthenticationFailed : AccountWithdrawalResult

    data class Failure(
        val message: String,
    ) : AccountWithdrawalResult
}

interface AccountWithdrawalRepository {
    suspend fun withdraw(): AccountWithdrawalResult
}

interface AccountWithdrawalLocalDataCleaner {
    suspend fun clearAfterWithdrawal()
}

fun provideAccountWithdrawalRepository(
    baseUrl: String,
    authSessionRepository: AuthSessionRepository,
    initSettingsRepository: InitSettingsRepository,
    bookmarkDao: BookmarkDao,
    isMockMode: Boolean,
): AccountWithdrawalRepository {
    val localDataCleaner =
        DefaultAccountWithdrawalLocalDataCleaner(
            bookmarkDao = bookmarkDao,
            initSettingsRepository = initSettingsRepository,
        )

    return if (isMockMode) {
        LocalOnlyAccountWithdrawalRepository(
            authSessionRepository = authSessionRepository,
            localDataCleaner = localDataCleaner,
        )
    } else {
        ServerAccountWithdrawalRepository(
            userRemoteDataSource = UserRemoteDataSource(httpJsonClient = HttpJsonClient(baseUrl = baseUrl)),
            authSessionRepository = authSessionRepository,
            localDataCleaner = localDataCleaner,
        )
    }
}

class DefaultAccountWithdrawalLocalDataCleaner(
    private val bookmarkDao: BookmarkDao,
    private val initSettingsRepository: InitSettingsRepository,
) : AccountWithdrawalLocalDataCleaner {
    override suspend fun clearAfterWithdrawal() {
        bookmarkDao.clearBookmarks()
        initSettingsRepository.clearInitSettings()
    }
}

class LocalOnlyAccountWithdrawalRepository(
    private val authSessionRepository: AuthSessionRepository,
    private val localDataCleaner: AccountWithdrawalLocalDataCleaner,
) : AccountWithdrawalRepository {
    override suspend fun withdraw(): AccountWithdrawalResult {
        if (authSessionRepository.getAuthGateState().authSession == null) {
            return AccountWithdrawalResult.MissingSession
        }

        runCatching { localDataCleaner.clearAfterWithdrawal() }
        authSessionRepository.clearAuthSession()

        return AccountWithdrawalResult.Success(message = DEFAULT_WITHDRAW_SUCCESS_MESSAGE)
    }
}

class ServerAccountWithdrawalRepository(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val authSessionRepository: AuthSessionRepository,
    private val localDataCleaner: AccountWithdrawalLocalDataCleaner,
) : AccountWithdrawalRepository {
    override suspend fun withdraw(): AccountWithdrawalResult {
        val authSession = authSessionRepository.getAuthGateState().authSession ?: return AccountWithdrawalResult.MissingSession

        return try {
            val message = userRemoteDataSource.withdraw(accessToken = authSession.accessToken)
            runCatching { localDataCleaner.clearAfterWithdrawal() }
            authSessionRepository.clearAuthSession()
            AccountWithdrawalResult.Success(message = message)
        } catch (exception: UserApiException) {
            if (exception.httpStatusCode == HTTP_UNAUTHORIZED || exception.httpStatusCode == HTTP_FORBIDDEN) {
                authSessionRepository.clearAuthSession()
                AccountWithdrawalResult.AuthenticationFailed
            } else {
                AccountWithdrawalResult.Failure(message = exception.message)
            }
        } catch (exception: Exception) {
            AccountWithdrawalResult.Failure(
                message = exception.message ?: DEFAULT_WITHDRAW_ERROR_MESSAGE,
            )
        }
    }
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val DEFAULT_WITHDRAW_SUCCESS_MESSAGE = "회원탈퇴가 완료되었습니다."
private const val DEFAULT_WITHDRAW_ERROR_MESSAGE = "회원탈퇴 처리에 실패했습니다. 다시 시도해주세요."
