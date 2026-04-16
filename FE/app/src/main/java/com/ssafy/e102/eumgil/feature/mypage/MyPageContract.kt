package com.ssafy.e102.eumgil.feature.mypage

data class MyPageUiState(
    val isDebugSectionVisible: Boolean = false,
    val isRuntimeToggleEnabled: Boolean = false,
    val isForceMockEnabled: Boolean = false,
)

sealed interface MyPageUiAction {
    data class ForceMockToggled(
        val isEnabled: Boolean,
    ) : MyPageUiAction
}
