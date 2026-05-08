package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceFeatureAvailability
import com.ssafy.e102.eumgil.core.model.PlaceFeatureType
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.PlacesRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryReadPlan
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySource
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacesRepositoryTest {
    @Test
    fun `getPlaces returns mock data when policy forces mock`() =
        runBlocking {
            val query = PlaceQuery(keyword = "elevator")
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource = PlacesRemoteDataSource(baseUrl = "https://example.com"),
                    localDataSource = PlacesLocalDataSource(),
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy = PlacesTestRepositorySourcePolicy(RepositoryReadPlan.mockOnly()),
                )

            val places = repository.getPlaces(query)

            assertEquals("mock-place-1", places.first().placeId)
        }

    @Test
    fun `getPlaces returns cached data when live policy falls back from remote to local`() =
        runBlocking {
            val query = PlaceQuery(keyword = "custom")
            val cachedPlace =
                PlaceSummary(
                    placeId = "cached-place-1",
                    name = "Cached Live Place",
                    address = "1 Cached-ro, Busan",
                    latitude = 35.1796,
                    longitude = 129.0756,
                    category = PlaceCategory.OTHER,
                )
            val localDataSource =
                PlacesLocalDataSource().apply {
                    updateCachedPlaces(query = query, places = listOf(cachedPlace))
                }
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource = PlacesRemoteDataSource(baseUrl = "https://example.com"),
                    localDataSource = localDataSource,
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val places = repository.getPlaces(query)

            assertEquals(listOf(cachedPlace), places)
        }

    @Test
    fun `getPlaces throws remote failure when live policy has no cached fallback`() =
        runBlocking {
            val query = PlaceQuery(keyword = "custom")
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource =
                        object : PlacesRemoteDataSource(
                            requestExecutor = { _, _, _ -> error("unused") },
                        ) {
                            override suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary> {
                                throw IllegalStateException("remote places failed")
                            }
                        },
                    localDataSource = PlacesLocalDataSource(),
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(
                            RepositoryReadPlan(
                                sources = listOf(RepositorySource.REMOTE, RepositorySource.LOCAL),
                            ),
                        ),
                )

            val failure = runCatching { repository.getPlaces(query) }.exceptionOrNull()

            assertEquals("remote places failed", failure?.message)
        }

    @Test
    fun `getPlaces returns remote data and caches it when remote succeeds`() =
        runBlocking {
            val query =
                PlaceQuery(
                    latitude = 35.1796,
                    longitude = 129.0756,
                    radiusMeters = 1200,
                )
            val localDataSource = PlacesLocalDataSource()
            val remotePlaces =
                listOf(
                    PlaceSummary(
                        placeId = "88",
                        name = "Remote Welfare Center",
                        address = "88 Welfare-ro, Busan",
                        latitude = 35.1801,
                        longitude = 129.0722,
                        category = PlaceCategory.WELFARE,
                        features =
                            listOf(
                                PlaceFeatureAvailability(
                                    featureType = PlaceFeatureType.ELEVATOR,
                                    isAvailable = true,
                                ),
                            ),
                    ),
                )
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource =
                        object : PlacesRemoteDataSource(
                            requestExecutor = { _, _, _ -> error("unused") },
                        ) {
                            override suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary> = remotePlaces
                        },
                    localDataSource = localDataSource,
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val places = repository.getPlaces(query)

            assertEquals(remotePlaces, places)
            assertEquals(places, localDataSource.getCachedPlaces(query))
        }

    @Test
    fun `getPlaceDetail returns remote detail and caches it when remote succeeds`() =
        runBlocking {
            val localDataSource = PlacesLocalDataSource()
            val remoteDetail =
                PlaceDetail(
                    placeId = "88",
                    name = "Remote Welfare Center",
                    address = "88 Welfare-ro, Busan",
                    latitude = 35.1801,
                    longitude = 129.0722,
                    category = PlaceCategory.WELFARE,
                    features =
                        listOf(
                            PlaceFeatureAvailability(
                                featureType = PlaceFeatureType.ELEVATOR,
                                isAvailable = true,
                            ),
                        ),
                    isBookmarked = true,
                    accessibilityTags = listOf("elevator"),
                    providerPlaceId = "kakao-88",
                    description = null,
                )
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource =
                        object : PlacesRemoteDataSource(
                            requestExecutor = { _, _, _ -> error("unused") },
                        ) {
                            override suspend fun getPlaceDetail(placeId: String): PlaceDetail? = remoteDetail
                        },
                    localDataSource = localDataSource,
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val detail = repository.getPlaceDetail("88")

            assertEquals(remoteDetail, detail)
            assertEquals(detail, localDataSource.getCachedPlaceDetail("88"))
        }

    @Test
    fun `getPlaceDetail returns null when remote detail responds with 404`() =
        runBlocking {
            val localDataSource = PlacesLocalDataSource()
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource =
                        object : PlacesRemoteDataSource(
                            requestExecutor = { _, _, _ -> error("unused") },
                        ) {
                            override suspend fun getPlaceDetail(placeId: String): PlaceDetail? = null
                        },
                    localDataSource = localDataSource,
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val detail = repository.getPlaceDetail("404")

            assertEquals(null, detail)
            assertEquals(null, localDataSource.getCachedPlaceDetail("404"))
        }

    @Test
    fun `getPlaceDetail returns cached detail when live policy falls back from remote to local`() =
        runBlocking {
            val cachedDetail =
                PlaceDetail(
                    placeId = "cached-place-1",
                    name = "Cached Place Detail",
                    address = "1 Cached-ro, Busan",
                    latitude = 35.1796,
                    longitude = 129.0756,
                    category = PlaceCategory.PUBLIC_OFFICE,
                    accessibilityTags = listOf("elevator"),
                )
            val localDataSource =
                PlacesLocalDataSource().apply {
                    updateCachedPlaceDetail(cachedDetail)
                }
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource = PlacesRemoteDataSource(baseUrl = "https://example.com"),
                    localDataSource = localDataSource,
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy = PlacesTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val detail = repository.getPlaceDetail("cached-place-1")

            assertEquals(cachedDetail, detail)
        }

    @Test
    fun `getPlaceDetail throws remote failure when live policy has no cached fallback`() =
        runBlocking {
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource =
                        object : PlacesRemoteDataSource(
                            requestExecutor = { _, _, _ -> error("unused") },
                        ) {
                            override suspend fun getPlaceDetail(placeId: String): PlaceDetail? {
                                throw IllegalStateException("remote place detail failed")
                            }
                        },
                    localDataSource = PlacesLocalDataSource(),
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(
                            RepositoryReadPlan(
                                sources = listOf(RepositorySource.REMOTE, RepositorySource.LOCAL),
                            ),
                        ),
                )

            val failure = runCatching { repository.getPlaceDetail("missing-place") }.exceptionOrNull()

            assertEquals("remote place detail failed", failure?.message)
        }
}

private class PlacesTestRepositorySourcePolicy(
    private val plan: RepositoryReadPlan,
) : RepositorySourcePolicy {
    override suspend fun readPlan(domain: RepositoryDomain): RepositoryReadPlan = plan
}
