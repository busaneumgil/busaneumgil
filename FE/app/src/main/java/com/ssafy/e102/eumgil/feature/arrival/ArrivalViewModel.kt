package com.ssafy.e102.eumgil.feature.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.RouteBookmarkDraft
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.repository.RouteBookmarkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArrivalViewModel(
    private val routeBookmarkRepository: RouteBookmarkRepository,
    private val currentRouteBookmarkDraft: RouteBookmarkDraft?,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ArrivalUiState())
    val uiState: StateFlow<ArrivalUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<ArrivalUiEvent>()
    val uiEvent: SharedFlow<ArrivalUiEvent> = mutableUiEvent.asSharedFlow()

    init {
        mutableUiState.update { state ->
            state.copy(
                routeSaveDraft = currentRouteBookmarkDraft?.toUiState(),
                routeNameInput = currentRouteBookmarkDraft?.defaultRouteName.orEmpty(),
                isRouteSaveUpdating = currentRouteBookmarkDraft != null,
            )
        }
        currentRouteBookmarkDraft?.let(::syncInitialRouteSaveState)
    }

    fun onAction(action: ArrivalUiAction) {
        when (action) {
            ArrivalUiAction.HomeClicked -> emitUiEvent(ArrivalUiEvent.NavigateToMap)
            ArrivalUiAction.ExploreNewRouteClicked -> emitUiEvent(ArrivalUiEvent.NavigateToSearch)
            is ArrivalUiAction.RatingSelected -> updateSelectedRating(action.rating)
            ArrivalUiAction.SaveRouteClicked -> openRouteSaveDialog()
            is ArrivalUiAction.RouteNameChanged -> updateRouteName(action.value)
            ArrivalUiAction.ConfirmRouteSaveClicked -> saveRouteBookmark()
            ArrivalUiAction.RouteSaveDialogDismissed -> dismissRouteSaveDialog()
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

    private fun syncInitialRouteSaveState(draft: RouteBookmarkDraft) {
        viewModelScope.launch {
            runCatching {
                routeBookmarkRepository.isBookmarked(draft)
            }.onSuccess { isSaved ->
                mutableUiState.update { state ->
                    state.copy(
                        isRouteSaveSelected = isSaved,
                        isRouteSaveUpdating = false,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                mutableUiState.update { state ->
                    state.copy(isRouteSaveUpdating = false)
                }
            }
        }
    }

    private fun openRouteSaveDialog() {
        mutableUiState.update { state ->
            val draft = state.routeSaveDraft
            if (!state.isRouteSaveEnabled || draft == null) {
                state
            } else {
                state.copy(
                    isRouteSaveDialogVisible = true,
                    routeNameInput =
                        state.routeNameInput.ifBlank {
                            draft.defaultRouteName
                        },
                )
            }
        }
    }

    private fun updateRouteName(value: String) {
        mutableUiState.update { state ->
            state.copy(routeNameInput = value)
        }
    }

    private fun dismissRouteSaveDialog() {
        mutableUiState.update { state ->
            state.copy(isRouteSaveDialogVisible = false)
        }
    }

    private fun saveRouteBookmark() {
        val draft = currentRouteBookmarkDraft ?: return
        val currentState = uiState.value
        if (!currentState.isRouteSaveConfirmEnabled) return

        mutableUiState.update { state ->
            state.copy(isRouteSaveUpdating = true)
        }

        viewModelScope.launch {
            runCatching {
                routeBookmarkRepository.saveRouteBookmark(
                    draft.toSaveRequest(routeName = currentState.routeNameInput),
                )
            }.onSuccess { savedBookmark ->
                mutableUiState.update { state ->
                    state.copy(
                        routeNameInput = savedBookmark.routeName,
                        isRouteSaveSelected = true,
                        isRouteSaveUpdating = false,
                        isRouteSaveDialogVisible = false,
                    )
                }
                emitUiEvent(
                    ArrivalUiEvent.ShowSnackbar(
                        messageResId = R.string.arrival_route_save_success_message,
                    ),
                )
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                mutableUiState.update { state ->
                    state.copy(isRouteSaveUpdating = false)
                }
                emitUiEvent(
                    ArrivalUiEvent.ShowSnackbar(
                        messageResId = R.string.arrival_route_save_failure_message,
                    ),
                )
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

    companion object {
        fun provideFactory(
            routeBookmarkRepository: RouteBookmarkRepository,
            currentRouteBookmarkDraft: RouteBookmarkDraft?,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ArrivalViewModel(
                        routeBookmarkRepository = routeBookmarkRepository,
                        currentRouteBookmarkDraft = currentRouteBookmarkDraft,
                    ) as T
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

private fun RouteBookmarkDraft.toUiState(): ArrivalRouteSaveDraftUiState =
    ArrivalRouteSaveDraftUiState(
        defaultRouteName = defaultRouteName,
        startLabel = startLabel.ifBlank { "출발지" },
        endLabel = endLabel.ifBlank { "도착지" },
        routeOptionLabel =
            when (routeOption) {
                RouteOption.SAFE -> "안전한 길"
                RouteOption.SHORTEST -> "최단거리"
            },
        distanceMeters = distanceMeters,
        durationMinutes = durationMinutes,
    )
