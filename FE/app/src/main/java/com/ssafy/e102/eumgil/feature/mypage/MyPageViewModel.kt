package com.ssafy.e102.eumgil.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPageViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = mutableUiState.asStateFlow()

    init {
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
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyPageViewModel::class.java)) {
                        return MyPageViewModel(settingsRepository = settingsRepository) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
