package com.ssafy.e102.eumgil.data.remote

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HttpJsonResponse(
    val statusCode: Int,
    val body: String,
)

class HttpJsonClient(
    private val baseUrl: String,
    private val connectTimeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    private val readTimeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
) {
    suspend fun postJson(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpJsonResponse =
        withContext(Dispatchers.IO) {
            val connection = openConnection(path)
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

            connection.outputStream.use { outputStream ->
                outputStream.write(body.toByteArray(Charsets.UTF_8))
            }

            connection.toHttpJsonResponse()
        }

    private fun openConnection(path: String): HttpURLConnection =
        URL(normalizeUrl(path)).openConnection().let { connection ->
            (connection as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
            }
        }

    private fun normalizeUrl(path: String): String {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val normalizedPath = path.trimStart('/')
        return "$normalizedBaseUrl/$normalizedPath"
    }

    private fun HttpURLConnection.toHttpJsonResponse(): HttpJsonResponse {
        val statusCode = responseCode
        val responseBody =
            runCatching {
                val stream = if (statusCode in 200..299) inputStream else errorStream
                stream?.readUtf8().orEmpty()
            }.getOrDefault("")

        disconnect()
        return HttpJsonResponse(statusCode = statusCode, body = responseBody)
    }

    private fun InputStream.readUtf8(): String =
        BufferedReader(InputStreamReader(this, Charsets.UTF_8)).use { reader ->
            buildString {
                while (true) {
                    val line = reader.readLine() ?: break
                    append(line)
                }
            }
        }

    private companion object {
        private const val DEFAULT_TIMEOUT_MILLIS = 10_000
    }
}
