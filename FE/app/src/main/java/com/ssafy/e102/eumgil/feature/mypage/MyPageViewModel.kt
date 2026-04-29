package com.ssafy.e102.eumgil.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.repository.AuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.feature.onboarding.MobilitySubtype
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPageViewModel(
    private val settingsRepository: SettingsRepository,
    private val authSessionRepository: AuthSessionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = mutableUiState.asStateFlow()

    private val uiEventChannel = Channel<MyPageUiEvent>(capacity = Channel.BUFFERED)
    val uiEvent = uiEventChannel.receiveAsFlow()

    init {
        observeInitSettings()
        observeRepositoryDebugSettings()
    }

    fun onAction(action: MyPageUiAction) {
        when (action) {
            is MyPageUiAction.ForceMockToggled -> {
                if (!mutableUiState.value.isRuntimeToggleEnabled) return

                viewModelScope.launch {
                    settingsRepository.setForceMockEnabled(action.isEnabled)
                }
            }
            MyPageUiAction.UserTypeChangeClicked -> {
                viewModelScope.launch {
                    uiEventChannel.send(MyPageUiEvent.NavigateToUserTypePrimary)
                }
            }
            MyPageUiAction.LogoutClicked -> {
                viewModelScope.launch {
                    authSessionRepository.clearAuthSession()
                    uiEventChannel.send(MyPageUiEvent.NavigateToLogin)
                }
            }
            is MyPageUiAction.MainMenuClicked -> {
                viewModelScope.launch {
                    when (action.menuItem) {
                        MyPageMenuItem.REPORT_HISTORY -> uiEventChannel.send(MyPageUiEvent.NavigateToReportHistory)
                        MyPageMenuItem.NOTICE,
                        MyPageMenuItem.APP_HELP -> uiEventChannel.send(MyPageUiEvent.ShowPreparingMessage)
                    }
                }
            }
        }
    }

    private fun observeInitSettings() {
        viewModelScope.launch {
            settingsRepository.observeInitSettings().collectLatest { initSettings ->
                mutableUiState.update { currentState ->
                    currentState.copy(
                        userMode = initSettings.toMyPageUserMode(),
                        mobilitySubtype = initSettings.toMyPageMobilitySubtype(),
                    )
                }
            }
        }
    }

    private fun observeRepositoryDebugSettings() {
        viewModelScope.launch {
            settingsRepository.observeRepositoryDebugSettings().collectLatest { debugSettings ->
                mutableUiState.update { currentState ->
                    currentState.copy(
                        isDebugSectionVisible = debugSettings.isRuntimeToggleAvailable,
                        isRuntimeToggleEnabled = debugSettings.isRuntimeToggleEnabled,
                        isForceMockEnabled = debugSettings.isForceMockEnabled,
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            authSessionRepository: AuthSessionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyPageViewModel::class.java)) {
                        return MyPageViewModel(
                            settingsRepository = settingsRepository,
                            authSessionRepository = authSessionRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private fun InitSettings.toMyPageUserMode(): MyPageUserMode =
    when (PrimaryUserType.fromRouteValue(selectedPrimaryUserType)) {
        PrimaryUserType.LOW_VISION -> MyPageUserMode.LOW_VISION
        PrimaryUserType.MOBILITY_IMPAIRED -> MyPageUserMode.MOBILITY_IMPAIRED
        null -> MyPageUserMode.UNKNOWN
    }

private fun InitSettings.toMyPageMobilitySubtype(): MyPageMobilitySubtype? =
    when (MobilitySubtype.fromRouteValue(selectedMobilitySubtype)) {
        MobilitySubtype.ELECTRIC_WHEELCHAIR -> MyPageMobilitySubtype.ELECTRIC_WHEELCHAIR
        MobilitySubtype.MANUAL_WHEELCHAIR -> MyPageMobilitySubtype.MANUAL_WHEELCHAIR
        MobilitySubtype.OTHER -> MyPageMobilitySubtype.OTHER
        null -> null
    }
