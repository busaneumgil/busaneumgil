package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.route.RoutePointDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.parseRouteSearchResponseDto
import org.json.JSONObject

open class RouteRemoteDataSource internal constructor(
    private val postRequestExecutor: suspend (String, String, Map<String, String>) -> HttpJsonResponse,
    private val accessTokenProvider: suspend () -> String? = { null },
) {
    constructor(
        baseUrl: String,
        accessTokenProvider: suspend () -> String? = { null },
    ) : this(
        postRequestExecutor = { path: String, body: String, headers: Map<String, String> ->
            HttpJsonClient(baseUrl = baseUrl).postJson(
                path = path,
                body = body,
                headers = headers,
            )
        },
        accessTokenProvider = accessTokenProvider,
    )

    open suspend fun searchWalkRoutes(request: RouteSearchRequestDto): RouteSearchResponseDto {
        val response =
            postRequestExecutor(
                "/routes/search/walk",
                createWalkSearchBody(request),
                bearerHeader(),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        return response.requireRouteSearchResponseDto(responseJson)
    }

    private suspend fun bearerHeader(): Map<String, String> =
        accessTokenProvider()
            ?.takeIf { accessToken -> accessToken.isNotBlank() }
            ?.let { accessToken -> mapOf("Authorization" to "Bearer $accessToken") }
            .orEmpty()

    private fun createWalkSearchBody(request: RouteSearchRequestDto): String =
        JSONObject()
            .put("startPoint", request.startPoint.toJsonObject())
            .put("endPoint", request.endPoint.toJsonObject())
            .toString()

    private fun RoutePointDto.toJsonObject(): JSONObject =
        JSONObject()
            .put("lat", lat)
            .put("lng", lng)

    private fun HttpJsonResponse.requireRouteSearchResponseDto(responseJson: JSONObject?) =
        if (statusCode in 200..299) {
            runCatching {
                parseRouteSearchResponseDto(body)
            }.getOrElse { error ->
                throw routeApiException(response = this, responseJson = responseJson, fallback = error.message)
            }
        } else {
            throw routeApiException(response = this, responseJson = responseJson)
        }

    private fun routeApiException(
        response: HttpJsonResponse,
        responseJson: JSONObject?,
        fallback: String? = null,
    ): RouteApiException =
        RouteApiException(
            httpStatusCode = response.statusCode,
            status = responseJson?.optString("status").orEmpty(),
            message =
                responseJson?.optString("message")
                    ?.takeIf(String::isNotBlank)
                    ?: fallback
                    ?: DEFAULT_ROUTE_API_ERROR_MESSAGE,
        )

    private fun String.toJsonObjectOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private companion object {
        private const val DEFAULT_ROUTE_API_ERROR_MESSAGE = "경로를 불러오지 못했습니다."
    }
}

class RouteApiException(
    val httpStatusCode: Int,
    val status: String,
    override val message: String,
) : RuntimeException(message)
