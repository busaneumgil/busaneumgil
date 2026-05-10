package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.route.RoutePointDto
import com.ssafy.e102.eumgil.data.route.RouteRatingRequestDto
import com.ssafy.e102.eumgil.data.route.RouteRatingResponseDto
import com.ssafy.e102.eumgil.data.route.RouteRerouteRequestDto
import com.ssafy.e102.eumgil.data.route.RouteRerouteResponseDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.RouteSelectRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSessionResponseDto
import com.ssafy.e102.eumgil.data.route.RouteTransitRefreshRequestDto
import com.ssafy.e102.eumgil.data.route.RouteTransitRefreshResponseDto
import com.ssafy.e102.eumgil.data.route.parseRouteRatingResponseDto
import com.ssafy.e102.eumgil.data.route.parseRouteRerouteResponseDto
import com.ssafy.e102.eumgil.data.route.parseRouteSearchResponseDto
import com.ssafy.e102.eumgil.data.route.parseRouteSessionResponseDto
import com.ssafy.e102.eumgil.data.route.parseRouteTransitRefreshResponseDto
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

    open suspend fun searchWalkRoutes(request: RouteSearchRequestDto): RouteSearchResponseDto =
        postRouteRequest(
            path = "/routes/search/walk",
            body = createRouteSearchBody(request),
            responseParser = ::parseRouteSearchResponseDto,
        )

    open suspend fun searchTransitRoutes(request: RouteSearchRequestDto): RouteSearchResponseDto =
        postRouteRequest(
            path = "/routes/search/transit",
            body = createRouteSearchBody(request),
            responseParser = ::parseRouteSearchResponseDto,
        )

    open suspend fun selectRoute(
        routeId: String,
        request: RouteSelectRequestDto,
    ): RouteSessionResponseDto =
        postRouteRequest(
            path = "/routes/$routeId/select",
            body = createSelectRouteBody(request),
            responseParser = ::parseRouteSessionResponseDto,
        )

    open suspend fun refreshTransit(
        routeId: String,
        request: RouteTransitRefreshRequestDto,
    ): RouteTransitRefreshResponseDto =
        postRouteRequest(
            path = "/routes/$routeId/transit-refresh",
            body = createTransitRefreshBody(request),
            responseParser = ::parseRouteTransitRefreshResponseDto,
        )

    open suspend fun reroute(request: RouteRerouteRequestDto): RouteRerouteResponseDto =
        postRouteRequest(
            path = "/routes/reroute",
            body = createRerouteBody(request),
            responseParser = ::parseRouteRerouteResponseDto,
        )

    open suspend fun endRoute(routeId: String): RouteSessionResponseDto =
        postRouteRequest(
            path = "/routes/$routeId/end",
            body = "",
            responseParser = ::parseRouteSessionResponseDto,
        )

    open suspend fun rateRoute(request: RouteRatingRequestDto): RouteRatingResponseDto =
        postRouteRequest(
            path = "/route-ratings",
            body = createRouteRatingBody(request),
            responseParser = ::parseRouteRatingResponseDto,
        )

    private suspend fun <T> postRouteRequest(
        path: String,
        body: String,
        responseParser: (String) -> T,
    ): T {
        val response = postRequestExecutor(path, body, bearerHeader())
        val responseJson = response.body.toJsonObjectOrNull()
        return response.requireRouteResponse(responseJson = responseJson, responseParser = responseParser)
    }

    private suspend fun bearerHeader(): Map<String, String> =
        accessTokenProvider()
            ?.takeIf { accessToken -> accessToken.isNotBlank() }
            ?.let { accessToken -> mapOf("Authorization" to "Bearer $accessToken") }
            .orEmpty()

    private fun createRouteSearchBody(request: RouteSearchRequestDto): String =
        JSONObject()
            .put("startPoint", request.startPoint.toJsonObject())
            .put("endPoint", request.endPoint.toJsonObject())
            .toString()

    private fun createSelectRouteBody(request: RouteSelectRequestDto): String =
        JSONObject()
            .put("searchId", request.searchId)
            .toString()

    private fun createTransitRefreshBody(request: RouteTransitRefreshRequestDto): String =
        JSONObject()
            .put("legSequence", request.legSequence)
            .toString()

    private fun createRerouteBody(request: RouteRerouteRequestDto): String =
        JSONObject()
            .put("routeId", request.routeId)
            .put("currentPoint", request.currentPoint.toJsonObject())
            .toString()

    private fun createRouteRatingBody(request: RouteRatingRequestDto): String =
        JSONObject()
            .put("sessionId", request.sessionId)
            .put("score", request.score)
            .toString()

    private fun RoutePointDto.toJsonObject(): JSONObject =
        JSONObject()
            .put("lat", lat)
            .put("lng", lng)

    private fun <T> HttpJsonResponse.requireRouteResponse(
        responseJson: JSONObject?,
        responseParser: (String) -> T,
    ): T =
        if (statusCode in 200..299) {
            runCatching {
                responseParser(body)
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
