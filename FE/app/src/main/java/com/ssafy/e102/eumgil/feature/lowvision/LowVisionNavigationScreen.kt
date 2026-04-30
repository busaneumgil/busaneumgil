package com.ssafy.e102.eumgil.feature.lowvision

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
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_voice_location_pin),
            contentDescription = null,
            tint = LowVisionNavigationYellow,
            modifier = Modifier.size(42.dp),
        )

        LowVisionNavigationActionCard(
            label = "목적지 저장",
            enabled = enabled,
            backgroundColor = LowVisionNavigationYellow,
            contentColor = LowVisionNavigationBlack,
            icon = {
                Box(
                    modifier =
                        Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LowVisionNavigationBlack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_voice_location_pin),
                        contentDescription = null,
                        tint = LowVisionNavigationYellow,
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

        LowVisionNavigationActionCard(
            label = "완료",
            enabled = enabled,
            backgroundColor = LowVisionNavigationBlack,
            contentColor = LowVisionNavigationYellow,
            border = BorderStroke(width = 2.dp, color = LowVisionNavigationYellow),
            icon = {
                Box(
                    modifier =
                        Modifier
                            .size(104.dp)
                            .border(
                                width = 8.dp,
                                color = LowVisionNavigationYellow,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_status_check),
                        contentDescription = null,
                        tint = LowVisionNavigationYellow,
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
