package com.ssafy.e102.eumgil.feature.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArrivalViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(ArrivalUiState())
    val uiState: StateFlow<ArrivalUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<ArrivalUiEvent>()
    val uiEvent: SharedFlow<ArrivalUiEvent> = mutableUiEvent.asSharedFlow()

    fun onAction(action: ArrivalUiAction) {
        when (action) {
            ArrivalUiAction.HomeClicked -> emitUiEvent(ArrivalUiEvent.NavigateToMap)
            ArrivalUiAction.ExploreNewRouteClicked -> emitUiEvent(ArrivalUiEvent.NavigateToSearch)
            is ArrivalUiAction.RatingSelected -> updateSelectedRating(action.rating)
            ArrivalUiAction.SaveRouteClicked ->
                mutableUiState.update { state ->
                    state.copy(isRouteSaveSelected = !state.isRouteSaveSelected)
                }
            ArrivalUiAction.SubmitEvaluationClicked ->
                mutableUiState.update { state ->
                    if (!state.isEvaluationSubmitEnabled) {
                        state
                    } else {
                        state.copy(isEvaluationSheetVisible = false)
                    }
                }
            ArrivalUiAction.EvaluationSheetDismissed ->
                mutableUiState.update { state ->
                    state.copy(isEvaluationSheetVisible = false)
                }
        }
    }

    private fun emitUiEvent(event: ArrivalUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    private fun updateSelectedRating(rating: Int) {
        val resolvedRating = rating.coerceIn(1, 5)
        mutableUiState.update { state ->
            state.copy(
                selectedRating = resolvedRating,
                selectedRatingLabel = resolvedRating.toArrivalEvaluationLabel(),
            )
        }
    }
}

private fun Int.toArrivalEvaluationLabel(): ArrivalEvaluationLabel =
    when (this) {
        1 -> ArrivalEvaluationLabel.VeryDissatisfied
        2 -> ArrivalEvaluationLabel.Dissatisfied
        3 -> ArrivalEvaluationLabel.Neutral
        4 -> ArrivalEvaluationLabel.Satisfied
        5 -> ArrivalEvaluationLabel.VerySatisfied
        else -> ArrivalEvaluationLabel.Idle
    }
