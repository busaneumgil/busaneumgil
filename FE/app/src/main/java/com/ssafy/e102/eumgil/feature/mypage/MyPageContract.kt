package com.ssafy.e102.eumgil.feature.mypage

data class MyPageUiState(
    val displayName: String? = null,
    val userMode: MyPageUserMode = MyPageUserMode.UNKNOWN,
    val mobilitySubtype: MyPageMobilitySubtype? = null,
    val isDebugSectionVisible: Boolean = false,
    val isRuntimeToggleEnabled: Boolean = false,
    val isForceMockEnabled: Boolean = false,
)

enum class MyPageUserMode {
    LOW_VISION,
    MOBILITY_IMPAIRED,
    UNKNOWN,
}

enum class MyPageMobilitySubtype {
    ELECTRIC_WHEELCHAIR,
    MANUAL_WHEELCHAIR,
    OTHER,
}

enum class MyPageMenuItem {
    NOTICE,
    REPORT_HISTORY,
    APP_HELP,
}

sealed interface MyPageUiAction {
    data class ForceMockToggled(
        val isEnabled: Boolean,
    ) : MyPageUiAction

    data object UserTypeChangeClicked : MyPageUiAction

    data object LogoutClicked : MyPageUiAction

    data class MainMenuClicked(
        val menuItem: MyPageMenuItem,
    ) : MyPageUiAction
}

sealed interface MyPageUiEvent {
    data object NavigateToUserTypePrimary : MyPageUiEvent

    data object NavigateToLogin : MyPageUiEvent

    data object NavigateToReportHistory : MyPageUiEvent

    data object ShowPreparingMessage : MyPageUiEvent
}
