package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.dto.SocialLoginResponseDto
import org.json.JSONObject

open class AuthRemoteDataSource(
    private val httpJsonClient: HttpJsonClient,
) {
    open suspend fun socialLogin(
        socialProvider: String,
        socialAccessToken: String,
    ): SocialLoginResponseDto {
        val response =
            httpJsonClient.postJson(
                path = "/auth/social-login",
                body =
                    JSONObject()
                        .put("socialProvider", socialProvider)
                        .put("socialAccessToken", socialAccessToken)
                        .toString(),
            )
        val responseJson = response.body.toJsonObjectOrNull()

        if (response.statusCode !in 200..299) {
            throw AuthApiException(
                httpStatusCode = response.statusCode,
                status = responseJson?.optString("status").orEmpty(),
                message =
                    responseJson?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: DEFAULT_AUTH_API_ERROR_MESSAGE,
            )
        }

        val dataJson =
            responseJson?.optJSONObject("data")
                ?: throw AuthApiException(
                    httpStatusCode = response.statusCode,
                    status = responseJson?.optString("status").orEmpty(),
                    message = DEFAULT_AUTH_API_ERROR_MESSAGE,
                )

        return SocialLoginResponseDto(
            signupRequired = dataJson.optBoolean("signupRequired"),
            signupToken = dataJson.optNullableString("signupToken"),
            accessToken = dataJson.optNullableString("accessToken"),
            refreshToken = dataJson.optNullableString("refreshToken"),
            userId = dataJson.optNullableString("userId"),
            selectedPrimaryUserType = dataJson.optNullableString("selectedPrimaryUserType"),
            selectedMobilitySubtype = dataJson.optNullableString("selectedMobilitySubtype"),
        )
    }

    private fun String.toJsonObjectOrNull(): JSONObject? =
        runCatching { JSONObject(this) }.getOrNull()

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) {
            null
        } else {
            optString(name).takeIf { it.isNotBlank() }
        }

    private companion object {
        private const val DEFAULT_AUTH_API_ERROR_MESSAGE = "인증 서버 요청에 실패했습니다."
    }
}

class AuthApiException(
    val httpStatusCode: Int,
    val status: String,
    override val message: String,
) : RuntimeException(message)
