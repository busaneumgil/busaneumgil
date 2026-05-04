package com.ssafy.e102.eumgil.feature.savedroute

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedRouteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `bookmark list renders content state`() =
        runTest {
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(bookmarks = listOf(testBookmark())),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(SavedRouteScreenState.CONTENT, uiState.screenState)
            assertEquals(1, uiState.places.size)
            assertEquals("부산역 엘리베이터", uiState.places.first().name)
        }

    @Test
    fun `empty bookmark list renders empty state`() =
        runTest {
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            assertEquals(SavedRouteScreenState.EMPTY, viewModel.uiState.value.screenState)
        }

    @Test
    fun `bookmark load failure renders error state`() =
        runTest {
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(failObserve = true),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(SavedRouteScreenState.ERROR, uiState.screenState)
            assertEquals("저장한 장소를 불러오지 못했습니다.", uiState.errorMessage)
        }

    @Test
    fun `remove bookmark updates list state`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository(bookmarks = listOf(testBookmark()))
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = bookmarkRepository,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(SavedRouteUiAction.BookmarkRemoveClicked(placeId = "bookmark-place-1"))
            advanceUntilIdle()

            assertEquals(SavedRouteScreenState.EMPTY, viewModel.uiState.value.screenState)
            assertEquals(emptyList<SavedPlaceUiModel>(), viewModel.uiState.value.places)
        }

    @Test
    fun `place click stores destination and navigates to map`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(bookmarks = listOf(testBookmark())),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SavedRouteUiAction.PlaceClicked(placeId = "bookmark-place-1"))
            advanceUntilIdle()

            val destination = destinationSelectionRepository.selectedDestination.value

            assertEquals(SavedRouteUiEvent.NavigateToMap, uiEvent.await())
            assertEquals("bookmark-place-1", destination?.placeId)
            assertEquals(PlaceCategory.ELEVATOR, destination?.category)
        }

    @Test
    fun `route guide click stores destination and navigates to route setting`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(bookmarks = listOf(testBookmark())),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SavedRouteUiAction.RouteGuideClicked(placeId = "bookmark-place-1"))
            advanceUntilIdle()

            val destination = destinationSelectionRepository.selectedDestination.value

            assertEquals(SavedRouteUiEvent.NavigateToRouteSetting, uiEvent.await())
            assertEquals("bookmark-place-1", destination?.placeId)
            assertEquals(PlaceCategory.ELEVATOR, destination?.category)
        }

    @Test
    fun `route guide click with invalid coordinate keeps user on saved place screen`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository =
                        FakeBookmarkRepository(
                            bookmarks = listOf(testBookmark(latitude = Double.NaN)),
                        ),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SavedRouteUiAction.RouteGuideClicked(placeId = "bookmark-place-1"))
            advanceUntilIdle()

            assertEquals(
                SavedRouteUiEvent.ShowSnackbar("저장한 장소의 좌표가 올바르지 않습니다."),
                uiEvent.await(),
            )
            assertNull(destinationSelectionRepository.selectedDestination.value)
            assertEquals("저장한 장소의 좌표가 올바르지 않습니다.", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `remove failure keeps content and exposes error message`() =
        runTest {
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository =
                        FakeBookmarkRepository(
                            bookmarks = listOf(testBookmark()),
                            failDelete = true,
                        ),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(SavedRouteUiAction.BookmarkRemoveClicked(placeId = "bookmark-place-1"))
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(SavedRouteScreenState.CONTENT, uiState.screenState)
            assertEquals(1, uiState.places.size)
            assertEquals("저장한 장소 삭제에 실패했습니다. 다시 시도해 주세요.", uiState.errorMessage)
        }
}

private class FakeBookmarkRepository(
    bookmarks: List<BookmarkData> = emptyList(),
    private val failObserve: Boolean = false,
    private val failDelete: Boolean = false,
) : BookmarkRepository {
    private val mutableBookmarks = MutableStateFlow(bookmarks)

    override fun observeBookmarks(): Flow<List<BookmarkData>> =
        if (failObserve) {
            flow { error("bookmark load failed") }
        } else {
            mutableBookmarks
        }

    override suspend fun isBookmarked(placeId: String): Boolean =
        mutableBookmarks.value.any { bookmark -> bookmark.placeId == placeId }

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        mutableBookmarks.value = mutableBookmarks.value.filterNot { it.placeId == bookmark.placeId } + bookmark
    }

    override suspend fun deleteBookmark(placeId: String) {
        if (failDelete) error("bookmark delete failed")
        mutableBookmarks.value = mutableBookmarks.value.filterNot { bookmark -> bookmark.placeId == placeId }
    }
}

private fun testBookmark(
    latitude: Double = 35.1151,
    longitude: Double = 129.0415,
): BookmarkData =
    BookmarkData(
        placeId = "bookmark-place-1",
        placeName = "부산역 엘리베이터",
        address = "부산 동구 중앙대로",
        latitude = latitude,
        longitude = longitude,
        category = "ELEVATOR",
    )
