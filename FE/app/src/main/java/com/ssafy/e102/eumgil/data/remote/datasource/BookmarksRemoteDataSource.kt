package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkPageDto
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkPointDto
import com.ssafy.e102.eumgil.data.remote.dto.CreateBookmarkResponseDto
import org.json.JSONArray
import org.json.JSONObject

open class BookmarksRemoteDataSource(
    private val httpJsonClient: HttpJsonClient,
) {
    open suspend fun getBookmarks(
        accessToken: String,
        page: Int? = null,
        size: Int? = null,
    ): BookmarkPageDto {
        val queryParams =
            buildMap {
                page?.let { put("page", it.toString()) }
                size?.let { put("size", it.toString()) }
            }

        val response =
            httpJsonClient.getJson(
                path = "/bookmarks",
                queryParams = queryParams,
                headers = bearerHeader(accessToken),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        val dataJson = response.requireDataJson(responseJson)

        return dataJson.toBookmarkPageDto()
    }

    open suspend fun createBookmark(
        accessToken: String,
        placeId: Long,
    ): CreateBookmarkResponseDto {
        val response =
            httpJsonClient.postJson(
                path = "/bookmarks",
                body = JSONObject().put("placeId", placeId).toString(),
                headers = bearerHeader(accessToken),
            )
        val responseJson = response.body.toJsonObjectOrNull()
        val dataJson = response.requireDataJson(responseJson)

        return CreateBookmarkResponseDto(
            bookmarkId =
                dataJson.optLongOrNull("bookmarkId")
                    ?: throw bookmarksApiException(response, responseJson),
            placeId =
                dataJson.optLongOrNull("placeId")
                    ?: throw bookmarksApiException(response, responseJson),
        )
    }

    open suspend fun deleteBookmark(
        accessToken: String,
        placeId: Long,
    ) {
        val response =
            httpJsonClient.deleteJson(
                path = "/bookmarks/places/$placeId",
                headers = bearerHeader(accessToken),
            )

        if (response.statusCode !in 200..299) {
            val responseJson = response.body.toJsonObjectOrNull()
            throw bookmarksApiException(response, responseJson)
        }
    }

    private fun bearerHeader(accessToken: String): Map<String, String> = mapOf("Authorization" to "Bearer $accessToken")

    private fun JSONObject.toBookmarkPageDto(): BookmarkPageDto {
        val contentJson = optJSONArray("content") ?: JSONArray()
        val items =
            (0 until contentJson.length()).map { index ->
                contentJson.getJSONObject(index).toBookmarkListItemDto()
            }

        return BookmarkPageDto(
            content = items,
            page = optInt("page"),
            size = optInt("size"),
            totalElements = optLong("totalElements"),
            totalPages = optInt("totalPages"),
            hasNext = optBoolean("hasNext"),
        )
    }

    private fun JSONObject.toBookmarkListItemDto(): BookmarkListItemDto {
        val pointJson =
            optJSONObject("point")
                ?: throw BookmarksApiException(
                    httpStatusCode = 0,
                    status = "",
                    message = DEFAULT_BOOKMARKS_API_ERROR_MESSAGE,
                )

        return BookmarkListItemDto(
            bookmarkId = optLong("bookmarkId"),
            placeId = optLong("placeId"),
            provider = optNullableString("provider"),
            providerPlaceId = optNullableString("providerPlaceId"),
            name = optString("name"),
            category = optString("category"),
            address = optNullableString("address"),
            point =
                BookmarkPointDto(
                    lat = pointJson.optDouble("lat"),
                    lng = pointJson.optDouble("lng"),
                ),
        )
    }

    private fun String.toJsonObjectOrNull(): JSONObject? = runCatching { JSONObject(this) }.getOrNull()

    private fun HttpJsonResponse.requireDataJson(responseJson: JSONObject?): JSONObject {
        if (statusCode !in 200..299) {
            throw bookmarksApiException(this, responseJson)
        }

        return responseJson?.optJSONObject("data")
            ?: throw bookmarksApiException(this, responseJson)
    }

    private fun bookmarksApiException(
        response: HttpJsonResponse,
        responseJson: JSONObject?,
    ): BookmarksApiException =
        BookmarksApiException(
            httpStatusCode = response.statusCode,
            status = responseJson?.optString("status").orEmpty(),
            message =
                responseJson?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_BOOKMARKS_API_ERROR_MESSAGE,
        )

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) {
            null
        } else {
            optString(name).takeIf { it.isNotBlank() }
        }

    private fun JSONObject.optLongOrNull(name: String): Long? =
        if (isNull(name)) {
            null
        } else {
            optLong(name, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        }

    private companion object {
        private const val DEFAULT_BOOKMARKS_API_ERROR_MESSAGE = "북마크 서버 요청에 실패했습니다."
    }
}

class BookmarksApiException(
    val httpStatusCode: Int,
    val status: String,
    override val message: String,
) : RuntimeException(message)
