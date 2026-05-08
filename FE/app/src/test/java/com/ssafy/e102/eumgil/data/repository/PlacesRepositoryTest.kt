package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceFeatureAvailability
import com.ssafy.e102.eumgil.core.model.PlaceFeatureType
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.remote.datasource.PlacesRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryReadPlan
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
    fun `getPlaces returns remote data and caches it when remote succeeds`() =
        runBlocking {
            val query =
                PlaceQuery(
                    latitude = 35.1796,
                    longitude = 129.0756,
                    radiusMeters = 1200,
                )
            val localDataSource = PlacesLocalDataSource()
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource =
                        PlacesRemoteDataSource(
                            requestExecutor = { _, _, _ ->
                                HttpJsonResponse(
                                    statusCode = 200,
                                    body =
                                        """
                                        {
                                          "status": "S2000",
                                          "data": {
                                            "places": [
                                              {
                                                "placeId": 88,
                                                "name": "Remote Welfare Center",
                                                "category": "WELFARE",
                                                "address": "88 Welfare-ro, Busan",
                                                "point": {
                                                  "lat": 35.1801,
                                                  "lng": 129.0722
                                                },
                                                "accessibilityFeatures": [
                                                  {
                                                    "featureType": "elevator",
                                                    "isAvailable": true
                                                  }
                                                ],
                                                "isBookmarked": false
                                              }
                                            ]
                                          },
                                          "message": "ok"
                                        }
                                        """.trimIndent(),
                                )
                            },
                        ),
                    localDataSource = localDataSource,
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val places = repository.getPlaces(query)

            assertEquals(1, places.size)
            assertEquals("88", places.first().placeId)
            assertEquals(PlaceCategory.WELFARE, places.first().category)
            assertEquals(
                listOf(
                    PlaceFeatureAvailability(
                        featureType = PlaceFeatureType.ELEVATOR,
                        isAvailable = true,
                    ),
                ),
                places.first().features,
            )
            assertEquals(places, localDataSource.getCachedPlaces(query))
        }

    @Test
    fun `getPlaceDetail returns remote detail and caches it when remote succeeds`() =
        runBlocking {
            val localDataSource = PlacesLocalDataSource()
            val repository =
                DefaultPlacesRepository(
                    remoteDataSource =
                        PlacesRemoteDataSource(
                            requestExecutor = { _, _, _ ->
                                HttpJsonResponse(
                                    statusCode = 200,
                                    body =
                                        """
                                        {
                                          "status": "S2000",
                                          "data": {
                                            "placeId": 88,
                                            "name": "Remote Welfare Center",
                                            "category": "WELFARE",
                                            "address": "88 Welfare-ro, Busan",
                                            "point": {
                                              "lat": 35.1801,
                                              "lng": 129.0722
                                            },
                                            "providerPlaceId": "kakao-88",
                                            "accessibilityFeatures": [
                                              {
                                                "featureType": "elevator",
                                                "isAvailable": true
                                              }
                                            ],
                                            "isBookmarked": true
                                          },
                                          "message": "ok"
                                        }
                                        """.trimIndent(),
                                )
                            },
                        ),
                    localDataSource = localDataSource,
                    mockDataSource = PlacesMockDataSource(),
                    sourcePolicy =
                        PlacesTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val detail = repository.getPlaceDetail("88")

            assertEquals(
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
                ),
                detail,
            )
            assertEquals(detail, localDataSource.getCachedPlaceDetail("88"))
        }
}

private class PlacesTestRepositorySourcePolicy(
    private val plan: RepositoryReadPlan,
) : RepositorySourcePolicy {
    override suspend fun readPlan(domain: RepositoryDomain): RepositoryReadPlan = plan
}
