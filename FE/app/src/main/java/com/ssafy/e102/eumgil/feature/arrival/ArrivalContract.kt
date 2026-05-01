package com.ssafy.e102.eumgil.feature.arrival

data class ArrivalUiState(
    val isEvaluationSheetVisible: Boolean = false,
)

sealed interface ArrivalUiAction {
    data object HomeClicked : ArrivalUiAction

    data object ExploreNewRouteClicked : ArrivalUiAction

    data object EvaluationSheetDismissed : ArrivalUiAction
}

sealed interface ArrivalUiEvent {
    data object NavigateToMap : ArrivalUiEvent

    data object NavigateToSearch : ArrivalUiEvent
}
