package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationPreviewRepository
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
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelEditingTargetTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `editing target configuration updates ui state`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            viewModel.onAction(SearchUiAction.EditingTargetConfigured(RouteEditingTarget.ORIGIN))

            assertEquals(RouteEditingTarget.ORIGIN, viewModel.uiState.value.editingTarget)
        }

    @Test
    fun `selection mode configuration updates ui state`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            viewModel.onAction(
                SearchUiAction.EditingTargetConfigured(
                    editingTarget = RouteEditingTarget.DESTINATION,
                    selectionMode = SearchSelectionMode.APPLY_TO_ROUTE,
                ),
            )

            assertEquals(SearchSelectionMode.APPLY_TO_ROUTE, viewModel.uiState.value.selectionMode)
        }

    @Test
    fun `map picker click in apply to route mode opens picker for active editing target`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent =
                backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
                    withTimeoutOrNull(100) { viewModel.uiEvent.first() }
                }

            viewModel.onAction(
                SearchUiAction.EditingTargetConfigured(
                    editingTarget = RouteEditingTarget.ORIGIN,
                    selectionMode = SearchSelectionMode.APPLY_TO_ROUTE,
                ),
            )
            viewModel.onAction(SearchUiAction.MapPickerClicked)
            advanceUntilIdle()

            assertEquals(RouteEditingTarget.ORIGIN, destinationSelectionRepository.editingTarget.value)
            assertEquals(
                SearchUiEvent.NavigateToRouteEndpointMapPicker(RouteEditingTarget.ORIGIN),
                uiEvent.await(),
            )
        }

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
    fun `search submit keeps apply to route selection mode in results navigation`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(
                SearchUiAction.EditingTargetConfigured(
                    editingTarget = RouteEditingTarget.ORIGIN,
                    selectionMode = SearchSelectionMode.APPLY_TO_ROUTE,
                ),
            )
            viewModel.onAction(SearchUiAction.QueryChanged(query = "city hall"))
            viewModel.onAction(SearchUiAction.SearchSubmitted)
            advanceUntilIdle()

            assertEquals(
                SearchUiEvent.NavigateToResults(
                    query = "city hall",
                    editingTarget = RouteEditingTarget.ORIGIN,
                    selectionMode = SearchSelectionMode.APPLY_TO_ROUTE,
                ),
                uiEvent.await(),
            )
        }

    @Test
    fun `apply to route search result click stores selected origin and prechecks route setting permission`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )
            val result = testSearchResult()

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(
                SearchUiAction.EditingTargetConfigured(
                    editingTarget = RouteEditingTarget.ORIGIN,
                    selectionMode = SearchSelectionMode.APPLY_TO_ROUTE,
                ),
            )
            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(result.toPlaceDestination(), destinationSelectionRepository.selectedOrigin.value)
            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            assertEquals(
                SearchUiEvent.NavigateToRouteSetting(locationPermissionPrechecked = true),
                uiEvent.await(),
            )
        }

    @Test
    fun `search result preview click previews selected origin candidate when editing target is origin`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    setEditingTarget(RouteEditingTarget.ORIGIN)
                }
            val destinationPreviewRepository = InMemoryDestinationPreviewRepository()
            val result = testSearchResult()
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    destinationPreviewRepository = destinationPreviewRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultPreviewClicked(result = result))
            advanceUntilIdle()

            assertEquals(null, destinationSelectionRepository.selectedOrigin.value)
            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            assertEquals(result.toPlaceDestination(), destinationPreviewRepository.pendingPreview.value?.destination)
            assertEquals(RouteEditingTarget.ORIGIN, destinationPreviewRepository.pendingPreview.value?.editingTarget)
            assertEquals(SearchUiEvent.NavigateToMapPreview, uiEvent.await())
        }

    @Test
    fun `search result preview click previews selected destination candidate when editing target is destination`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    setEditingTarget(RouteEditingTarget.DESTINATION)
                }
            val destinationPreviewRepository = InMemoryDestinationPreviewRepository()
            val result = testSearchResult()
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    destinationPreviewRepository = destinationPreviewRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultPreviewClicked(result = result))
            advanceUntilIdle()

            assertEquals(null, destinationSelectionRepository.selectedOrigin.value)
            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            assertEquals(result.toPlaceDestination(), destinationPreviewRepository.pendingPreview.value?.destination)
            assertEquals(RouteEditingTarget.DESTINATION, destinationPreviewRepository.pendingPreview.value?.editingTarget)
            assertEquals(SearchUiEvent.NavigateToMapPreview, uiEvent.await())
        }

    @Test
    fun `provider only search result can preview origin when coordinates are valid`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    setEditingTarget(RouteEditingTarget.ORIGIN)
                }
            val destinationPreviewRepository = InMemoryDestinationPreviewRepository()
            val result =
                SearchResult(
                    placeId = "provider:kakao:987654321",
                    serverPlaceId = null,
                    providerPlaceId = "987654321",
                    title = "Provider Only Cafe",
                    subtitle = "2 Gwangbok-ro, Busan",
                    latitude = 35.1010,
                    longitude = 129.0330,
                    matched = false,
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = EditingTargetFakeSearchRepository(),
                    bookmarkRepository = EditingTargetFakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    destinationPreviewRepository = destinationPreviewRepository,
            )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultPreviewClicked(result = result))
            advanceUntilIdle()

            assertEquals(null, destinationSelectionRepository.selectedOrigin.value)
            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            assertEquals("provider:kakao:987654321", destinationPreviewRepository.pendingPreview.value?.destination?.placeId)
            assertEquals(RouteEditingTarget.ORIGIN, destinationPreviewRepository.pendingPreview.value?.editingTarget)
            assertEquals(SearchUiEvent.NavigateToMapPreview, uiEvent.await())
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

    override suspend fun saveBookmark(bookmark: BookmarkData): BookmarkData = bookmark

    override suspend fun deleteBookmark(placeId: String) = Unit
}
