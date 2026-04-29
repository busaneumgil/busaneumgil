package com.ssafy.e102.eumgil.feature.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilTheme
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

private val NavigationBackground = Color(0xFF1C1C1E)
private val NavigationYellow = Color(0xFFFFD400)
private val NavigationBlack = Color(0xFF000000)

@Composable
fun NavigationScreen(
    uiState: NavigationUiState,
    onAction: (NavigationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = uiState.isExitEnabled

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(NavigationBackground)
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 24.dp, bottom = EumSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_voice_location_pin),
            contentDescription = null,
            tint = NavigationYellow,
            modifier = Modifier.size(42.dp),
        )

        NavigationCompletionCard(
            label = "도착지 저장",
            enabled = enabled,
            backgroundColor = NavigationYellow,
            contentColor = NavigationBlack,
            icon = {
                Box(
                    modifier =
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavigationBlack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_voice_location_pin),
                        contentDescription = null,
                        tint = NavigationYellow,
                        modifier = Modifier.size(70.dp),
                    )
                }
            },
            onClick = { onAction(NavigationUiAction.SaveBookmarkClicked) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 260.dp),
        )

        NavigationCompletionCard(
            label = "완료",
            enabled = enabled,
            backgroundColor = NavigationBlack,
            contentColor = NavigationYellow,
            border = BorderStroke(width = 2.dp, color = NavigationYellow),
            icon = {
                Box(
                    modifier =
                        Modifier
                            .size(104.dp)
                            .border(
                                width = 8.dp,
                                color = NavigationYellow,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_status_check),
                        contentDescription = null,
                        tint = NavigationYellow,
                        modifier = Modifier.size(62.dp),
                    )
                }
            },
            onClick = { onAction(NavigationUiAction.NavigationCompleteClicked) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 260.dp),
        )
    }
}

@Composable
private fun NavigationCompletionCard(
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
                .clickable(enabled = enabled, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = label
                },
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = border,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(modifier = Modifier.padding(bottom = 36.dp)) {
                icon()
            }
            Text(
                text = label,
                color = contentColor.copy(alpha = contentAlpha),
                fontSize = 48.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                maxLines = 2,
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    name = "Navigation completion",
    backgroundColor = 0xFF1C1C1E,
)
@Composable
private fun NavigationCompletionPreview() {
    BusanEumgilTheme {
        NavigationScreen(
            uiState =
                NavigationUiState(
                    screenState = NavigationScreenState.Ready,
                    exitCta = NavigationCtaUiState(isEnabled = true),
                ),
            onAction = {},
        )
    }
}
