package com.ssafy.e102.eumgil.feature.lowvision

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

private val LowVisionYellow = Color(0xFFFFD400)

@Composable
fun LowVisionMyPageScreen(
    onModeChangeClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Text(
                text = stringResource(id = R.string.low_vision_my_page_title),
                modifier = Modifier.fillMaxWidth(),
                color = LowVisionYellow,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 60.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(56.dp))

            LowVisionMyPageAction(
                labelRes = R.string.low_vision_my_page_mode_change,
                iconRes = R.drawable.ic_lowvision_mode_change,
                filled = true,
                onClick = onModeChangeClick,
            )
            LowVisionMyPageAction(
                labelRes = R.string.low_vision_my_page_app_info,
                iconRes = R.drawable.ic_status_safe_info,
                filled = false,
                onClick = onAppInfoClick,
            )
            LowVisionMyPageAction(
                labelRes = R.string.low_vision_my_page_logout,
                iconRes = R.drawable.ic_lowvision_logout,
                filled = false,
                onClick = onLogoutClick,
            )
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.MY_PAGE,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
fun LowVisionAppInfoScreen(
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(id = R.string.low_vision_app_info_title),
                modifier = Modifier.fillMaxWidth(),
                color = LowVisionYellow,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 56.sp,
                textAlign = TextAlign.Center,
            )
            LowVisionInfoPanel(
                titleRes = R.string.low_vision_app_info_service_title,
                bodyRes = R.string.low_vision_app_info_service_body,
            )
            LowVisionInfoPanel(
                titleRes = R.string.low_vision_app_info_support_title,
                bodyRes = R.string.low_vision_app_info_support_body,
            )
        }

        LowVisionBottomNav(
            selectedTab = LowVisionBottomTab.MY_PAGE,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun LowVisionMyPageAction(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(id = labelRes)
    val backgroundColor = if (filled) LowVisionYellow else Color.Black
    val contentColor = if (filled) Color.Black else LowVisionYellow

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                }
                .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (filled) null else BorderStroke(width = 3.dp, color = LowVisionYellow),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(54.dp),
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = contentColor,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 46.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LowVisionInfoPanel(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(width = 3.dp, color = LowVisionYellow, shape = RoundedCornerShape(16.dp))
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = titleRes),
            color = LowVisionYellow,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 36.sp,
        )
        Text(
            text = stringResource(id = bodyRes),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp,
        )
    }
}
