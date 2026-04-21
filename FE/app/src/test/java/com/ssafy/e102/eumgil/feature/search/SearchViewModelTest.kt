package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `search result click stores selected destination and emits route setting navigation`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )

            advanceUntilIdle()
            val uiEvent = async { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(result.toPlaceDestination(), destinationSelectionRepository.selectedDestination.value)
            assertEquals(SearchUiEvent.NavigateToRouteSetting, uiEvent.await())
        }

    @Test
    fun `search result click with invalid coordinates keeps user on search and exposes handoff error`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )
            val invalidResult =
                SearchResult(
                    placeId = "place-invalid",
                    title = "Broken Coordinates Place",
                    subtitle = "Unknown address",
                    latitude = Double.NaN,
                    longitude = 129.0750,
                    category = PlaceCategory.OTHER,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = invalidResult))
            advanceUntilIdle()

            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            val resultState = viewModel.uiState.value.resultState
            assertTrue(resultState is SearchResultUiState.Error)
            assertEquals(
                "좌표 정보가 올바르지 않아 경로 설정으로 넘길 수 없습니다.",
                (resultState as SearchResultUiState.Error).message,
            )
        }
}

private class FakeSearchRepository : SearchRepository {
    override suspend fun search(query: SearchQuery): List<SearchResult> = emptyList()

    override suspend fun getRecentSearches(): List<RecentSearch> = emptyList()

    override suspend fun saveRecentSearch(keyword: String) = Unit
}
