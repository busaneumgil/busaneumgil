package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LowVisionCategoryScreenTest {
    @Test
    fun `category screen uses large two by two quadrant ratios`() {
        assertEquals(2, LowVisionCategoryLayoutDefaults.columnCount)
        assertEquals(2, LowVisionCategoryLayoutDefaults.rowCount)
        assertEquals(24.dp, LowVisionCategoryLayoutDefaults.gridGap)
        assertEquals(1f, LowVisionCategoryLayoutDefaults.cardColumnWeight)
        assertEquals(286.dp, LowVisionCategoryLayoutDefaults.cardMinHeight)
    }

    @Test
    fun `category card graphic and text stay large for low vision users`() {
        val contentHeight =
            LowVisionCategoryLayoutDefaults.cardVerticalPadding.value * 2 +
                LowVisionCategoryLayoutDefaults.cardIconSize.value +
                LowVisionCategoryLayoutDefaults.cardIconTextGap.value +
                LowVisionCategoryLayoutDefaults.cardLabelLineHeight.value

        assertTrue(contentHeight <= LowVisionCategoryLayoutDefaults.cardContentBudgetHeightDp)
        assertEquals(116.dp, LowVisionCategoryLayoutDefaults.cardIconSize)
        assertEquals(38.sp, LowVisionCategoryLayoutDefaults.cardLabelFontSize)
    }

    @Test
    fun `category scroll content leaves room to reach lower buttons`() {
        assertEquals(112.dp, LowVisionCategoryLayoutDefaults.scrollBottomSpacer)
    }

    @Test
    fun `category labels prefer one line and allow safe two line wrapping`() {
        assertEquals(2, LowVisionCategoryLayoutDefaults.cardLabelMaxLines)
        assertEquals("Lodging", lowVisionCategoryDisplayLabel("Lodging"))
        assertEquals("Other\nObstacle", lowVisionCategoryDisplayLabel("Other Obstacle"))
    }
}
