package com.ssafy.e102.eumgil.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private const val INIT_SETTINGS_DATASTORE_NAME: String = "init_settings"

val Context.initSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = INIT_SETTINGS_DATASTORE_NAME,
)

object InitSettingsPreferences {
    val disabilityType = stringPreferencesKey("disability_type")
    val disabilityLevel = stringPreferencesKey("disability_level")
    val isLocationTermsAgreed = booleanPreferencesKey("location_terms_agreed")
    val isPrivacyPolicyAgreed = booleanPreferencesKey("privacy_policy_agreed")
}
