package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilTheme
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

private val NavigationBackground = Color(0xFF1C1C1E)
private val NavigationDivider = Color(0xFF3A3A3C)
private val NavigationAmber = Color(0xFFF2B705)
private val NavigationTextOnAmber = Color(0xFF1C1C1E)
private val NavigationMuted = Color(0xFF8E8E93)
private val NavigationExitRed = Color(0xFFFF6B6B)

@Composable
fun NavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(NavigationBackground),
    ) {
        NavigationMetricsRow(
            distanceLabel = uiState.stepCard.metrics.getOrNull(0)?.value ?: "-",
            timeLabel = uiState.stepCard.metrics.getOrNull(1)?.value ?: "-",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.medium)
                    .padding(top = EumSpacing.large),
        )

        Spacer(modifier = Modifier.height(EumSpacing.large))

        NavigationLocationCard(
            locationLabel = uiState.mapOverlay.currentLocation?.label ?: "현재 위치",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = EumSpacing.medium),
        )

        Spacer(modifier = Modifier.height(EumSpacing.medium))

        NavigationExitCard(
            label = uiState.exitCta.label,
            supportingText = uiState.exitCta.supportingText,
            enabled = uiState.isExitEnabled,
            onClick = { onAction(NavigationUiAction.ExitNavigationClicked) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.medium),
        )

        Spacer(modifier = Modifier.height(EumSpacing.medium))
    }
}

@Composable
private fun NavigationMetricsRow(
    distanceLabel: String,
    timeLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationMetricItem(
            label = "남은 거리",
            value = distanceLabel,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "남은 거리 $distanceLabel"
                    },
        )

        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(76.dp)
                    .background(NavigationDivider),
        )

        NavigationMetricItem(
            label = "예상 시간",
            value = timeLabel,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "예상 시간 $timeLabel"
                    },
        )
    }
}

@Composable
private fun NavigationMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = NavigationMuted,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NavigationAmber,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun NavigationLocationCard(
    locationLabel: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = "현재 위치 $locationLabel"
            },
        shape = RoundedCornerShape(EumRadius.large),
        color = NavigationAmber,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(EumSpacing.xLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_voice_location_pin),
                contentDescription = null,
                tint = NavigationTextOnAmber,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(EumSpacing.medium))
            Text(
                text = locationLabel,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavigationTextOnAmber,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
            )
        }
    }
}

@Composable
private fun NavigationExitCard(
    label: String,
    supportingText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val alpha = if (enabled) 1f else 0.45f

    Row(
        modifier =
            modifier
                .height(96.dp)
                .clip(RoundedCornerShape(EumRadius.large))
                .background(Color(0xFF2C2C2E))
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = NavigationAmber),
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "$label $supportingText"
                }
                .padding(horizontal = EumSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(EumRadius.full))
                    .background(NavigationExitRed.copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_close),
                contentDescription = null,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(28.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavigationAmber.copy(alpha = alpha),
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f * alpha),
                maxLines = 2,
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    name = "Navigation ready",
    backgroundColor = 0xFF1C1C1E,
)
@Composable
private fun NavigationReadyPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState =
                NavigationUiState(
                    screenState = NavigationScreenState.Ready,
                    stepCard =
                        NavigationStepCardUiState(
                            metrics =
                                listOf(
                                    NavigationStepMetricUiState(label = "남은 거리", value = "50m"),
                                    NavigationStepMetricUiState(label = "예상 시간", value = "2분"),
                                ),
                        ),
                    exitCta =
                        NavigationCtaUiState(
                            label = "안내 종료",
                            supportingText = "안내를 종료하고 지도로 돌아갑니다.",
                            isEnabled = true,
                        ),
                ),
            onAction = {},
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    name = "Navigation loading",
    backgroundColor = 0xFF1C1C1E,
)
@Composable
private fun NavigationLoadingPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState = NavigationUiState(),
            onAction = {},
        )
    }
}
