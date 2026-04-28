package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.data.local.datasource.SearchLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.SearchMockDataSource
import com.ssafy.e102.eumgil.data.remote.datasource.SearchRemoteDataSource
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryDomain
import com.ssafy.e102.eumgil.data.repository.policy.RepositoryReadPlan
import com.ssafy.e102.eumgil.data.repository.policy.RepositorySourcePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRepositoryTest {
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
