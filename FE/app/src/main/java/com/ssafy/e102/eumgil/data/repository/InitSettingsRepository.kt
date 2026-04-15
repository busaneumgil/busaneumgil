package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import kotlinx.coroutines.flow.Flow

interface InitSettingsRepository {
    fun observeInitSettings(): Flow<InitSettings>

    suspend fun getInitSettings(): InitSettings

    suspend fun saveDisabilityType(disabilityType: String)

    suspend fun saveDisabilityLevel(disabilityLevel: String)

    suspend fun saveLocationTermsAgreement(
        isLocationTermsAgreed: Boolean,
        isPrivacyPolicyAgreed: Boolean,
    )
}

class DefaultInitSettingsRepository(
    private val localDataSource: InitSettingsLocalDataSource,
) : InitSettingsRepository {
    override fun observeInitSettings(): Flow<InitSettings> = localDataSource.observeInitSettings()

    override suspend fun getInitSettings(): InitSettings = localDataSource.getInitSettings()

    override suspend fun saveDisabilityType(disabilityType: String) {
        localDataSource.saveDisabilityType(disabilityType)
    }

    override suspend fun saveDisabilityLevel(disabilityLevel: String) {
        localDataSource.saveDisabilityLevel(disabilityLevel)
    }

    override suspend fun saveLocationTermsAgreement(
        isLocationTermsAgreed: Boolean,
        isPrivacyPolicyAgreed: Boolean,
    ) {
        localDataSource.saveLocationTermsAgreement(
            isLocationTermsAgreed = isLocationTermsAgreed,
            isPrivacyPolicyAgreed = isPrivacyPolicyAgreed,
        )
    }
}
