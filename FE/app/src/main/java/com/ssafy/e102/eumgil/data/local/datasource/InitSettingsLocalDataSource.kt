package com.ssafy.e102.eumgil.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.local.datastore.InitSettingsPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class InitSettingsLocalDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    fun observeInitSettings(): Flow<InitSettings> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                InitSettings(
                    disabilityType = preferences[InitSettingsPreferences.disabilityType],
                    disabilityLevel = preferences[InitSettingsPreferences.disabilityLevel],
                    isLocationTermsAgreed =
                        preferences[InitSettingsPreferences.isLocationTermsAgreed] ?: false,
                    isPrivacyPolicyAgreed =
                        preferences[InitSettingsPreferences.isPrivacyPolicyAgreed] ?: false,
                )
            }

    suspend fun getInitSettings(): InitSettings = observeInitSettings().first()

    suspend fun saveDisabilityType(disabilityType: String) {
        dataStore.edit { preferences ->
            val currentType = preferences[InitSettingsPreferences.disabilityType]

            preferences[InitSettingsPreferences.disabilityType] = disabilityType
            if (currentType != disabilityType) {
                preferences.remove(InitSettingsPreferences.disabilityLevel)
                preferences.remove(InitSettingsPreferences.isLocationTermsAgreed)
                preferences.remove(InitSettingsPreferences.isPrivacyPolicyAgreed)
            }
        }
    }

    suspend fun saveDisabilityLevel(disabilityLevel: String) {
        dataStore.edit { preferences ->
            preferences[InitSettingsPreferences.disabilityLevel] = disabilityLevel
        }
    }

    suspend fun saveLocationTermsAgreement(
        isLocationTermsAgreed: Boolean,
        isPrivacyPolicyAgreed: Boolean,
    ) {
        dataStore.edit { preferences ->
            preferences[InitSettingsPreferences.isLocationTermsAgreed] = isLocationTermsAgreed
            preferences[InitSettingsPreferences.isPrivacyPolicyAgreed] = isPrivacyPolicyAgreed
        }
    }
}
