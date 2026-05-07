package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelEditingTargetTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `search submit keeps destination editing target in results navigation`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.QueryChanged(query = "city hall"))
            viewModel.onAction(SearchUiAction.SearchSubmitted)
            advanceUntilIdle()

            assertEquals(
                SearchUiEvent.NavigateToResults(
                    query = "city hall",
                    editingTarget = RouteEditingTarget.DESTINATION,
                ),
                uiEvent.await(),
            )
        }

    @Test
    fun `search result click stores selected origin when editing target is origin`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    setEditingTarget(RouteEditingTarget.ORIGIN)
                }
            val result = testSearchResult()
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(result.toPlaceDestination(), destinationSelectionRepository.selectedOrigin.value)
            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            assertEquals(SearchUiEvent.NavigateToRouteSetting, uiEvent.await())
        }

    @Test
    fun `search result click keeps updating selected destination when editing target is destination`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    setEditingTarget(RouteEditingTarget.DESTINATION)
                }
            val result = testSearchResult()
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(null, destinationSelectionRepository.selectedOrigin.value)
            assertEquals(result.toPlaceDestination(), destinationSelectionRepository.selectedDestination.value)
            assertEquals(SearchUiEvent.NavigateToRouteSetting, uiEvent.await())
        }
}

private fun testSearchResult(): SearchResult =
    SearchResult(
        placeId = "place-1",
        title = "Busan City Hall",
        subtitle = "123 Jungang-daero, Busan",
        latitude = 35.1797,
        longitude = 129.0750,
        category = PlaceCategory.TOURIST_ATTRACTION,
    )

private class EditingTargetFakeSearchRepository : SearchRepository {
    override suspend fun search(query: SearchQuery): List<SearchResult> = emptyList()

    override suspend fun getRecentSearches(): List<RecentSearch> = emptyList()

    override suspend fun saveRecentSearch(keyword: String) = Unit

    override suspend fun getRecentDestinations(): List<RecentDestination> = emptyList()

    override suspend fun saveRecentDestination(destination: RecentDestination) = Unit
}

private class EditingTargetFakeBookmarkRepository : BookmarkRepository {
    private val bookmarks = MutableStateFlow<List<BookmarkData>>(emptyList())

    override fun observeBookmarks(): Flow<List<BookmarkData>> = bookmarks

    override suspend fun isBookmarked(placeId: String): Boolean = false

    override suspend fun saveBookmark(bookmark: BookmarkData) = Unit

    override suspend fun deleteBookmark(placeId: String) = Unit
}
