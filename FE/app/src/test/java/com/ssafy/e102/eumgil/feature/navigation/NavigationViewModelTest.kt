package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.testing.MainDispatcherRule
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
class NavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial ui state exposes navigation shell placeholders`() =
        runTest {
            val viewModel = NavigationViewModel()

            assertEquals(1, viewModel.uiState.value.currentStepIndex)
            assertEquals(5, viewModel.uiState.value.totalStepCount)
            assertEquals("1 / 5", viewModel.uiState.value.progressText)
            assertEquals("120m 앞에서 오른쪽으로 이동하세요", viewModel.uiState.value.currentInstruction)
            assertEquals("1.2km", viewModel.uiState.value.remainingDistanceText)
            assertEquals("약 18분", viewModel.uiState.value.estimatedTimeText)
            assertFalse(viewModel.uiState.value.isExitEnabled)
        }

    @Test
    fun `back action emits navigate back event`() =
        runTest {
            val viewModel = NavigationViewModel()
            val eventDeferred = async { viewModel.uiEvent.first() }

            viewModel.onAction(NavigationUiAction.BackClicked)
            advanceUntilIdle()

            assertEquals(NavigationUiEvent.NavigateBack, eventDeferred.await())
        }
}
