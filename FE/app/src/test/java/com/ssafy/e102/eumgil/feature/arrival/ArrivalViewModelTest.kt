package com.ssafy.e102.eumgil.feature.arrival

import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArrivalViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `entry state auto opens route evaluation sheet with unsaved destination`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isEvaluationSheetVisible)
            assertEquals(0, viewModel.uiState.value.selectedRating)
            assertEquals(ArrivalEvaluationLabel.Idle, viewModel.uiState.value.selectedRatingLabel)
            assertFalse(viewModel.uiState.value.isEvaluationSubmitEnabled)
            assertFalse(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertTrue(viewModel.uiState.value.isRouteSaveEnabled)
        }

    @Test
    fun `entry state reflects already saved destination bookmark`() =
        runTest {
            val destination = testDestination()
            val viewModel =
                createViewModel(
                    bookmarkRepository =
                        FakeBookmarkRepository(
                            bookmarks = listOf(destination.toBookmarkData()),
                        ),
                )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
        }

    @Test
    fun `selecting rating updates satisfaction label and enables submit`() {
        val viewModel = createViewModel()

        viewModel.onAction(ArrivalUiAction.RatingSelected(4))

        assertEquals(4, viewModel.uiState.value.selectedRating)
        assertEquals(ArrivalEvaluationLabel.Satisfied, viewModel.uiState.value.selectedRatingLabel)
        assertTrue(viewModel.uiState.value.isEvaluationSubmitEnabled)
    }

    @Test
    fun `save route action stores destination bookmark and emits success snackbar`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel = createViewModel(bookmarkRepository = bookmarkRepository)
            advanceUntilIdle()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.SaveRouteClicked)
            advanceUntilIdle()

            assertEquals(
                listOf(testDestination().toBookmarkData()),
                bookmarkRepository.bookmarks.value,
            )
            assertTrue(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertEquals(
                ArrivalUiEvent.ShowSnackbar(R.string.arrival_bookmark_save_success_message),
                eventDeferred.await(),
            )
        }

    @Test
    fun `save route action removes existing destination bookmark and emits success snackbar`() =
        runTest {
            val destination = testDestination()
            val bookmarkRepository =
                FakeBookmarkRepository(
                    bookmarks = listOf(destination.toBookmarkData()),
                )
            val viewModel = createViewModel(bookmarkRepository = bookmarkRepository)
            advanceUntilIdle()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.SaveRouteClicked)
            advanceUntilIdle()

            assertTrue(bookmarkRepository.bookmarks.value.isEmpty())
            assertFalse(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertEquals(
                ArrivalUiEvent.ShowSnackbar(R.string.arrival_bookmark_delete_success_message),
                eventDeferred.await(),
            )
        }

    @Test
    fun `save route failure restores previous bookmark state and emits failure snackbar`() =
        runTest {
            val bookmarkRepository =
                FakeBookmarkRepository(
                    saveFailure = IllegalStateException("save failed"),
                )
            val viewModel = createViewModel(bookmarkRepository = bookmarkRepository)
            advanceUntilIdle()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.SaveRouteClicked)
            advanceUntilIdle()

            assertTrue(bookmarkRepository.bookmarks.value.isEmpty())
            assertFalse(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertEquals(
                ArrivalUiEvent.ShowSnackbar(R.string.arrival_bookmark_update_failure_message),
                eventDeferred.await(),
            )
        }

    @Test
    fun `submitting evaluation hides sheet only after rating is selected`() {
        val viewModel = createViewModel()

        viewModel.onAction(ArrivalUiAction.SubmitEvaluationClicked)

        assertTrue(viewModel.uiState.value.isEvaluationSheetVisible)

        viewModel.onAction(ArrivalUiAction.RatingSelected(5))
        viewModel.onAction(ArrivalUiAction.SubmitEvaluationClicked)

        assertFalse(viewModel.uiState.value.isEvaluationSheetVisible)
    }

    @Test
    fun `dismissing evaluation sheet keeps arrival screen visible`() {
        val viewModel = createViewModel()

        viewModel.onAction(ArrivalUiAction.EvaluationSheetDismissed)

        assertFalse(viewModel.uiState.value.isEvaluationSheetVisible)
    }

    @Test
    fun `home action emits navigate to map event`() =
        runTest {
            val viewModel = createViewModel()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.HomeClicked)
            advanceUntilIdle()

            assertEquals(ArrivalUiEvent.NavigateToMap, eventDeferred.await())
        }

    @Test
    fun `explore new route action emits navigate to search event`() =
        runTest {
            val viewModel = createViewModel()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.ExploreNewRouteClicked)
            advanceUntilIdle()

            assertEquals(ArrivalUiEvent.NavigateToSearch, eventDeferred.await())
        }
}

private fun createViewModel(
    bookmarkRepository: BookmarkRepository = FakeBookmarkRepository(),
    destinationSelectionRepository: DestinationSelectionRepository =
        FakeDestinationSelectionRepository(destination = testDestination()),
): ArrivalViewModel =
    ArrivalViewModel(
        bookmarkRepository = bookmarkRepository,
        destinationSelectionRepository = destinationSelectionRepository,
    )

private class FakeBookmarkRepository(
    bookmarks: List<BookmarkData> = emptyList(),
    private val saveFailure: Throwable? = null,
    private val deleteFailure: Throwable? = null,
) : BookmarkRepository {
    val bookmarks = MutableStateFlow(bookmarks)

    override fun observeBookmarks(): Flow<List<BookmarkData>> = bookmarks

    override suspend fun isBookmarked(placeId: String): Boolean =
        bookmarks.value.any { bookmark -> bookmark.placeId == placeId }

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        saveFailure?.let { throw it }
        bookmarks.value = bookmarks.value.filterNot { it.placeId == bookmark.placeId } + bookmark
    }

    override suspend fun deleteBookmark(placeId: String) {
        deleteFailure?.let { throw it }
        bookmarks.value = bookmarks.value.filterNot { bookmark -> bookmark.placeId == placeId }
    }
}

private class FakeDestinationSelectionRepository(
    destination: PlaceDestination? = null,
) : DestinationSelectionRepository {
    private val mutableSelectedDestination = MutableStateFlow(destination)
    private val mutableSelectionRequests = MutableSharedFlow<PlaceDestination>(extraBufferCapacity = 1)

    override val selectedDestination: StateFlow<PlaceDestination?> = mutableSelectedDestination.asStateFlow()
    override val selectionRequests: Flow<PlaceDestination> = mutableSelectionRequests.asSharedFlow()

    override fun updateSelectedDestination(destination: PlaceDestination) {
        mutableSelectedDestination.value = destination
        mutableSelectionRequests.tryEmit(destination)
    }

    override fun clearSelectedDestination() {
        mutableSelectedDestination.value = null
    }
}

private fun testDestination(): PlaceDestination =
    PlaceDestination(
        placeId = "arrival-destination",
        name = "부산시민공원",
        address = "부산광역시 부산진구 시민공원로 73",
        latitude = 35.1681,
        longitude = 129.0574,
        category = null,
    )

private fun PlaceDestination.toBookmarkData(): BookmarkData =
    BookmarkData(
        placeId = placeId,
        placeName = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        category = category?.name,
    )
