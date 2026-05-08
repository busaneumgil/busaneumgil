package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.data.local.datasource.SearchLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.remote.datasource.SearchRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryReadPlan
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySource
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRepositoryTest {
    @Test
    fun `search returns remote provider results and caches them for the same query`() =
        runBlocking {
            val query = SearchQuery(keyword = "Busan Tower", limit = 2)
            val localDataSource = SearchLocalDataSource()
            val repository =
                DefaultSearchRepository(
                    remoteDataSource =
                        SearchRemoteDataSource(
                            getRequestExecutor = { _, _, _ ->
                                HttpJsonResponse(
                                    statusCode = 200,
                                    body =
                                        """
                                        {
                                          "status": "S2000",
                                          "data": {
                                            "places": [
                                              {
                                                "placeId": 10,
                                                "provider": "KAKAO",
                                                "providerPlaceId": "123456789",
                                                "name": "Busan Tower",
                                                "category": "TOURIST_SPOT",
                                                "address": "1 Yongdusan-gil, Busan",
                                                "distanceMeter": 350,
                                                "point": {
                                                  "lat": 35.1000,
                                                  "lng": 129.0320
                                                },
                                                "accessibilityFeatures": [],
                                                "matched": true
                                              },
                                              {
                                                "placeId": null,
                                                "provider": "KAKAO",
                                                "providerPlaceId": "987654321",
                                                "name": "Provider Only Cafe",
                                                "category": null,
                                                "address": "2 Gwangbok-ro, Busan",
                                                "distanceMeter": 120,
                                                "point": {
                                                  "lat": 35.1010,
                                                  "lng": 129.0330
                                                },
                                                "accessibilityFeatures": [],
                                                "matched": false
                                              }
                                            ],
                                            "nextCursor": null,
                                            "size": 2,
                                            "totalElements": 2,
                                            "hasNext": false
                                          },
                                          "message": "ok"
                                        }
                                        """.trimIndent(),
                                )
                            },
                            postRequestExecutor = { _, _, _ -> error("voice analyze should not run from search()") },
                            accessTokenProvider = { "access-token" },
                        ),
                    localDataSource = localDataSource,
                    mockDataSource = SearchMockDataSource(),
                    sourcePolicy = SearchTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val results = repository.search(query)

            assertEquals(listOf("10", "provider:kakao:987654321"), results.map(SearchResult::placeId))
            assertEquals(listOf("10", null), results.map(SearchResult::serverPlaceId))
            assertEquals(results, localDataSource.getCachedResults(query))
        }

    @Test
    fun `search returns mock data when policy forces mock`() =
        runBlocking {
            val query = SearchQuery(keyword = "Braille")
            val repository =
                DefaultSearchRepository(
                    remoteDataSource = SearchRemoteDataSource(baseUrl = "https://example.com"),
                    localDataSource = SearchLocalDataSource(),
                    mockDataSource = SearchMockDataSource(),
                    sourcePolicy = SearchTestRepositorySourcePolicy(RepositoryReadPlan.mockOnly()),
                )

            val results = repository.search(query)

            assertEquals("mock-place-3", results.first().placeId)
        }

    @Test
    fun `search returns cached data when live policy falls back from remote to local`() =
        runBlocking {
            val query = SearchQuery(keyword = "custom")
            val cachedResult =
                SearchResult(
                    placeId = "cached-search-1",
                    title = "Cached Search Result",
                    subtitle = "1 Cached-ro, Busan",
                    latitude = 35.1796,
                    longitude = 129.0756,
                )
            val localDataSource =
                SearchLocalDataSource().apply {
                    updateCachedResults(query = query, results = listOf(cachedResult))
                }
            val repository =
                DefaultSearchRepository(
                    remoteDataSource = SearchRemoteDataSource(baseUrl = "https://example.com"),
                    localDataSource = localDataSource,
                    mockDataSource = SearchMockDataSource(),
                    sourcePolicy =
                        SearchTestRepositorySourcePolicy(RepositoryReadPlan.remoteLocalMock()),
                )

            val results = repository.search(query)

            assertEquals(listOf(cachedResult), results)
        }

    @Test
    fun `search throws remote failure when live policy has no cached fallback`() =
        runBlocking {
            val query = SearchQuery(keyword = "custom")
            val repository =
                DefaultSearchRepository(
                    remoteDataSource =
                        object : SearchRemoteDataSource(
                            getRequestExecutor = { _, _, _ -> error("unused") },
                            postRequestExecutor = { _, _, _ -> error("unused") },
                        ) {
                            override suspend fun search(query: SearchQuery): List<SearchResult> {
                                throw IllegalStateException("remote search failed")
                            }
                        },
                    localDataSource = SearchLocalDataSource(),
                    mockDataSource = SearchMockDataSource(),
                    sourcePolicy =
                        SearchTestRepositorySourcePolicy(
                            RepositoryReadPlan(
                                sources = listOf(RepositorySource.REMOTE, RepositorySource.LOCAL),
                            ),
                        ),
                )

            val failure = runCatching { repository.search(query) }.exceptionOrNull()

            assertEquals("remote search failed", failure?.message)
        }

    @Test
    fun `recent destinations keep latest order and dedupe by place`() =
        runBlocking {
            val repository =
                DefaultSearchRepository(
                    remoteDataSource = SearchRemoteDataSource(baseUrl = "https://example.com"),
                    localDataSource = SearchLocalDataSource(),
                    mockDataSource = SearchMockDataSource(),
                    sourcePolicy = SearchTestRepositorySourcePolicy(RepositoryReadPlan.localOnly()),
                )

            repository.saveRecentDestination(
                RecentDestination(
                    placeId = "place-1",
                    name = "Busan City Hall",
                    address = "1 Jungang-daero, Busan",
                    latitude = 35.1796,
                    longitude = 129.0756,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                    searchedAtMillis = 1_000L,
                ),
            )
            repository.saveRecentDestination(
                RecentDestination(
                    placeId = "place-2",
                    name = "Busan Station",
                    address = "2 Jungang-daero, Busan",
                    latitude = 35.1152,
                    longitude = 129.0416,
                    category = PlaceCategory.ELEVATOR,
                    searchedAtMillis = 2_000L,
                ),
            )
            repository.saveRecentDestination(
                RecentDestination(
                    placeId = "place-1",
                    name = "Busan City Hall",
                    address = "1 Jungang-daero, Busan",
                    latitude = 35.1796,
                    longitude = 129.0756,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                    searchedAtMillis = 3_000L,
                ),
            )

            val results = repository.getRecentDestinations()

            assertEquals(listOf("place-1", "place-2"), results.map { recentDestination -> recentDestination.placeId })
        }
}

private class SearchTestRepositorySourcePolicy(
    private val plan: RepositoryReadPlan,
) : RepositorySourcePolicy {
    override suspend fun readPlan(domain: RepositoryDomain): RepositoryReadPlan = plan
}
