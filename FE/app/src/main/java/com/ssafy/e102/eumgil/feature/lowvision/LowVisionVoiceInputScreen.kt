package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import com.ssafy.e102.eumgil.feature.lowvision.component.LowVisionBottomNav

/**
 * 시각지원 모드 음성 입력(녹음 진행) 화면.
 *
 * 출처: Figma file MREqSzkmwhRcXnFS3lzW17, node 371:300 ("div.phone").
 *
 * 디자인 규칙:
 *   - color/yellow/50  = #FFD400  (Gold)            — 마이크 원 보더, 입력중 텍스트, 활성 nav
 *   - color/black/solid = #000000                   — 배경
 *   - color/grey/15    = #252525  (Mine Shaft 변)  — nav 상단 1dp 보더
 *   - color/grey/33    = #555555  (Emperor)         — 비활성 nav 라벨
 *   - 마이크 원 140dp, 5dp 노랑 보더, 84dp 마이크 아이콘
 *   - "입력중" 라벨 44.8sp Black weight (Pretendard 의도)
 *
 * 마이크 원 자체는 단일 클릭이 아니라 녹음 진행 중 시각 피드백이지만, 시각지원
 * 사용자가 즉시 종료할 수 있도록 본 화면 어디나 탭하면 [onCancelRecording]을 호출한다.
 */
@Composable
fun LowVisionVoiceInputScreen(
    uiState: LowVisionVoiceInputUiState,
    onCancelRecording: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val recordingLabel = stringResource(id = R.string.low_vision_voice_input_recording_label)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .statusBarsPadding()
                .lowVisionButtonSemantics(
                    label = recordingLabel,
                    actionHint = "두 번 탭하면 입력을 종료합니다.",
                )
                .clickable { onCancelRecording() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(25.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .border(
                            width = 5.dp,
                            color = Color(0xFFFFD400),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_voice_mic),
                        contentDescription = null,
                        tint = Color(0xFFFFD400),
                        modifier = Modifier.size(84.dp),
                    )
                }

                Text(
                    text = recordingLabel,
                    color = Color(0xFFFFD400),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Spacer(modifier = Modifier.size(0.dp))

        LowVisionBottomNav(
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
        )
    }
}
