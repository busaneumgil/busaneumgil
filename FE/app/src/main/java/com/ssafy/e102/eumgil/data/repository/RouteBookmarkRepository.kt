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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    private val authSessionRepository: AuthSessionRepository? = null,
    private val favoriteRoutesRemoteDataSource: FavoriteRoutesRemoteDataSource? = null,
    private val accessTokenProvider: suspend () -> String? = { null },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : RouteBookmarkRepository {
    override fun observeRouteBookmarks(): Flow<List<RouteBookmark>> =
        observeAccountScope().flatMapLatest { accountScopeKey ->
            if (accountScopeKey == null) {
                flowOf(emptyList())
            } else {
                favoriteRouteDao
                    .observeFavoriteRoutes(accountScopeKey)
                    .onStart { refreshFromServerIfPossible(accountScopeKey) }
                    .map { entities -> entities.map(FavoriteRouteEntity::toRouteBookmark) }
            }
        }

    override suspend fun isBookmarked(draft: RouteBookmarkDraft): Boolean {
        val accountScopeKey = getCurrentAccountScopeKey() ?: return false
        val cached = favoriteRouteDao.getFavoriteRoutes(accountScopeKey)
        return cached.any { entity -> entity.matchesSignature(draft) }
    }

    override suspend fun saveRouteBookmark(request: RouteBookmarkSaveRequest): RouteBookmark {
        val accountScopeKey = getCurrentAccountScopeKey() ?: return request.toUncachedRouteBookmark(clock())
        val now = clock()
        val serverFavRouteId =
            runCatching { trySaveOnServer(request) }.getOrNull()
        val resolvedFavoriteRouteId = serverFavRouteId ?: request.localCacheFavoriteRouteId()
        val existingEntity = favoriteRouteDao.getFavoriteRoute(accountScopeKey, resolvedFavoriteRouteId)

        val cachedEntity =
            FavoriteRouteEntity(
                accountScopeKey = accountScopeKey,
                favoriteRouteId = resolvedFavoriteRouteId,
                routeName = request.routeName.trim().ifBlank { request.fallbackRouteName() },
                originName = request.startLabel,
                originLatitude = request.startPoint.latitude,
                originLongitude = request.startPoint.longitude,
                destinationName = request.endLabel,
                destinationLatitude = request.endPoint.latitude,
                destinationLongitude = request.endPoint.longitude,
                transportMode = WALK_TRANSPORT_MODE,
                routeOption = request.routeOption.name,
                summaryDistanceMeters = request.distanceMeters,
                summaryDurationSeconds = request.durationMinutes?.let { it * 60 },
                createdAt = existingEntity?.createdAt ?: now,
                updatedAt = now,
            )
        favoriteRouteDao.upsertFavoriteRoute(cachedEntity)

        return RouteBookmark(
            bookmarkId = cachedEntity.favoriteRouteId.toString(),
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
            transportMode = cachedEntity.transportMode,
            routeOptionLabel = cachedEntity.routeOption,
            distanceMeters = request.distanceMeters,
            durationMinutes = request.durationMinutes,
            createdAt = cachedEntity.createdAt,
            updatedAt = cachedEntity.updatedAt,
        )
    }

    override suspend fun deleteRouteBookmark(bookmarkId: String) {
        val accountScopeKey = getCurrentAccountScopeKey() ?: return
        val favoriteRouteId = bookmarkId.toLongOrNull() ?: return
        runCatching { tryDeleteOnServer(favoriteRouteId) }
        favoriteRouteDao.deleteFavoriteRoute(accountScopeKey, favoriteRouteId)
    }

    private suspend fun trySaveOnServer(request: RouteBookmarkSaveRequest): Long? {
        val datasource = favoriteRoutesRemoteDataSource ?: return null
        val token = resolveAccessToken() ?: return null
        val routeId = request.routeId?.trim()?.takeIf(String::isNotEmpty) ?: return null

        val response =
            datasource.createFavoriteRoute(
                accessToken = token,
                routeId = routeId,
                startLabel = request.startLabel,
                endLabel = request.endLabel,
            )
        return response.favRouteId
    }

    private suspend fun tryDeleteOnServer(favoriteRouteId: Long) {
        val datasource = favoriteRoutesRemoteDataSource ?: return
        val token = resolveAccessToken() ?: return
        if (favoriteRouteId <= 0L) return

        datasource.deleteFavoriteRoute(accessToken = token, favRouteId = favoriteRouteId)
    }

    private suspend fun refreshFromServerIfPossible(accountScopeKey: String) {
        runCatching {
            val datasource = favoriteRoutesRemoteDataSource ?: return@runCatching
            val token = resolveAccessToken() ?: return@runCatching

            val page =
                datasource.getFavoriteRoutes(
                    accessToken = token,
                    cursor = null,
                    size = DEFAULT_PAGE_SIZE,
                )

            val now = clock()
            val cachedById =
                favoriteRouteDao
                    .getFavoriteRoutes(accountScopeKey)
                    .associateBy(FavoriteRouteEntity::favoriteRouteId)

            favoriteRouteDao.clearFavoriteRoutes(accountScopeKey)
            favoriteRouteDao.upsertFavoriteRoutes(
                page.content.map { item ->
                    val cached = cachedById[item.favRouteId]
                    item.toFavoriteRouteEntity(
                        accountScopeKey = accountScopeKey,
                        createdAt = cached?.createdAt ?: now,
                        updatedAt = now,
                        cachedDistanceMeters = cached?.summaryDistanceMeters,
                        cachedDurationSeconds = cached?.summaryDurationSeconds,
                    )
                },
            )
        }
    }

    private fun observeAccountScope(): Flow<String?> =
        authSessionRepository?.observeAccountScopeKey() ?: flowOf(DEFAULT_TEST_ACCOUNT_SCOPE_KEY)

    private suspend fun getCurrentAccountScopeKey(): String? =
        authSessionRepository?.getAccountScopeKey() ?: DEFAULT_TEST_ACCOUNT_SCOPE_KEY

    private suspend fun resolveAccessToken(): String? =
        authSessionRepository?.getCurrentAuthSession()?.accessToken ?: accessTokenProvider()

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 50
        private const val WALK_TRANSPORT_MODE = "WALK"
        private const val DEFAULT_TEST_ACCOUNT_SCOPE_KEY = "test-account"
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
    routeId
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { "route-bookmark:$it" }
        ?: "route-bookmark:${startPoint.latitude},${startPoint.longitude}|${endPoint.latitude},${endPoint.longitude}|${routeOption.name}"

private fun RouteBookmarkSaveRequest.fallbackRouteName(): String = "$startLabel-$endLabel"

private fun FavoriteRouteEntity.matchesSignature(draft: RouteBookmarkDraft): Boolean =
    originLatitude == draft.startPoint.latitude &&
        originLongitude == draft.startPoint.longitude &&
        destinationLatitude == draft.endPoint.latitude &&
        destinationLongitude == draft.endPoint.longitude &&
        routeOption == draft.routeOption.name

private fun FavoriteRouteEntity.toRouteBookmark(): RouteBookmark =
    RouteBookmark(
        bookmarkId = favoriteRouteId.toString(),
        routeName = routeName,
        startLabel = originName,
        endLabel = destinationName,
        startPoint = GeoCoordinate(latitude = originLatitude, longitude = originLongitude),
        endPoint = GeoCoordinate(latitude = destinationLatitude, longitude = destinationLongitude),
        routeOption = routeOption.toRouteOptionOrDefault(),
        transportMode = transportMode,
        routeOptionLabel = routeOption,
        distanceMeters = summaryDistanceMeters,
        durationMinutes = summaryDurationSeconds?.let { it / 60 },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun FavoriteRouteListItemDto.toFavoriteRouteEntity(
    accountScopeKey: String,
    createdAt: Long,
    updatedAt: Long,
    cachedDistanceMeters: Int? = null,
    cachedDurationSeconds: Int? = null,
): FavoriteRouteEntity =
    FavoriteRouteEntity(
        accountScopeKey = accountScopeKey,
        favoriteRouteId = favRouteId,
        routeName = routeName,
        originName = startLabel,
        originLatitude = startPoint.lat,
        originLongitude = startPoint.lng,
        destinationName = endLabel,
        destinationLatitude = endPoint.lat,
        destinationLongitude = endPoint.lng,
        transportMode = transportMode,
        routeOption = routeOption,
        summaryDistanceMeters = cachedDistanceMeters,
        summaryDurationSeconds = cachedDurationSeconds,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun String?.toRouteOptionOrDefault(): RouteOption =
    this?.let { value ->
        RouteOption.values().firstOrNull { it.name == value }
    } ?: RouteOption.SAFE

private fun RouteBookmarkSaveRequest.localCacheFavoriteRouteId(): Long {
    val signature = bookmarkId()
    val hash =
        signature.fold(1_125_899_906_842_597L) { accumulator, character ->
            (accumulator * 31L) + character.code.toLong()
        }
    return when {
        hash == 0L -> -1L
        hash > 0L -> -hash
        hash == Long.MIN_VALUE -> Long.MIN_VALUE + 1L
        else -> hash
    }
}

private fun RouteBookmarkSaveRequest.toUncachedRouteBookmark(now: Long): RouteBookmark =
    RouteBookmark(
        bookmarkId = localCacheFavoriteRouteId().toString(),
        routeName = routeName.trim().ifBlank { fallbackRouteName() },
        startLabel = startLabel,
        endLabel = endLabel,
        startPoint = startPoint,
        endPoint = endPoint,
        routeOption = routeOption,
        distanceMeters = distanceMeters,
        durationMinutes = durationMinutes,
        createdAt = now,
        updatedAt = now,
    )
