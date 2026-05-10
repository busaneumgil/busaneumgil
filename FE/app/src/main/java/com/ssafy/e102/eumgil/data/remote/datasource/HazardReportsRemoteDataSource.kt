package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.remote.dto.CreateHazardReportRequestDto
import com.ssafy.e102.eumgil.data.remote.dto.CreateHazardReportResponseDto
import org.json.JSONArray
import org.json.JSONObject

open class HazardReportsRemoteDataSource(
    private val httpJsonClient: HttpJsonClient,
) {
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
            httpJsonClient.postJson(
                path = "/hazard-reports",
                body = requestJson.toString(),
                headers = bearerHeader(accessToken),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        val dataJson = response.requireDataJson(responseJson)

        return CreateHazardReportResponseDto(
            reportId =
                dataJson.optLongOrNull("reportId")
                    ?: throw hazardReportsApiException(response, responseJson),
        )
    }

    private fun bearerHeader(accessToken: String): Map<String, String> = mapOf("Authorization" to "Bearer $accessToken")

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
        if (isNull(name)) {
            null
        } else {
            optLong(name, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        }

    private companion object {
        private const val DEFAULT_HAZARD_REPORTS_API_ERROR_MESSAGE = "제보 서버 요청에 실패했습니다."
    }
}

class HazardReportsApiException(
    val httpStatusCode: Int,
    val status: String,
    override val message: String,
) : RuntimeException(message)
