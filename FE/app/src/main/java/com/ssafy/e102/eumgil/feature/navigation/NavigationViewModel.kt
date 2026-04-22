package com.ssafy.e102.eumgil.feature.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NavigationViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<NavigationUiEvent>()
    val uiEvent: SharedFlow<NavigationUiEvent> = mutableUiEvent.asSharedFlow()

    fun onAction(action: NavigationUiAction) {
        when (action) {
            NavigationUiAction.BackClicked -> emitUiEvent(NavigationUiEvent.NavigateBack)
            NavigationUiAction.EndNavigationClicked -> emitUiEvent(NavigationUiEvent.NavigateToMap)
            NavigationUiAction.VoiceGuidanceToggled -> toggleVoiceGuidance()
            NavigationUiAction.RerouteClicked -> setReroutingState()
            NavigationUiAction.CompleteMockNavigationClicked -> completeMockNavigation()
            NavigationUiAction.RetryClicked -> retryMockNavigation()
        }
    }

    private fun toggleVoiceGuidance() {
        mutableUiState.update { state ->
            state.copy(isVoiceGuidanceEnabled = !state.isVoiceGuidanceEnabled)
        }
    }

    private fun setReroutingState() {
        mutableUiState.update { state ->
            state.copy(
                screenState = NavigationScreenState.Rerouting,
                currentInstruction = "경로를 다시 확인하는 중입니다.",
                isRerouting = true,
                errorMessage = null,
            )
        }
        emitUiEvent(NavigationUiEvent.ShowSnackbar("재탐색 mock 상태로 전환했습니다."))
    }

    private fun completeMockNavigation() {
        mutableUiState.update { state ->
            state.copy(
                screenState = NavigationScreenState.Completed,
                currentInstruction = "목적지에 도착했습니다.",
                remainingDistanceText = "0m",
                estimatedTimeText = "완료",
                currentStepIndex = state.totalStepCount,
                isRerouting = false,
                errorMessage = null,
            )
        }
    }

    private fun retryMockNavigation() {
        mutableUiState.update {
            NavigationUiState(
                screenState = NavigationScreenState.Guiding,
                isVoiceGuidanceEnabled = it.isVoiceGuidanceEnabled,
            )
        }
    }

    private fun emitUiEvent(event: NavigationUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NavigationViewModel() as T
            }
    }
}
