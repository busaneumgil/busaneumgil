package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import kotlinx.coroutines.flow.Flow

interface SettingsRepository : InitSettingsRepository

class DefaultSettingsRepository(
    private val initSettingsLocalDataSource: InitSettingsLocalDataSource,
) : SettingsRepository {

    override fun observeInitSettings(): Flow<InitSettings> = initSettingsLocalDataSource.observeInitSettings()

    override suspend fun getInitSettings(): InitSettings = initSettingsLocalDataSource.getInitSettings()

    override suspend fun savePrimaryUserType(selectedPrimaryUserType: String) {
        initSettingsLocalDataSource.savePrimaryUserType(selectedPrimaryUserType)
    }

    override suspend fun saveMobilitySubtype(selectedMobilitySubtype: String) {
        initSettingsLocalDataSource.saveMobilitySubtype(selectedMobilitySubtype)
    }

    override suspend fun saveLowVisionFollowUpCompleted(isCompleted: Boolean) {
        initSettingsLocalDataSource.saveLowVisionFollowUpCompleted(isCompleted = isCompleted)
    }

    override suspend fun saveLocationTermsAgreement(
        isLocationTermsAgreed: Boolean,
        isPrivacyPolicyAgreed: Boolean,
    ) {
        initSettingsLocalDataSource.saveLocationTermsAgreement(
            isLocationTermsAgreed = isLocationTermsAgreed,
            isPrivacyPolicyAgreed = isPrivacyPolicyAgreed,
        )
    }

    override suspend fun clearInitSettings() {
        initSettingsLocalDataSource.clearInitSettings()
    }
}
