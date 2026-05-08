package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.data.local.datasource.PlacesLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.PlacesMockDataSource
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
}

private class PlacesTestRepositorySourcePolicy(
    private val plan: RepositoryReadPlan,
) : RepositorySourcePolicy {
    override suspend fun readPlan(domain: RepositoryDomain): RepositoryReadPlan = plan
}
