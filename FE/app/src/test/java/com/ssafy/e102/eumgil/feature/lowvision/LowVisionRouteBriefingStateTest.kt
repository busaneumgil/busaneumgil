package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.core.model.RouteSegment
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

    @Test
    fun `route segment briefing instruction keeps only distance and core action`() {
        assertEquals(
            "100m \uD6C4 \uC9C1\uC9C4",
            RouteSegment(
                sequence = 4,
                distanceMeters = 100,
                guidanceMessage = "100\uBBF8\uD130 \uC9C1\uC9C4 \uD6C4 \uD6A1\uB2E8\uBCF4\uB3C4\uB97C \uAC74\uB108\uC138\uC694",
            ).toCompactBriefingInstruction(),
        )
        assertEquals(
            "40m \uD6C4 \uC6B0\uD68C\uC804",
            RouteSegment(
                sequence = 5,
                distanceMeters = 40,
                guidanceMessage = "\uC624\uB978\uCABD\uC73C\uB85C \uC774\uB3D9\uD558\uC138\uC694",
            ).toCompactBriefingInstruction(),
        )
        assertEquals(
            "\uB3C4\uCC29",
            RouteSegment(
                sequence = 6,
                distanceMeters = 0,
                guidanceMessage = "\uB3C4\uCC29\uC9C0 \uC6B0\uCE21",
            ).toCompactBriefingInstruction(),
        )
    }

    @Test
    fun `route segment detailed briefing preserves concrete guidance sentences`() {
        assertEquals(
            "100\uBBF8\uD130 \uC9C1\uC9C4 \uD6C4 \uD6A1\uB2E8\uBCF4\uB3C4\uB97C \uAC74\uB108\uC138\uC694",
            RouteSegment(
                sequence = 4,
                distanceMeters = 100,
                guidanceMessage = "100\uBBF8\uD130 \uC9C1\uC9C4 \uD6C4 \uD6A1\uB2E8\uBCF4\uB3C4\uB97C \uAC74\uB108\uC138\uC694",
            ).toDetailedBriefingInstruction(),
        )
        assertEquals(
            "40m \uC624\uB978\uCABD\uC73C\uB85C \uC774\uB3D9\uD558\uC138\uC694",
            RouteSegment(
                sequence = 5,
                distanceMeters = 40,
                guidanceMessage = "\uC624\uB978\uCABD\uC73C\uB85C \uC774\uB3D9\uD558\uC138\uC694",
            ).toDetailedBriefingInstruction(),
        )
    }

    private fun briefingStep(sequence: Int): LowVisionRouteBriefingStepUiState =
        LowVisionRouteBriefingStepUiState(
            sequence = sequence,
            instruction = "${sequence}번 안내",
            icon = LowVisionRouteBriefingStepIcon.STRAIGHT,
        )
}
