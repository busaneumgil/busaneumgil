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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArrivalViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `entry state hides route evaluation sheet by default`() {
        val viewModel = ArrivalViewModel()

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
