package com.ssafy.e102.eumgil.core.model

import java.security.MessageDigest

private const val LOCAL_ONLY_ACCOUNT_SCOPE_KEY: String = "session::local-only"

fun AuthSession.resolveAccountScopeKey(): String {
    userId?.trim()?.takeIf(String::isNotEmpty)?.let { userId ->
        return "user::$userId"
    }
    if (accessToken == LOCAL_ONLY_AUTH_SESSION_MARKER) {
        return LOCAL_ONLY_ACCOUNT_SCOPE_KEY
    }
    refreshToken?.trim()?.takeIf(String::isNotEmpty)?.let { refreshToken ->
        return "session::${refreshToken.sha256Prefix()}"
    }
    accessToken.trim().takeIf(String::isNotEmpty)?.let { accessToken ->
        return "session::${accessToken.sha256Prefix()}"
    }
    return LOCAL_ONLY_ACCOUNT_SCOPE_KEY
}

private fun String.sha256Prefix(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return buildString(capacity = 16) {
        repeat(8) { index ->
            append("%02x".format(digest[index]))
        }
    }
}
