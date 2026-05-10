package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionRouteBriefingScreenTest {
    @Test
    fun `briefing step rows prioritize showing instruction sentences`() {
        assertEquals(2, LowVisionRouteBriefingLayoutDefaults.stepInstructionMaxLines)
        assertEquals(118.dp, LowVisionRouteBriefingLayoutDefaults.stepRowMinHeight)
        assertTrue(LowVisionRouteBriefingLayoutDefaults.stepInstructionFontSize <= 34.sp)
        assertTrue(LowVisionRouteBriefingLayoutDefaults.stepInstructionLineHeight <= 40.sp)
    }
}
