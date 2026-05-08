package com.ssafy.e102.eumgil.data.local.datasource

import com.ssafy.e102.eumgil.data.local.dao.AppSettingDao
import com.ssafy.e102.eumgil.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebugSettingsLocalDataSource(
    private val appSettingDao: AppSettingDao,
) {
    fun observeForceMockEnabled(): Flow<Boolean> =
        appSettingDao.observeSetting(DEBUG_FORCE_MOCK_KEY).map { setting ->
            setting?.booleanValue == true
        }

    suspend fun getForceMockEnabled(): Boolean =
        appSettingDao.getSetting(DEBUG_FORCE_MOCK_KEY)?.booleanValue == true

    suspend fun setForceMockEnabled(isEnabled: Boolean) {
        appSettingDao.upsertSetting(
            AppSettingEntity(
                settingKey = DEBUG_FORCE_MOCK_KEY,
                booleanValue = isEnabled,
            ),
        )
    }

    companion object {
        private const val DEBUG_FORCE_MOCK_KEY: String = "debug.repository.force_mock"
    }
}
