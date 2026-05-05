package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.data.remote.datasource.AuthRemoteDataSource

interface AuthSignupRepository {
    suspend fun completePendingSignup(requiredTermsAccepted: Boolean)
}

class LocalOnlyAuthSignupRepository : AuthSignupRepository {
    override suspend fun completePendingSignup(requiredTermsAccepted: Boolean) = Unit
}

class ServerAuthSignupRepository(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val authSessionRepository: AuthSessionRepository,
    private val settingsRepository: SettingsRepository,
) : AuthSignupRepository {
    override suspend fun completePendingSignup(requiredTermsAccepted: Boolean) {
        val authGateState = authSessionRepository.getAuthGateState()
        val signupToken = authGateState.signupToken ?: return
        val initSettings = settingsRepository.getInitSettings()
        val selectedPrimaryUserType =
            initSettings.selectedPrimaryUserType
                ?: throw IllegalStateException("사용자 유형을 먼저 선택해주세요.")
        val selectedMobilitySubtype =
            when (selectedPrimaryUserType) {
                ROUTE_PRIMARY_USER_TYPE_LOW_VISION -> null
                ROUTE_PRIMARY_USER_TYPE_MOBILITY_IMPAIRED ->
                    initSettings.selectedMobilitySubtype
                        ?: throw IllegalStateException("보행약자 세부 유형을 먼저 선택해주세요.")
                else -> throw IllegalStateException("지원하지 않는 사용자 유형입니다.")
            }

        val response =
            authRemoteDataSource.signup(
                signupToken = signupToken,
                selectedPrimaryUserType = selectedPrimaryUserType.toPrimaryUserTypeServerValue(),
                selectedMobilitySubtype = selectedMobilitySubtype?.toMobilitySubtypeServerValue(),
                requiredTermsAccepted = requiredTermsAccepted,
            )

        authSessionRepository.saveAuthSession(
            authSession =
                AuthSession(
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    userId = response.userId,
                    selectedPrimaryUserType = response.selectedPrimaryUserType,
                    selectedMobilitySubtype = response.selectedMobilitySubtype,
                ),
            isProfileCompleted = true,
        )
        settingsRepository.syncOnboardingStateFromServer(
            selectedPrimaryUserType = response.selectedPrimaryUserType,
            selectedMobilitySubtype = response.selectedMobilitySubtype,
        )
    }
}
