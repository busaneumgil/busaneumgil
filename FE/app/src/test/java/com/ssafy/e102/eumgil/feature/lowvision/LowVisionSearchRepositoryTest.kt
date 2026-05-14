package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchPage
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceAnalysis
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionSearchRepositoryTest {
    @Test
    fun `search delegates to backing repository instead of low vision mock data`() =
        runBlocking {
            val delegate =
                RecordingSearchRepository(
                    searchResults =
                        listOf(
                            SearchResult(
                                placeId = "delegate-place",
                                title = "Delegate Result",
                                subtitle = "remote",
                                latitude = 35.0,
                                longitude = 129.0,
                            ),
                        ),
                )
            val repository = LowVisionSearchRepository(delegate = delegate)

            val results = repository.search(SearchQuery(keyword = "Braille"))

            assertEquals(listOf("delegate-place"), results.map(SearchResult::placeId))
            assertTrue(delegate.searchRequests.isEmpty())
            assertEquals(listOf(SearchQuery(keyword = "Braille")), delegate.searchPageRequests)
        }

    @Test
    fun `searchPage delegates pagination to backing repository for live results`() =
        runBlocking {
            val expectedPage =
                SearchPage(
                    results =
                        listOf(
                            SearchResult(
                                placeId = "delegate-page-place",
                                title = "Delegate Page Result",
                                subtitle = "remote page",
                                latitude = 35.1,
                                longitude = 129.1,
                            ),
                        ),
                    nextCursor = "cursor-3",
                    hasNext = true,
                )
            val delegate = RecordingSearchRepository(searchPage = expectedPage)
            val repository = LowVisionSearchRepository(delegate = delegate)
            val query = SearchQuery(keyword = "museum", cursor = "cursor-2")

            val page = repository.searchPage(query)

            assertEquals(expectedPage, page)
            assertEquals(listOf(query), delegate.searchPageRequests)
        }

    @Test
    fun `category result uses places repository category filter around current location`() =
        runBlocking {
            val delegate = RecordingSearchRepository()
            val placesRepository =
                RecordingPlacesRepository(
                    places =
                        listOf(
                            PlaceSummary(
                                placeId = "food-place",
                                name = "Accessible Restaurant",
                                address = "Busan",
                                latitude = 35.2,
                                longitude = 129.2,
                                category = PlaceCategory.FOOD_CAFE,
                                accessibilityTags = listOf("guidance-facility"),
                            ),
                        ),
                )
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    placesRepository = placesRepository,
                    currentLocationProvider = {
                        LocationSnapshot(
                            latitude = 35.1796,
                            longitude = 129.0756,
                            accuracyMeters = 12f,
                            recordedAtEpochMillis = System.currentTimeMillis(),
                        )
                    },
                )

            val page = repository.searchPage(SearchQuery(keyword = "음식점"))

            assertEquals(listOf("food-place"), page.results.map(SearchResult::placeId))
            assertEquals(listOf("Accessible Restaurant"), page.results.map(SearchResult::title))
            assertEquals(listOf("Busan"), page.results.map(SearchResult::subtitle))
            assertEquals(listOf(listOf("guidance-facility")), page.results.map(SearchResult::accessibilityTagKeys))
            assertTrue(delegate.searchPageRequests.isEmpty())

            val query = placesRepository.queries.single()
            assertEquals(setOf(PlaceCategory.FOOD_CAFE, PlaceCategory.RESTAURANT), query.categories)
            assertEquals(35.1796, query.latitude ?: 0.0, 0.0)
            assertEquals(129.0756, query.longitude ?: 0.0, 0.0)
        }

    @Test
    fun `low vision search skips recent search persistence but keeps recent destination persistence`() =
        runBlocking {
            val delegate = RecordingSearchRepository()
            val repository = LowVisionSearchRepository(delegate = delegate)
            val destination =
                RecentDestination(
                    placeId = "place-1",
                    name = "Busan City Hall",
                    latitude = 35.1796,
                    longitude = 129.0756,
                )

            repository.saveRecentSearch("busan")
            repository.saveRecentDestination(destination)

            assertTrue(delegate.savedRecentSearches.isEmpty())
            assertEquals(listOf(destination), delegate.savedRecentDestinations)
        }
}

private class RecordingSearchRepository(
    private val searchResults: List<SearchResult> = emptyList(),
    private val searchPage: SearchPage = SearchPage(results = searchResults),
) : SearchRepository {
    val searchRequests: MutableList<SearchQuery> = mutableListOf()
    val searchPageRequests: MutableList<SearchQuery> = mutableListOf()
    val savedRecentSearches: MutableList<String> = mutableListOf()
    val savedRecentDestinations: MutableList<RecentDestination> = mutableListOf()

    override suspend fun search(query: SearchQuery): List<SearchResult> {
        searchRequests += query
        return searchResults
    }

    override suspend fun searchPage(query: SearchQuery): SearchPage {
        searchPageRequests += query
        return searchPage
    }

    override suspend fun analyzeVoiceSearch(
        text: String,
        mode: com.ssafy.e102.eumgil.core.model.SearchVoiceMode,
    ): SearchVoiceAnalysis = SearchVoiceAnalysis(intent = com.ssafy.e102.eumgil.core.model.SearchVoiceIntent.UNKNOWN)

    override suspend fun getRecentSearches(): List<RecentSearch> = emptyList()

    override suspend fun saveRecentSearch(keyword: String) {
        savedRecentSearches += keyword
    }

    override suspend fun getRecentDestinations(): List<RecentDestination> = emptyList()

    override suspend fun saveRecentDestination(destination: RecentDestination) {
        savedRecentDestinations += destination
    }
}

private class RecordingPlacesRepository(
    private val places: List<PlaceSummary>,
) : PlacesRepository {
    val queries: MutableList<PlaceQuery> = mutableListOf()

    override suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary> {
        queries += query
        return places
    }

    override suspend fun getPlaceDetail(placeId: String): PlaceDetail? = null
}
