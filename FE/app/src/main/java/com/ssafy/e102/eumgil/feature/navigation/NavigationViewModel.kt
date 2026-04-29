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
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import java.util.Locale
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
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<NavigationUiEvent>()
    val uiEvent: SharedFlow<NavigationUiEvent> = mutableUiEvent.asSharedFlow()

    private var initialBriefingRequested = false
    private var navigationRequest: RouteNavigationRequest? = null
    private var remainingDistanceCalculator = RemainingDistanceCalculator(emptyList())
    private var lastProcessedLocationEpochMillis: Long? = null

    init {
        collectLocationUpdates()
    }

    fun bindNavigationRequest(request: RouteNavigationRequest) {
        navigationRequest = request
        remainingDistanceCalculator = RemainingDistanceCalculator(request.selectedRoute.previewPolyline.points)
        lastProcessedLocationEpochMillis = null

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
                tts =
                    state.tts.copy(
                        briefingText = briefingText,
                        fallbackMessage = state.tts.toFallbackMessage(),
                    ),
            )
        }

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
                    saveDestinationBookmarkAndNavigate()
                }
            }
            NavigationUiAction.NavigationCompleteClicked -> {
                if (uiState.value.isExitEnabled) {
                    currentLocationManager.stopLocationUpdates()
                    emitUiEvents(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToLowVisionHome)
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
            val nextTts =
                state.tts.copy(
                    isEnabled = isEnabled,
                    canSpeak = canSpeak,
                    status = status,
                )
            state.copy(tts = nextTts.copy(fallbackMessage = nextTts.toFallbackMessage()))
        }
    }

    override fun onCleared() {
        currentLocationManager.stopLocationUpdates()
        super.onCleared()
    }

    private fun collectLocationUpdates() {
        viewModelScope.launch {
            currentLocationManager.latestLocation
                .filterNotNull()
                .collect { snapshot ->
                    onLocationUpdated(snapshot)
                }
        }
    }

    private fun onLocationUpdated(snapshot: LocationSnapshot) {
        if (!shouldProcessLocation(snapshot)) return
        if (remainingDistanceCalculator.isEmpty) return

        val current = GeoCoordinate(latitude = snapshot.latitude, longitude = snapshot.longitude)
        val remainingMeters = remainingDistanceCalculator.calculateRemainingDistanceMeters(current)
        val estimatedMinutes =
            (remainingMeters / DEFAULT_WALKING_SPEED_METERS_PER_MINUTE)
                .toInt()
                .coerceAtLeast(0)

        mutableUiState.update { state ->
            val updatedMetrics = state.stepCard.metrics.toMutableList()
            if (updatedMetrics.size >= 2) {
                updatedMetrics[0] =
                    updatedMetrics[0].copy(
                        value = remainingMeters.toInt().toNavigationDistanceLabel(),
                    )
                updatedMetrics[1] =
                    updatedMetrics[1].copy(
                        value = estimatedMinutes.toNavigationEtaLabel(),
                    )
            }
            state.copy(
                stepCard = state.stepCard.copy(metrics = updatedMetrics),
                mapOverlay =
                    state.mapOverlay.copy(
                        currentLocation =
                            state.mapOverlay.currentLocation?.copy(
                                coordinate = current,
                            ),
                    ),
            )
        }
    }

    private fun shouldProcessLocation(snapshot: LocationSnapshot): Boolean {
        val lastProcessed = lastProcessedLocationEpochMillis
        if (lastProcessed != null && snapshot.recordedAtEpochMillis >= lastProcessed) {
            val elapsedMillis = snapshot.recordedAtEpochMillis - lastProcessed
            if (elapsedMillis < MIN_LOCATION_UPDATE_INTERVAL_MILLIS) {
                return false
            }
        }
        lastProcessedLocationEpochMillis = snapshot.recordedAtEpochMillis
        return true
    }

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

    private fun saveDestinationBookmarkAndNavigate() {
        viewModelScope.launch {
            navigationRequest?.toDestinationBookmarkData()?.let { bookmark ->
                bookmarkRepository.saveBookmark(bookmark)
            }
            currentLocationManager.stopLocationUpdates()
            mutableUiEvent.emit(NavigationUiEvent.StopBriefing)
            mutableUiEvent.emit(NavigationUiEvent.NavigateToSavedRoute)
        }
    }

    companion object {
        fun provideFactory(
            currentLocationManager: CurrentLocationManager,
            bookmarkRepository: BookmarkRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NavigationViewModel(
                        currentLocationManager = currentLocationManager,
                        bookmarkRepository = bookmarkRepository,
                    ) as T
            }
    }
}

private fun RouteNavigationRequest.toDestinationBookmarkData(): BookmarkData {
    val destinationWaypoint = destination
    val placeId = destinationWaypoint.placeId ?: destinationWaypoint.toNavigationDestinationPlaceId()
    val placeName = destinationWaypoint.name.orEmpty().ifBlank { "목적지" }

    return BookmarkData(
        placeId = placeId,
        placeName = placeName,
        address = destinationWaypoint.address?.takeIf { address -> address.isNotBlank() },
        latitude = destinationWaypoint.coordinate.latitude,
        longitude = destinationWaypoint.coordinate.longitude,
        category = destinationWaypoint.category?.name,
    )
}

private fun RouteWaypoint.toNavigationDestinationPlaceId(): String =
    "navigation-destination:${coordinate.latitude},${coordinate.longitude}"

internal fun calculateRemainingDistanceMeters(
    current: GeoCoordinate,
    polyline: List<GeoCoordinate>,
): Double {
    return RemainingDistanceCalculator(polyline).calculateRemainingDistanceMeters(current)
}

internal class RemainingDistanceCalculator(
    private val polyline: List<GeoCoordinate>,
) {
    private val cumulativeDistanceMeters: List<Double> =
        buildList {
            var total = 0.0
            add(total)
            for (index in 0 until polyline.lastIndex) {
                total += haversineDistanceMeters(polyline[index], polyline[index + 1])
                add(total)
            }
        }

    val isEmpty: Boolean
        get() = polyline.isEmpty()

    fun calculateRemainingDistanceMeters(current: GeoCoordinate): Double {
        if (polyline.isEmpty()) return 0.0

        val nearestIndex =
            polyline.indices.minByOrNull { index ->
                haversineDistanceMeters(current, polyline[index])
            } ?: 0

        val distanceToNearestPoint = haversineDistanceMeters(current, polyline[nearestIndex])
        val routeTailDistance = cumulativeDistanceMeters.last() - cumulativeDistanceMeters[nearestIndex]
        return distanceToNearestPoint + routeTailDistance
    }
}

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val DEFAULT_WALKING_SPEED_METERS_PER_MINUTE = 80.0
private const val MIN_LOCATION_UPDATE_INTERVAL_MILLIS = 1_000L

internal fun haversineDistanceMeters(a: GeoCoordinate, b: GeoCoordinate): Double {
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val sinHalfLat = sin(dLat / 2)
    val sinHalfLon = sin(dLon / 2)
    val h =
        sinHalfLat.pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sinHalfLon.pow(2)
    return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
}

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
        NavigationScreenState.Ready -> "$destinationName 방향 경로 안내를 시작합니다."
        NavigationScreenState.Empty -> "$destinationName 방향 거리 요약을 먼저 표시합니다."
    }
}

private fun RouteNavigationRequest.toMapOverlayUiState(): NavigationMapOverlayUiState {
    val selectedRoutePolyline = selectedRoute.previewPolyline.points
    val routeSegments =
        selectedRoute.segments.map { segment ->
            NavigationMapSegmentUiState(
                sequence = segment.sequence,
                polyline = segment.polyline.points,
                distanceMeters = segment.distanceMeters,
                riskLevel = segment.riskLevel,
                guidanceMessage = segment.guidanceMessage,
            )
        }
    val originPoint = origin.toNavigationMapPointUiState(fallbackLabel = "출발지")
    val destinationPoint = destination.toNavigationMapPointUiState(fallbackLabel = "목적지")

    return NavigationMapOverlayUiState(
        isDisplayable = selectedRoute.previewPolyline.isRenderable,
        currentLocation = originPoint,
        origin = originPoint,
        destination = destinationPoint,
        selectedRoutePolyline = selectedRoutePolyline,
        routeSegments = routeSegments,
    )
}

private fun RouteWaypoint.toNavigationMapPointUiState(fallbackLabel: String): NavigationMapPointUiState =
    NavigationMapPointUiState(
        label = name.orEmpty().ifBlank { fallbackLabel },
        coordinate = coordinate,
    )

private fun RouteNavigationRequest.toStepCardUiState(screenState: NavigationScreenState): NavigationStepCardUiState =
    when (screenState) {
        NavigationScreenState.Loading -> NavigationStepCardUiState()
        NavigationScreenState.Ready -> toReadyStepCardUiState()
        NavigationScreenState.Empty -> toEmptyStepCardUiState()
    }

private fun NavigationStepCardUiState.toNavigationBriefingText(): String =
    listOf(
        supportingText,
        "$distanceLabel 후 $instruction",
    ).joinToString(separator = " ")

private fun NavigationTtsUiState.toFallbackMessage(): String =
    when {
        !isEnabled -> NAVIGATION_TTS_DISABLED_MESSAGE
        status == NavigationTtsStatus.Unavailable -> NAVIGATION_TTS_UNAVAILABLE_MESSAGE
        status == NavigationTtsStatus.Initializing -> NAVIGATION_TTS_PREPARING_MESSAGE
        else -> ""
    }

private fun RouteNavigationRequest.toReadyStepCardUiState(): NavigationStepCardUiState {
    val primarySegment =
        selectedRoute.segments.firstOrNull { segment ->
            segment.guidanceMessage.isNotBlank()
        } ?: selectedRoute.segments.firstOrNull()

    return NavigationStepCardUiState(
        sectionLabel = "다음 안내",
        statusLabel = selectedRoute.routeOption.toRouteOptionLabel(),
        emphasisLabel = selectedRoute.summary.riskLevel.toRiskLabel(),
        distanceLabel =
            primarySegment?.distanceMeters?.toNavigationDistanceLabel()
                ?: selectedRoute.summary.distanceMeters.toNavigationDistanceLabel(),
        instruction =
            primarySegment?.guidanceMessage
                ?.trim()
                ?.takeIf { guidanceMessage -> guidanceMessage.isNotEmpty() }
                ?: "목적지 방향으로 계속 이동하세요",
        supportingText =
            "${destination.name.orEmpty().ifBlank { "목적지" }} 방향으로 " +
                "${selectedRoute.title.toNavigationRouteTitle(selectedRoute.routeOption)} 경로를 따라 이동합니다.",
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

private fun RouteNavigationRequest.toEmptyStepCardUiState(): NavigationStepCardUiState =
    NavigationStepCardUiState(
        sectionLabel = "다음 안내",
        statusLabel = selectedRoute.routeOption.toRouteOptionLabel(),
        emphasisLabel = selectedRoute.summary.riskLevel.toRiskLabel(),
        distanceLabel = selectedRoute.summary.distanceMeters.toNavigationDistanceLabel(),
        instruction = "현재 안내 메시지를 준비하지 못했습니다",
        supportingText =
            "${destination.name.orEmpty().ifBlank { "목적지" }} 방향으로 거리와 예상 시간 요약만 먼저 표시합니다.",
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
                    value = "안내 없음",
                ),
            ),
    )

private fun NavigationScreenState.toExitCtaUiState(): NavigationCtaUiState =
    when (this) {
        NavigationScreenState.Loading ->
            NavigationCtaUiState(
                label = "안내 준비 중",
                supportingText = "경로 정보가 준비되면 종료 버튼이 활성화됩니다.",
                isEnabled = false,
            )
        NavigationScreenState.Ready,
        NavigationScreenState.Empty ->
            NavigationCtaUiState(
                label = "안내 종료",
                supportingText = "안내를 종료하고 지도로 돌아갑니다.",
                isEnabled = true,
            )
    }

private fun String.toNavigationRouteTitle(routeOption: RouteOption): String =
    trim().ifBlank { routeOption.toRouteOptionLabel() }

private fun RouteOption.toRouteOptionLabel(): String =
    when (this) {
        RouteOption.SAFE -> "안전 우선"
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
        else -> String.format(Locale.US, "%.1fkm", this / 1_000f)
    }

private fun Int.toNavigationEtaLabel(): String =
    if (this > 0) {
        "${this}분"
    } else {
        "확인 중"
    }
