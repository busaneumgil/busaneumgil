package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiState

private val LowVisionNavigationBackground = Color(0xFF1C1C1E)
private val LowVisionNavigationYellow = Color(0xFFFFD400)
private val LowVisionNavigationBlack = Color(0xFF000000)
private val LowVisionNavigationMuted = Color(0xFFBDBDBD)

internal enum class LowVisionNavigationPage(
    val title: String,
) {
    GUIDE(title = "안내 정보"),
    DISTANCE(title = "남은 거리"),
    TIME(title = "예상 시간"),
    STEP(title = "진행 단계"),
}

internal val lowVisionNavigationPages: List<LowVisionNavigationPage> = LowVisionNavigationPage.entries

internal fun nextLowVisionNavigationPageIndex(currentIndex: Int): Int =
    (currentIndex + 1).coerceAtMost(lowVisionNavigationPages.lastIndex)

internal fun previousLowVisionNavigationPageIndex(currentIndex: Int): Int =
    (currentIndex - 1).coerceAtLeast(0)

@Composable
fun LowVisionNavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = lowVisionNavigationPages[pageIndex]

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LowVisionNavigationBackground)
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 24.dp, bottom = EumSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LowVisionNavigationPageCard(
            page = page,
            uiState = uiState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .heightIn(min = 360.dp),
        )

        LowVisionNavigationPageActions(
            pageIndex = pageIndex,
            onPreviousClick = { pageIndex = previousLowVisionNavigationPageIndex(pageIndex) },
            onNextClick = {
                if (pageIndex == lowVisionNavigationPages.lastIndex) {
                    onAction(NavigationUiAction.NavigationCompleteClicked)
                } else {
                    pageIndex = nextLowVisionNavigationPageIndex(pageIndex)
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 180.dp),
        )
    }
}

@Composable
private fun LowVisionNavigationPageCard(
    page: LowVisionNavigationPage,
    uiState: NavigationUiState,
    modifier: Modifier = Modifier,
) {
    val content = page.toPageContent(uiState)

    Surface(
        modifier =
            modifier
                .semantics {
                    contentDescription =
                        listOf(content.title, content.primaryText, content.secondaryText)
                            .filter(String::isNotBlank)
                            .joinToString(". ")
                },
        shape = RoundedCornerShape(24.dp),
        color = LowVisionNavigationBlack,
        border = BorderStroke(width = 2.dp, color = LowVisionNavigationYellow),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = content.title,
                color = LowVisionNavigationYellow,
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
            )
            Text(
                text = content.primaryText,
                color = Color.White,
                fontSize = content.primaryFontSizeSp.sp,
                lineHeight = content.primaryLineHeightSp.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                maxLines = content.primaryMaxLines,
                modifier = Modifier.padding(top = 34.dp),
            )
            if (content.secondaryText.isNotBlank()) {
                Text(
                    text = content.secondaryText,
                    color = LowVisionNavigationMuted,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp,
                    maxLines = 4,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun LowVisionNavigationPageActions(
    pageIndex: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LowVisionNavigationTextCard(
            label = "이전",
            actionHint = "이전 안내 정보로 이동합니다.",
            enabled = pageIndex > 0,
            backgroundColor = LowVisionNavigationBlack,
            contentColor = LowVisionNavigationYellow,
            border = BorderStroke(width = 2.dp, color = LowVisionNavigationYellow),
            onClick = onPreviousClick,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxSize(),
        )
        LowVisionNavigationTextCard(
            label = if (pageIndex == lowVisionNavigationPages.lastIndex) "안내 완료" else "다음",
            actionHint =
                if (pageIndex == lowVisionNavigationPages.lastIndex) {
                    "두 번 탭하면 안내 완료 화면으로 이동합니다."
                } else {
                    "다음 안내 정보로 이동합니다."
                },
            enabled = true,
            backgroundColor = LowVisionNavigationYellow,
            contentColor = LowVisionNavigationBlack,
            onClick = onNextClick,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxSize(),
        )
    }
}

@Composable
private fun LowVisionNavigationTextCard(
    label: String,
    actionHint: String,
    enabled: Boolean,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.45f

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .lowVisionButtonSemantics(label, actionHint)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = border,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = contentColor.copy(alpha = contentAlpha),
                fontSize = 40.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                maxLines = 1,
            )
        }
    }
}

private data class LowVisionNavigationPageContent(
    val title: String,
    val primaryText: String,
    val secondaryText: String,
    val primaryFontSizeSp: Int = 56,
    val primaryLineHeightSp: Int = 64,
    val primaryMaxLines: Int = 2,
)

private fun LowVisionNavigationPage.toPageContent(uiState: NavigationUiState): LowVisionNavigationPageContent {
    val metrics = uiState.stepCard.metrics
    val distance = metrics.getOrNull(0)?.value.orEmpty().ifBlank { "-" }
    val eta = metrics.getOrNull(1)?.value.orEmpty().ifBlank { "-" }
    val progress = metrics.getOrNull(2)?.value.orEmpty().ifBlank { "-" }

    return when (this) {
        LowVisionNavigationPage.GUIDE ->
            LowVisionNavigationPageContent(
                title = title,
                primaryText = uiState.stepCard.instruction,
                secondaryText = uiState.stepCard.supportingText,
                primaryFontSizeSp = 38,
                primaryLineHeightSp = 46,
                primaryMaxLines = 3,
            )
        LowVisionNavigationPage.DISTANCE ->
            LowVisionNavigationPageContent(
                title = title,
                primaryText = distance,
                secondaryText = "목적지까지 남은 거리",
            )
        LowVisionNavigationPage.TIME ->
            LowVisionNavigationPageContent(
                title = title,
                primaryText = eta,
                secondaryText = "목적지까지 예상 시간",
            )
        LowVisionNavigationPage.STEP ->
            LowVisionNavigationPageContent(
                title = title,
                primaryText = progress,
                secondaryText = "현재 진행 단계",
            )
    }
}
