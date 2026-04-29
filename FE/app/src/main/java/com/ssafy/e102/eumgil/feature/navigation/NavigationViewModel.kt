package com.ssafy.e102.eumgil.feature.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationViewModel(
    private val currentLocationManager: CurrentLocationManager,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<NavigationUiEvent>()
    val uiEvent: SharedFlow<NavigationUiEvent> = mutableUiEvent.asSharedFlow()

    private var initialBriefingRequested = false

    /** 경로 폴리라인 전체 (현재 위치 기반 잔여 거리 계산용). */
    private var routePolyline: List<GeoCoordinate> = emptyList()

    /** 도보 평균 속도: 80m/min (≒ 4.8km/h). */
    private val walkingSpeedMetersPerMinute = 80

    init {
        collectLocationUpdates()
    }

    // ── 위치 업데이트 구독 ─────────────────────────────────────────────────────

    private fun collectLocationUpdates() {
        viewModelScope.launch {
            currentLocationManager.latestLocation
                .filterNotNull()
                .collect { snapshot ->
                    onLocationUpdated(snapshot)
                }
        }
    }

    /**
     * 새 GPS 위치가 도착하면:
     * 1. 폴리라인에서 가장 가까운 지점을 찾아 그 이후의 잔여 거리(m)를 합산.
     * 2. 잔여 거리를 도보 속도로 나눠 예상 시간(분)을 계산.
     * 3. UiState의 metrics를 갱신.
     */
    private fun onLocationUpdated(snapshot: LocationSnapshot) {
        val polyline = routePolyline
        if (polyline.isEmpty()) return

        val current = GeoCoordinate(latitude = snapshot.latitude, longitude = snapshot.longitude)
        val remainingMeters = calculateRemainingDistanceMeters(current, polyline)
        val estimatedMinutes = (remainingMeters / walkingSpeedMetersPerMinute).toInt().coerceAtLeast(0)

        mutableUiState.update { state ->
            val updatedMetrics = state.stepCard.metrics.toMutableList()
            if (updatedMetrics.size >= 2) {
                updatedMetrics[0] = updatedMetrics[0].copy(
                    value = remainingMeters.toInt().toNavigationDistanceLabel(),
                )
                updatedMetrics[1] = updatedMetrics[1].copy(
                    value = estimatedMinutes.toNavigationEtaLabel(),
                )
            }
            state.copy(
                stepCard = state.stepCard.copy(metrics = updatedMetrics),
                mapOverlay = state.mapOverlay.copy(
                    currentLocation = state.mapOverlay.currentLocation?.copy(
                        coordinate = current,
                    ),
                ),
            )
        }
    }

    /**
     * Haversine 공식으로 폴리라인 잔여 거리를 계산합니다.
     *
     * 현재 위치에서 가장 가까운 폴리라인 포인트를 찾고,
     * 그 포인트부터 종점까지의 선분 거리를 합산합니다.
     *
     * 참고: Haversine formula — https://www.movable-type.co.uk/scripts/latlong.html
     */
    private fun calculateRemainingDistanceMeters(
        current: GeoCoordinate,
        polyline: List<GeoCoordinate>,
    ): Double {
        if (polyline.isEmpty()) return 0.0

        // 가장 가까운 폴리라인 포인트 인덱스를 찾음
        val nearestIndex = polyline.indices.minByOrNull { index ->
            haversineDistanceMeters(current, polyline[index])
        } ?: 0

        // 현재 위치 → 가장 가까운 포인트 거리 + 이후 선분들의 거리 합
        var remaining = haversineDistanceMeters(current, polyline[nearestIndex])
        for (i in nearestIndex until polyline.lastIndex) {
            remaining += haversineDistanceMeters(polyline[i], polyline[i + 1])
        }
        return remaining
    }

    // ── 공개 메서드 ────────────────────────────────────────────────────────────

    fun bindNavigationRequest(request: RouteNavigationRequest) {
        routePolyline = request.selectedRoute.previewPolyline.points

        val screenState = request.toScreenState()
        val stepCard = request.toStepCardUiState(screenState)
        val briefingText = stepCard.toNavigationBriefingText()
        initialBriefingRequested = false

        mutableUiState.update { state ->
            state.copy(
                screenState = screenState,
                mapPlaceholderDescription = request.toMapPlaceholderDescription(screenState),
                mapOverlay = request.toMapOverlayUiState(),
                stepCard = stepCard,
                exitCta = screenState.toExitCtaUiState(),
                tts = state.tts.copy(
                    briefingText = briefingText,
                    fallbackMessage = state.tts.toFallbackMessage(),
                ),
            )
        }

        // 경로 진입 시 위치 추적 시작
        currentLocationManager.startLocationUpdates()
    }

    fun onAction(action: NavigationUiAction) {
        when (action) {
            NavigationUiAction.NavigationEntered -> requestInitialBriefingIfNeeded()
            NavigationUiAction.BackClicked -> {
                currentLocationManager.stopLocationUpdates()
                emitUiEvents(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateBack)
            }
            NavigationUiAction.ExitNavigationClicked -> {
                if (uiState.value.isExitEnabled) {
                    currentLocationManager.stopLocationUpdates()
                    emitUiEvents(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToMap)
                }
            }
            NavigationUiAction.SaveBookmarkClicked -> {
                if (uiState.value.isExitEnabled) {
                    currentLocationManager.stopLocationUpdates()
                    emitUiEvents(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToSavedRoute)
                }
            }
            NavigationUiAction.NavigationCompleteClicked -> {
                if (uiState.value.isExitEnabled) {
                    currentLocationManager.stopLocationUpdates()
                    emitUiEvents(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToMap)
                }
            }
            is NavigationUiAction.VoiceGuidanceToggled -> onVoiceGuidanceToggled(action.enabled)
            NavigationUiAction.BriefingReplayClicked -> requestBriefing()
            NavigationUiAction.StopBriefingClicked -> emitUiEvent(NavigationUiEvent.StopBriefing)
        }
    }

    fun updateTextToSpeechState(
        isEnabled: Boolean,
        canSpeak: Boolean,
        status: NavigationTtsStatus,
    ) {
        mutableUiState.update { state ->
            val nextTts = state.tts.copy(
                isEnabled = isEnabled,
                canSpeak = canSpeak,
                status = status,
            )
            state.copy(tts = nextTts.copy(fallbackMessage = nextTts.toFallbackMessage()))
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentLocationManager.stopLocationUpdates()
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────────

    private fun emitUiEvent(event: NavigationUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    private fun emitUiEvents(vararg events: NavigationUiEvent) {
        viewModelScope.launch {
            events.forEach { event -> mutableUiEvent.emit(event) }
        }
    }

    private fun requestInitialBriefingIfNeeded() {
        if (initialBriefingRequested) return
        initialBriefingRequested = true
        requestBriefing()
    }

    private fun onVoiceGuidanceToggled(enabled: Boolean) {
        mutableUiState.update { state ->
            val nextTts = state.tts.copy(isEnabled = enabled)
            state.copy(tts = nextTts.copy(fallbackMessage = nextTts.toFallbackMessage()))
        }

        if (enabled) {
            val tts = uiState.value.tts
            if (tts.canRequestBriefing) {
                emitUiEvents(
                    NavigationUiEvent.SetVoiceGuidanceEnabled(enabled = true),
                    NavigationUiEvent.SpeakBriefing(tts.briefingText),
                )
            } else {
                emitUiEvent(NavigationUiEvent.SetVoiceGuidanceEnabled(enabled = true))
            }
        } else {
            emitUiEvents(
                NavigationUiEvent.SetVoiceGuidanceEnabled(enabled = false),
                NavigationUiEvent.StopBriefing,
            )
        }
    }

    private fun requestBriefing() {
        val tts = uiState.value.tts
        if (!tts.canRequestBriefing) return
        emitUiEvent(NavigationUiEvent.SpeakBriefing(tts.briefingText))
    }

    companion object {
        fun provideFactory(
            currentLocationManager: CurrentLocationManager,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NavigationViewModel(
                        currentLocationManager = currentLocationManager,
                    ) as T
            }
    }
}

// ── Haversine 공식 ─────────────────────────────────────────────────────────────
// 출처: https://www.movable-type.co.uk/scripts/latlong.html

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * 두 GPS 좌표 간의 거리(m)를 Haversine 공식으로 계산합니다.
 */
internal fun haversineDistanceMeters(a: GeoCoordinate, b: GeoCoordinate): Double {
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val sinHalfLat = sin(dLat / 2)
    val sinHalfLon = sin(dLon / 2)
    val h = sinHalfLat.pow(2) +
        cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinHalfLon.pow(2)
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
}

// ── RouteNavigationRequest 확장 함수 ──────────────────────────────────────────

private fun RouteNavigationRequest.toScreenState(): NavigationScreenState =
    if (selectedRoute.segments.isEmpty()) {
        NavigationScreenState.Empty
    } else {
        NavigationScreenState.Ready
    }

private fun RouteNavigationRequest.toMapPlaceholderDescription(screenState: NavigationScreenState): String {
    val destinationName = destination.name.orEmpty().ifBlank { "목적지" }
    return when (screenState) {
        NavigationScreenState.Loading -> "현재 위치와 경로 안내를 준비 중입니다."
        NavigationScreenState.Ready -> "$destinationName 방향 경로 오버레이가 이 영역에 연결될 예정입니다."
        NavigationScreenState.Empty -> "$destinationName 방향 거리 요약을 먼저 표시하고 있습니다."
    }
}

private fun RouteNavigationRequest.toMapOverlayUiState(): NavigationMapOverlayUiState {
    val selectedRoutePolyline = selectedRoute.previewPolyline.points
    val routeSegments = selectedRoute.segments.map { segment ->
        NavigationMapSegmentUiState(
        