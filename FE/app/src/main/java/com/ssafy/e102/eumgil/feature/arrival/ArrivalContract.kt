package com.ssafy.e102.eumgil.feature.arrival

import com.ssafy.e102.eumgil.R

data class ArrivalUiState(
    val isEvaluationSheetVisible: Boolean = true,
    val selectedRating: Int = 0,
    val selectedRatingLabel: ArrivalEvaluationLabel = ArrivalEvaluationLabel.Idle,
    val isRouteSaveSelected: Boolean = false,
    val isRouteSaveUpdating: Boolean = false,
    val hasRouteSaveTarget: Boolean = false,
) {
    val isEvaluationSubmitEnabled: Boolean
        get() = selectedRating > 0

    val isRouteSaveEnabled: Boolean
        get() = hasRouteSaveTarget && !isRouteSaveUpdating
}

enum class ArrivalEvaluationLabel(val labelResId: Int?) {
    Idle(labelResId = null),
    VeryDissatisfied(labelResId = R.string.arrival_evaluation_rating_very_dissatisfied),
    Dissatisfied(labelResId = R.string.arrival_evaluation_rating_dissatisfied),
    Neutral(labelResId = R.string.arrival_evaluation_rating_neutral),
    Satisfied(labelResId = R.string.arrival_evaluation_rating_satisfied),
    VerySatisfied(labelResId = R.string.arrival_evaluation_rating_very_satisfied),
}

sealed interface ArrivalUiAction {
    data object HomeClicked : ArrivalUiAction

    data object ExploreNewRouteClicked : ArrivalUiAction

    data class RatingSelected(val rating: Int) : ArrivalUiAction

    data object SaveRouteClicked : ArrivalUiAction

    data object SubmitEvaluationClicked : ArrivalUiAction

    data object EvaluationSheetDismissed : ArrivalUiAction
}

sealed interface ArrivalUiEvent {
    data object NavigateToMap : ArrivalUiEvent

    data object NavigateToSearch : ArrivalUiEvent

    data class ShowSnackbar(
        val messageResId: Int,
    ) : ArrivalUiEvent
}
