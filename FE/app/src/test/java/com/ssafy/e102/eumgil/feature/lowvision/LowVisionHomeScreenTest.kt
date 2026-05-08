package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LowVisionHomeScreenTest {
    @Test
    fun `home screen shows the low vision brand title in the reserved header slot`() {
        assertEquals("\uBD80\uC0B0\uC774\uC74C\uAE38", LowVisionHomeLayoutDefaults.headerTitle)
        assertEquals(48.dp, LowVisionHomeLayoutDefaults.headerSlotHeight)
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

    @Test
    fun `home action labels match terms guide card typography`() {
        assertEquals(48.sp, LowVisionHomeLayoutDefaults.actionLabelFontSize)
        assertEquals(FontWeight.Black, LowVisionHomeLayoutDefaults.actionLabelFontWeight)
    }
}
