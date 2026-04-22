package com.ssafy.e102.eumgil.feature.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
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

    fun bindNavigationRequest(request: RouteNavigationRequest) {
        mutableUiState.update { state ->
            state.copy(
                mapPlaceholderDescription = request.toMapPlaceholderDescription(),
                stepCard = request.toStepCardUiState(),
            )
        }
    }

    fun onAction(action: NavigationUiAction) {
        when (action) {
            NavigationUiAction.BackClicked -> emitUiEvent(NavigationUiEvent.NavigateBack)
            NavigationUiAction.ExitNavigationClicked -> {
                if (uiState.value.isExitEnabled) {
                    emitUiEvent(NavigationUiEvent.NavigateToMap)
                }
            }
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

private fun RouteNavigationRequest.toMapPlaceholderDescription(): String {
    val destinationName = destination.name.orEmpty().ifBlank { "목적지" }
    return "$destinationName 방향 경로 오버레이가 이 영역에 연결될 예정입니다."
}

private fun RouteNavigationRequest.toStepCardUiState(): NavigationStepCardUiState {
    val primarySegment =
        selectedRoute.segments.firstOrNull { segment ->
            segment.guidanceMessage.isNotBlank()
        } ?: selectedRoute.segments.firstOrNull()

    return NavigationStepCardUiState(
        sectionLabel = "다음 안내",
        statusLabel = selectedRoute.routeOption.toRouteOptionLabel(),
        emphasisLabel = selectedRoute.summary.riskLevel.toRiskLabel(),
        distanceLabel = primarySegment?.distanceMeters?.toNavigationDistanceLabel() ?: selectedRoute.summary.distanceMeters.toNavigationDistanceLabel(),
        instruction =
            primarySegment?.guidanceMessage
                ?.trim()
                ?.takeIf { guidanceMessage -> guidanceMessage.isNotEmpty() }
                ?: "목적지 방향으로 계속 이동하세요",
        supportingText =
            "${destination.name.orEmpty().ifBlank { "목적지" }} 방향으로 ${selectedRoute.title} 경로를 따라 이동합니다.",
        metrics =
            listOf(
                NavigationStepMetricUiState(
                    label = "남은 거리",
                    value = selectedRoute.summary.distanceMeters.toNavigationDistanceLabel(),
                ),
                NavigationStepMetricUiState(
                    label = "예상 시간",
                    value = selectedRoute.summary.estimatedTimeMinutes.toNavigationEtaLabel(),
                ),
                NavigationStepMetricUiState(
                    label = "진행 단계",
                    value = "1 / ${selectedRoute.segments.size.coerceAtLeast(1)}",
                ),
            ),
    )
}

private fun RouteOption.toRouteOptionLabel(): String =
    when (this) {
        RouteOption.SAFE -> "SAFE 우선"
        RouteOption.SHORTEST -> "최단 거리"
    }

private fun RouteRiskLevel.toRiskLabel(): String =
    when (this) {
        RouteRiskLevel.LOW -> "위험도 낮음"
        RouteRiskLevel.MEDIUM -> "위험도 보통"
        RouteRiskLevel.HIGH -> "위험도 높음"
    }

private fun Int.toNavigationDistanceLabel(): String =
    when {
        this <= 0 -> "확인 중"
        this < 1_000 -> "${this}m"
        else -> String.format(java.util.Locale.US, "%.1fkm", this / 1_000f)
    }

private fun Int.toNavigationEtaLabel(): String =
    if (this > 0) {
        "${this}분"
    } else {
        "확인 중"
    }
