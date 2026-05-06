package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.data.remote.datasource.UserApiException
import com.ssafy.e102.eumgil.data.remote.datasource.UserRemoteDataSource

data class UserProfile(
    val userId: String?,
    val socialProvider: String?,
    val selectedPrimaryUserType: String?,
    val selectedMobilitySubtype: String?,
)

sealed interface UserProfileSyncResult {
    data class Success(
        val profile: UserProfile,
    ) : UserProfileSyncResult

    data object MissingSession : UserProfileSyncResult

    data object AuthenticationFailed : UserProfileSyncResult

    data class Failure(
        val message: String,
    ) : UserProfileSyncResult
}

interface UserProfileRepository {
    suspend fun syncMyProfile(): UserProfileSyncResult
}

class LocalOnlyUserProfileRepository : UserProfileRepository {
    override suspend fun syncMyProfile(): UserProfileSyncResult =
        UserProfileSyncResult.Success(
            profile =
                UserProfile(
                    userId = null,
                    socialProvider = null,
                    selectedPrimaryUserType = null,
                    selectedMobilitySubtype = null,
                ),
        )
}

class ServerUserProfileRepository(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val authSessionRepository: AuthSessionRepository,
    private val settingsRepository: SettingsRepository,
) : UserProfileRepository {
    override suspend fun syncMyProfile(): UserProfileSyncResult {
        val authGateState = authSessionRepository.getAuthGateState()
        val authSession = authGateState.authSession ?: return UserProfileSyncResult.MissingSession

        return try {
            val response = userRemoteDataSource.getMe(accessToken = authSession.accessToken)
            val synchronizedSession =
                authSession.mergeUserProfile(
                    userId = response.userId,
                    selectedPrimaryUserType = response.selectedPrimaryUserType,
                    selectedMobilitySubtype = response.selectedMobilitySubtype,
                )

            authSessionRepository.saveAuthSession(
                authSession = synchronizedSession,
                isProfileCompleted =
                    authGateState.isProfileCompleted || response.selectedPrimaryUserType != null,
            )
            response.selectedPrimaryUserType?.let { selectedPrimaryUserType ->
                settingsRepository.syncOnboardingStateFromServer(
                    selectedPrimaryUserType = selectedPrimaryUserType,
                    selectedMobilitySubtype = response.selectedMobilitySubtype,
                )
            }

            UserProfileSyncResult.Success(
                profile =
                    UserProfile(
                        userId = synchronizedSession.userId,
                        socialProvider = response.socialProvider,
                        selectedPrimaryUserType = synchronizedSession.selectedPrimaryUserType,
                        selectedMobilitySubtype = synchronizedSession.selectedMobilitySubtype,
                    ),
            )
        } catch (exception: UserApiException) {
            if (exception.httpStatusCode == HTTP_UNAUTHORIZED || exception.httpStatusCode == HTTP_FORBIDDEN) {
                UserProfileSyncResult.AuthenticationFailed
            } else {
                UserProfileSyncResult.Failure(message = exception.message)
            }
        } catch (exception: Exception) {
            UserProfileSyncResult.Failure(
                message = exception.message ?: DEFAULT_USER_PROFILE_SYNC_ERROR_MESSAGE,
            )
        }
    }

    private fun AuthSession.mergeUserProfile(
        userId: String?,
        selectedPrimaryUserType: String?,
        selectedMobilitySubtype: String?,
    ): AuthSession {
        val synchronizedPrimaryUserType = selectedPrimaryUserType ?: this.selectedPrimaryUserType
        val synchronizedMobilitySubtype =
            if (selectedPrimaryUserType == null) {
                this.selectedMobilitySubtype
            } else {
                selectedMobilitySubtype
            }

        return copy(
            userId = userId ?: this.userId,
            selectedPrimaryUserType = synchronizedPrimaryUserType,
            selectedMobilitySubtype = synchronizedMobilitySubtype,
        )
    }

    private companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val DEFAULT_USER_PROFILE_SYNC_ERROR_MESSAGE = "Failed to synchronize user profile."
    }
}
