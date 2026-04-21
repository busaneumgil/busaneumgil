package com.ssafy.e102.eumgil.feature.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePreviewModel
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSegmentSafetyFlags
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.toRouteWaypoint
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RouteSettingViewModel(
    private val routeRepository: RouteRepository,
    private val destinationSelectionRepository: DestinationSelectionRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RouteSettingUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<RouteSettingUiEvent>()
    val uiEvent: SharedFlow<RouteSettingUiEvent> = mutableUiEvent.asSharedFlow()

    private var latestSearchData: RouteSearchData? = null

    init {
        observeSelectedDestination()
    }

    fun onAction(action: RouteSettingUiAction) {
        when (action) {
            RouteSettingUiAction.BackClicked -> emitUiEvent(RouteSettingUiEvent.NavigateBack)
            is RouteSettingUiAction.RouteOptionSelected -> selectRouteOption(action.routeOption)
            RouteSettingUiAction.StartNavigationClicked -> startNavigation()
        }
    }

    private fun observeSelectedDestination() {
        viewModelScope.launch {
            destinationSelectionRepository.selectedDestination.collectLatest { selectedDestination ->
                loadRouteShell(selectedDestination = selectedDestination)
            }
        }
    }

    private suspend fun loadRouteShell(selectedDestination: PlaceDestination?) {
        latestSearchData = null
        val selectedOption = mutableUiState.value.selectedOption

        mutableUiState.update { state ->
            state.copy(
                isLoading = true,
                loadErrorMessage = null,
                origin = originLocationUiState(),
                destination = destinationLocationUiState(selectedDestination?.toRouteWaypoint() ?: DEFAULT_DESTINATION),
                isUsingFallbackDestination = selectedDestination == null,
                optionCards = emptyList(),
                selectedRoute = null,
                sourceLabel = null,
                ctaAcknowledged = false,
            )
        }

        runCatching {
            routeRepository.getRouteSearchData(buildQuery(selectedDestination = selectedDestination))
        }.onSuccess { searchData ->
            latestSearchData = searchData
            mutableUiState.value =
                buildUiState(
                    searchData = searchData,
                    selectedDestination = selectedDestination,
                    requestedOption = selectedOption,
                    ctaAcknowledged = false,
                )
        }.onFailure { throwable ->
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    loadErrorMessage = throwable.message ?: DEFAULT_ROUTE_LOAD_ERROR_MESSAGE,
                    ctaAcknowledged = false,
                )
            }
        }
    }

    private fun selectRouteOption(routeOption: RouteOption) {
        val searchData = latestSearchData
        if (searchData == null) {
            mutableUiState.update { state -> state.copy(selectedOption = routeOption) }
            return
        }

        mutableUiState.value =
            buildUiState(
                searchData = searchData,
                selectedDestination = destinationSelectionRepository.selectedDestination.value,
                requestedOption = routeOption,
                ctaAcknowledged = mutableUiState.value.ctaAcknowledged,
            )
    }

    private fun startNavigation() {
        val searchData = latestSearchData ?: return
        val selectedRoute =
            searchData.findRoute(uiState.value.selectedOption)
                ?: searchData.primaryRoute
                ?: return

        mutableUiState.update { state ->
            state.copy(ctaAcknowledged = true)
        }

        emitUiEvent(
            RouteSettingUiEvent.StartNavigationRequested(
                request =
                    RouteNavigationRequest(
                        origin = searchData.result.origin,
                        destination = searchData.result.destination,
                        selectedRoute = selectedRoute,
                        source = searchData.source,
                    ),
            ),
        )
    }

    private fun buildUiState(
        searchData: RouteSearchData,
        selectedDestination: PlaceDestination?,
        requestedOption: RouteOption,
        ctaAcknowledged: Boolean,
    ): RouteSettingUiState {
        val availableRoutes = searchData.routes.sortedBy { route -> route.routeOption.routeSortOrder() }
        val resolvedOption =
            if (searchData.findRoute(requestedOption) != null) {
                requestedOption
            } else {
                availableRoutes.firstOrNull()?.routeOption ?: requestedOption
            }
        val selectedRoute =
            searchData.findRoute(resolvedOption)
                ?: availableRoutes.firstOrNull()

        return RouteSettingUiState(
            isLoading = false,
            loadErrorMessage = null,
            origin = originLocationUiState(searchData.result.origin),
            destination = destinationLocationUiState(searchData.result.destination),
            isUsingFallbackDestination = selectedDestination == null,
            selectedOption = resolvedOption,
            optionCards =
                availableRoutes.map { route ->
                    route.toOptionCardUiState(
                        isSelected = route.routeOption == resolvedOption,
                    )
                },
            selectedRoute = selectedRoute?.toSelectedRouteUiState(),
            sourceLabel = searchData.source.label,
            ctaAcknowledged = ctaAcknowledged,
        )
    }

    private fun originLocationUiState(origin: RouteWaypoint = DEFAULT_ORIGIN): RouteLocationUiState =
        origin.toLocationUiState(addressFallback = DEFAULT_ORIGIN_SUPPORTING_TEXT)

    private fun destinationLocationUiState(destination: RouteWaypoint): RouteLocationUiState =
        destination.toLocationUiState(addressFallback = DEFAULT_DESTINATION_ADDRESS_FALLBACK)

    private fun buildQuery(selectedDestination: PlaceDestination?): RouteSearchQuery =
        RouteSearchQuery(
            origin = DEFAULT_ORIGIN,
            destination = selectedDestination?.toRouteWaypoint() ?: DEFAULT_DESTINATION,
        )

    private fun RouteCandidate.toOptionCardUiState(isSelected: Boolean): RouteOptionCardUiState =
        routeOption.optionCardPresentation(summary = summary).let { presentation ->
            RouteOptionCardUiState(
                routeOption = routeOption,
                title = presentation.title,
                description = presentation.description,
                distanceMeters = summary.distanceMeters,
                estimatedTimeMinutes = summary.estimatedTimeMinutes,
                riskLevel = summary.riskLevel,
                summaryLabel = summary.toSummaryLabel(),
                selectionLabel = if (isSelected) OPTION_SELECTION_SELECTED else OPTION_SELECTION_AVAILABLE,
                highlightLabel = presentation.highlightLabel,
                metrics = presentation.metrics,
                badges = routeBadges(includeSafePriority = false),
                isSelected = isSelected,
            )
        }

    private fun RouteCandidate.toSelectedRouteUiState(): RouteSelectedRouteUiState =
        RouteSelectedRouteUiState(
            routeOption = routeOption,
            optionTitle = routeOption.toOptionTitle(),
            title = title,
            distanceMeters = summary.distanceMeters,
            estimatedTimeMinutes = summary.estimatedTimeMinutes,
            riskLevel = summary.riskLevel,
            guidanceMessage = segments.primaryGuidanceMessage(),
            summaryLabel = summary.toSummaryLabel(),
            estimatedTimeLabel = summary.estimatedTimeMinutes.toEstimatedTimeLabel(),
            distanceLabel = summary.distanceMeters.toDistanceLabel(),
            riskLabel = summary.riskLevel.toRiskLabel(),
            renderableSegmentLabel = preview.toRenderableSegmentLabel(),
            summaryMetrics =
                listOf(
                    RouteSummaryMetricUiState(
                        label = SUMMARY_METRIC_TIME_LABEL,
                        value = summary.estimatedTimeMinutes.toEstimatedTimeLabel(),
                    ),
                    RouteSummaryMetricUiState(
                        label = SUMMARY_METRIC_DISTANCE_LABEL,
                        value = summary.distanceMeters.toDistanceLabel(),
                    ),
                    RouteSummaryMetricUiState(
                        label = SUMMARY_METRIC_RISK_LABEL,
                        value = summary.riskLevel.toRiskLabel(),
                    ),
                    RouteSummaryMetricUiState(
                        label = SUMMARY_METRIC_RENDERABLE_LABEL,
                        value = preview.toRenderableSegmentLabel(),
                    ),
                ),
            previewPoints = previewPolyline.points,
            segmentCount = preview.segmentCount,
            renderableSegmentCount = preview.renderableSegmentCount,
            fallbackSegmentCount = preview.fallbackSegmentCount,
            previewFallbackNotice = preview.fallbackNotice(),
            badges = routeBadges(includeSafePriority = true),
        )

    private fun RouteCandidate.routeBadges(includeSafePriority: Boolean): List<RouteOptionBadge> {
        val aggregateFlags =
            segments.fold(RouteSegmentSafetyFlags()) { flags, segment ->
                flags.merge(segment.safetyFlags)
            }

        return buildList {
            if (includeSafePriority && routeOption == RouteOption.SAFE) {
                add(RouteOptionBadge.SAFE_PRIORITY)
            }
            if (!aggregateFlags.hasStairs && !aggregateFlags.hasCurbGap) {
                add(RouteOptionBadge.STEP_FREE)
            }
            if (aggregateFlags.hasAudioSignal) {
                add(RouteOptionBadge.AUDIO_SIGNAL)
            }
            if (aggregateFlags.hasBrailleBlock) {
                add(RouteOptionBadge.BRAILLE_BLOCK)
            }
            if (aggregateFlags.hasCrosswalk && aggregateFlags.hasSignal) {
                add(RouteOptionBadge.SIGNAL_CROSSWALK)
            }
            if (aggregateFlags.hasCurbGap) {
                add(RouteOptionBadge.CURB_GAP)
            }
            if (aggregateFlags.hasCrosswalk && !aggregateFlags.hasSignal) {
                add(RouteOptionBadge.UNSIGNALIZED_CROSSWALK)
            }
        }.distinct().take(MAX_ROUTE_BADGE_COUNT)
    }

    private fun emitUiEvent(event: RouteSettingUiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }

    companion object {
        fun provideFactory(
            routeRepository: RouteRepository,
            destinationSelectionRepository: DestinationSelectionRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(RouteSettingViewModel::class.java)) {
                        return RouteSettingViewModel(
                            routeRepository = routeRepository,
                            destinationSelectionRepository = destinationSelectionRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }

        private val DEFAULT_ORIGIN =
            RouteWaypoint(
                name = DEFAULT_ORIGIN_LABEL,
                address = DEFAULT_ORIGIN_SUPPORTING_TEXT,
                coordinate = GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
            )

        private val DEFAULT_DESTINATION =
            RouteWaypoint(
                name = "부산역",
                address = "부산 동구 중앙대로 206",
                coordinate = GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
            )
    }
}

private fun RouteWaypoint.toLocationUiState(addressFallback: String?): RouteLocationUiState =
    RouteLocationUiState(
        name = name.orEmpty(),
        supportingText = address?.takeIf { value -> value.isNotBlank() } ?: addressFallback,
        coordinate = coordinate,
    )

private fun RouteSummary.toSummaryLabel(): String =
    "${estimatedTimeMinutes.toEstimatedTimeLabel()} · ${distanceMeters.toDistanceLabel()}"

private fun RouteOption.optionCardPresentation(summary: RouteSummary): RouteOptionCardPresentation =
    when (this) {
        RouteOption.SAFE ->
            RouteOptionCardPresentation(
                title = OPTION_TITLE_SAFE,
                description = OPTION_DESCRIPTION_SAFE,
                highlightLabel = OPTION_HIGHLIGHT_RECOMMENDED,
                metrics =
                    listOf(
                        RouteOptionCardMetricUiState(
                            label = SUMMARY_METRIC_RISK_LABEL,
                            value = summary.riskLevel.toRiskValueLabel(),
                        ),
                        RouteOptionCardMetricUiState(
                            label = SUMMARY_METRIC_TIME_LABEL,
                            value = summary.estimatedTimeMinutes.toEstimatedTimeLabel(),
                        ),
                    ),
            )

        RouteOption.SHORTEST ->
            RouteOptionCardPresentation(
                title = OPTION_TITLE_SHORTEST,
                description = OPTION_DESCRIPTION_SHORTEST,
                highlightLabel = null,
                metrics =
                    listOf(
                        RouteOptionCardMetricUiState(
                            label = SUMMARY_METRIC_TIME_LABEL,
                            value = summary.estimatedTimeMinutes.toEstimatedTimeLabel(),
                        ),
                        RouteOptionCardMetricUiState(
                            label = SUMMARY_METRIC_DISTANCE_LABEL,
                            value = summary.distanceMeters.toDistanceLabel(),
                        ),
                    ),
            )
    }

private fun RouteOption.toOptionTitle(): String =
    when (this) {
        RouteOption.SAFE -> OPTION_TITLE_SAFE
        RouteOption.SHORTEST -> OPTION_TITLE_SHORTEST
    }

private fun List<RouteSegment>.primaryGuidanceMessage(): String =
    firstNotNullOfOrNull { segment ->
        segment.guidanceMessage.takeIf { guidanceMessage -> guidanceMessage.isNotBlank() }
    } ?: DEFAULT_GUIDANCE_MESSAGE

private fun Int.toEstimatedTimeLabel(): String =
    if (this > 0) {
        "${this}분"
    } else {
        SUMMARY_VALUE_PENDING
    }

private fun Int.toDistanceLabel(): String =
    when {
        this <= 0 -> SUMMARY_VALUE_PENDING
        this < METERS_PER_KILOMETER -> "$this m"
        else -> String.format(Locale.US, "%.1f km", this / METERS_PER_KILOMETER.toFloat())
    }

private fun RouteRiskLevel.toRiskLabel(): String =
    when (this) {
        RouteRiskLevel.LOW -> RISK_LABEL_LOW
        RouteRiskLevel.MEDIUM -> RISK_LABEL_MEDIUM
        RouteRiskLevel.HIGH -> RISK_LABEL_HIGH
    }

private fun RouteRiskLevel.toRiskValueLabel(): String =
    when (this) {
        RouteRiskLevel.LOW -> RISK_VALUE_LOW
        RouteRiskLevel.MEDIUM -> RISK_VALUE_MEDIUM
        RouteRiskLevel.HIGH -> RISK_VALUE_HIGH
    }

private fun RoutePreviewModel.toRenderableSegmentLabel(): String =
    "${renderableSegmentCount.coerceAtLeast(0)}/${segmentCount.coerceAtLeast(0)}"

private fun RoutePreviewModel.fallbackNotice(): String? =
    if (fallbackSegmentCount > 0 && renderableSegmentCount == 0) {
        PREVIEW_FALLBACK_NOTICE
    } else {
        null
    }

private fun RouteSegmentSafetyFlags.merge(other: RouteSegmentSafetyFlags): RouteSegmentSafetyFlags =
    RouteSegmentSafetyFlags(
        hasStairs = hasStairs || other.hasStairs,
        hasCurbGap = hasCurbGap || other.hasCurbGap,
        hasCrosswalk = hasCrosswalk || other.hasCrosswalk,
        hasSignal = hasSignal || other.hasSignal,
        hasAudioSignal = hasAudioSignal || other.hasAudioSignal,
        hasBrailleBlock = hasBrailleBlock || other.hasBrailleBlock,
    )

private fun RouteOption.routeSortOrder(): Int =
    when (this) {
        RouteOption.SAFE -> 0
        RouteOption.SHORTEST -> 1
    }

private const val DEFAULT_ORIGIN_LABEL = "현재 위치"
private const val DEFAULT_ORIGIN_SUPPORTING_TEXT = "실시간 위치 연동 전까지 데모 좌표를 출발지로 사용합니다."
private const val DEFAULT_DESTINATION_ADDRESS_FALLBACK = "주소 정보 없음"
private const val DEFAULT_ROUTE_LOAD_ERROR_MESSAGE = "경로 fixture를 불러오지 못했습니다."
private const val DEFAULT_GUIDANCE_MESSAGE = "선택한 경로를 따라 이동합니다."
private const val SUMMARY_VALUE_PENDING = "확인 중"
private const val OPTION_TITLE_SAFE = "SAFE 우선"
private const val OPTION_DESCRIPTION_SAFE = "안전 요소와 보행 위험을 함께 고려해 우선 제안하는 경로입니다."
private const val OPTION_TITLE_SHORTEST = "최단 거리"
private const val OPTION_DESCRIPTION_SHORTEST = "이동 시간을 줄이는 기준으로 빠른 경로를 비교합니다."
private const val OPTION_HIGHLIGHT_RECOMMENDED = "추천"
private const val OPTION_SELECTION_SELECTED = "현재 선택됨"
private const val OPTION_SELECTION_AVAILABLE = "탭하여 선택"
private const val SUMMARY_METRIC_TIME_LABEL = "예상 시간"
private const val SUMMARY_METRIC_DISTANCE_LABEL = "예상 거리"
private const val SUMMARY_METRIC_RISK_LABEL = "위험도"
private const val SUMMARY_METRIC_RENDERABLE_LABEL = "렌더링 구간"
private const val RISK_LABEL_LOW = "위험도 낮음"
private const val RISK_LABEL_MEDIUM = "위험도 보통"
private const val RISK_LABEL_HIGH = "위험도 높음"
private const val RISK_VALUE_LOW = "낮음"
private const val RISK_VALUE_MEDIUM = "보통"
private const val RISK_VALUE_HIGH = "높음"
private const val PREVIEW_FALLBACK_NOTICE = "일부 구간은 geometry fallback 상태라 preview 없이 요약 정보만 표시합니다."
private const val METERS_PER_KILOMETER = 1_000
private const val MAX_ROUTE_BADGE_COUNT = 3

private data class RouteOptionCardPresentation(
    val title: String,
    val description: String,
    val highlightLabel: String?,
    val metrics: List<RouteOptionCardMetricUiState>,
)
