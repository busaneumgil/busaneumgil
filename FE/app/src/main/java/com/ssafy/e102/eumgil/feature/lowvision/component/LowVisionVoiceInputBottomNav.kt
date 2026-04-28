package com.ssafy.e102.eumgil.feature.lowvision.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * 시각지원 모드 음성 입력(녹음) 화면용 하단 네비.
 *
 * Figma node 371:311 — 75dp 높이, 상단 1dp #252525 보더, 활성 노랑 텍스트+아이콘,
 * 비활성 Emperor(#555). 홈 nav보다 비활성 색이 더 어둡다.
 */
@Composable
fun LowVisionVoiceInputBottomNav(
    selectedTab: LowVisionBottomTab,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        LowVisionBottomTab.HOME to LowVisionVoiceInputNavItem(
            iconRes = R.drawable.ic_nav_home_filled,
            labelRes = R.string.low_vision_nav_home,
        ),
        LowVisionBottomTab.BOOKMARK to LowVisionVoiceInputNavItem(
            iconRes = R.drawable.ic_nav_bookmark_outline,
            labelRes = R.string.low_vision_nav_bookmark,
        ),
        LowVisionBottomTab.CATEGORY to LowVisionVoiceInputNavItem(
            iconRes = R.drawable.ic_nav_category_grid,
            labelRes = R.string.low_vision_nav_category,
        ),
        LowVisionBottomTab.MY_PAGE to LowVisionVoiceInputNavItem(
            iconRes = R.drawable.ic_nav_person_outline,
            labelRes = R.string.low_vision_nav_my_page,
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF252525))
            .padding(top = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .background(Color.Black)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items.forEach { (tab, item) ->
                val tint = if (tab == selectedTab) Color(0xFFFFD400) else Color(0xFF555555)
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = stringResource(id = item.labelRes),
                        tint = tint,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        text = stringResource(id = item.labelRes),
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private data class LowVisionVoiceInputNavItem(
    val iconRes: Int,
    val labelRes: Int,
)
