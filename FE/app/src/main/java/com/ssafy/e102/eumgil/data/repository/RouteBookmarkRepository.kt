package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteBookmark
import com.ssafy.e102.eumgil.core.model.RouteBookmarkDraft
import com.ssafy.e102.eumgil.core.model.RouteBookmarkSaveRequest
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.local.dao.FavoriteRouteDao
import com.ssafy.e102.eumgil.data.local.entity.FavoriteRouteEntity
import com.ssafy.e102.eumgil.data.remote.datasource.FavoriteRoutesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.FavoriteRouteListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.FavoriteRoutePointDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

interface RouteBookmarkRepository {
    fun observeRouteBookmarks(): Flow<List<RouteBookmark>>

    suspend fun isBookmarked(draft: RouteBookmarkDraft): Boolean

    suspend fun saveRouteBookmark(request: RouteBookmarkSaveRequest): RouteBookmark

    suspend fun deleteRouteBookmark(bookmarkId: String)
}

class DefaultRouteBookmarkRepository(
    private val favoriteRouteDao: FavoriteRouteDao,
    private val favoriteRoutesRemoteDataSource: FavoriteRoutesRemoteDataSource? = null,
    private val accessTokenProvider: suspend () -> String? = { null },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : RouteBookmarkRepository {
    override fun observeRouteBookmarks(): Flow<List<RouteBookmark>> =
        favoriteRouteDao
            .observeFavoriteRoutes()
            .onStart { refreshFromServerIfPossible() }
            .map { entities -> entities.map(FavoriteRouteEntity::toRouteBookmark) }

    override suspend fun isBookmarked(draft: RouteBookmarkDraft): Boolean {
        val cached = favoriteRouteDao.observeFavoriteRoutes().first()
        return cached.any { entity -> entity.matchesSignature(draft) }
    }

    override suspend fun saveRouteBookmark(request: RouteBookmarkSaveRequest): RouteBookmark {
        val now = clock()

        val serverFavRouteId =
            runCatching { trySaveOnServer(request) }.getOrNull()

        val resolvedBookmarkId =
            serverFavRouteId?.toString() ?: request.fallbackBookmarkId()

        val cachedEntity =
            FavoriteRouteEntity(
                favoriteRouteId = serverFavRouteId ?: 0L,
                routeName = request.routeName.trim().ifBlank { request.fallbackRouteName() },
                originName = request.startLabel,
                originLatitude = request.startPoint.latitude,
                originLongitude = request.startPoint.longitude,
                destinationName = request.endLabel,
                destinationLatitude = request.endPoint.latitude,
                destinationLongitude = request.endPoint.longitude,
                routeOption = request.routeOption.name,
                summaryDistanceMeters = request.distanceMeters,
                summaryDurationSeconds = request.durationMinutes?.let { it * 60 },
                createdAt = now,
                updatedAt = now,
            )
        favoriteRouteDao.upsertFavoriteRoute(cachedEntity)

        return RouteBookmark(
            bookmarkId = resolvedBookmarkId,
            routeName = cachedEntity.routeName,
            startLabel = cachedEntity.originName,
            endLabel = cachedEntity.destinationName,
            startPoint = GeoCoordinate(latitude = cachedEntity.originLatitude, longitude = cachedEntity.originLongitude),
            endPoint =
                GeoCoordinate(
                    latitude = cachedEntity.destinationLatitude,
                    longitude = cachedEntity.destinationLongitude,
                ),
            routeOption = request.routeOption,
            distanceMeters = request.distanceMeters,
            durationMinutes = request.durationMinutes,
            createdAt = cachedEntity.createdAt,
            updatedAt = cachedEntity.updatedAt,
        )
    }

    override suspend fun deleteRouteBookmark(bookmarkId: String) {
        runCatching { tryDeleteOnServer(bookmarkId) }

        val numericId = bookmarkId.toLongOrNull()
        if (numericId != null) {
            favoriteRouteDao.deleteFavoriteRoute(numericId)
        }
    }

    private suspend fun trySaveOnServer(request: RouteBookmarkSaveRequest): Long? {
        val datasource = favoriteRoutesRemoteDataSource ?: return null
        val token = accessTokenProvider() ?: return null

        val response =
            datasource.createFavoriteRoute(
                accessToken = token,
                startLabel = request.startLabel,
                endLabel = request.endLabel,
                startPoint = FavoriteRoutePointDto(lat = request.startPoint.latitude, lng = request.startPoint.longitude),
                endPoint = FavoriteRoutePointDto(lat = request.endPoint.latitude, lng = request.endPoint.longitude),
                routeOption = request.routeOption.name,
            )
        return response.favRouteId
    }

    private suspend fun tryDeleteOnServer(bookmarkId: String) {
        val datasource = favoriteRoutesRemoteDataSource ?: return
        val token = accessTokenProvider() ?: return
        val numericId = bookmarkId.toLongOrNull() ?: return

        datasource.deleteFavoriteRoute(accessToken = token, favRouteId = numericId)
    }

    private suspend fun refreshFromServerIfPossible() {
        runCatching {
            val datasource = favoriteRoutesRemoteDataSource ?: return@runCatching
            val token = accessTokenProvider() ?: return@runCatching

            val page =
                datasource.getFavoriteRoutes(
                    accessToken = token,
                    cursor = null,
                    size = DEFAULT_PAGE_SIZE,
                )

            val now = clock()
            favoriteRouteDao.clearFavoriteRoutes()
            favoriteRouteDao.upsertFavoriteRoutes(
                page.content.map { item -> item.toFavoriteRouteEntity(createdAt = now, updatedAt = now) },
            )
        }
    }

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 50
    }
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

private fun RouteBookmarkSaveRequest.fallbackBookmarkId(): String = bookmarkId()

private fun RouteBookmarkSaveRequest.fallbackRouteName(): String = "$startLabel-$endLabel"

private fun FavoriteRouteEntity.matchesSignature(draft: RouteBookmarkDraft): Boolean =
    originLatitude == draft.startPoint.latitude &&
        originLongitude == draft.startPoint.longitude &&
        destinationLatitude == draft.endPoint.latitude &&
        destinationLongitude == draft.endPoint.longitude &&
        routeOption == draft.routeOption.name

private fun FavoriteRouteEntity.toRouteBookmark(): RouteBookmark =
    RouteBookmark(
        bookmarkId = if (favoriteRouteId > 0) favoriteRouteId.toString() else "route-bookmark-cache:$favoriteRouteId",
        routeName = routeName,
        startLabel = originName,
        endLabel = destinationName,
        startPoint = GeoCoordinate(latitude = originLatitude, longitude = originLongitude),
        endPoint = GeoCoordinate(latitude = destinationLatitude, longitude = destinationLongitude),
        routeOption = routeOption.toRouteOptionOrDefault(),
        distanceMeters = summaryDistanceMeters,
        durationMinutes = summaryDurationSeconds?.let { it / 60 },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun FavoriteRouteListItemDto.toFavoriteRouteEntity(
    createdAt: Long,
    updatedAt: Long,
): FavoriteRouteEntity =
    FavoriteRouteEntity(
        favoriteRouteId = favRouteId,
        routeName = routeName,
        originName = startLabel,
        originLatitude = startPoint.lat,
        originLongitude = startPoint.lng,
        destinationName = endLabel,
        destinationLatitude = endPoint.lat,
        destinationLongitude = endPoint.lng,
        routeOption = routeOption,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun String?.toRouteOptionOrDefault(): RouteOption =
    this?.let { value ->
        RouteOption.values().firstOrNull { it.name == value }
    } ?: RouteOption.SAFE
