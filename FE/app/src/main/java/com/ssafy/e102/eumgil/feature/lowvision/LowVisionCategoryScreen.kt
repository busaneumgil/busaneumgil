package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

private val LowVisionCategoryYellow = Color(0xFFFFD400)
private val LowVisionCategoryBackground = Color(0xFF0D0D0F)

private val lowVisionCategoryOptions =
    listOf(
        LowVisionCategoryOption(
            label = "화장실",
            iconRes = R.drawable.ic_place_restroom,
        ),
        LowVisionCategoryOption(
            label = "음식점",
            iconRes = R.drawable.ic_place_restaurant,
        ),
        LowVisionCategoryOption(
            label = "숙박시설",
            iconRes = R.drawable.ic_place_lodging,
        ),
        LowVisionCategoryOption(
            label = "병원",
            iconRes = R.drawable.ic_place_hospital,
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
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = LowVisionScreenDefaults.screenHorizontalPadding,
                        vertical = LowVisionScreenDefaults.screenVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            LowVisionCategoryHeader(onBackClick = onBackClick)
            LowVisionCategoryGrid(onCategorySelected = onCategorySelected)
            Spacer(modifier = Modifier.height(12.dp))
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
                    .size(64.dp)
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
                modifier = Modifier.size(52.dp),
            )
        }
        Text(
            text = "카테고리",
            color = LowVisionCategoryYellow,
            fontSize = 52.sp,
            lineHeight = 60.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun LowVisionCategoryGrid(onCategorySelected: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        lowVisionCategoryOptions.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                rowOptions.forEach { option ->
                    LowVisionCategoryCard(
                        option = option,
                        onClick = { onCategorySelected(option.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 3.dp,
                    color = LowVisionCategoryYellow,
                    shape = RoundedCornerShape(18.dp),
                )
                .background(LowVisionCategoryBackground)
                .lowVisionButtonSemantics(
                    label = option.label,
                    actionHint = "두 번 탭하면 ${option.label} 결과를 봅니다.",
                )
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = option.iconRes),
            contentDescription = null,
            tint = LowVisionCategoryYellow,
            modifier = Modifier.size(108.dp),
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = option.label,
            color = LowVisionCategoryYellow,
            fontSize = 38.sp,
            lineHeight = 46.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
    }
}

private data class LowVisionCategoryOption(
    val label: String,
    @DrawableRes val iconRes: Int,
)
