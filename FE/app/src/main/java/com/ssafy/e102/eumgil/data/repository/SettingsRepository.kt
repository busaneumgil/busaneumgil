package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.core.model.RepositoryDebugSettings
import com.ssafy.e102.eumgil.data.local.datasource.DebugSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.datasource.InitSettingsLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository : InitSettingsRepository {
    fun observeRepositoryDebugSettings(): Flow<RepositoryDebugSettings>

    suspend fun getRepositoryDebugSettings(): RepositoryDebugSettings

    suspend fun setForceMockEnabled(isEnabled: Boolean)
}

class DefaultSettingsRepository(
    private val initSettingsLocalDataSource: InitSettingsLocalDataSource,
    debugSettingsLocalDataSourceProvider: () -> DebugSettingsLocalDataSource,
) : SettingsRepository {
    private val debugSettingsLocalDataSource by lazy(
        LazyThreadSafetyMode.NONE,
        debugSettingsLocalDataSourceProvider,
    )

    override fun observeInitSettings(): Flow<InitSettings> = initSettingsLocalDataSource.observeInitSettings()

    override suspend fun getInitSettings(): InitSettings = initSettingsLocalDataSource.getInitSettings()

    override suspend fun saveDisabilityType(disabilityType: String) {
        initSettingsLocalDataSource.saveDisabilityType(disabilityType)
    }

    override suspend fun saveDisabilityLevel(disabilityLevel: String) {
        initSettingsLocalDataSource.saveDisabilityLevel(disabilityLevel)
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

    override fun observeRepositoryDebugSettings(): Flow<RepositoryDebugSettings> =
        debugSettingsLocalDataSource
            .observeForceMockEnabled()
            .map(::toRepositoryDebugSettings)

    override suspend fun getRepositoryDebugSettings(): RepositoryDebugSettings =
        toRepositoryDebugSettings(debugSettingsLocalDataSource.getForceMockEnabled())

    override suspend fun setForceMockEnabled(isEnabled: Boolean) {
        if (!AppEnvironment.isDebugBuild || AppEnvironment.isMockMode) return
        debugSettingsLocalDataSource.setForceMockEnabled(isEnabled)
    }

    private fun toRepositoryDebugSettings(isStoredForceMockEnabled: Boolean): RepositoryDebugSettings =
        RepositoryDebugSettings(
            isRuntimeToggleAvailable = AppEnvironment.isDebugBuild,
            isRuntimeToggleEnabled = AppEnvironment.isDebugBuild && !AppEnvironment.isMockMode,
            isForceMockEnabled =
                AppEnvironment.isMockMode ||
                    (AppEnvironment.isDebugBuild && isStoredForceMockEnabled),
        )
}
