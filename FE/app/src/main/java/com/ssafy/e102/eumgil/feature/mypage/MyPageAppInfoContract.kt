package com.ssafy.e102.eumgil.feature.mypage

data class MyPageAppInfoUiState(
    val accountActionState: MyPageAppInfoAccountActionState = MyPageAppInfoAccountActionState.Idle,
) {
    val isWithdrawLoading: Boolean
        get() = accountActionState == MyPageAppInfoAccountActionState.Loading
}

sealed interface MyPageAppInfoUiAction {
    data object WithdrawClicked : MyPageAppInfoUiAction
}

sealed interface MyPageAppInfoUiEvent {
    data class ShowSnackbar(
        val message: String,
    ) : MyPageAppInfoUiEvent

    data object NavigateToLogin : MyPageAppInfoUiEvent
}

sealed interface MyPageAppInfoAccountActionState {
    data object Idle : MyPageAppInfoAccountActionState

    data object Loading : MyPageAppInfoAccountActionState

    data object Success : MyPageAppInfoAccountActionState

    data class Failure(
        val message: String,
    ) : MyPageAppInfoAccountActionState
}
