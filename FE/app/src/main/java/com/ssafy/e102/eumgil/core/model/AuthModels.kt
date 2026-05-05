package com.ssafy.e102.eumgil.core.model

data class AuthSession
    @JvmOverloads
    constructor(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String? = null,
    val selectedPrimaryUserType: String? = null,
    val selectedMobilitySubtype: String? = null,
)

data class AuthGateState
    @JvmOverloads
    constructor(
    val authSession: AuthSession? = null,
    val isProfileCompleted: Boolean = false,
    val signupToken: String? = null,
) {
    val hasSession: Boolean
        get() = authSession != null

    val hasPendingSignup: Boolean
        get() = signupToken != null
}
