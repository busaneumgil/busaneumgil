package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.remote.dto.UserMeResponseDto
import org.json.JSONObject

open class UserRemoteDataSource(
    private val httpJsonClient: HttpJsonClient,
) {
    open suspend fun getMe(accessToken: String): UserMeResponseDto {
        val response =
            httpJsonClient.getJson(
                path = "/users/me",
                headers =
                    mapOf(
                        AUTHORIZATION_HEADER_NAME to "$BEARER_PREFIX $accessToken",
                    ),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        val dataJson = response.requireDataJson(responseJson)

        return UserMeResponseDto(
            userId = dataJson.optNullableString("userId"),
            socialProvider = dataJson.optNullableString("socialProvider"),
            selectedPrimaryUserType = dataJson.optNullableString("selectedPrimaryUserType"),
            selectedMobilitySubtype = dataJson.optNullableString("selectedMobilitySubtype"),
        )
    }

    private fun String.toJsonObjectOrNull(): JSONObject? =
        runCatching { JSONObject(this) }.getOrNull()

    private fun HttpJsonResponse.requireDataJson(responseJson: JSONObject?): JSONObject {
        if (statusCode !in 200..299) {
            throw UserApiException(
                httpStatusCode = statusCode,
                status = responseJson?.optString("status").orEmpty(),
                message =
                    responseJson?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: DEFAULT_USER_API_ERROR_MESSAGE,
            )
        }

        return responseJson?.optJSONObject("data")
            ?: throw UserApiException(
                httpStatusCode = statusCode,
                status = responseJson?.optString("status").orEmpty(),
                message = DEFAULT_USER_API_ERROR_MESSAGE,
            )
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) {
            null
        } else {
            optString(name).takeIf { it.isNotBlank() }
        }

    private companion object {
        private const val AUTHORIZATION_HEADER_NAME = "Authorization"
        private const val BEARER_PREFIX = "Bearer"
        private const val DEFAULT_USER_API_ERROR_MESSAGE = "User profile request failed."
    }
}

class UserApiException(
    val httpStatusCode: Int,
    val status: String,
    override val message: String,
) : RuntimeException(message)
