package com.ssafy.e102.eumgil.feature.lowvision

import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionRouteBriefingStateTest {
    @Test
    fun `briefing steps are exposed in three step windows`() {
        val steps = (1..7).map(::briefingStep)

        assertEquals(listOf(1, 2, 3), steps.visibleBriefingSteps(0).map { it.sequence })
        assertEquals(listOf(4, 5, 6), steps.visibleBriefingSteps(3).map { it.sequence })
        assertEquals(listOf(7), steps.visibleBriefingSteps(6).map { it.sequence })
    }

    @Test
    fun `briefing step window clamps progress beyond route length to final window`() {
        val steps = (1..7).map(::briefingStep)

        assertEquals(listOf(7), steps.visibleBriefingSteps(99).map { it.sequence })
    }

    @Test
    fun `briefing speech text speaks the current visible step window`() {
        val steps = (1..5).map(::briefingStep)

        assertEquals(
            "경로 브리핑. 4번. 4번 안내. 5번. 5번 안내.",
            steps.briefingSpeechTextFrom(startIndex = 3),
        )
    }

    private fun briefingStep(sequence: Int): LowVisionRouteBriefingStepUiState =
        LowVisionRouteBriefingStepUiState(
            sequence = sequence,
            instruction = "${sequence}번 안내",
            icon = LowVisionRouteBriefingStepIcon.STRAIGHT,
        )
}
