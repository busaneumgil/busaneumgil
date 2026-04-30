package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiState

private val LowVisionNavigationBackground = Color(0xFF0D0D0F)
private val LowVisionNavigationPanel = Color(0xFF202123)
private val LowVisionNavigationYellow = Color(0xFFFFD400)
private val LowVisionNavigationCoral = Color(0xFFFF8B78)
private val LowVisionNavigationInactive = Color(0xFFE7E7E7)
private val LowVisionNavigationDivider = Color(0xFF36363A)

internal data class LowVisionNavigationMetricSection(
    val label: String,
    val metricIndex: Int,
) {
    fun talkBackText(value: String): String = "$label $value"
}

internal data class LowVisionNavigationActionCard(
    val label: String,
    @DrawableRes val iconRes: Int,
)

internal fun lowVisionNavigationMetricSections(): List<LowVisionNavigationMetricSection> =
    listOf(
        LowVisionNavigationMetricSection(label = "남은 거리", metricIndex = 0),
        LowVisionNavigationMetricSection(label = "남은 시간", metricIndex = 1),
    )

internal fun lowVisionNavigationActionCards(): List<LowVisionNavigationActionCard> =
    listOf(
        LowVisionNavigationActionCard(
            label = "현재 위치",
            iconRes = R.drawable.ic_voice_location_pin,
        ),
        LowVisionNavigationActionCard(
            label = "안내 종료",
            iconRes = R.drawable.ic_action_close,
        ),
    )

internal fun lowVisionNavigationBottomTabs(): List<LowVisionBottomTab> =
    listOf(
        LowVisionBottomTab.HOME,
        LowVisionBottomTab.BOOKMARK,
        LowVisionBottomTab.CATEGORY,
        LowVisionBottomTab.MY_PAGE,
    )

@Composable
fun LowVisionNavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
    onTabSelected: (LowVisionBottomTab) -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LowVisionNavigationBackground),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 70.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            LowVisionNavigationMetricHeader(
                uiState = uiState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(184.dp),
            )

            LowVisionCurrentLocationCard(
                card = lowVisionNavigationActionCards().first(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )

            LowVisionExitNavigationCard(
                card = lowVisionNavigationActionCards()[1],
                enabled = uiState.isExitEnabled,
                onClick = { onAction(NavigationUiAction.ExitNavigationClicked) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1.55f),
            )
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.HOME,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun LowVisionNavigationMetricHeader(
    uiState: NavigationUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lowVisionNavigationMetricSections().forEachIndexed { index, section ->
            val value = uiState.stepCard.metrics.getOrNull(section.metricIndex)?.value.orEmpty().ifBlank { "-" }
            LowVisionNavigationMetricItem(
                section = section,
                value = value,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            )
            if (index == 0) {
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .height(132.dp)
                            .background(LowVisionNavigationDivider),
                )
            }
        }
    }
}

@Composable
private fun LowVisionNavigationMetricItem(
    section: LowVisionNavigationMetricSection,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clearAndSetSemantics {
                    contentDescription = section.talkBackText(value)
                }
                .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = section.label,
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = value,
            color = LowVisionNavigationYellow,
            fontSize = 88.sp,
            lineHeight = 96.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun LowVisionCurrentLocationCard(
    card: LowVisionNavigationActionCard,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = LowVisionNavigationYellow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = card.label
                    }
                    .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = card.iconRes),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(96.dp),
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = card.label,
                color = Color.Black,
                fontSize = 50.sp,
                lineHeight = 58.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LowVisionExitNavigationCard(
    card: LowVisionNavigationActionCard,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (enabled) 1f else 0.55f

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(18.dp))
                .lowVisionButtonSemantics(
                    label = card.label,
                    actionHint = "두 번 탭하면 길 안내를 종료합니다.",
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = LowVisionNavigationPanel,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = LowVisionNavigationCoral.copy(alpha = contentAlpha),
            ) {
                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = card.iconRes),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(58.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(38.dp))
            Text(
                text = card.label,
                color = LowVisionNavigationInactive.copy(alpha = contentAlpha),
                fontSize = 60.sp,
                lineHeight = 68.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
