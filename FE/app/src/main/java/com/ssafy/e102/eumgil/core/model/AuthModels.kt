package com.ssafy.e102.eumgil.core.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
)

data class AuthGateState(
    val authSession: AuthSession? = null,
    val isProfileCompleted: Boolean = false,
) {
    val hasSession: Boolean
        get() = authSession != null
}
