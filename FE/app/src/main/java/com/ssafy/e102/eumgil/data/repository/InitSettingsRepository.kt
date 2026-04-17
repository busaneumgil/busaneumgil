package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.InitSettings
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
