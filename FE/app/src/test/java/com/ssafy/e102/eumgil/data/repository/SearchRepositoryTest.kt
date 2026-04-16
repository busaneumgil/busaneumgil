package com.ssafy.e102.eumgil.data.repository

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
}

private class SearchTestRepositorySourcePolicy(
    private val plan: RepositoryReadPlan,
) : RepositorySourcePolicy {
    override suspend fun readPlan(domain: RepositoryDomain): RepositoryReadPlan = plan
}
