package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceAnalysis
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionSearchRepositoryTest {
    @Test
    fun `search uses low vision mock results instead of delegate search backend`() =
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

            assertEquals(listOf("mock-place-3"), results.map(SearchResult::placeId))
            assertTrue(delegate.searchRequests.isEmpty())
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

private class RecordingSearchRepository(
    private val searchResults: List<SearchResult> = emptyList(),
) : SearchRepository {
    val searchRequests: MutableList<SearchQuery> = mutableListOf()
    val savedRecentSearches: MutableList<String> = mutableListOf()
    val savedRecentDestinations: MutableList<RecentDestination> = mutableListOf()

    override suspend fun search(query: SearchQuery): List<SearchResult> {
        searchRequests += query
        return searchResults
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
