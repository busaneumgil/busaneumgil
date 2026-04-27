package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.AuthSessionSnapshot
import com.ssafy.e102.eumgil.core.model.AuthSessionSource

interface AuthSessionRepository {
    suspend fun getAuthSessionSnapshot(): AuthSessionSnapshot

    suspend fun markProfileCompleted()
}

class LocalMockAuthSessionRepository(
    initialSnapshot: AuthSessionSnapshot = AuthSessionSnapshot.LocalMockReady,
) : AuthSessionRepository {
    private var snapshot: AuthSessionSnapshot =
        initialSnapshot.copy(source = AuthSessionSource.LOCAL_MOCK)

    override suspend fun getAuthSessionSnapshot(): AuthSessionSnapshot = snapshot

    override suspend fun markProfileCompleted() {
        snapshot =
            snapshot.copy(
                isAuthenticated = true,
                isProfileCompleted = true,
                source = AuthSessionSource.LOCAL_MOCK,
            )
    }
}
