package com.ssafy.e102.eumgil.feature.lowvision

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionHomeBottomNav

/**
 * 시각지원 모드 메인 홈 화면.
 *
 * 출처: Figma file MREqSzkmwhRcXnFS3lzW17, node 371:105 ("home").
 *
 * 디자인 규칙(Figma get_variable_defs):
 *   - color/yellow/50  = #FFD400 (Gold)        — 카드, 활성 nav
 *   - color/black/solid = #000000              — 배경
 *   - color/grey/12    = #1E1E1E                — info-box 배경
 *   - color/grey/20    = #333333                — info-box 보더
 *   - color/grey/47    = #777777 (Boulder)     — 비활성 nav
 *   - color/grey/80    = #CCCCCC (Silver)      — info-box 보조 텍스트
 *   - 카드 radius 24dp, info-box radius 16dp
 *   - 카드 라벨 28.8sp Bold(letter spacing -1), info-title 17.6sp Bold,
 *     info-body 14.4sp Regular, nav 11sp Regular
 *
 * Figma의 시뮬레이터 chrome(상단 9:41 status bar, 하단 안드로이드 navigation bar
 * 모형)은 시스템 시스템바가 처리하는 영역이므로 의도적으로 그리지 않는다.
 */
@Composable
fun LowVisionHomeScreen(
    uiState: LowVisionHomeUiState,
    onVoiceInputClick: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val voiceCardA11y = stringResource(id = R.string.low_vision_home_voice_card_a11y)
    val locationCardA11y = stringResource(id = R.string.low_vision_home_location_card_a11y)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1) 음성 입력 카드 (큰 카드, 252.7dp)
            HomeYellowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(252.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = voiceCardA11y
                    },
                iconRes = R.drawable.ic_voice_mic,
                iconSize = 64.dp,
                label = stringResource(id = R.string.low_vision_home_voice_input_label),
                labelSize = 28.sp,
                onClick = onVoiceInputClick,
            )

            // 2) 현재 위치 카드 (작은 카드, 117.92dp)
            HomeYellowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = locationCardA11y
                    },
                iconRes = R.drawable.ic_voice_location_pin,
                iconSize = 40.dp,
                label = stringResource(id = R.string.low_vision_home_current_location_label),
                labelSize = 22.sp,
                onClick = onCurrentLocationClick,
            )

            // 3) 입력 대기 상태 정보 박스
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E1E))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF333333),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 25.dp, vertical = 21.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_voice_speaker_wave),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.low_vision_home_status_title),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.low_vision_home_status_description),
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        LowVisionHomeBottomNav(
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
        )
    }
}

@Composable
private fun HomeYellowCard(
    iconRes: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    label: String,
    labelSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFFD400))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = label,
                color = Color.Black,
                fontSize = labelSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
        }
    }
}
