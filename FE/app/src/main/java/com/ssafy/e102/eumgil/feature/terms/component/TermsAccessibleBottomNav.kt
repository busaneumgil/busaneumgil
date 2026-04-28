package com.ssafy.e102.eumgil.feature.terms.component

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
import com.ssafy.e102.eumgil.feature.terms.TermsBottomTab

/**
 * High-contrast bottom navigation for the terms walkthrough (Figma node 328:511).
 *
 * Selected tab paints a yellow background with a black icon/label to satisfy the
 * voice-guide / large-contrast accessibility requirement. Other tabs use Boulder
 * (#777) text on the same black surface.
 */
@Composable
fun TermsAccessibleBottomNav(
    selectedTab: TermsBottomTab,
    onTabSelected: (TermsBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        TermsBottomTab.HOME to TermsBottomNavItem(
            iconRes = R.drawable.ic_nav_home_filled,
            labelRes = R.string.terms_guide_nav_home,
        ),
        TermsBottomTab.BOOKMARK to TermsBottomNavItem(
            iconRes = R.drawable.ic_nav_bookmark_outline,
            labelRes = R.string.terms_guide_nav_bookmark,
        ),
        TermsBottomTab.CATEGORY to TermsBottomNavItem(
            iconRes = R.drawable.ic_nav_category_grid,
            labelRes = R.string.terms_guide_nav_category,
        ),
        TermsBottomTab.MY to TermsBottomNavItem(
            iconRes = R.drawable.ic_nav_person_outline,
            labelRes = R.string.terms_guide_nav_my,
        ),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(71.dp)
            .background(Color.Black),
        verticalAlignment = Alignment.Top,
    ) {
        items.forEach { (tab, item) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        color = if (tab == selectedTab) Color(0xFFFFCC00) else Color.Transparent,
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(top = 15.5.dp, bottom = 17.5.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val tint = if (tab == selectedTab) Color.Black else Color(0xFF777777)
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = stringResource(id = item.labelRes),
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(id = item.labelRes),
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private data class TermsBottomNavItem(
    val iconRes: Int,
    val labelRes: Int,
)
