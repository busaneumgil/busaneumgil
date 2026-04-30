package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LowVisionHomeScreenTest {
    @Test
    fun `home screen reserves the low vision title slot without visible text`() {
        assertEquals(60.dp, LowVisionHomeLayoutDefaults.headerSlotHeight)
    }

    @Test
    fun `home screen gives voice action twice the remaining area of current location`() {
        assertEquals(2f, LowVisionHomeLayoutDefaults.voiceActionCardWeight)
        assertEquals(1f, LowVisionHomeLayoutDefaults.currentLocationCardWeight)
        assertEquals(
            2f,
            LowVisionHomeLayoutDefaults.voiceActionCardWeight /
                LowVisionHomeLayoutDefaults.currentLocationCardWeight,
        )
    }

    @Test
    fun `home screen hides status guide while voice output is not wired`() {
        assertFalse(LowVisionHomeLayoutDefaults.showsStatusGuide)
    }
}
