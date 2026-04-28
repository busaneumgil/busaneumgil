package com.ssafy.e102.eumgil.feature.lowvision.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.lowvision.LowVisionBottomTab

/**
 * 시각지원 모드 메인 홈 화면용 하단 네비.
 *
 * Figma node 371:157 — 80dp 높이, 활성 탭은 노랑(#FFD400) 아이콘+라벨, 비활성은
 * Boulder(#777). 약관 walkthrough의 nav가 활성 탭에 노랑 배경을 깔던 것과 다르게
 * 본 셸은 텍스트·아이콘 색만 노랑으로 바뀐다.
 */
@Composable
fun LowVisionHomeBottomNav(
    selectedTab: LowVisionBottomTab,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        LowVisionBottomTab.HOME to LowVisionNavItem(
            iconRes = R.drawable.ic_nav_home_filled,
            labelRes = R.string.low_vision_nav_home,
        ),
        LowVisionBottomTab.BOOKMARK to LowVisionNavItem(
            iconRes = R.drawable.ic_nav_bookmark_outline,
            labelRes = R.string.low_vision_nav_bookmark,
        ),
        LowVisionBottomTab.CATEGORY to LowVisionNavItem(
            iconRes = R.drawable.ic_nav_category_grid,
            labelRes = R.string.low_vision_nav_category,
        ),
        LowVisionBottomTab.MY_PAGE to LowVisionNavItem(
            iconRes = R.drawable.ic_nav_person_outline,
            labelRes = R.string.low_vision_nav_my_page,
        ),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.Black)
            .padding(horizontal = 27.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { (tab, item) ->
            val tint = if (tab == selectedTab) Color(0xFFFFD400) else Color(0xFF777777)
            Column(
                modifier = Modifier
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = stringResource(id = item.labelRes),
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(id = item.labelRes),
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

private data class LowVisionNavItem(
    val iconRes: Int,
    val labelRes: Int,
)
