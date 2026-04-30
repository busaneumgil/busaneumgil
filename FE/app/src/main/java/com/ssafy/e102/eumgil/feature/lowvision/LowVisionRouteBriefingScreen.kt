package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

private val BriefingBackground = Color(0xFF0D0D0F)
private val BriefingYellow = Color(0xFFFFD400)
private val BriefingBlack = Color(0xFF000000)

@Composable
fun LowVisionRouteBriefingScreen(
    uiState: LowVisionRouteBriefingUiState,
    isPlaying: Boolean,
    onPlaybackClick: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(BriefingBackground)
                .statusBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "경로 브리핑",
                color = Color.White,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
            Box(
                modifier =
                    Modifier
                        .height(7.dp)
                        .fillMaxWidth(0.24f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(BriefingYellow),
            )

            Text(
                text = "전체 경로",
                color = BriefingYellow,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                uiState.steps.forEach { step ->
                    BriefingStepRow(step = step)
                }
            }

            BriefingPlaybackButton(
                isPlaying = isPlaying,
                enabled = !uiState.isLoading && uiState.steps.isNotEmpty(),
                onClick = onPlaybackClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(260.dp),
            )
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.HOME,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun BriefingStepRow(step: LowVisionRouteBriefingStepUiState) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(82.dp)
                .semantics {
                    contentDescription = "${step.sequence}번. ${step.instruction}"
                },
        shape = RoundedCornerShape(8.dp),
        color = BriefingYellow,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = step.sequence.toString().padStart(2, '0'),
                color = BriefingBlack,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
            Text(
                text = step.instruction,
                color = BriefingBlack,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text =
                    when (step.icon) {
                        LowVisionRouteBriefingStepIcon.STRAIGHT -> "↑"
                        LowVisionRouteBriefingStepIcon.TRANSIT -> "▣"
                        LowVisionRouteBriefingStepIcon.TURN -> "↱"
                    },
                color = BriefingBlack,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
private fun BriefingPlaybackButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isPlaying) "중지" else "시작"
    val icon = if (isPlaying) "■" else "▶"
    val alpha = if (enabled) 1f else 0.45f

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .lowVisionButtonSemantics(
                    label = label,
                    actionHint =
                        if (isPlaying) {
                            "두 번 탭하면 브리핑을 중지합니다."
                        } else {
                            "두 번 탭하면 브리핑을 시작합니다."
                        },
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = BriefingYellow.copy(alpha = alpha),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = icon,
                color = BriefingBlack,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
            Spacer(modifier = Modifier.size(34.dp))
            Text(
                text = label,
                color = BriefingBlack,
                fontSize = 64.sp,
                lineHeight = 70.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
        }
    }
}
