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
import com.ssafy.e102.eumgil.core.model.toRouteWaypointOrNull
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
    private var hasLoadedInitialDestination: Boolean = false

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
                val shouldLoadFromState = !hasLoadedInitialDestination || selectedDestination == null
                if (!shouldLoadFromState) {
                    return@collectLatest
                }

                hasLoadedInitialDestination = true
                loadRouteShell(
                    destinationResolution = resolveDestination(selectedDestination),
                    resetSelectedOption = true,
                )
            }
        }

        viewModelScope.launch {
            // This ViewModel is activity-scoped, so same-place reselection needs an explicit request flow.
            destinationSelectionRepository.selectionRequests.collectLatest { selectedDestination ->
                hasLoadedInitialDestination = true
                loadRouteShell(
                    destinationResolution = resolveDestination(selectedDestination),
                    resetSelectedOption = true,
                )
            }
        }
    }

    private suspend fun loadRouteShell(
        destinationResolution: RouteDestinationResolution,
        resetSelectedOption: Boolean,
    ) {
        latestSearchData = null
        val selectedOption =
            if (resetSelectedOption) {
                DEFAULT_SELECTED_OPTION
            } else {
                mutableUiState.value.selectedOption
            }

        mutableUiState.update { state ->
            state.copy(
                isLoading = true,
                loadErrorMessage = null,
                origin = originLocationUiState(),
                destination = destinationResolution.destinationUiState,
                destinationHandoffState = destinationResolution.handoffState,
                destinationFallbackMessage = destinationResolution.fallbackMessage,
                isUsingFallbackDestination = destinationResolution.isUsingFallbackDestination,
                selectedOption = selectedOption,
                optionCards = emptyList(),
                selectedRoute = null,
                routePreviewMap =
                    loadingRoutePreviewMapUiState(
                        originCoordinate = DEFAULT_ORIGIN.coordinate,
                        destinationResolution = destinationResolution,
                    ),
                sourceLabel = null,
                cta = loadingCtaUiState(),
                ctaAcknowledged = false,
            )
        }

        runCatching {
            routeRepository.getRouteSearchData(buildQuery(destinationResolution = destinationResolution))
        }.onSuccess { searchData ->
            latestSearchData = searchData
            mutableUiState.value =
                buildUiState(
                    searchData = searchData,
                    destinationResolution = destinationResolution,
                    requestedOption = selectedOption,
                    ctaAcknowledged = false,
                )
        }.onFailure { throwable ->
            mutableUiState.update { state ->
                state.copy(
                    isLoading = false,
                    loadErrorMessage = throwable.message ?: DEFAULT_ROUTE_LOAD_ERROR_MESSAGE,
                    routePreviewMap =
                        errorRoutePreviewMapUiState(
                            originCoordinate = DEFAULT_ORIGIN.coordinate,
                            destinationResolution = destinationResolution,
                            message = throwable.message ?: DEFAULT_ROUTE_LOAD_ERROR_MESSAGE,
                        ),
                    cta = errorCtaUiState(),
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
                destinationResolution = resolveDestination(destinationSelectionRepository.selectedDestination.value),
                requestedOption = routeOption,
                ctaAcknowledged = mutableUiState.value.ctaAcknowledged,
            )
    }

    private fun startNavigation() {
        if (mutableUiState.value.ctaAcknowledged) {
            return
        }
        if (mutableUiState.value.destinationHandoffState != RouteDestinationHandoffState.DIRECT) {
            return
        }
        val searchData = latestSearchData ?: return
        val selectedRoute =
            searchData.findRoute(uiState.value.selectedOption)
                ?: searchData.primaryRoute
                ?: return

        mutableUiState.update { state ->
            state.copy(
                cta =
                    buildCtaUiState(
                        selectedRoute = state.selectedRoute,
                        ctaAcknowledged = true,
                        destinationHandoffState = state.destinationHandoffState,
                    ),
                ctaAcknowledged = true,
            )
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
        destinationResolution: RouteDestinationResolution,
        requestedOption: RouteOption,
        ctaAcknowledged: Boolean,
    ): RouteSettingUiState {
        val availableRoutes = searchData.routes.sortedBy { route -> route.routeOption.routeSortOrder() }
        val resolvedDestination = destinationLocationUiState(searchData.result.destination)
        val resolvedOption =
            if (searchData.findRoute(requestedOption) != null) {
                requestedOption
            } else {
                availableRoutes.firstOrNull()?.routeOption ?: requestedOption
            }
        val selectedRoute =
            searchData.findRoute(resolvedOption)
                ?: availableRoutes.firstOrNull()
        val selectedRouteUiState = selectedRoute?.toSelectedRouteUiState(destination = resolvedDestination)
        val routePreviewMapUiState =
            selectedRoute.toRoutePreviewMapUiState(
                originCoordinate = searchData.result.origin.coordinate,
                destinationCoordinate = searchData.result.destination.coordinate,
                destinationHandoffState = destinationResolution.handoffState,
            )

        return RouteSettingUiState(
            isLoading = false,
            loadErrorMessage = null,
            origin = originLocationUiState(searchData.result.origin),
            destination = resolvedDestination,
            destinationHandoffState = destinationResolution.handoffState,
            destinationFallbackMessage = destinationResolution.fallbackMessage,
            isUsingFallbackDestination = destinationResolution.isUsingFallbackDestination,
            selectedOption = resolvedOption,
            optionCards =
                availableRoutes.map { route ->
                    route.toOptionCardUiState(
                        isSelected = route.routeOption == resolvedOption,
                    )
                },
            selectedRoute = selectedRouteUiState,
            routePreviewMap = routePreviewMapUiState,
            sourceLabel = searchData.source.label,
            cta =
                buildCtaUiState(
                    selectedRoute = selectedRouteUiState,
                    ctaAcknowledged = ctaAcknowledged,
                    destinationHandoffState = destinationResolution.handoffState,
                ),
            ctaAcknowledged = ctaAcknowledged,
        )
    }

    private fun originLocationUiState(origin: RouteWaypoint = DEFAULT_ORIGIN): RouteLocationUiState =
        origin.toLocationUiState(addressFallback = DEFAULT_ORIGIN_SUPPORTING_TEXT)

    private fun destinationLocationUiState(destination: RouteWaypoint): RouteLocationUiState =
        destination.toLocationUiState(addressFallback = DEFAULT_DESTINATION_ADDRESS_FALLBACK)

    private fun buildQuery(destinationResolution: RouteDestinationResolution): RouteSearchQuery =
        RouteSearchQuery(
            origin = DEFAULT_ORIGIN,
            destination = destinationResolution.routeDestination,
        )

    private fun resolveDestination(selectedDestination: PlaceDestination?): RouteDestinationResolution =
        when {
            selectedDestination == null ->
                RouteDestinationResolution(
                    routeDestination = DEFAULT_DESTINATION,
                    destinationUiState = destinationLocationUiState(DEFAULT_DESTINATION),
                    handoffState = RouteDestinationHandoffState.EMPTY,
                    fallbackMessage = DESTINATION_FALLBACK_EMPTY_MESSAGE,
                )

            else -> {
                val routeDestination = selectedDestination.toRouteWaypointOrNull()
                if (routeDestination == null) {
                    RouteDestinationResolution(
                        routeDestination = DEFAULT_DESTINATION,
                        destinationUiState = destinationLocationUiState(DEFAULT_DESTINATION),
                        handoffState = RouteDestinationHandoffState.INVALID_COORDINATE,
                        fallbackMessage = DESTINATION_FALLBACK_INVALID_COORDINATE_MESSAGE,
                    )
                } else {
                    RouteDestinationResolution(
                        routeDestination = routeDestination,
                        destinationUiState = destinationLocationUiState(routeDestination),
                        handoffState = RouteDestinationHandoffState.DIRECT,
                    )
                }
            }
        }

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

    private fun RouteCandidate.toSelectedRouteUiState(destination: RouteLocationUiState): RouteSelectedRouteUiState =
        RouteSelectedRouteUiState(
            routeOption = routeOption,
            destination = destination,
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
        placeId = placeId,
        name = name.orEmpty(),
        supportingText = address?.takeIf { value -> value.isNotBlank() } ?: addressFallback,
        coordinate = coordinate,
        category = category,
        metadataLabel = buildLocationMetadataLabel(placeId = placeId, category = category),
    )

private fun loadingRoutePreviewMapUiState(
    originCoordinate: GeoCoordinate,
    destinationResolution: RouteDestinationResolution,
): RoutePreviewMapUiState =
    RoutePreviewMapUiState(
        status = RoutePreviewMapStatus.LOADING,
        originCoordinate = originCoordinate,
        destinationCoordinate = destinationResolution.routeDestination.coordinate,
        fallbackMessage = ROUTE_PREVIEW_MAP_LOADING_MESSAGE,
    )

private fun errorRoutePreviewMapUiState(
    originCoordinate: GeoCoordinate,
    destinationResolution: RouteDestinationResolution,
    message: String,
): RoutePreviewMapUiState =
    RoutePreviewMapUiState(
        status = RoutePreviewMapStatus.ERROR,
        originCoordinate = originCoordinate,
        destinationCoordinate = destinationResolution.routeDestination.coordinate,
        fallbackMessage = message,
    )

private fun RouteCandidate?.toRoutePreviewMapUiState(
    originCoordinate: GeoCoordinate,
    destinationCoordinate: GeoCoordinate,
    destinationHandoffState: RouteDestinationHandoffState,
): RoutePreviewMapUiState =
    when {
        destinationHandoffState == RouteDestinationHandoffState.EMPTY ->
            RoutePreviewMapUiState(
                status = RoutePreviewMapStatus.NO_DESTINATION,
                originCoordinate = originCoordinate,
                fallbackMessage = ROUTE_PREVIEW_MAP_NO_DESTINATION_MESSAGE,
            )

        destinationHandoffState == RouteDestinationHandoffState.INVALID_COORDINATE ->
            RoutePreviewMapUiState(
                status = RoutePreviewMapStatus.INVALID_DESTINATION,
                originCoordinate = originCoordinate,
                fallbackMessage = ROUTE_PREVIEW_MAP_INVALID_DESTINATION_MESSAGE,
            )

        this == null ->
            RoutePreviewMapUiState(
                status = RoutePreviewMapStatus.NO_ROUTE,
                originCoordinate = originCoordinate,
                destinationCoordinate = destinationCoordinate,
                fallbackMessage = ROUTE_PREVIEW_MAP_NO_ROUTE_MESSAGE,
            )

        !previewPolyline.isRenderable ->
            RoutePreviewMapUiState(
                status = RoutePreviewMapStatus.POLYLINE_UNAVAILABLE,
                routeOption = routeOption,
                originCoordinate = originCoordinate,
                destinationCoordinate = destinationCoordinate,
                fallbackMessage = ROUTE_PREVIEW_MAP_POLYLINE_UNAVAILABLE_MESSAGE,
            )

        else ->
            RoutePreviewMapUiState(
                status = RoutePreviewMapStatus.READY,
                routeOption = routeOption,
                originCoordinate = originCoordinate,
                destinationCoordinate = destinationCoordinate,
                polyline = previewPolyline.points,
            )
    }

private fun buildLocationMetadataLabel(
    placeId: String?,
    category: com.ssafy.e102.eumgil.core.model.PlaceCategory?,
): String? {
    val metadataParts = buildList {
        placeId?.takeIf { value -> value.isNotBlank() }?.let { value ->
            add("ID $value")
        }
        category?.let { value ->
            add("Category ${value.name}")
        }
    }

    return metadataParts.takeIf(List<String>::isNotEmpty)?.joinToString(separator = " | ")
}

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

private fun loadingCtaUiState(): RouteSettingCtaUiState =
    RouteSettingCtaUiState(
        label = CTA_LABEL_START,
        supportingText = CTA_SUPPORTING_LOADING,
        isEnabled = false,
    )

private fun errorCtaUiState(): RouteSettingCtaUiState =
    RouteSettingCtaUiState(
        label = CTA_LABEL_START,
        supportingText = CTA_SUPPORTING_ERROR,
        isEnabled = false,
    )

private fun buildCtaUiState(
    selectedRoute: RouteSelectedRouteUiState?,
    ctaAcknowledged: Boolean,
    destinationHandoffState: RouteDestinationHandoffState,
): RouteSettingCtaUiState =
    when {
        ctaAcknowledged ->
            RouteSettingCtaUiState(
                label = CTA_LABEL_START,
                supportingText = CTA_SUPPORTING_ACKNOWLEDGED,
                isEnabled = false,
            )

        destinationHandoffState == RouteDestinationHandoffState.EMPTY ->
            RouteSettingCtaUiState(
                label = CTA_LABEL_START,
                supportingText = CTA_SUPPORTING_WAITING_HANDOFF,
                isEnabled = false,
            )

        destinationHandoffState == RouteDestinationHandoffState.INVALID_COORDINATE ->
            RouteSettingCtaUiState(
                label = CTA_LABEL_START,
                supportingText = CTA_SUPPORTING_INVALID_HANDOFF,
                isEnabled = false,
            )

        selectedRoute == null ->
            RouteSettingCtaUiState(
                label = CTA_LABEL_START,
                supportingText = CTA_SUPPORTING_EMPTY,
                isEnabled = false,
            )

        else ->
            RouteSettingCtaUiState(
                label = CTA_LABEL_START,
                supportingText = CTA_SUPPORTING_READY,
                isEnabled = true,
            )
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
private const val CTA_LABEL_START = "선택한 경로로 안내 시작"
private const val CTA_SUPPORTING_READY = "201 작업에서 route setting handoff를 navigation 진행 화면으로 연결합니다."
private const val CTA_SUPPORTING_ACKNOWLEDGED = "내비게이션 진행 화면 연결은 다음 스레드에서 마무리합니다."
private const val CTA_SUPPORTING_WAITING_HANDOFF = "검색 또는 지도에서 목적지를 선택하면 안내 시작을 활성화합니다."
private const val CTA_SUPPORTING_INVALID_HANDOFF = "목적지 좌표를 다시 확인하면 안내 시작을 활성화합니다."
private const val CTA_SUPPORTING_ERROR = "경로 정보를 다시 불러오면 시작 CTA를 활성화할 수 있습니다."
private const val CTA_SUPPORTING_LOADING = "fixture 기반 route summary를 불러오는 동안 CTA를 잠시 비활성화합니다."
private const val CTA_SUPPORTING_EMPTY = "표시할 경로가 준비되면 시작 CTA를 활성화합니다."
private const val DESTINATION_FALLBACK_EMPTY_MESSAGE = "검색 handoff 전에는 fixture 목적지를 기본 도착지로 유지합니다."
private const val DESTINATION_FALLBACK_INVALID_COORDINATE_MESSAGE = "선택한 목적지 좌표를 확인할 수 없어 fixture 목적지로 대체했습니다."
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
private const val ROUTE_PREVIEW_MAP_LOADING_MESSAGE = "Route preview map is loading."
private const val ROUTE_PREVIEW_MAP_NO_DESTINATION_MESSAGE = "Destination is required before showing a route preview map."
private const val ROUTE_PREVIEW_MAP_INVALID_DESTINATION_MESSAGE = "Destination coordinate is invalid."
private const val ROUTE_PREVIEW_MAP_NO_ROUTE_MESSAGE = "No selected route is available for the preview map."
private const val ROUTE_PREVIEW_MAP_POLYLINE_UNAVAILABLE_MESSAGE = "Selected route preview polyline needs at least two points."
private const val METERS_PER_KILOMETER = 1_000
private const val MAX_ROUTE_BADGE_COUNT = 3
private val DEFAULT_SELECTED_OPTION = RouteOption.SAFE

private data class RouteOptionCardPresentation(
    val title: String,
    val description: String,
    val highlightLabel: String?,
    val metrics: List<RouteOptionCardMetricUiState>,
)

private data class RouteDestinationResolution(
    val routeDestination: RouteWaypoint,
    val destinationUiState: RouteLocationUiState,
    val handoffState: RouteDestinationHandoffState,
    val fallbackMessage: String? = null,
) {
    val isUsingFallbackDestination: Boolean
        get() = handoffState != RouteDestinationHandoffState.DIRECT
}
