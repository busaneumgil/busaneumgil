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
                    selectedPrimaryUserType =
                        preferences[InitSettingsPreferences.selectedPrimaryUserType],
                    selectedMobilitySubtype =
                        preferences[InitSettingsPreferences.selectedMobilitySubtype],
                    isLowVisionFollowUpCompleted =
                        preferences[InitSettingsPreferences.isLowVisionFollowUpCompleted] ?: false,
                    isLocationTermsAgreed =
                        preferences[InitSettingsPreferences.isLocationTermsAgreed] ?: false,
                    isPrivacyPolicyAgreed =
                        preferences[InitSettingsPreferences.isPrivacyPolicyAgreed] ?: false,
                )
            }

    suspend fun getInitSettings(): InitSettings = observeInitSettings().first()

    suspend fun savePrimaryUserType(selectedPrimaryUserType: String) {
        dataStore.edit { preferences ->
            val currentType = preferences[InitSettingsPreferences.selectedPrimaryUserType]

            preferences[InitSettingsPreferences.selectedPrimaryUserType] = selectedPrimaryUserType
            if (currentType != selectedPrimaryUserType || selectedPrimaryUserType == LOW_VISION_ROUTE_VALUE) {
                preferences.remove(InitSettingsPreferences.selectedMobilitySubtype)
            }
            if (selectedPrimaryUserType != LOW_VISION_ROUTE_VALUE || currentType != selectedPrimaryUserType) {
                preferences.remove(InitSettingsPreferences.isLowVisionFollowUpCompleted)
            }
        }
    }

    suspend fun saveMobilitySubtype(selectedMobilitySubtype: String) {
        dataStore.edit { preferences ->
            preferences[InitSettingsPreferences.selectedMobilitySubtype] = selectedMobilitySubtype
        }
    }

    suspend fun saveLowVisionFollowUpCompleted(isCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[InitSettingsPreferences.isLowVisionFollowUpCompleted] = isCompleted
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

    suspend fun clearInitSettings() {
        dataStore.edit { preferences ->
            preferences.remove(InitSettingsPreferences.selectedPrimaryUserType)
            preferences.remove(InitSettingsPreferences.selectedMobilitySubtype)
            preferences.remove(InitSettingsPreferences.isLowVisionFollowUpCompleted)
            preferences.remove(InitSettingsPreferences.isLocationTermsAgreed)
            preferences.remove(InitSettingsPreferences.isPrivacyPolicyAgreed)
        }
    }

    private companion object {
        private const val LOW_VISION_ROUTE_VALUE = "low_vision"
    }
}
