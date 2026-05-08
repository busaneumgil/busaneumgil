package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteBookmarkDraft
import com.ssafy.e102.eumgil.core.model.RouteBookmarkSaveRequest
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.local.dao.FavoriteRouteDao
import com.ssafy.e102.eumgil.data.local.entity.FavoriteRouteEntity
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.FavoriteRoutesRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.CreateFavoriteRouteResponseDto
import com.ssafy.e102.eumgil.data.remote.dto.FavoriteRouteListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.FavoriteRoutePageDto
import com.ssafy.e102.eumgil.data.remote.dto.FavoriteRoutePointDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteBookmarkRepositoryTest {
    @Test
    fun `observeRouteBookmarks fetches from server and replaces cache when token is provided`() =
        runBlocking {
            val serverItem =
                FavoriteRouteListItemDto(
                    favRouteId = 7L,
                    routeName = "집에서 병원",
                    startLabel = "부산시민공원",
                    endLabel = "부산역",
                    startPoint = FavoriteRoutePointDto(lat = 35.1686, lng = 129.0576),
                    endPoint = FavoriteRoutePointDto(lat = 35.1152, lng = 129.0422),
                    transportMode = "WALK",
                    routeOption = "SAFE",
                )
            val staleEntity = testFavoriteRouteEntity(favoriteRouteId = 99L, routeName = "stale-cache")
            val fakeDao = FakeFavoriteRouteDao(routes = listOf(staleEntity))
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource(serverContent = listOf(serverItem))

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            val bookmarks = repository.observeRouteBookmarks().first()

            assertEquals(1, bookmarks.size)
            assertEquals("7", bookmarks[0].bookmarkId)
            assertEquals("집에서 병원", bookmarks[0].routeName)
            assertEquals(RouteOption.SAFE, bookmarks[0].routeOption)
            assertEquals("WALK", bookmarks[0].transportMode)
            assertEquals("SAFE", bookmarks[0].routeOptionLabel)
        }

    @Test
    fun `observeRouteBookmarks preserves cached distance and duration after server refresh`() =
        runBlocking {
            val cachedEntity =
                testFavoriteRouteEntity(favoriteRouteId = 7L, routeName = "cached")
                    .copy(summaryDistanceMeters = 3250, summaryDurationSeconds = 1440)
            val serverItem =
                FavoriteRouteListItemDto(
                    favRouteId = 7L,
                    routeName = "renamed",
                    startLabel = "출발",
                    endLabel = "도착",
                    startPoint = FavoriteRoutePointDto(lat = 35.0, lng = 129.0),
                    endPoint = FavoriteRoutePointDto(lat = 35.1, lng = 129.1),
                    transportMode = "WALK",
                    routeOption = "SAFE",
                )
            val fakeDao = FakeFavoriteRouteDao(routes = listOf(cachedEntity))
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource(serverContent = listOf(serverItem))

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            val bookmarks = repository.observeRouteBookmarks().first()

            assertEquals(1, bookmarks.size)
            assertEquals(3250, bookmarks[0].distanceMeters)
            assertEquals(24, bookmarks[0].durationMinutes)
            assertEquals("renamed", bookmarks[0].routeName)
        }

    @Test
    fun `observeRouteBookmarks preserves transit transport mode and unknown route option label`() =
        runBlocking {
            val serverItem =
                FavoriteRouteListItemDto(
                    favRouteId = 11L,
                    routeName = "transit-route",
                    startLabel = "출발",
                    endLabel = "도착",
                    startPoint = FavoriteRoutePointDto(lat = 35.0, lng = 129.0),
                    endPoint = FavoriteRoutePointDto(lat = 35.1, lng = 129.1),
                    transportMode = "PUBLIC_TRANSIT",
                    routeOption = "MIN_TRANSFER",
                )
            val fakeDao = FakeFavoriteRouteDao()
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource(serverContent = listOf(serverItem))

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            val bookmarks = repository.observeRouteBookmarks().first()

            assertEquals("PUBLIC_TRANSIT", bookmarks[0].transportMode)
            assertEquals("MIN_TRANSFER", bookmarks[0].routeOptionLabel)
            assertEquals(RouteOption.SAFE, bookmarks[0].routeOption)
        }

    @Test
    fun `observeRouteBookmarks falls back to cache when server fetch fails`() =
        runBlocking {
            val cachedEntity = testFavoriteRouteEntity(favoriteRouteId = 1L, routeName = "cached")
            val fakeDao = FakeFavoriteRouteDao(routes = listOf(cachedEntity))
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource(throwOnGet = true)

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            val bookmarks = repository.observeRouteBookmarks().first()

            assertEquals(1, bookmarks.size)
            assertEquals("cached", bookmarks[0].routeName)
        }

    @Test
    fun `observeRouteBookmarks skips server fetch when access token is null`() =
        runBlocking {
            val cachedEntity = testFavoriteRouteEntity(favoriteRouteId = 1L, routeName = "cached")
            val fakeDao = FakeFavoriteRouteDao(routes = listOf(cachedEntity))
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource()

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { null },
                )

            val bookmarks = repository.observeRouteBookmarks().first()

            assertEquals(1, bookmarks.size)
            assertEquals(0, fakeDataSource.getFavoriteRoutesCallCount)
        }

    @Test
    fun `saveRouteBookmark posts to server and returns bookmark with server id`() =
        runBlocking {
            val fakeDao = FakeFavoriteRouteDao()
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource(createdId = 42L)

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            val saved = repository.saveRouteBookmark(testSaveRequest())

            assertEquals("42", saved.bookmarkId)
            assertEquals(1, fakeDataSource.createCallCount)
            assertEquals(1, fakeDao.routes().size)
        }

    @Test
    fun `saveRouteBookmark caches locally when access token is null`() =
        runBlocking {
            val fakeDao = FakeFavoriteRouteDao()
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource()

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { null },
                )

            val saved = repository.saveRouteBookmark(testSaveRequest())

            assertTrue(saved.bookmarkId.startsWith("route-bookmark:"))
            assertEquals(0, fakeDataSource.createCallCount)
            assertEquals(1, fakeDao.routes().size)
        }

    @Test
    fun `deleteRouteBookmark calls server and removes cache when bookmarkId is numeric`() =
        runBlocking {
            val cachedEntity = testFavoriteRouteEntity(favoriteRouteId = 7L, routeName = "to-delete")
            val fakeDao = FakeFavoriteRouteDao(routes = listOf(cachedEntity))
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource()

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            repository.deleteRouteBookmark("7")

            assertEquals(listOf(7L), fakeDataSource.deletedFavRouteIds)
            assertEquals(0, fakeDao.routes().size)
        }

    @Test
    fun `deleteRouteBookmark removes cache even when server call fails`() =
        runBlocking {
            val cachedEntity = testFavoriteRouteEntity(favoriteRouteId = 7L, routeName = "to-delete")
            val fakeDao = FakeFavoriteRouteDao(routes = listOf(cachedEntity))
            val fakeDataSource = FakeFavoriteRoutesRemoteDataSource(throwOnDelete = true)

            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            repository.deleteRouteBookmark("7")

            assertEquals(0, fakeDao.routes().size)
        }

    @Test
    fun `isBookmarked returns true when matching coordinates and option are in cache`() =
        runBlocking {
            val cachedEntity =
                FavoriteRouteEntity(
                    favoriteRouteId = 1L,
                    routeName = "test",
                    originName = "출발",
                    originLatitude = 35.1686,
                    originLongitude = 129.0576,
                    destinationName = "도착",
                    destinationLatitude = 35.1152,
                    destinationLongitude = 129.0422,
                    routeOption = "SAFE",
                )
            val fakeDao = FakeFavoriteRouteDao(routes = listOf(cachedEntity))
            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = null,
                    accessTokenProvider = { null },
                )

            val matched =
                repository.isBookmarked(
                    RouteBookmarkDraft(
                        startLabel = "출발",
                        endLabel = "도착",
                        startPoint = GeoCoordinate(latitude = 35.1686, longitude = 129.0576),
                        endPoint = GeoCoordinate(latitude = 35.1152, longitude = 129.0422),
                        routeOption = RouteOption.SAFE,
                    ),
                )

            assertTrue(matched)
        }

    @Test
    fun `isBookmarked returns false when no matching signature in cache`() =
        runBlocking {
            val fakeDao = FakeFavoriteRouteDao()
            val repository =
                DefaultRouteBookmarkRepository(
                    favoriteRouteDao = fakeDao,
                    favoriteRoutesRemoteDataSource = null,
                    accessTokenProvider = { null },
                )

            val matched =
                repository.isBookmarked(
                    RouteBookmarkDraft(
                        startLabel = "출발",
                        endLabel = "도착",
                        startPoint = GeoCoordinate(latitude = 0.0, longitude = 0.0),
                        endPoint = GeoCoordinate(latitude = 0.0, longitude = 0.0),
                        routeOption = RouteOption.SAFE,
                    ),
                )

            assertFalse(matched)
        }
}

private fun testSaveRequest(): RouteBookmarkSaveRequest =
    RouteBookmarkSaveRequest(
        routeName = "집에서 병원",
        startLabel = "부산시민공원",
        endLabel = "부산역",
        startPoint = GeoCoordinate(latitude = 35.1686, longitude = 129.0576),
        endPoint = GeoCoordinate(latitude = 35.1152, longitude = 129.0422),
        routeOption = RouteOption.SAFE,
    )

private fun testFavoriteRouteEntity(
    favoriteRouteId: Long,
    routeName: String,
): FavoriteRouteEntity =
    FavoriteRouteEntity(
        favoriteRouteId = favoriteRouteId,
        routeName = routeName,
        originName = "출발지",
        originLatitude = 35.0,
        originLongitude = 129.0,
        destinationName = "도착지",
        destinationLatitude = 35.1,
        destinationLongitude = 129.1,
        routeOption = "SAFE",
    )

private class FakeFavoriteRouteDao(
    routes: List<FavoriteRouteEntity> = emptyList(),
) : FavoriteRouteDao {
    private val state = MutableStateFlow(routes)

    fun routes(): List<FavoriteRouteEntity> = state.value

    override fun observeFavoriteRoutes(): Flow<List<FavoriteRouteEntity>> = state

    override fun observeFavoriteRoute(favoriteRouteId: Long): Flow<FavoriteRouteEntity?> =
        MutableStateFlow(state.value.firstOrNull { it.favoriteRouteId == favoriteRouteId })

    override suspend fun getFavoriteRoute(favoriteRouteId: Long): FavoriteRouteEntity? =
        state.value.firstOrNull { it.favoriteRouteId == favoriteRouteId }

    override suspend fun upsertFavoriteRoute(favoriteRoute: FavoriteRouteEntity) {
        state.value = state.value.upsert(favoriteRoute)
    }

    override suspend fun upsertFavoriteRoutes(favoriteRoutes: List<FavoriteRouteEntity>) {
        favoriteRoutes.forEach { upsertFavoriteRoute(it) }
    }

    override suspend fun deleteFavoriteRoute(favoriteRouteId: Long) {
        state.value = state.value.filterNot { it.favoriteRouteId == favoriteRouteId }
    }

    override suspend fun clearFavoriteRoutes() {
        state.value = emptyList()
    }
}

private fun List<FavoriteRouteEntity>.upsert(entity: FavoriteRouteEntity): List<FavoriteRouteEntity> =
    if (entity.favoriteRouteId == 0L) {
        this + entity.copy(favoriteRouteId = (maxOfOrNull { it.favoriteRouteId } ?: 0L) + 1L)
    } else {
        filterNot { it.favoriteRouteId == entity.favoriteRouteId } + entity
    }

private class FakeFavoriteRoutesRemoteDataSource(
    private val serverContent: List<FavoriteRouteListItemDto> = emptyList(),
    private val createdId: Long = 1L,
    private val throwOnGet: Boolean = false,
    private val throwOnDelete: Boolean = false,
) : FavoriteRoutesRemoteDataSource(httpJsonClient = HttpJsonClient(baseUrl = "http://test.invalid")) {
    val deletedFavRouteIds = mutableListOf<Long>()
    var getFavoriteRoutesCallCount: Int = 0
        private set
    var createCallCount: Int = 0
        private set

    override suspend fun getFavoriteRoutes(
        accessToken: String,
        cursor: Long?,
        size: Int?,
    ): FavoriteRoutePageDto {
        getFavoriteRoutesCallCount++
        if (throwOnGet) throw RuntimeException("server get failure")
        return FavoriteRoutePageDto(
            content = serverContent,
            size = size ?: serverContent.size,
            nextCursor = null,
            hasNext = false,
        )
    }

    override suspend fun createFavoriteRoute(
        accessToken: String,
        startLabel: String,
        endLabel: String,
        startPoint: FavoriteRoutePointDto,
        endPoint: FavoriteRoutePointDto,
        routeOption: String,
    ): CreateFavoriteRouteResponseDto {
        createCallCount++
        return CreateFavoriteRouteResponseDto(favRouteId = createdId)
    }

    override suspend fun updateFavoriteRoute(
        accessToken: String,
        favRouteId: Long,
        startLabel: String?,
        endLabel: String?,
        startPoint: FavoriteRoutePointDto?,
        endPoint: FavoriteRoutePointDto?,
        routeOption: String?,
    ) {
        // no-op for test
    }

    override suspend fun deleteFavoriteRoute(
        accessToken: String,
        favRouteId: Long,
    ) {
        if (throwOnDelete) throw RuntimeException("server delete failure")
        deletedFavRouteIds.add(favRouteId)
    }
}
