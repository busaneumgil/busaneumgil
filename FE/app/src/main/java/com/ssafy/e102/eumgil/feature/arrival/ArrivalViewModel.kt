package com.ssafy.e102.eumgil.feature.arrival

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
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
    private val bookmarkRepository: BookmarkRepository,
    destinationSelectionRepository: DestinationSelectionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ArrivalUiState())
    val uiState: StateFlow<ArrivalUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<ArrivalUiEvent>()
    val uiEvent: SharedFlow<ArrivalUiEvent> = mutableUiEvent.asSharedFlow()

    private val arrivalDestination = destinationSelectionRepository.selectedDestination.value

    init {
        mutableUiState.update { state ->
            state.copy(
                hasRouteSaveTarget = arrivalDestination != null,
                isRouteSaveUpdating = arrivalDestination != null,
            )
        }
        if (arrivalDestination != null) {
            syncInitialRouteSaveState(arrivalDestination)
        }
    }

    fun onAction(action: ArrivalUiAction) {
        when (action) {
            ArrivalUiAction.HomeClicked -> emitUiEvent(ArrivalUiEvent.NavigateToMap)
            ArrivalUiAction.ExploreNewRouteClicked -> emitUiEvent(ArrivalUiEvent.NavigateToSearch)
            is ArrivalUiAction.RatingSelected -> updateSelectedRating(action.rating)
            ArrivalUiAction.SaveRouteClicked -> toggleRouteSave()
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

    private fun syncInitialRouteSaveState(destination: PlaceDestination) {
        viewModelScope.launch {
            runCatching {
                bookmarkRepository.isBookmarked(destination.placeId)
            }.onSuccess { isBookmarked ->
                mutableUiState.update { state ->
                    state.copy(
                        isRouteSaveSelected = isBookmarked,
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

    private fun toggleRouteSave() {
        val destination = arrivalDestination ?: return
        val currentState = uiState.value
        if (!currentState.isRouteSaveEnabled) return

        val nextBookmarked = !currentState.isRouteSaveSelected
        mutableUiState.update { state ->
            state.copy(
                isRouteSaveSelected = nextBookmarked,
                isRouteSaveUpdating = true,
            )
        }

        viewModelScope.launch {
            runCatching {
                if (nextBookmarked) {
                    bookmarkRepository.saveBookmark(destination.toBookmarkData())
                } else {
                    bookmarkRepository.deleteBookmark(destination.placeId)
                }
            }.onSuccess {
                mutableUiState.update { state ->
                    state.copy(
                        isRouteSaveSelected = nextBookmarked,
                        isRouteSaveUpdating = false,
                    )
                }
                emitUiEvent(
                    ArrivalUiEvent.ShowSnackbar(
                        messageResId =
                            if (nextBookmarked) {
                                R.string.arrival_bookmark_save_success_message
                            } else {
                                R.string.arrival_bookmark_delete_success_message
                            },
                    ),
                )
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                mutableUiState.update { state ->
                    state.copy(
                        isRouteSaveSelected = currentState.isRouteSaveSelected,
                        isRouteSaveUpdating = false,
                    )
                }
                emitUiEvent(
                    ArrivalUiEvent.ShowSnackbar(
                        messageResId = R.string.arrival_bookmark_update_failure_message,
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
            bookmarkRepository: BookmarkRepository,
            destinationSelectionRepository: DestinationSelectionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ArrivalViewModel(
                        bookmarkRepository = bookmarkRepository,
                        destinationSelectionRepository = destinationSelectionRepository,
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

private fun PlaceDestination.toBookmarkData(): BookmarkData =
    BookmarkData(
        placeId = placeId,
        placeName = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        category = category?.name,
    )
