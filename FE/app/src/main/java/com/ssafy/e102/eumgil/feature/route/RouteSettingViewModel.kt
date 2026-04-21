package com.ssafy.e102.eumgil.feature.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RouteSettingViewModel(
    private val destinationSelectionRepository: DestinationSelectionRepository,
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            RouteSettingUiState(
                destination = destinationSelectionRepository.selectedDestination.value,
            ),
        )
    val uiState: StateFlow<RouteSettingUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<RouteSettingUiEvent>()
    val uiEvent: SharedFlow<RouteSettingUiEvent> = mutableUiEvent.asSharedFlow()

    init {
        observeSelectedDestination()
    }

    fun onAction(action: RouteSettingUiAction) {
        when (action) {
            RouteSettingUiAction.BackClicked -> emitUiEvent(RouteSettingUiEvent.NavigateBack)
            RouteSettingUiAction.StartPointClicked ->
                emitUiEvent(RouteSettingUiEvent.ShowSnackbar("출발지 수정은 실제 위치 연동 단계에서 연결합니다."))
            RouteSettingUiAction.DestinationClicked -> emitUiEvent(RouteSettingUiEvent.NavigateToSearch)
            is RouteSettingUiAction.RouteOptionToggled -> toggleRouteOption(action.option)
            RouteSettingUiAction.FindRouteClicked -> findMockRoute()
            RouteSettingUiAction.StartNavigationClicked -> startNavigation()
        }
    }

    private fun observeSelectedDestination() {
        viewModelScope.launch {
            destinationSelectionRepository.selectedDestination.collectLatest { destination ->
                mutableUiState.update { state ->
                    state.copy(
                        destination = destination,
                        screenState =
                            when {
                                destination == null -> RouteSettingScreenState.Editing
                                state.screenState == RouteSettingScreenState.Failure ->
                                    RouteSettingScreenState.Editing
                                else -> state.screenState
                            },
                        errorMessage = null,
                    )
                }
            }
        }
    }

    private fun toggleRouteOption(option: RouteSettingOption) {
        mutableUiState.update { state ->
            val nextOptions =
                if (option in state.selectedOptions) {
                    state.selectedOptions - option
                } else {
                    state.selectedOptions + option
                }

            state.copy(
                selectedOptions = nextOptions,
                screenState = RouteSettingScreenState.Editing,
                errorMessage = null,
            )
        }
    }

    private fun findMockRoute() {
        val destination = uiState.value.destination
        if (destination == null) {
            mutableUiState.update {
                it.copy(
                    screenState = RouteSettingScreenState.Failure,
                    errorMessage = "목적지를 선택해야 경로를 확인할 수 있습니다.",
                )
            }
            return
        }

        mutableUiState.update {
            it.copy(
                screenState = RouteSettingScreenState.PreviewReady,
                errorMessage = null,
            )
        }
        emitUiEvent(RouteSettingUiEvent.ShowSnackbar("${destination.name}까지 mock 경로 시안을 준비했습니다."))
    }

    private fun startNavigation() {
        if (!uiState.value.isNavigationStartEnabled) {
            emitUiEvent(RouteSettingUiEvent.ShowSnackbar("경로 시안을 먼저 확인해주세요."))
            return
        }

        emitUiEvent(RouteSettingUiEvent.NavigateToNavigation)
    }

    private fun emitUiEvent(event: RouteSettingUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        fun provideFactory(
            destinationSelectionRepository: DestinationSelectionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RouteSettingViewModel(
                        destinationSelectionRepository = destinationSelectionRepository,
                    ) as T
            }
    }
}
