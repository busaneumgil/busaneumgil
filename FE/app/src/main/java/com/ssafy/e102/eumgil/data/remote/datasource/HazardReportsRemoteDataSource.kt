package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.remote.dto.CreateHazardReportRequestDto
import com.ssafy.e102.eumgil.data.remote.dto.CreateHazardReportResponseDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportDetailDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportPageDto
import com.ssafy.e102.eumgil.data.remote.dto.HazardReportPointDto
import org.json.JSONArray
import org.json.JSONObject

open class HazardReportsRemoteDataSource private constructor(
    private val getExecutor: suspend (
        path: String,
        queryParams: Map<String, String>,
        headers: Map<String, String>,
    ) -> HttpJsonResponse,
    private val postExecutor: suspend (
        path: String,
        body: String,
        headers: Map<String, String>,
    ) -> HttpJsonResponse,
) {
    constructor(httpJsonClient: HttpJsonClient) : this(
        getExecutor = httpJsonClient::getJson,
        postExecutor = httpJsonClient::postJson,
    )

    internal constructor(
        requestExecutor: suspend (
            path: String,
            queryParams: Map<String, String>,
            headers: Map<String, String>,
        ) -> HttpJsonResponse,
    ) : this(
        getExecutor = requestExecutor,
        postExecutor = { _, _, _ -> error("POST executor is not configured.") },
    )

    open suspend fun createHazardReport(
        accessToken: String,
        request: CreateHazardReportRequestDto,
    ): CreateHazardReportResponseDto {
        val requestJson =
            JSONObject()
                .put("reportType", request.reportType)
                .apply {
                    request.description?.takeIf(String::isNotBlank)?.let { put("description", it) }
                }
                .put(
                    "reportPoint",
                    JSONObject()
                        .put("lat", request.reportPoint.lat)
                        .put("lng", request.reportPoint.lng),
                )
                .put("imageUrls", JSONArray(request.imageUrls))

        val response =
            postExecutor(
                "/hazard-reports",
                requestJson.toString(),
                bearerHeader(accessToken),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        val dataJson = response.requireDataJson(responseJson)

        return CreateHazardReportResponseDto(
            reportId =
                dataJson.optLongOrNull("reportId")
                    ?: throw hazardReportsApiException(response, responseJson),
        )
    }

    open suspend fun getMyHazardReports(
        accessToken: String,
        cursor: Long? = null,
        size: Int? = null,
    ): HazardReportPageDto {
        val queryParams =
            buildMap {
                cursor?.let { put("cursor", it.toString()) }
                size?.let { put("size", it.toString()) }
            }
        val response =
            getExecutor(
                "/hazard-reports/me",
                queryParams,
                bearerHeader(accessToken),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        val dataJson = response.requireDataJson(responseJson)

        return dataJson.toHazardReportPageDto()
    }

    open suspend fun getMyHazardReportDetail(
        accessToken: String,
        reportId: Long,
    ): HazardReportDetailDto {
        val response =
            getExecutor(
                "/hazard-reports/me/$reportId",
                emptyMap(),
                bearerHeader(accessToken),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        val dataJson = response.requireDataJson(responseJson)

        return dataJson.toHazardReportDetailDto()
    }

    private fun bearerHeader(accessToken: String): Map<String, String> = mapOf("Authorization" to "Bearer $accessToken")

    private fun JSONObject.toHazardReportPageDto(): HazardReportPageDto {
        val contentJson = optJSONArray("content") ?: JSONArray()
        val items =
            List(contentJson.length()) { index ->
                contentJson.getJSONObject(index).toHazardReportListItemDto()
            }

        return HazardReportPageDto(
            content = items,
            size = optInt("size"),
            nextCursor = optLongOrNull("nextCursor"),
            hasNext = optBoolean("hasNext"),
        )
    }

    private fun JSONObject.toHazardReportListItemDto(): HazardReportListItemDto =
        HazardReportListItemDto(
            reportId = requireLong("reportId"),
            reportType = requireString("reportType"),
            reportPoint = requireReportPointDto("reportPoint"),
            createdAt = requireString("createdAt"),
            representativeImageUrl = optNullableString("representativeImageUrl"),
        )

    private fun JSONObject.toHazardReportDetailDto(): HazardReportDetailDto =
        HazardReportDetailDto(
            reportId = requireLong("reportId"),
            reportType = requireString("reportType"),
            description = optNullableString("description"),
            reportPoint = requireReportPointDto("reportPoint"),
            createdAt = requireString("createdAt"),
            imageUrls = optJSONArray("imageUrls")?.toStringList().orEmpty(),
        )

    private fun JSONObject.requireReportPointDto(name: String): HazardReportPointDto {
        val pointJson =
            optJSONObject(name)
                ?: throw HazardReportsApiException(
                    httpStatusCode = 0,
                    status = "",
                    message = DEFAULT_HAZARD_REPORTS_API_ERROR_MESSAGE,
                )

        return HazardReportPointDto(
            lat = pointJson.optDouble("lat"),
            lng = pointJson.optDouble("lng"),
        )
    }

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { index -> optString(index) }
            .filter(String::isNotBlank)

    private fun String.toJsonObjectOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private fun HttpJsonResponse.requireDataJson(responseJson: JSONObject?): JSONObject {
        if (statusCode !in 200..299) {
            throw hazardReportsApiException(this, responseJson)
        }

        return responseJson?.optJSONObject("data")
            ?: throw hazardReportsApiException(this, responseJson)
    }

    private fun hazardReportsApiException(
        response: HttpJsonResponse,
        responseJson: JSONObject?,
    ): HazardReportsApiException =
        HazardReportsApiException(
            httpStatusCode = response.statusCode,
            status = responseJson?.optString("status").orEmpty(),
            message =
                responseJson?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_HAZARD_REPORTS_API_ERROR_MESSAGE,
        )

    private fun JSONObject.optLongOrNull(name: String): Long? =
        if (isNull(name) || has(name).not()) {
            null
        } else {
            optLong(name, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        }

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name) || has(name).not()) {
            null
        } else {
            optString(name).takeIf { it.isNotBlank() }
        }

    private fun JSONObject.requireLong(name: String): Long =
        optLongOrNull(name)
            ?: throw HazardReportsApiException(
                httpStatusCode = 0,
                status = "",
                message = DEFAULT_HAZARD_REPORTS_API_ERROR_MESSAGE,
            )

    private fun JSONObject.requireString(name: String): String =
        optNullableString(name)
            ?: throw HazardReportsApiException(
                httpStatusCode = 0,
                status = "",
                message = DEFAULT_HAZARD_REPORTS_API_ERROR_MESSAGE,
            )

    private companion object {
        private const val DEFAULT_HAZARD_REPORTS_API_ERROR_MESSAGE = "제보 서버 요청에 실패했습니다."
    }
}

class HazardReportsApiException(
    val httpStatusCode: Int,
    val status: String,
    override val message: String,
) : RuntimeException(message)
