package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Route wrapper for [LowVisionVoiceInputScreen].
 *
 * 녹음 종료/취소 콜백([onRecordingFinished])과 탭 선택 콜백을 NavGraph가 받는다.
 * 실제 STT 시작/종료는 별도 ViewModel에서 처리하도록 본 래퍼에서는 호출만 위임한다.
 */
@Composable
fun LowVisionVoiceInputRoute(
    onCancelRecording: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LowVisionVoiceInputScreen(
        uiState = LowVisionVoiceInputUiState(selectedTab = LowVisionBottomTab.HOME),
        onCancelRecording = onCancelRecording,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}
