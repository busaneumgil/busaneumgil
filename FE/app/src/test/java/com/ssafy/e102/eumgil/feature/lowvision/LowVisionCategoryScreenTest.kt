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
        assertEquals(96.dp, LowVisionCategoryLayoutDefaults.cardIconSize)
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
        assertEquals("승강기", lowVisionCategoryDisplayLabel("승강기"))
        assertEquals("엘리베이터", lowVisionCategoryDisplayLabel("엘리베이터"))
        assertEquals(
            "\uC219\uBC15\n\uC2DC\uC124",
            lowVisionCategoryDisplayLabel("\uC219\uBC15\uC2DC\uC124"),
        )
    }

    @Test
    fun `category options remove braille block and parking while keeping wheelchair charging below fold`() {
        assertEquals(
            listOf("화장실", "음식점", "승강기", "관광지"),
            lowVisionCategoryOptions.take(4).map { option -> option.label },
        )
        assertEquals(
            listOf("휠체어 충전"),
            lowVisionCategoryOptions.drop(4).map { option -> option.label },
        )
    }

    @Test
    fun `category card accessibility hint announces result guidance`() {
        assertEquals(
            "승강기, 엘리베이터에 대한 결과를 안내합니다.",
            lowVisionCategoryOptions.first { option -> option.label == "승강기" }.resultA11yHint,
        )
        assertEquals(
            "무장애 관광지. 편하게 즐길 수 있는 관광지를 안내합니다.",
            lowVisionCategoryOptions.first { option -> option.label == "관광지" }.resultA11yHint,
        )
    }
}
