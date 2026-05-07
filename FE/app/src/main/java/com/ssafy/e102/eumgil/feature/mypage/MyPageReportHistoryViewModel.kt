package com.ssafy.e102.eumgil.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.data.repository.ReportOutboxData
import com.ssafy.e102.eumgil.data.repository.ReportRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPageReportHistoryViewModel(
    private val reportRepository: ReportRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MyPageReportHistoryUiState())
    val uiState: StateFlow<MyPageReportHistoryUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<MyPageReportHistoryUiEvent>()
    val uiEvent: SharedFlow<MyPageReportHistoryUiEvent> = mutableUiEvent.asSharedFlow()

    private var observeHistoryJob: Job? = null

    init {
        observeReportHistory()
    }

    fun onAction(action: MyPageReportHistoryUiAction) {
        when (action) {
            MyPageReportHistoryUiAction.BackClicked ->
                emitUiEvent(MyPageReportHistoryUiEvent.NavigateBack)
            is MyPageReportHistoryUiAction.ReportClicked ->
                emitUiEvent(MyPageReportHistoryUiEvent.ShowSnackbar(PREPARING_MESSAGE))
            MyPageReportHistoryUiAction.ReportCtaClicked ->
                emitUiEvent(MyPageReportHistoryUiEvent.NavigateToReport)
            MyPageReportHistoryUiAction.RetryClicked ->
                observeReportHistory()
        }
    }

    private fun observeReportHistory() {
        observeHistoryJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                screenState = MyPageReportHistoryScreenState.LOADING,
                errorMessage = null,
            )
        }
        observeHistoryJob =
            viewModelScope.launch {
                reportRepository.observeReportHistory()
                    .catch {
                        mutableUiState.update { state ->
                            state.copy(
                                screenState = MyPageReportHistoryScreenState.ERROR,
                                reports = emptyList(),
                                errorMessage = REPORT_HISTORY_LOAD_FAILURE_MESSAGE,
                            )
                        }
                    }
                    .collectLatest { outboxItems ->
                        val reports =
                            outboxItems
                                .sortedByDescending(ReportOutboxData::updatedAtMillis)
                                .map(ReportOutboxData::toReportHistoryUiModel)
                        mutableUiState.update { state ->
                            state.copy(
                                screenState =
                                    if (reports.isEmpty()) {
                                        MyPageReportHistoryScreenState.EMPTY
                                    } else {
                                        MyPageReportHistoryScreenState.CONTENT
                                    },
                                reports = reports,
                                errorMessage = null,
                            )
                        }
                    }
            }
    }

    private fun emitUiEvent(event: MyPageReportHistoryUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        const val PREPARING_MESSAGE = "준비 중입니다."
        private const val REPORT_HISTORY_LOAD_FAILURE_MESSAGE = "제보 내역을 불러오지 못했습니다."

        fun provideFactory(reportRepository: ReportRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyPageReportHistoryViewModel::class.java)) {
                        return MyPageReportHistoryViewModel(reportRepository = reportRepository) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private fun ReportOutboxData.toReportHistoryUiModel(): MyPageReportHistoryUiModel =
    MyPageReportHistoryUiModel(
        outboxId = outboxId,
        title = reportCategory.toReportHistoryTitle(),
        address = address?.takeIf { it.isNotBlank() } ?: "주소 정보 없음",
        submittedAtText = updatedAtMillis.formatSubmittedAt(),
        photoUri = photoUri?.takeIf { it.isNotBlank() },
        updatedAtMillis = updatedAtMillis,
    )

private fun String.toReportHistoryTitle(): String =
    when (this) {
        // 서버 명세 6종 (제보 API 명세 2026-04-29 기준)
        "STAIRS_STEP" -> "계단·단차 있음"
        "BRAILLE_BLOCK" -> "점자블록 문제"
        "SIDEWALK_MISSING" -> "인도 없음"
        "RAMP" -> "경사로 문제"
        "SIDEWALK_WIDTH" -> "인도폭 문제"
        "OTHER_OBSTACLE" -> "기타 장애물"
        // legacy 값 backward compatibility (저장된 구 데이터 표시용)
        "STAIRS" -> "계단·단차 있음"
        "TACTILE_BLOCK", "GUIDANCE_BLOCK" -> "점자블록 문제"
        "SLOPE" -> "경사로 문제"
        "CONSTRUCTION", "ELEVATOR", "FACILITY_DAMAGE" -> "기타 장애물"
        else -> "제보 내역"
    }

private fun Long.formatSubmittedAt(): String =
    SimpleDateFormat("yyyy.MM.dd (E) HH:mm", Locale.KOREAN).format(Date(this))
