package com.ssafy.e102.eumgil.feature.arrival

import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteBookmark
import com.ssafy.e102.eumgil.core.model.RouteBookmarkDraft
import com.ssafy.e102.eumgil.core.model.RouteBookmarkSaveRequest
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.repository.RouteBookmarkRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArrivalViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `entry state auto opens route evaluation sheet with unsaved route draft`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isEvaluationSheetVisible)
            assertEquals(0, viewModel.uiState.value.selectedRating)
            assertEquals(ArrivalEvaluationLabel.Idle, viewModel.uiState.value.selectedRatingLabel)
            assertFalse(viewModel.uiState.value.isEvaluationSubmitEnabled)
            assertNotNull(viewModel.uiState.value.routeSaveDraft)
            assertEquals("부산시청-해운대해수욕장", viewModel.uiState.value.routeNameInput)
            assertFalse(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertTrue(viewModel.uiState.value.isRouteSaveEnabled)
        }

    @Test
    fun `entry state reflects already saved route bookmark`() =
        runTest {
            val draft = testRouteBookmarkDraft()
            val viewModel =
                createViewModel(
                    routeBookmarkRepository =
                        FakeRouteBookmarkRepository(
                            savedBookmarks =
                                listOf(
                                    testRouteBookmark(
                                        request = draft.toSaveRequest(),
                                    ),
                                ),
                        ),
                )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertFalse(viewModel.uiState.value.isRouteSaveEnabled)
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
    fun `save route action opens route name dialog with default name`() =
        runTest {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAction(ArrivalUiAction.SaveRouteClicked)

            assertTrue(viewModel.uiState.value.isRouteSaveDialogVisible)
            assertEquals("부산시청-해운대해수욕장", viewModel.uiState.value.routeNameInput)
        }

    @Test
    fun `confirm route save stores route bookmark and emits success snackbar`() =
        runTest {
            val routeBookmarkRepository = FakeRouteBookmarkRepository()
            val viewModel = createViewModel(routeBookmarkRepository = routeBookmarkRepository)
            advanceUntilIdle()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.SaveRouteClicked)
            viewModel.onAction(ArrivalUiAction.RouteNameChanged("출근 경로"))
            viewModel.onAction(ArrivalUiAction.ConfirmRouteSaveClicked)
            advanceUntilIdle()

            assertEquals(
                "출근 경로",
                routeBookmarkRepository.savedBookmarks.value.single().routeName,
            )
            assertTrue(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertFalse(viewModel.uiState.value.isRouteSaveDialogVisible)
            assertEquals(
                ArrivalUiEvent.ShowSnackbar(R.string.arrival_route_save_success_message),
                eventDeferred.await(),
            )
        }

    @Test
    fun `route save failure keeps dialog open and emits failure snackbar`() =
        runTest {
            val viewModel =
                createViewModel(
                    routeBookmarkRepository =
                        FakeRouteBookmarkRepository(
                            saveFailure = IllegalStateException("save failed"),
                        ),
                )
            advanceUntilIdle()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.SaveRouteClicked)
            viewModel.onAction(ArrivalUiAction.ConfirmRouteSaveClicked)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRouteSaveSelected)
            assertFalse(viewModel.uiState.value.isRouteSaveUpdating)
            assertTrue(viewModel.uiState.value.isRouteSaveDialogVisible)
            assertEquals(
                ArrivalUiEvent.ShowSnackbar(R.string.arrival_route_save_failure_message),
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
    routeBookmarkRepository: RouteBookmarkRepository = FakeRouteBookmarkRepository(),
    routeBookmarkDraft: RouteBookmarkDraft = testRouteBookmarkDraft(),
): ArrivalViewModel =
    ArrivalViewModel(
        routeBookmarkRepository = routeBookmarkRepository,
        currentRouteBookmarkDraft = routeBookmarkDraft,
    )

private class FakeRouteBookmarkRepository(
    savedBookmarks: List<RouteBookmark> = emptyList(),
    private val saveFailure: Throwable? = null,
) : RouteBookmarkRepository {
    val savedBookmarks = MutableStateFlow(savedBookmarks)

    override fun observeRouteBookmarks(): Flow<List<RouteBookmark>> = savedBookmarks

    override suspend fun isBookmarked(draft: RouteBookmarkDraft): Boolean =
        savedBookmarks.value.any { bookmark ->
            bookmark.startPoint == draft.startPoint &&
                bookmark.endPoint == draft.endPoint &&
                bookmark.routeOption == draft.routeOption
        }

    override suspend fun saveRouteBookmark(request: RouteBookmarkSaveRequest): RouteBookmark {
        saveFailure?.let { throw it }
        val savedBookmark = testRouteBookmark(request = request)
        savedBookmarks.value = listOf(savedBookmark)
        return savedBookmark
    }

    override suspend fun deleteRouteBookmark(bookmarkId: String) = Unit
}

private fun testRouteBookmarkDraft(): RouteBookmarkDraft =
    RouteBookmarkDraft(
        startLabel = "부산시청",
        endLabel = "해운대해수욕장",
        startPoint = GeoCoordinate(latitude = 35.1798, longitude = 129.0750),
        endPoint = GeoCoordinate(latitude = 35.1587, longitude = 129.1604),
        routeOption = RouteOption.SAFE,
        distanceMeters = 11_200,
        durationMinutes = 28,
    )

private fun testRouteBookmark(
    request: RouteBookmarkSaveRequest,
): RouteBookmark =
    RouteBookmark(
        bookmarkId = "route-bookmark:test",
        routeName = request.routeName,
        startLabel = request.startLabel,
        endLabel = request.endLabel,
        startPoint = request.startPoint,
        endPoint = request.endPoint,
        routeOption = request.routeOption,
        distanceMeters = request.distanceMeters,
        durationMinutes = request.durationMinutes,
        createdAt = 1L,
        updatedAt = 1L,
    )
