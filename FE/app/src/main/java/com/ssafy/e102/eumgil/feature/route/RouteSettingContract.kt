package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.PlaceDestination

data class RouteSettingUiState(
    val startPoint: RouteSettingPointUiModel = RouteSettingPointUiModel.CurrentLocation,
    val destination: PlaceDestination? = null,
    val selectedOptions: Set<RouteSettingOption> = RouteSettingOption.defaultSelection,
    val screenState: RouteSettingScreenState = RouteSettingScreenState.Editing,
    val errorMessage: String? = null,
) {
    val isSearchEnabled: Boolean
        get() = screenState != RouteSettingScreenState.FindingRoute

    val isRouteFindEnabled: Boolean
        get() = destination != null && screenState != RouteSettingScreenState.FindingRoute

    val isNavigationStartEnabled: Boolean
        get() = destination != null && screenState == RouteSettingScreenState.PreviewReady
}

data class RouteSettingPointUiModel(
    val title: String,
    val description: String,
    val coordinateText: String? = null,
    val isAvailable: Boolean = true,
) {
    companion object {
        val CurrentLocation: RouteSettingPointUiModel =
            RouteSettingPointUiModel(
                title = "현재 위치",
                description = "실제 위치 연동 전까지 현재 위치 placeholder를 사용합니다.",
            )
    }
}

enum class RouteSettingOption(
    val label: String,
    val description: String,
) {
    ACCESSIBILITY_FIRST(
        label = "접근성 우선",
        description = "경사로, 엘리베이터, 보행 안전 정보를 우선 고려합니다.",
    ),
    AVOID_STEEP_SLOPE(
        label = "급경사 회피",
        description = "경사가 큰 구간을 피하는 경로 시안입니다.",
    ),
    USE_ELEVATOR_RAMP(
        label = "엘리베이터/경사로 고려",
        description = "수직 이동과 단차 회피 시설을 경로 조건에 포함합니다.",
    );

    companion object {
        val defaultSelection: Set<RouteSettingOption> =
            setOf(ACCESSIBILITY_FIRST, AVOID_STEEP_SLOPE, USE_ELEVATOR_RAMP)
    }
}

enum class RouteSettingScreenState {
    Editing,
    FindingRoute,
    PreviewReady,
    Failure,
}

sealed interface RouteSettingUiAction {
    data object BackClicked : RouteSettingUiAction

    data object StartPointClicked : RouteSettingUiAction

    data object DestinationClicked : RouteSettingUiAction

    data class RouteOptionToggled(
        val option: RouteSettingOption,
    ) : RouteSettingUiAction

    data object FindRouteClicked : RouteSettingUiAction

    data object StartNavigationClicked : RouteSettingUiAction
}

sealed interface RouteSettingUiEvent {
    data object NavigateBack : RouteSettingUiEvent

    data object NavigateToSearch : RouteSettingUiEvent

    data object NavigateToNavigation : RouteSettingUiEvent

    data class ShowSnackbar(
        val message: String,
    ) : RouteSettingUiEvent
}
