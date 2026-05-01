package com.ssafy.e102.eumgil.feature.arrival

import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
    fun `entry state auto opens route evaluation sheet by default`() {
        val viewModel = ArrivalViewModel()

        assertTrue(viewModel.uiState.value.isEvaluationSheetVisible)
        assertEquals(0, viewModel.uiState.value.selectedRating)
        assertEquals(ArrivalEvaluationLabel.Idle, viewModel.uiState.value.selectedRatingLabel)
        assertFalse(viewModel.uiState.value.isEvaluationSubmitEnabled)
        assertFalse(viewModel.uiState.value.isRouteSaveSelected)
    }

    @Test
    fun `selecting rating updates satisfaction label and enables submit`() {
        val viewModel = ArrivalViewModel()

        viewModel.onAction(ArrivalUiAction.RatingSelected(4))

        assertEquals(4, viewModel.uiState.value.selectedRating)
        assertEquals(ArrivalEvaluationLabel.Satisfied, viewModel.uiState.value.selectedRatingLabel)
        assertTrue(viewModel.uiState.value.isEvaluationSubmitEnabled)
    }

    @Test
    fun `save route action toggles local saved selection`() {
        val viewModel = ArrivalViewModel()

        viewModel.onAction(ArrivalUiAction.SaveRouteClicked)

        assertTrue(viewModel.uiState.value.isRouteSaveSelected)

        viewModel.onAction(ArrivalUiAction.SaveRouteClicked)

        assertFalse(viewModel.uiState.value.isRouteSaveSelected)
    }

    @Test
    fun `submitting evaluation hides sheet only after rating is selected`() {
        val viewModel = ArrivalViewModel()

        viewModel.onAction(ArrivalUiAction.SubmitEvaluationClicked)

        assertTrue(viewModel.uiState.value.isEvaluationSheetVisible)

        viewModel.onAction(ArrivalUiAction.RatingSelected(5))
        viewModel.onAction(ArrivalUiAction.SubmitEvaluationClicked)

        assertFalse(viewModel.uiState.value.isEvaluationSheetVisible)
    }

    @Test
    fun `dismissing evaluation sheet keeps arrival screen visible`() {
        val viewModel = ArrivalViewModel()

        viewModel.onAction(ArrivalUiAction.EvaluationSheetDismissed)

        assertFalse(viewModel.uiState.value.isEvaluationSheetVisible)
    }

    @Test
    fun `home action emits navigate to map event`() =
        runTest {
            val viewModel = ArrivalViewModel()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.HomeClicked)
            advanceUntilIdle()

            assertEquals(ArrivalUiEvent.NavigateToMap, eventDeferred.await())
        }

    @Test
    fun `explore new route action emits navigate to search event`() =
        runTest {
            val viewModel = ArrivalViewModel()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(ArrivalUiAction.ExploreNewRouteClicked)
            advanceUntilIdle()

            assertEquals(ArrivalUiEvent.NavigateToSearch, eventDeferred.await())
        }
}
