package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

private val CompleteBackground = Color(0xFF0D0D0F)
private val CompleteYellow = LowVisionScreenDefaults.brandYellow
private val CompleteBlack = Color(0xFF000000)

@Composable
fun LowVisionNavigationCompleteScreen(
    isSaveEnabled: Boolean,
    onSaveClick: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(CompleteBackground)
                .statusBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Text(
                text = "안내 완료",
                color = Color.White,
                fontSize = 52.sp,
                lineHeight = 60.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
            )

            LowVisionCompleteSaveButton(
                enabled = isSaveEnabled,
                onClick = onSaveClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 360.dp),
            )
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.HOME,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun LowVisionCompleteSaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.45f

    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .lowVisionButtonSemantics(
                    label = "목적지 저장",
                    actionHint = "저장 후 북마크로 이동합니다.",
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = CompleteYellow.copy(alpha = alpha),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "목적지 저장",
                color = CompleteBlack,
                fontSize = 54.sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 0.sp,
                maxLines = 2,
            )
        }
    }
}
