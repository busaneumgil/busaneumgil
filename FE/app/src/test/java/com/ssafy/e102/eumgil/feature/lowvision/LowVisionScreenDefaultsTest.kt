package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionScreenDefaultsTest {
    @Test
    fun `low vision tab header keeps bookmark position with softer type scale`() {
        assertEquals(24.dp, LowVisionScreenDefaults.screenHorizontalPadding)
        assertEquals(36.dp, LowVisionScreenDefaults.screenVerticalPadding)
        assertEquals(40.sp, LowVisionScreenDefaults.headerFontSize)
        assertEquals(48.sp, LowVisionScreenDefaults.headerLineHeight)
        assertEquals(24.dp, LowVisionScreenDefaults.headerGap)
    }

    @Test
    fun `low vision yellow matches home screen brand tone`() {
        assertEquals(Color(0xFFFFCC00), LowVisionScreenDefaults.brandYellow)
    }
}
