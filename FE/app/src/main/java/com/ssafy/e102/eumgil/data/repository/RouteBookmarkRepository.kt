package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.RouteBookmark
import com.ssafy.e102.eumgil.core.model.RouteBookmarkDraft
import com.ssafy.e102.eumgil.core.model.RouteBookmarkSaveRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface RouteBookmarkRepository {
    fun observeRouteBookmarks(): Flow<List<RouteBookmark>>

    suspend fun isBookmarked(draft: RouteBookmarkDraft): Boolean

    suspend fun saveRouteBookmark(request: RouteBookmarkSaveRequest): RouteBookmark

    suspend fun deleteRouteBookmark(bookmarkId: String)
}

class FakeRouteBookmarkRepository(
    private val clock: () -> Long = { System.currentTimeMillis() },
) : RouteBookmarkRepository {
    private val routeBookmarks = MutableStateFlow(emptyList<RouteBookmark>())

    override fun observeRouteBookmarks(): Flow<List<RouteBookmark>> = routeBookmarks

    override suspend fun isBookmarked(draft: RouteBookmarkDraft): Boolean =
        routeBookmarks.value.any { bookmark ->
            bookmark.routeSignature() == draft.routeSignature()
        }

    override suspend fun saveRouteBookmark(request: RouteBookmarkSaveRequest): RouteBookmark {
        val now = clock()
        val bookmarkId = request.bookmarkId()
        val existingBookmark =
            routeBookmarks.value.firstOrNull { bookmark ->
                bookmark.bookmarkId == bookmarkId
            }
        val savedBookmark =
            RouteBookmark(
                bookmarkId = bookmarkId,
                routeName = request.routeName.trim().ifBlank { "${request.startLabel}-${request.endLabel}" },
                startLabel = request.startLabel,
                endLabel = request.endLabel,
                startPoint = request.startPoint,
                endPoint = request.endPoint,
                routeOption = request.routeOption,
                distanceMeters = request.distanceMeters,
                durationMinutes = request.durationMinutes,
                createdAt = existingBookmark?.createdAt ?: now,
                updatedAt = now,
            )

        routeBookmarks.update { currentBookmarks ->
            (currentBookmarks.filterNot { bookmark -> bookmark.bookmarkId == bookmarkId } + savedBookmark)
                .sortedByDescending(RouteBookmark::updatedAt)
        }

        return savedBookmark
    }

    override suspend fun deleteRouteBookmark(bookmarkId: String) {
        routeBookmarks.update { currentBookmarks ->
            currentBookmarks.filterNot { bookmark -> bookmark.bookmarkId == bookmarkId }
        }
    }
}

private fun RouteBookmarkDraft.routeSignature(): String =
    "${startPoint.latitude},${startPoint.longitude}|${endPoint.latitude},${endPoint.longitude}|${routeOption.name}"

private fun RouteBookmark.routeSignature(): String =
    "${startPoint.latitude},${startPoint.longitude}|${endPoint.latitude},${endPoint.longitude}|${routeOption.name}"

private fun RouteBookmarkSaveRequest.bookmarkId(): String =
    "route-bookmark:${startPoint.latitude},${startPoint.longitude}|${endPoint.latitude},${endPoint.longitude}|${routeOption.name}"
