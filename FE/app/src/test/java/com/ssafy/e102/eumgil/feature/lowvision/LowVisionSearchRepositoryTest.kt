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
import com.ssafy.e102.eumgil.core.model.SearchVoiceIntent
import com.ssafy.e102.eumgil.core.model.SearchVoiceMode
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    fun `searchPage sorts live results by current location for low vision mode`() =
        runBlocking {
            val delegate =
                RecordingSearchRepository(
                    searchPage =
                        SearchPage(
                            results =
                                listOf(
                                    SearchResult(
                                        placeId = "far-place",
                                        title = "Far Place",
                                        subtitle = "far",
                                        latitude = 35.1820,
                                        longitude = 129.0790,
                                    ),
                                    SearchResult(
                                        placeId = "near-place",
                                        title = "Near Place",
                                        subtitle = "near",
                                        latitude = 35.1797,
                                        longitude = 129.0757,
                                    ),
                                ),
                        ),
                )
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    currentLocationProvider = { freshCurrentLocationSnapshot() },
                )

            val page = repository.searchPage(SearchQuery(keyword = "시설"))

            assertEquals(listOf("near-place", "far-place"), page.results.map(SearchResult::placeId))
            assertEquals(
                listOf(
                    SearchQuery(
                        keyword = "시설",
                        latitude = 35.1796,
                        longitude = 129.0756,
                        radiusMeters = 3_000,
                    ),
                ),
                delegate.searchPageRequests,
            )
        }

    @Test
    fun `searchPage does not use stale location as a live search anchor`() =
        runBlocking {
            val staleLocation =
                LocationSnapshot(
                    latitude = 35.1796,
                    longitude = 129.0756,
                    accuracyMeters = 12f,
                    recordedAtEpochMillis = 1L,
                )
            val delegate =
                RecordingSearchRepository(
                    searchPage =
                        SearchPage(
                            results =
                                listOf(
                                    SearchResult(
                                        placeId = "far-place",
                                        title = "Far Place",
                                        subtitle = "far",
                                        latitude = 35.1820,
                                        longitude = 129.0790,
                                    ),
                                    SearchResult(
                                        placeId = "near-place",
                                        title = "Near Place",
                                        subtitle = "near",
                                        latitude = 35.1797,
                                        longitude = 129.0757,
                                    ),
                                ),
                        ),
                )
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    currentLocationProvider = { staleLocation },
                )

            val page = repository.searchPage(SearchQuery(keyword = "시설"))

            assertEquals(listOf("far-place", "near-place"), page.results.map(SearchResult::placeId))
            assertEquals(listOf(SearchQuery(keyword = "시설")), delegate.searchPageRequests)
        }

    @Test
    fun `category result uses places repository category filter around current location and sorts nearest first`() =
        runBlocking {
            val delegate = RecordingSearchRepository()
            val placesRepository =
                RecordingPlacesRepository(
                    places =
                        listOf(
                            PlaceSummary(
                                placeId = "far-place",
                                name = "Far Place",
                                address = "Busan",
                                latitude = 35.1896,
                                longitude = 129.0856,
                                category = PlaceCategory.RESTAURANT,
                                accessibilityTags = emptyList(),
                            ),
                            PlaceSummary(
                                placeId = "food-place",
                                name = "Accessible Restaurant",
                                address = "Busan",
                                latitude = 35.1797,
                                longitude = 129.0757,
                                category = PlaceCategory.FOOD_CAFE,
                                accessibilityTags = listOf("guidance-facility"),
                            ),
                        ),
                )
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    placesRepository = placesRepository,
                    currentLocationProvider = { freshCurrentLocationSnapshot() },
                )

            val page = repository.searchPage(SearchQuery(keyword = "음식점"))

            assertEquals(listOf("food-place", "far-place"), page.results.map(SearchResult::placeId))
            assertEquals(listOf("Accessible Restaurant", "Far Place"), page.results.map(SearchResult::title))
            assertEquals(listOf("Busan", "Busan"), page.results.map(SearchResult::subtitle))
            assertEquals(
                listOf(listOf("guidance-facility"), emptyList<String>()),
                page.results.map(SearchResult::accessibilityTagKeys),
            )
            assertTrue(delegate.searchPageRequests.isEmpty())

            val query = placesRepository.queries.single()
            assertEquals(setOf(PlaceCategory.FOOD_CAFE, PlaceCategory.RESTAURANT), query.categories)
            assertEquals(3_000, query.radiusMeters)
            assertEquals(35.1796, query.latitude ?: 0.0, 0.0)
            assertEquals(129.0756, query.longitude ?: 0.0, 0.0)
        }

    @Test
    fun `category result falls back to live keyword search when places repository is empty`() =
        runBlocking {
            val delegate =
                RecordingSearchRepository(
                    searchPage =
                        SearchPage(
                            results =
                                listOf(
                                    SearchResult(
                                        placeId = "hospital-search",
                                        title = "Nearby Hospital",
                                        subtitle = "Busan",
                                        latitude = 35.1797,
                                        longitude = 129.0757,
                                    ),
                                ),
                        ),
                )
            val placesRepository = RecordingPlacesRepository(places = emptyList())
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    placesRepository = placesRepository,
                    currentLocationProvider = { freshCurrentLocationSnapshot() },
                )

            val page = repository.searchPage(SearchQuery(keyword = "병원"))

            assertEquals(listOf("hospital-search"), page.results.map(SearchResult::placeId))
            assertEquals(setOf(PlaceCategory.HEALTHCARE), placesRepository.queries.single().categories)
            assertEquals(3_000, placesRepository.queries.single().radiusMeters)
            assertEquals(
                listOf(
                    SearchQuery(
                        keyword = "병원",
                        latitude = 35.1796,
                        longitude = 129.0756,
                        radiusMeters = 3_000,
                    ),
                ),
                delegate.searchPageRequests,
            )
        }

    @Test
    fun `category result falls back to live keyword search when places repository fails`() =
        runBlocking {
            val delegate =
                RecordingSearchRepository(
                    searchPage =
                        SearchPage(
                            results =
                                listOf(
                                    SearchResult(
                                        placeId = "hospital-search",
                                        title = "Nearby Hospital",
                                        subtitle = "Busan",
                                        latitude = 35.1797,
                                        longitude = 129.0757,
                                    ),
                                ),
                        ),
                )
            val placesRepository =
                RecordingPlacesRepository(
                    places = emptyList(),
                    failure = IllegalStateException("places unavailable"),
                )
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    placesRepository = placesRepository,
                    currentLocationProvider = { freshCurrentLocationSnapshot() },
                )

            val page = repository.searchPage(SearchQuery(keyword = "병원"))

            assertEquals(listOf("hospital-search"), page.results.map(SearchResult::placeId))
            assertEquals(
                listOf(
                    SearchQuery(
                        keyword = "병원",
                        latitude = 35.1796,
                        longitude = 129.0756,
                        radiusMeters = 3_000,
                    ),
                ),
                delegate.searchPageRequests,
            )
        }

    @Test
    fun `category result reinforces welfare and public office live keyword fallback when exact label is empty`() =
        runBlocking {
            val scenarios =
                listOf(
                    Triple(
                        "복지관",
                        "복지센터",
                        PlaceCategory.WELFARE,
                    ),
                    Triple(
                        "관공서",
                        "주민센터",
                        PlaceCategory.PUBLIC_OFFICE,
                    ),
                )

            scenarios.forEach { (label, reinforcedKeyword, expectedCategory) ->
                val delegate =
                    RecordingSearchRepository(
                        searchPagesByKeyword =
                            mapOf(
                                label to SearchPage(results = emptyList()),
                                reinforcedKeyword to
                                    SearchPage(
                                        results =
                                            listOf(
                                                SearchResult(
                                                    placeId = "$reinforcedKeyword-place",
                                                    title = "$reinforcedKeyword Result",
                                                    subtitle = "Busan",
                                                    latitude = 35.1797,
                                                    longitude = 129.0757,
                                                ),
                                            ),
                                    ),
                            ),
                    )
                val placesRepository = RecordingPlacesRepository(places = emptyList())
                val repository =
                    LowVisionSearchRepository(
                        delegate = delegate,
                        placesRepository = placesRepository,
                        currentLocationProvider = { freshCurrentLocationSnapshot() },
                    )

                val page = repository.searchPage(SearchQuery(keyword = label))

                assertEquals(listOf("$reinforcedKeyword-place"), page.results.map(SearchResult::placeId))
                assertEquals(expectedCategory, placesRepository.queries.single().categories.single())
                assertEquals(listOf(label, reinforcedKeyword), delegate.searchPageRequests.map(SearchQuery::keyword))
                delegate.searchPageRequests.forEach { request ->
                    assertEquals(35.1796, request.latitude ?: 0.0, 0.0)
                    assertEquals(129.0756, request.longitude ?: 0.0, 0.0)
                    assertEquals(3_000, request.radiusMeters)
                }
            }
        }

    @Test
    fun `category result maps every low vision category label to place filters`() =
        runBlocking {
            val expectedFiltersByLabel =
                mapOf(
                    "음식점" to setOf(PlaceCategory.FOOD_CAFE, PlaceCategory.RESTAURANT),
                    "관광지" to setOf(PlaceCategory.TOURIST_SPOT, PlaceCategory.TOURIST_ATTRACTION),
                    "숙박시설" to setOf(PlaceCategory.ACCOMMODATION),
                    "병원" to setOf(PlaceCategory.HEALTHCARE),
                    "복지관" to setOf(PlaceCategory.WELFARE),
                    "관공서" to setOf(PlaceCategory.PUBLIC_OFFICE),
                )

            expectedFiltersByLabel.forEach { (label, expectedFilters) ->
                val placesRepository =
                    RecordingPlacesRepository(
                        places =
                            listOf(
                                PlaceSummary(
                                    placeId = "$label-place",
                                    name = "$label Result",
                                    address = "Busan",
                                    latitude = 35.1797,
                                    longitude = 129.0757,
                                    category = expectedFilters.first(),
                                    accessibilityTags = emptyList(),
                                ),
                            ),
                    )
                val repository =
                    LowVisionSearchRepository(
                        delegate = RecordingSearchRepository(),
                        placesRepository = placesRepository,
                        currentLocationProvider = { freshCurrentLocationSnapshot() },
                    )

                val page = repository.searchPage(SearchQuery(keyword = label))

                assertEquals(listOf("$label-place"), page.results.map(SearchResult::placeId))
                assertEquals(expectedFilters, placesRepository.queries.single().categories)
            }
        }

    @Test
    fun `category result requires current location instead of using a default fallback coordinate`() =
        runBlocking {
            val delegate = RecordingSearchRepository()
            val placesRepository =
                RecordingPlacesRepository(places = emptyList())
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    placesRepository = placesRepository,
                    currentLocationProvider = { null },
                )

            val error =
                assertThrows(LowVisionCurrentLocationRequiredException::class.java) {
                    runBlocking {
                        repository.searchPage(SearchQuery(keyword = "음식점"))
                    }
                }

            assertEquals(LOW_VISION_CURRENT_LOCATION_REQUIRED_MESSAGE, error.message)
            assertTrue(placesRepository.queries.isEmpty())
            assertTrue(delegate.searchPageRequests.isEmpty())
        }

    @Test
    fun `category result uses approximate known current location when fresh location is unavailable`() =
        runBlocking {
            val approximateKnownLocation =
                LocationSnapshot(
                    latitude = 35.1811,
                    longitude = 129.0772,
                    accuracyMeters = 120f,
                    recordedAtEpochMillis = 1L,
                )
            val placesRepository = RecordingPlacesRepository(places = emptyList())
            val delegate = RecordingSearchRepository()
            val repository =
                LowVisionSearchRepository(
                    delegate = delegate,
                    placesRepository = placesRepository,
                    currentLocationProvider = { approximateKnownLocation },
                )

            repository.searchPage(SearchQuery(keyword = "\uBCD1\uC6D0"))

            assertEquals(35.1811, placesRepository.queries.single().latitude ?: 0.0, 0.0)
            assertEquals(129.0772, placesRepository.queries.single().longitude ?: 0.0, 0.0)
            assertEquals(3_000, placesRepository.queries.single().radiusMeters)
        }

    @Test
    fun `recent storage delegates to backing repository`() =
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

            assertEquals(listOf("busan"), delegate.savedRecentSearches)
            assertEquals(listOf(destination), delegate.savedRecentDestinations)
        }
}

private fun freshCurrentLocationSnapshot(): LocationSnapshot =
    LocationSnapshot(
        latitude = 35.1796,
        longitude = 129.0756,
        accuracyMeters = 12f,
        recordedAtEpochMillis = System.currentTimeMillis(),
    )

private class RecordingSearchRepository(
    private val searchResults: List<SearchResult> = emptyList(),
    private val searchPage: SearchPage = SearchPage(results = searchResults),
    private val searchPagesByKeyword: Map<String, SearchPage> = emptyMap(),
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
        return searchPagesByKeyword[query.keyword] ?: searchPage
    }

    override suspend fun analyzeVoiceSearch(
        text: String,
        mode: SearchVoiceMode,
    ): SearchVoiceAnalysis = SearchVoiceAnalysis(intent = SearchVoiceIntent.UNKNOWN)

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
    private val failure: Throwable? = null,
) : PlacesRepository {
    val queries: MutableList<PlaceQuery> = mutableListOf()

    override suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary> {
        queries += query
        failure?.let { error -> throw error }
        return places
    }

    override suspend fun getPlaceDetail(placeId: String): PlaceDetail? = null
}
