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

            assertEquals("다음 안내", viewModel.uiState.value.stepCard.sectionLabel)
            assertEquals("안내 중", viewModel.uiState.value.stepCard.statusLabel)
            assertEquals("우회전", viewModel.uiState.value.stepCard.emphasisLabel)
            assertEquals("120m", viewModel.uiState.value.stepCard.distanceLabel)
            assertEquals("120m 앞에서 오른쪽 보행로로 이동하세요", viewModel.uiState.value.stepCard.instruction)
            assertEquals(
                "횡단보도를 지난 뒤 점자블록을 따라 우측 보행로로 진입합니다.",
                viewModel.uiState.value.stepCard.supportingText,
            )
            assertEquals(3, viewModel.uiState.value.stepCard.metrics.size)
            assertEquals("남은 거리", viewModel.uiState.value.stepCard.metrics[0].label)
            assertEquals("1.2km", viewModel.uiState.value.stepCard.metrics[0].value)
            assertEquals("예상 시간", viewModel.uiState.value.stepCard.metrics[1].label)
            assertEquals("약 18분", viewModel.uiState.value.stepCard.metrics[1].value)
            assertEquals("진행 단계", viewModel.uiState.value.stepCard.metrics[2].label)
            assertEquals("1 / 5", viewModel.uiState.value.stepCard.metrics[2].value)
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
