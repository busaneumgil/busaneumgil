package com.ssafy.e102.eumgil.feature.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSegmentSafetyFlags
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.toRouteWaypoint
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
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
                destination = destinationLocationUiState(selectedDestination = selectedDestination),
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
            origin = originLocationUiState(),
            destination = destinationLocationUiState(selectedDestination = selectedDestination),
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

    private fun originLocationUiState(): RouteLocationUiState =
        RouteLocationUiState(
            name = DEFAULT_ORIGIN_LABEL,
            supportingText = DEFAULT_ORIGIN_SUPPORTING_TEXT,
            coordinate = DEFAULT_ORIGIN.coordinate,
        )

    private fun destinationLocationUiState(selectedDestination: PlaceDestination?): RouteLocationUiState {
        val destination =
            selectedDestination?.toRouteWaypoint()
                ?: DEFAULT_DESTINATION

        return RouteLocationUiState(
            name = destination.name.orEmpty(),
            supportingText = destination.address,
            coordinate = destination.coordinate,
        )
    }

    private fun buildQuery(selectedDestination: PlaceDestination?): RouteSearchQuery =
        RouteSearchQuery(
            origin = DEFAULT_ORIGIN,
            destination = selectedDestination?.toRouteWaypoint() ?: DEFAULT_DESTINATION,
        )

    private fun RouteCandidate.toOptionCardUiState(isSelected: Boolean): RouteOptionCardUiState =
        RouteOptionCardUiState(
            routeOption = routeOption,
            title = title,
            distanceMeters = summary.distanceMeters,
            estimatedTimeMinutes = summary.estimatedTimeMinutes,
            riskLevel = summary.riskLevel,
            badges = routeBadges(),
            isRecommended = routeOption == RouteOption.SAFE,
            isSelected = isSelected,
        )

    private fun RouteCandidate.toSelectedRouteUiState(): RouteSelectedRouteUiState =
        RouteSelectedRouteUiState(
            routeOption = routeOption,
            title = title,
            distanceMeters = summary.distanceMeters,
            estimatedTimeMinutes = summary.estimatedTimeMinutes,
            riskLevel = summary.riskLevel,
            guidanceMessage = segments.firstOrNull()?.guidanceMessage ?: DEFAULT_GUIDANCE_MESSAGE,
            previewPoints = previewPolyline.points,
            segmentCount = preview.segmentCount,
            renderableSegmentCount = preview.renderableSegmentCount,
            fallbackSegmentCount = preview.fallbackSegmentCount,
            badges = routeBadges(),
        )

    private fun RouteCandidate.routeBadges(): List<RouteOptionBadge> {
        val aggregateFlags =
            segments.fold(RouteSegmentSafetyFlags()) { flags, segment ->
                flags.merge(segment.safetyFlags)
            }

        return buildList {
            if (routeOption == RouteOption.SAFE) {
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
private const val DEFAULT_ROUTE_LOAD_ERROR_MESSAGE = "경로 fixture를 불러오지 못했습니다."
private const val DEFAULT_GUIDANCE_MESSAGE = "선택한 경로를 따라 이동합니다."
private const val MAX_ROUTE_BADGE_COUNT = 3
