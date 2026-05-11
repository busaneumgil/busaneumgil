package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionNavigationCompleteScreenTest {
    @Test
    fun `navigation complete screen exposes save and complete cards`() {
        assertEquals(
            listOf("\uB3C4\uCC29\uC9C0 \uC800\uC7A5", "\uC644\uB8CC"),
            lowVisionNavigationCompleteCards().map(LowVisionNavigationCompleteCard::label),
        )
    }

    @Test
    fun `navigation complete screen follows mockup proportions`() {
        assertEquals(24.dp, LowVisionNavigationCompleteLayoutDefaults.horizontalPadding)
        assertEquals(44.dp, LowVisionNavigationCompleteLayoutDefaults.verticalPadding)
        assertEquals(28.dp, LowVisionNavigationCompleteLayoutDefaults.cardGap)
        assertEquals(26.dp, LowVisionNavigationCompleteLayoutDefaults.cardCornerRadius)
        assertEquals(64.sp, LowVisionNavigationCompleteLayoutDefaults.titleFontSize)
    }
}
