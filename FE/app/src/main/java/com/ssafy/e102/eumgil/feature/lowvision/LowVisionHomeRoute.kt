package com.ssafy.e102.eumgil.feature.lowvision

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Route wrapper for [LowVisionHomeScreen].
 *
 * 화면 자체는 stateless에 가깝고 네비 선택만 saveable로 들고 있는다.
 * 실제 화면 전환(음성 입력 카드 → 음성 입력 화면 등)은 [onVoiceInputClick]·
 * [onCurrentLocationClick]·[onTabSelected] 콜백을 NavGraph가 받아서 처리한다.
 */
@Composable
fun LowVisionHomeRoute(
    onVoiceInputClick: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LowVisionFontTheme {
        LowVisionHomeScreen(
            uiState = LowVisionHomeUiState(selectedTab = LowVisionBottomTab.HOME),
            onVoiceInputClick = onVoiceInputClick,
            onCurrentLocationClick = onCurrentLocationClick,
            onTabSelected = onTabSelected,
            modifier = modifier,
        )
    }
}
