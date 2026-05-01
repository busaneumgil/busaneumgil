package com.ssafy.e102.eumgil.feature.arrival

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalViewModelTest {
    @Test
    fun `entry state shows route evaluation sheet by default`() {
        val viewModel = ArrivalViewModel()

        assertTrue(viewModel.uiState.value.isEvaluationSheetVisible)
    }

    @Test
    fun `dismissing evaluation sheet keeps arrival screen visible`() {
        val viewModel = ArrivalViewModel()

        viewModel.onAction(ArrivalUiAction.EvaluationSheetDismissed)

        assertFalse(viewModel.uiState.value.isEvaluationSheetVisible)
    }
}
