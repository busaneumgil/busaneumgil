package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiState

private val LowVisionNavigationBackground = Color(0xFF1C1C1E)
private val LowVisionNavigationYellow = Color(0xFFFFD400)
private val LowVisionNavigationBlack = Color(0xFF000000)
private val LowVisionNavigationMuted = Color(0xFFBDBDBD)

@Composable
fun LowVisionNavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = uiState.isExitEnabled

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
        LowVisionGuidanceCard(
            uiState = uiState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .heightIn(min = 360.dp),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 180.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LowVisionNavigationActionCard(
                label = "목적지 저장",
                enabled = enabled,
                backgroundColor = LowVisionNavigationYellow,
                contentColor = LowVisionNavigationBlack,
                icon = {
                    Box(
                        modifier =
                            Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LowVisionNavigationBlack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_voice_location_pin),
                            contentDescription = null,
                            tint = LowVisionNavigationYellow,
                            modifier = Modifier.size(50.dp),
                        )
                    }
                },
                onClick = { onAction(NavigationUiAction.SaveBookmarkClicked) },
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize(),
            )

            LowVisionNavigationActionCard(
                label = "안내 완료",
                enabled = enabled,
                backgroundColor = LowVisionNavigationBlack,
                contentColor = LowVisionNavigationYellow,
                border = BorderStroke(width = 2.dp, color = LowVisionNavigationYellow),
                icon = {
                    Box(
                        modifier =
                            Modifier
                                .size(78.dp)
                                .border(
                                    width = 6.dp,
                                    color = LowVisionNavigationYellow,
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_status_check),
                            contentDescription = null,
                            tint = LowVisionNavigationYellow,
                            modifier = Modifier.size(46.dp),
                        )
                    }
                },
                onClick = { onAction(NavigationUiAction.NavigationCompleteClicked) },
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LowVisionGuidanceCard(
    uiState: NavigationUiState,
    modifier: Modifier = Modifier,
) {
    val metrics = uiState.stepCard.metrics
    val distance = metrics.getOrNull(0)?.value.orEmpty().ifBlank { "-" }
    val eta = metrics.getOrNull(1)?.value.orEmpty().ifBlank { "-" }
    val progress = metrics.getOrNull(2)?.value.orEmpty().ifBlank { "-" }
    val instruction = uiState.stepCard.instruction
    val supportingText = uiState.stepCard.supportingText

    Surface(
        modifier =
            modifier
                .semantics {
                    contentDescription = listOf("길 안내 중", instruction, supportingText).joinToString(". ")
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
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "길 안내 중",
                    color = LowVisionNavigationYellow,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp,
                )
                Text(
                    text = instruction,
                    color = Color.White,
                    fontSize = 38.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp,
                    maxLines = 3,
                )
                Text(
                    text = supportingText,
                    color = LowVisionNavigationMuted,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.sp,
                    maxLines = 3,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LowVisionGuidanceMetric(label = "거리", value = distance, modifier = Modifier.weight(1f))
                LowVisionGuidanceMetric(label = "시간", value = eta, modifier = Modifier.weight(1f))
                LowVisionGuidanceMetric(label = "단계", value = progress, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LowVisionGuidanceMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(LowVisionNavigationYellow)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = LowVisionNavigationBlack,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
            maxLines = 1,
        )
        Text(
            text = value,
            color = LowVisionNavigationBlack,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun LowVisionNavigationActionCard(
    label: String,
    enabled: Boolean,
    backgroundColor: Color,
    contentColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    val contentAlpha = if (enabled) 1f else 0.45f

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .lowVisionButtonSemantics(label)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = border,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.padding(bottom = 20.dp)) {
                icon()
            }
            Text(
                text = label,
                color = contentColor.copy(alpha = contentAlpha),
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                maxLines = 2,
            )
        }
    }
}
