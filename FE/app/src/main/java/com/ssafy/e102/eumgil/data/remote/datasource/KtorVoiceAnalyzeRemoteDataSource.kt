package com.ssafy.e102.eumgil.data.remote.datasource

import android.util.Log
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.dto.VoiceAnalyzeHistoryDto
import com.ssafy.e102.eumgil.data.remote.dto.VoiceAnalyzeResponseDto
import org.json.JSONArray
import org.json.JSONObject

class KtorVoiceAnalyzeRemoteDataSource(
    private val httpJsonClient: HttpJsonClient,
) : VoiceAnalyzeRemoteDataSource {

    override suspend fun analyze(
        text: String,
        mode: String,
        history: List<VoiceAnalyzeHistoryDto>,
    ): VoiceAnalyzeResponseDto {
        try {
            val historyArray = JSONArray().apply {
                history.forEach { item ->
                    put(
                        JSONObject()
                            .put("role", item.role)
                            .put("content", item.content),
                    )
                }
            }
            val body =
                JSONObject()
                    .put("text", text)
                    .put("mode", mode)
                    .put("history", historyArray)
                    .toString()

            val response = httpJsonClient.postJson(
                path = "/voice/analyze",
                body = body,
            )

            val responseJson = response.body.toJsonObjectOrNull()
            val dataJson =
                responseJson?.optJSONObject("data")
                    ?: throw VoiceAnalyzeApiException(
                        httpStatusCode = response.statusCode,
                        message = "음성 분석 응답 파싱 실패 (statusCode=${response.statusCode})",
                    )

            val confirmed: Boolean? = if (dataJson.isNull("confirmed")) null else dataJson.optBoolean("confirmed")

            return VoiceAnalyzeResponseDto(
                intent = dataJson.optString("intent", "UNKNOWN"),
                placeName = if (dataJson.isNull("placeName")) null else dataJson.optString("placeName").takeIf { it.isNotBlank() },
                confirmed = confirmed,
                confirmationMessage = if (dataJson.isNull("confirmationMessage")) null else dataJson.optString("confirmationMessage").takeIf { it.isNotBlank() },
            )
        } catch (e: Exception) {
            Log.e(TAG, "API 호출 실패: ${e.message}")
            throw e
        }
    }

    private companion object {
        private const val TAG = "VoiceAnalyzeAPI"
    }

    private fun String.toJsonObjectOrNull(): JSONObject? =
        runCatching { JSONObject(this) }.getOrNull()
}

class VoiceAnalyzeApiException(
    val httpStatusCode: Int,
    override val message: String,
) : RuntimeException(message)
