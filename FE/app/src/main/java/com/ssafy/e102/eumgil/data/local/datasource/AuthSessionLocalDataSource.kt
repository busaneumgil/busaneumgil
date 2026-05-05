package com.ssafy.e102.eumgil.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AuthSessionLocalDataSource(
    private val dataStore: DataStore<Preferences>,
) {
    fun observeAuthGateState(): Flow<AuthGateState> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                val accessToken = preferences[AuthSessionPreferenceKeys.accessToken]
                AuthGateState(
                    authSession =
                        accessToken?.let {
                            AuthSession(
                                accessToken = it,
                                refreshToken = preferences[AuthSessionPreferenceKeys.refreshToken],
                                userId = preferences[AuthSessionPreferenceKeys.userId],
                                selectedPrimaryUserType =
                                    preferences[AuthSessionPreferenceKeys.selectedPrimaryUserType],
                                selectedMobilitySubtype =
                                    preferences[AuthSessionPreferenceKeys.selectedMobilitySubtype],
                            )
                        },
                    isProfileCompleted =
                        accessToken != null &&
                            (preferences[AuthSessionPreferenceKeys.isProfileCompleted] ?: false),
                    signupToken = preferences[AuthSessionPreferenceKeys.signupToken],
                )
            }

    suspend fun getAuthGateState(): AuthGateState = observeAuthGateState().first()

    suspend fun saveAuthSession(
        authSession: AuthSession,
        isProfileCompleted: Boolean,
    ) {
        dataStore.edit { preferences ->
            preferences[AuthSessionPreferenceKeys.accessToken] = authSession.accessToken
            authSession.refreshToken?.let { refreshToken ->
                preferences[AuthSessionPreferenceKeys.refreshToken] = refreshToken
            } ?: preferences.remove(AuthSessionPreferenceKeys.refreshToken)
            authSession.userId?.let { userId ->
                preferences[AuthSessionPreferenceKeys.userId] = userId
            } ?: preferences.remove(AuthSessionPreferenceKeys.userId)
            authSession.selectedPrimaryUserType?.let { selectedPrimaryUserType ->
                preferences[AuthSessionPreferenceKeys.selectedPrimaryUserType] = selectedPrimaryUserType
            } ?: preferences.remove(AuthSessionPreferenceKeys.selectedPrimaryUserType)
            authSession.selectedMobilitySubtype?.let { selectedMobilitySubtype ->
                preferences[AuthSessionPreferenceKeys.selectedMobilitySubtype] = selectedMobilitySubtype
            } ?: preferences.remove(AuthSessionPreferenceKeys.selectedMobilitySubtype)
            preferences[AuthSessionPreferenceKeys.isProfileCompleted] = isProfileCompleted
            preferences.remove(AuthSessionPreferenceKeys.signupToken)
        }
    }

    suspend fun saveSignupToken(signupToken: String) {
        dataStore.edit { preferences ->
            preferences[AuthSessionPreferenceKeys.signupToken] = signupToken
            preferences.remove(AuthSessionPreferenceKeys.accessToken)
            preferences.remove(AuthSessionPreferenceKeys.refreshToken)
            preferences.remove(AuthSessionPreferenceKeys.userId)
            preferences.remove(AuthSessionPreferenceKeys.selectedPrimaryUserType)
            preferences.remove(AuthSessionPreferenceKeys.selectedMobilitySubtype)
            preferences.remove(AuthSessionPreferenceKeys.isProfileCompleted)
        }
    }

    suspend fun clearSignupToken() {
        dataStore.edit { preferences ->
            preferences.remove(AuthSessionPreferenceKeys.signupToken)
        }
    }

    suspend fun markProfileCompleted() {
        dataStore.edit { preferences ->
            if (preferences[AuthSessionPreferenceKeys.accessToken] != null) {
                preferences[AuthSessionPreferenceKeys.isProfileCompleted] = true
            }
        }
    }

    suspend fun clearAuthSession() {
        dataStore.edit { preferences ->
            preferences.remove(AuthSessionPreferenceKeys.accessToken)
            preferences.remove(AuthSessionPreferenceKeys.refreshToken)
            preferences.remove(AuthSessionPreferenceKeys.userId)
            preferences.remove(AuthSessionPreferenceKeys.selectedPrimaryUserType)
            preferences.remove(AuthSessionPreferenceKeys.selectedMobilitySubtype)
            preferences.remove(AuthSessionPreferenceKeys.isProfileCompleted)
            preferences.remove(AuthSessionPreferenceKeys.signupToken)
        }
    }
}

private object AuthSessionPreferenceKeys {
    val accessToken = stringPreferencesKey("access_token")
    val refreshToken = stringPreferencesKey("refresh_token")
    val userId = stringPreferencesKey("user_id")
    val selectedPrimaryUserType = stringPreferencesKey("selected_primary_user_type")
    val selectedMobilitySubtype = stringPreferencesKey("selected_mobility_subtype")
    val isProfileCompleted = booleanPreferencesKey("profile_completed")
    val signupToken = stringPreferencesKey("signup_token")
}
