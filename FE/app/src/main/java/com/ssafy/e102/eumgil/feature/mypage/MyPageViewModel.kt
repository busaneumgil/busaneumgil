package com.ssafy.e102.eumgil.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.data.repository.AuthLogoutRepository
import com.ssafy.e102.eumgil.data.repository.AuthLogoutResult
import com.ssafy.e102.eumgil.data.repository.AuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.data.repository.UserProfileRepository
import com.ssafy.e102.eumgil.data.repository.UserProfileSyncResult
import com.ssafy.e102.eumgil.feature.onboarding.MobilitySubtype
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import kotlinx.coroutines.Job
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
    private val authLogoutRepository: AuthLogoutRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = mutableUiState.asStateFlow()

    private val uiEventChannel = Channel<MyPageUiEvent>(capacity = Channel.BUFFERED)
    val uiEvent = uiEventChannel.receiveAsFlow()
    private var logoutJob: Job? = null

    init {
        observeInitSettings()
        observeRepositoryDebugSettings()
        refreshMyProfile()
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
            MyPageUiAction.LogoutClicked -> logout()
            is MyPageUiAction.MainMenuClicked -> {
                viewModelScope.launch {
                    when (action.menuItem) {
                        MyPageMenuItem.REPORT_HISTORY -> uiEventChannel.send(MyPageUiEvent.NavigateToReportHistory)
                        MyPageMenuItem.APP_HELP -> uiEventChannel.send(MyPageUiEvent.NavigateToAppInfo)
                        MyPageMenuItem.NOTICE -> uiEventChannel.send(MyPageUiEvent.ShowPreparingMessage)
                    }
                }
            }
        }
    }

    private fun logout() {
        if (mutableUiState.value.isLogoutLoading) return

        mutableUiState.update { state -> state.copy(isLogoutLoading = true) }
        logoutJob?.cancel()
        logoutJob =
            viewModelScope.launch {
                when (val result = authLogoutRepository.logout()) {
                    is AuthLogoutResult.Success -> finishLogout()
                    AuthLogoutResult.MissingSession,
                    AuthLogoutResult.AuthenticationFailed
                    -> finishLogout()
                    is AuthLogoutResult.Failure -> {
                        mutableUiState.update { state -> state.copy(isLogoutLoading = false) }
                        uiEventChannel.send(MyPageUiEvent.ShowSnackbar(message = result.message))
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

    private fun refreshMyProfile() {
        viewModelScope.launch {
            when (userProfileRepository.syncMyProfile()) {
                is UserProfileSyncResult.Success -> Unit
                UserProfileSyncResult.MissingSession,
                UserProfileSyncResult.AuthenticationFailed
                -> {
                    authSessionRepository.clearAuthSession()
                    uiEventChannel.send(MyPageUiEvent.NavigateToLogin)
                }
                is UserProfileSyncResult.Failure -> {
                    uiEventChannel.send(MyPageUiEvent.ShowProfileSyncFailedMessage)
                }
            }
        }
    }

    private suspend fun finishLogout() {
        mutableUiState.update { state -> state.copy(isLogoutLoading = false) }
        uiEventChannel.send(MyPageUiEvent.NavigateToLogin)
    }

    override fun onCleared() {
        logoutJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            authSessionRepository: AuthSessionRepository,
            authLogoutRepository: AuthLogoutRepository,
            userProfileRepository: UserProfileRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyPageViewModel::class.java)) {
                        return MyPageViewModel(
                            settingsRepository = settingsRepository,
                            authSessionRepository = authSessionRepository,
                            authLogoutRepository = authLogoutRepository,
                            userProfileRepository = userProfileRepository,
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
