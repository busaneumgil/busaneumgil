package com.ssafy.e102.eumgil.data.repository.policy

import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.data.local.dao.AppSettingDao
import com.ssafy.e102.eumgil.data.local.datasource.DebugSettingsLocalDataSource
import com.ssafy.e102.eumgil.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RepositorySourcePolicyTest {
    @Test
    fun `settings domain always uses local only plan`() =
        runBlocking {
            val policy =
                DefaultRepositorySourcePolicy(
                    debugSettingsLocalDataSource = DebugSettingsLocalDataSource(FakeAppSettingDao()),
                )

            assertEquals(
                RepositoryReadPlan.localOnly(),
                policy.readPlan(RepositoryDomain.SETTINGS),
            )
        }

    @Test
    fun `places and search domains switch between live fallback and mock only`() =
        runBlocking {
            val debugSettingsLocalDataSource = DebugSettingsLocalDataSource(FakeAppSettingDao())
            val policy =
                DefaultRepositorySourcePolicy(
                    debugSettingsLocalDataSource = debugSettingsLocalDataSource,
                )

            val expectedDefaultPlan =
                if (AppEnvironment.isMockMode) {
                    RepositoryReadPlan.mockOnly()
                } else {
                    RepositoryReadPlan.remoteLocalMock()
                }

            assertEquals(expectedDefaultPlan, policy.readPlan(RepositoryDomain.PLACES))
            assertEquals(expectedDefaultPlan, policy.readPlan(RepositoryDomain.SEARCH))

            debugSettingsLocalDataSource.setForceMockEnabled(true)

            assertEquals(
                RepositoryReadPlan.mockOnly(),
                policy.readPlan(RepositoryDomain.PLACES),
            )
            assertEquals(
                RepositoryReadPlan.mockOnly(),
                policy.readPlan(RepositoryDomain.SEARCH),
            )
        }
}

private class FakeAppSettingDao : AppSettingDao {
    private val settingsState = MutableStateFlow<Map<String, AppSettingEntity>>(emptyMap())

    override fun observeSettings(): Flow<List<AppSettingEntity>> =
        settingsState.map { settings ->
            settings.values.sortedBy(AppSettingEntity::settingKey)
        }

    override fun observeSetting(settingKey: String): Flow<AppSettingEntity?> =
        settingsState.map { settings -> settings[settingKey] }

    override suspend fun getSetting(settingKey: String): AppSettingEntity? = settingsState.value[settingKey]

    override suspend fun upsertSetting(setting: AppSettingEntity) {
        settingsState.value = settingsState.value + (setting.settingKey to setting)
    }

    override suspend fun upsertSettings(settings: List<AppSettingEntity>) {
        settingsState.value =
            buildMap {
                putAll(settingsState.value)
                settings.forEach { setting -> put(setting.settingKey, setting) }
            }
    }

    override suspend fun deleteSetting(settingKey: String) {
        settingsState.value = settingsState.value - settingKey
    }

    override suspend fun clearSettings() {
        settingsState.value = emptyMap()
    }
}
