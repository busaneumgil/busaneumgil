package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

private val LowVisionCategoryYellow = LowVisionScreenDefaults.brandYellow
private val LowVisionCategoryBackground = Color(0xFF0D0D0F)

internal object LowVisionCategoryLayoutDefaults {
    const val columnCount = 2
    const val rowCount = 2
    const val cardColumnWeight = 1f
    const val cardContentBudgetHeightDp = 240f
    val headerGridGap = 28.dp
    val gridGap = 24.dp
    val cardMinHeight = 286.dp
    val backButtonSize = 64.dp
    val backIconSize = 52.dp
    val headerFontSize = 52.sp
    val headerLineHeight = 60.sp
    val scrollBottomSpacer = 112.dp
    val cardCornerRadius = 18.dp
    val cardBorderWidth = 3.dp
    val cardHorizontalPadding = 16.dp
    val cardVerticalPadding = 24.dp
    val cardIconSize = 96.dp
    val cardIconTextGap = 28.dp
    val cardLabelFontSize = 38.sp
    val cardLabelLineHeight = 46.sp
    const val cardLabelMaxLines = 2
}

internal fun lowVisionCategoryDisplayLabel(label: String): String =
    when (val trimmedLabel = label.trim()) {
        "\uC219\uBC15\uC2DC\uC124" -> "\uC219\uBC15\n\uC2DC\uC124"
        else -> trimmedLabel.replace(Regex("\\s+"), "\n")
    }

internal fun lowVisionCategoryResultA11yHint(label: String): String =
    "${label.trim()}에 대한 결과를 안내합니다."

internal val lowVisionCategoryOptions =
    listOf(
        LowVisionCategoryOption(
            label = "\uD654\uC7A5\uC2E4",
            iconRes = R.drawable.ic_lowvision_category_restroom,
        ),
        LowVisionCategoryOption(
            label = "\uC74C\uC2DD\uC810",
            iconRes = R.drawable.ic_lowvision_category_restaurant,
        ),
        LowVisionCategoryOption(
            label = "\uC2B9\uAC15\uAE30",
            talkBackLabel = "\uC2B9\uAC15\uAE30, \uC5D8\uB9AC\uBCA0\uC774\uD130",
            iconRes = R.drawable.ic_lowvision_category_elevator,
        ),
        LowVisionCategoryOption(
            label = "\uAD00\uAD11\uC9C0",
            resultA11yHintOverride = "\uBB34\uC7A5\uC560 \uAD00\uAD11\uC9C0. \uD3B8\uD558\uAC8C \uC990\uAE38 \uC218 \uC788\uB294 \uAD00\uAD11\uC9C0\uB97C \uC548\uB0B4\uD569\uB2C8\uB2E4.",
            iconRes = R.drawable.ic_lowvision_category_tourism,
        ),
        LowVisionCategoryOption(
            label = "\uD720\uCCB4\uC5B4 \uCDA9\uC804",
            iconRes = R.drawable.ic_lowvision_category_charging,
        ),
    )

@Composable
fun LowVisionCategoryScreen(
    onBackClick: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LowVisionCategoryBackground),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(
                        horizontal = LowVisionScreenDefaults.screenHorizontalPadding,
                        vertical = LowVisionScreenDefaults.screenVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(LowVisionCategoryLayoutDefaults.headerGridGap),
        ) {
            LowVisionCategoryHeader(onBackClick = onBackClick)
            LowVisionCategoryGrid(
                onCategorySelected = onCategorySelected,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.CATEGORY,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun LowVisionCategoryHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(LowVisionCategoryLayoutDefaults.backButtonSize)
                    .lowVisionButtonSemantics(
                        label = "뒤로",
                        actionHint = "두 번 탭하면 홈으로 이동합니다.",
                    )
                    .clickable(role = Role.Button, onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_back),
                contentDescription = null,
                tint = LowVisionCategoryYellow,
                modifier = Modifier.size(LowVisionCategoryLayoutDefaults.backIconSize),
            )
        }
        Text(
            text = "카테고리",
            color = LowVisionCategoryYellow,
            fontSize = LowVisionCategoryLayoutDefaults.headerFontSize,
            lineHeight = LowVisionCategoryLayoutDefaults.headerLineHeight,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun LowVisionCategoryGrid(
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val cardHeight =
            ((maxHeight - LowVisionCategoryLayoutDefaults.gridGap) / LowVisionCategoryLayoutDefaults.rowCount)
                .coerceAtLeast(LowVisionCategoryLayoutDefaults.cardMinHeight)

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LowVisionCategoryLayoutDefaults.gridGap),
        ) {
            lowVisionCategoryOptions
                .chunked(LowVisionCategoryLayoutDefaults.columnCount)
                .forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LowVisionCategoryLayoutDefaults.gridGap),
                    ) {
                        rowOptions.forEach { option ->
                            LowVisionCategoryCard(
                                option = option,
                                onClick = { onCategorySelected(option.label) },
                                modifier =
                                    Modifier
                                        .weight(LowVisionCategoryLayoutDefaults.cardColumnWeight)
                                        .height(cardHeight),
                            )
                        }
                    }
                }
            Spacer(modifier = Modifier.height(LowVisionCategoryLayoutDefaults.scrollBottomSpacer))
        }
    }
}

@Composable
private fun LowVisionCategoryCard(
    option: LowVisionCategoryOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(LowVisionCategoryLayoutDefaults.cardCornerRadius))
                .border(
                    width = LowVisionCategoryLayoutDefaults.cardBorderWidth,
                    color = LowVisionCategoryYellow,
                    shape = RoundedCornerShape(LowVisionCategoryLayoutDefaults.cardCornerRadius),
                )
                .background(LowVisionCategoryBackground)
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = option.resultA11yHint
                }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    horizontal = LowVisionCategoryLayoutDefaults.cardHorizontalPadding,
                    vertical = LowVisionCategoryLayoutDefaults.cardVerticalPadding,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = option.iconRes),
            contentDescription = null,
            tint = LowVisionCategoryYellow,
            modifier = Modifier.size(LowVisionCategoryLayoutDefaults.cardIconSize),
        )
        Spacer(modifier = Modifier.height(LowVisionCategoryLayoutDefaults.cardIconTextGap))
        Text(
            text = lowVisionCategoryDisplayLabel(option.label),
            modifier = Modifier.fillMaxWidth(),
            color = LowVisionCategoryYellow,
            fontSize = LowVisionCategoryLayoutDefaults.cardLabelFontSize,
            lineHeight = LowVisionCategoryLayoutDefaults.cardLabelLineHeight,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = LowVisionCategoryLayoutDefaults.cardLabelMaxLines,
        )
    }
}

internal data class LowVisionCategoryOption(
    val label: String,
    val talkBackLabel: String = label,
    val resultA11yHintOverride: String? = null,
    @DrawableRes val iconRes: Int,
) {
    val resultA11yHint: String
        get() = resultA11yHintOverride ?: lowVisionCategoryResultA11yHint(talkBackLabel)
}
