package com.ssafy.e102.eumgil.feature.mypage

data class MyPageReportHistoryUiState(
    val screenState: MyPageReportHistoryScreenState = MyPageReportHistoryScreenState.LOADING,
    val reports: List<MyPageReportHistoryUiModel> = emptyList(),
    val errorMessage: String? = null,
)

data class MyPageReportHistoryUiModel(
    val outboxId: String,
    val title: String,
    val address: String,
    val submittedAtText: String,
    val photoUri: String?,
    val updatedAtMillis: Long,
)

enum class MyPageReportHistoryScreenState {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR,
}

sealed interface MyPageReportHistoryUiAction {
    data object BackClicked : MyPageReportHistoryUiAction

    data object ReportCtaClicked : MyPageReportHistoryUiAction

    data object RetryClicked : MyPageReportHistoryUiAction

    data class ReportClicked(
        val outboxId: String,
    ) : MyPageReportHistoryUiAction
}

sealed interface MyPageReportHistoryUiEvent {
    data object NavigateBack : MyPageReportHistoryUiEvent

    data object NavigateToReport : MyPageReportHistoryUiEvent

    data class ShowSnackbar(
        val message: String,
    ) : MyPageReportHistoryUiEvent
}
