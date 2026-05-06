package com.ssafy.e102.eumgil.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.data.repository.AccountWithdrawalRepository
import com.ssafy.e102.eumgil.data.repository.AccountWithdrawalResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPageAppInfoViewModel(
    private val accountWithdrawalRepository: AccountWithdrawalRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyPageAppInfoUiState())
    val uiState: StateFlow<MyPageAppInfoUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<MyPageAppInfoUiEvent>()
    val uiEvent: SharedFlow<MyPageAppInfoUiEvent> = mutableUiEvent.asSharedFlow()

    private var withdrawJob: Job? = null

    fun onAction(action: MyPageAppInfoUiAction) {
        when (action) {
            MyPageAppInfoUiAction.WithdrawClicked -> withdrawAccount()
        }
    }

    private fun withdrawAccount() {
        if (mutableUiState.value.isWithdrawLoading) return

        withdrawJob?.cancel()
        withdrawJob =
            viewModelScope.launch {
                mutableUiState.update { state ->
                    state.copy(accountActionState = MyPageAppInfoAccountActionState.Loading)
                }

                when (val result = accountWithdrawalRepository.withdraw()) {
                    is AccountWithdrawalResult.Success -> {
                        mutableUiState.update { state ->
                            state.copy(accountActionState = MyPageAppInfoAccountActionState.Success)
                        }
                        mutableUiEvent.emit(MyPageAppInfoUiEvent.NavigateToLogin)
                    }
                    AccountWithdrawalResult.MissingSession,
                    AccountWithdrawalResult.AuthenticationFailed
                    -> {
                        mutableUiState.update { state ->
                            state.copy(accountActionState = MyPageAppInfoAccountActionState.Idle)
                        }
                        mutableUiEvent.emit(MyPageAppInfoUiEvent.NavigateToLogin)
                    }
                    is AccountWithdrawalResult.Failure -> {
                        mutableUiState.update { state ->
                            state.copy(
                                accountActionState = MyPageAppInfoAccountActionState.Failure(result.message),
                            )
                        }
                        mutableUiEvent.emit(MyPageAppInfoUiEvent.ShowSnackbar(message = result.message))
                        mutableUiState.update { state ->
                            state.copy(accountActionState = MyPageAppInfoAccountActionState.Idle)
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        withdrawJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun provideFactory(accountWithdrawalRepository: AccountWithdrawalRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyPageAppInfoViewModel::class.java)) {
                        return MyPageAppInfoViewModel(
                            accountWithdrawalRepository = accountWithdrawalRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
