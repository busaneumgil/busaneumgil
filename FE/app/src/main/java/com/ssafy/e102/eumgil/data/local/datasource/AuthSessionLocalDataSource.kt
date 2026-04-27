package com.ssafy.e102.eumgil.data.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.data.local.datastore.AuthSessionPreferences
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
                val accessToken = preferences[AuthSessionPreferences.accessToken]
                AuthGateState(
                    authSession =
                        accessToken?.let {
                            AuthSession(
                                accessToken = it,
                                refreshToken = preferences[AuthSessionPreferences.refreshToken],
                            )
                        },
                    isProfileCompleted =
                        accessToken != null &&
                            (preferences[AuthSessionPreferences.isProfileCompleted] ?: false),
                )
            }

    suspend fun getAuthGateState(): AuthGateState = observeAuthGateState().first()

    suspend fun saveAuthSession(
        authSession: AuthSession,
        isProfileCompleted: Boolean,
    ) {
        dataStore.edit { preferences ->
            preferences[AuthSessionPreferences.accessToken] = authSession.accessToken
            authSession.refreshToken?.let { refreshToken ->
                preferences[AuthSessionPreferences.refreshToken] = refreshToken
            } ?: preferences.remove(AuthSessionPreferences.refreshToken)
            preferences[AuthSessionPreferences.isProfileCompleted] = isProfileCompleted
        }
    }

    suspend fun markProfileCompleted() {
        dataStore.edit { preferences ->
            if (preferences[AuthSessionPreferences.accessToken] != null) {
                preferences[AuthSessionPreferences.isProfileCompleted] = true
            }
        }
    }

    suspend fun clearAuthSession() {
        dataStore.edit { preferences ->
            preferences.remove(AuthSessionPreferences.accessToken)
            preferences.remove(AuthSessionPreferences.refreshToken)
            preferences.remove(AuthSessionPreferences.isProfileCompleted)
        }
    }
}
