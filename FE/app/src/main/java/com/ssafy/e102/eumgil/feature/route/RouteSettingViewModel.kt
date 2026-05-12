package com.ssafy.e102.eumgil.feature.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.MapPlaceClickType
import com.ssafy.e102.eumgil.core.model.MapPlaceDetailRequest
import com.ssafy.e102.eumgil.core.model.MapTappedPlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePreviewModel
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSegmentSafetyFlags
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.toRecentDestination
import com.ssafy.e102.eumgil.core.model.toRouteWaypointOrNull
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.data.remote.datasource.RouteApiException
import com.ssafy.e102.eumgil.data.remote.datasource.RouteFailureKind
import com.ssafy.e102.eumgil.feature.navigation.haversineDistanceMeters
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val currentLocationManager: CurrentLocationManager = NoOpCurrentLocationManager,
    private val locationPermissionManager: LocationPermissionManager? = null,
    private val placesRepository: PlacesRepository? = null,
    private val searchRepository: SearchRepository? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RouteSettingUiState())
    val uiState = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<RouteSettingUiEvent>()
    val uiEvent: SharedFlow<RouteSettingUiEvent> = mutableUiEvent.asSharedFlow()

    private var latestSearchDataByMode: Map<RouteTravelMode, RouteSearchData> = emptyMap()
    private var selectedOptionByMode: Map<RouteTravelMode, RouteOption> = defaultSelectedOptionsByMode()
    private var latestLocationSnapshot: LocationSnapshot? = currentLocationManager.latestLocation.value
    private var hasLoadedInitialRoute: Boolean = false
    private var isStartNavigationInFlight: Boolean = false
    private var isRouteReloadInFlight: Boolean = false
    private var pendingRouteReloadRequest: RouteReloadRequest? = null
    private var lastCompletedRouteReloadSignature: RouteReloadSignature? = null
    private var lastSuccessfulAutoOrigin: RouteWaypoint? = null
    private var hasStartedActiveLocationUpdates: Boolean = false
    private var stagedTransitEnhancementJob: Job? = null
    private var activeRouteLoadId: Long = 0L

    init {
        currentLocationManager.refreshLatestLocation()
        observeSelectionRequests()
        observeLocationUpdates()
        observeLocationPermissionState()
        requestRouteReload(
            resetSelectedOption = true,
            force = true,
        )
    }

    fun onAction(action: RouteSettingUiAction) {
        when (action) {
            RouteSettingUiAction.BackClicked -> emitUiEvent(RouteSettingUiEvent.NavigateBack)
            is RouteSettingUiAction.WaypointClicked -> openWaypointSearch(action.editingTarget)
            is RouteSettingUiAction.TravelModeSelected -> selectTravelMode(action.mode)
            is RouteSettingUiAction.RouteOptionSelected -> selectRouteOption(action.routeOption)
            is RouteSettingUiAction.RouteOptionDetailClicked -> openRouteDetail(action.routeOption)
            RouteSettingUiAction.WaypointsSwapClicked -> swapWaypoints()
            RouteSettingUiAction.StartNavigationClicked -> startNavigation()
        }
    }

    fun startLocationUpdates() {
        hasStartedActiveLocationUpdates = true
        syncLocationAccess(requestPermissionIfNeeded = true)
    }

    fun stopLocationUpdates() {
        hasStartedActiveLocationUpdates = false
        currentLocationManager.stopLocationUpdates()
    }

    private fun observeSelectionRequests() {
        viewModelScope.launch {
            // This ViewModel is activity-scoped, so same-place reselection needs an explicit request flow.
            destinationSelectionRepository.selectionRequests.collectLatest {
                requestRouteReload(
                    resetSelectedOption = true,
                    force = true,
                )
            }
        }
    }

    private fun observeLocationUpdates() {
        viewModelScope.launch {
            currentLocationManager.latestLocation.collectLatest { snapshot ->
                val selectedOrigin = destinationSelectionRepository.selectedOrigin.value
                val previousLocationSnapshot = latestLocationSnapshot
                latestLocationSnapshot = snapshot

                if (!hasLoadedInitialRoute || selectedOrigin != null) {
                    return@collectLatest
                }

                val currentOrigin = snapshot?.toRouteWaypoint() ?: return@collectLatest
                val previousOrigin = lastSuccessfulAutoOrigin ?: previousLocationSnapshot?.toRouteWaypoint()
                if (previousOrigin == null) {
                    requestRouteReload(
                        resetSelectedOption = true,
                        force = true,
                    )
                    return@collectLatest
                }
                if (shouldReloadForAutomaticOriginUpdate(previousOrigin = previousOrigin, currentOrigin = currentOrigin, snapshot = snapshot)) {
                    requestRouteReload(resetSelectedOption = true)
                }
            }
        }
    }

    private fun observeLocationPermissionState() {
        val permissionManager = locationPermissionManager ?: return
        viewModelScope.launch {
            permissionManager.permissionState.collectLatest { permissionState ->
                if (!hasStartedActiveLocationUpdates || !shouldUseAutomaticOrigin()) {
                    return@collectLatest
                }

                when (permissionState) {
                    is LocationPermissionState.Granted ->
                        startCurrentLocationTracking(forceReload = latestLocationSnapshot == null)

                    LocationPermissionState.Denied -> currentLocationManager.stopLocationUpdates()
                    is LocationPermissionState.Unavailable -> currentLocationManager.stopLocationUpdates()
                }
            }
        }
    }

    private fun syncLocationAccess(requestPermissionIfNeeded: Boolean) {
        if (!shouldUseAutomaticOrigin()) {
            currentLocationManager.startLocationUpdates()
            return
        }

        val permissionManager = locationPermissionManager
        if (permissionManager == null) {
            startCurrentLocationTracking(forceReload = latestLocationSnapshot == null)
            return
        }

        permissionManager.refreshPermissionState()
        when (permissionManager.permissionState.value) {
            is LocationPermissionState.Granted ->
                startCurrentLocationTracking(forceReload = latestLocationSnapshot == null)

            LocationPermissionState.Denied -> {
                currentLocationManager.stopLocationUpdates()
                if (requestPermissionIfNeeded) {
                    emitUiEvent(RouteSettingUiEvent.RequestLocationPermission)
                }
                reloadAutomaticOriginIfMissing()
            }

            is LocationPermissionState.Unavailable -> {
                currentLocationManager.stopLocationUpdates()
                reloadAutomaticOriginIfMissing()
            }
        }
    }

    private fun startCurrentLocationTracking(forceReload: Boolean) {
        currentLocationManager.startLocationUpdates()
        if (forceReload) {
            reloadAutomaticOriginIfMissing()
        }
    }

    private fun reloadAutomaticOriginIfMissing() {
        if (!shouldUseAutomaticOrigin() || latestLocationSnapshot != null) {
            return
        }

        requestRouteReload(
            resetSelectedOption = false,
            force = true,
        )
    }

    private fun shouldUseAutomaticOrigin(): Boolean =
        destinationSelectionRepository.selectedOrigin.value == null

    private fun requestRouteReload(
        resetSelectedOption: Boolean,
        force: Boolean = false,
    ) {
        cancelStagedTransitEnhancement()
        pendingRouteReloadRequest =
            pendingRouteReloadRequest.mergeWith(
                RouteReloadRequest(
                    resetSelectedOption = resetSelectedOption,
                    force = force,
                ),
            )
        if (isRouteReloadInFlight) {
            return
        }
        isRouteReloadInFlight = true
        viewModelScope.launch {
            drainRouteReloadQueue()
        }
    }

    private suspend fun drainRouteReloadQueue() {
        try {
            while (true) {
                val request = pendingRouteReloadRequest ?: break
                pendingRouteReloadRequest = null
                performRouteReload(request)
            }
        } finally {
            isRouteReloadInFlight = false
            if (pendingRouteReloadRequest != null) {
                requestRouteReload(
                    resetSelectedOption = pendingRouteReloadRequest?.resetSelectedOption == true,
                    force = pendingRouteReloadRequest?.force == true,
                )
            }
        }
    }

    private suspend fun performRouteReload(request: RouteReloadRequest) {
        val loadId = beginRouteLoad()
        val selectedOrigin = destinationSelectionRepository.selectedOrigin.value
        val selectedDestination = destinationSelectionRepository.selectedDestination.value
        hasLoadedInitialRoute = true
        val originResolution =
            resolveOrigin(
                selectedOrigin = selectedOrigin,
                locationSnapshot = latestLocationSnapshot,
            )
        val destinationResolution = resolveDestination(selectedDestination)
        val signature =
            RouteReloadSignature(
                originPlaceId = selectedOrigin?.placeId,
                originCoordinate = originResolution.routeOrigin.coordinate,
                destinationPlaceId = selectedDestination?.placeId,
                destinationCoordinate = destinationResolution.routeDestination.coordinate,
                destinationHandoffState = destinationResolution.handoffState,
            )
        if (!request.force && signature == lastCompletedRouteReloadSignature) {
            return
        }
        loadRouteShell(
            loadId = loadId,
            originResolution = originResolution,
            destinationResolution = destinationResolution,
            resetSelectedOption = request.resetSelectedOption,
        )
        lastCompletedRouteReloadSignature = signature
    }

    private suspend fun loadRouteShell(
        loadId: Long,
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
        resetSelectedOption: Boolean,
    ) {
        latestSearchDataByMode = emptyMap()
        isStartNavigationInFlight = false
        if (resetSelectedOption) {
            selectedOptionByMode = defaultSelectedOptionsByMode()
        }
        val walkSelectedOption = selectedOptionForMode(RouteTravelMode.WALK)

        mutableUiState.update { state ->
            state.copy(
                isLoading = true,
                loadErrorMessage = null,
                loadNoticeMessage = null,
                loadDebugMessage = buildPendingRouteLoadDebugMessage(RouteTravelMode.WALK),
                originState = originResolution.originState,
                originStatus = originResolution.originStatus,
                origin = originResolution.originUiState,
                destination = destinationResolution.destinationUiState,
                destinationHandoffState = destinationResolution.handoffState,
                destinationFallbackMessage = destinationResolution.fallbackMessage,
                isUsingFallbackDestination = destinationResolution.isUsingFallbackDestination,
                selectedTravelMode = DEFAULT_TRAVEL_MODE,
                pendingTravelMode = null,
                selectedOption = walkSelectedOption,
                optionCards = emptyList(),
                selectedRoute = null,
                routePreviewMap =
                    loadingRoutePreviewMapUiState(
                        originCoordinate = originResolution.routeOrigin.coordinate,
                        destinationResolution = destinationResolution,
                    ),
                sourceLabel = null,
                cta = loadingCtaUiState(),
                ctaAcknowledged = false,
            )
        }

        val walkSearchData =
            runCatching {
                fetchSearchData(
                    mode = RouteTravelMode.WALK,
                    originResolution = originResolution,
                    destinationResolution = destinationResolution,
                )
            }.getOrElse { throwable ->
                applyModeLoadFailure(
                    mode = RouteTravelMode.WALK,
                    selectedOption = walkSelectedOption,
                    originResolution = originResolution,
                    destinationResolution = destinationResolution,
                    throwable = throwable,
                )
                return
            }

        val defaultTravelMode = determineDefaultTravelMode(walkSearchData)
        if (defaultTravelMode == RouteTravelMode.WALK) {
            mutableUiState.value =
                buildUiState(
                    searchData = walkSearchData,
                    originResolution = originResolution,
                    destinationResolution = destinationResolution,
                    selectedTravelMode = RouteTravelMode.WALK,
                    requestedOption = walkSelectedOption,
                    ctaAcknowledged = false,
                )
            rememberSuccessfulAutomaticOrigin(originResolution)
            return
        }

        val transitSelectedOption = selectedOptionForMode(RouteTravelMode.TRANSIT)
        mutableUiState.value =
            buildPendingTransitUiState(
                walkSearchData = walkSearchData,
                originResolution = originResolution,
                destinationResolution = destinationResolution,
                selectedOption = transitSelectedOption,
            )
        rememberSuccessfulAutomaticOrigin(originResolution)
        stagedTransitEnhancementJob =
            viewModelScope.launch {
                completeTransitEnhancement(
                    loadId = loadId,
                    walkSearchData = walkSearchData,
                    transitSelectedOption = transitSelectedOption,
                    originResolution = originResolution,
                    destinationResolution = destinationResolution,
                )
            }
    }

    private suspend fun completeTransitEnhancement(
        loadId: Long,
        walkSearchData: RouteSearchData,
        transitSelectedOption: RouteOption,
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
    ) {
        try {
            val transitSearchData =
                runCatching {
                    fetchSearchData(
                        mode = RouteTravelMode.TRANSIT,
                        originResolution = originResolution,
                        destinationResolution = destinationResolution,
                    )
                }.getOrElse { throwable ->
                    if (throwable is CancellationException) throw throwable
                    latestSearchDataByMode = latestSearchDataByMode - RouteTravelMode.TRANSIT
                    if (!isActiveRouteLoad(loadId)) {
                        return
                    }
                    val walkSelectedOption = selectedOptionForMode(RouteTravelMode.WALK)
                    mutableUiState.value =
                        buildUiState(
                            searchData = walkSearchData,
                            originResolution = originResolution,
                            destinationResolution = destinationResolution,
                            selectedTravelMode = RouteTravelMode.WALK,
                            requestedOption = walkSelectedOption,
                            ctaAcknowledged = false,
                        ).copy(
                            loadNoticeMessage = throwable.toRouteLoadErrorMessage(),
                            loadDebugMessage =
                                combineRouteLoadDebugMessages(
                                    primary = walkSearchData.toRouteLoadDebugMessage(RouteTravelMode.WALK),
                                    secondary = throwable.toRouteLoadDebugMessage(RouteTravelMode.TRANSIT),
                                ),
                        )
                    return
                }
            if (!isActiveRouteLoad(loadId)) {
                return
            }
            mutableUiState.value =
                buildUiState(
                    searchData = transitSearchData,
                    originResolution = originResolution,
                    destinationResolution = destinationResolution,
                    selectedTravelMode = RouteTravelMode.TRANSIT,
                    requestedOption = transitSelectedOption,
                    ctaAcknowledged = false,
                )
        } finally {
            if (isActiveRouteLoad(loadId)) {
                stagedTransitEnhancementJob = null
            }
        }
    }

    private fun beginRouteLoad(): Long {
        activeRouteLoadId += 1L
        return activeRouteLoadId
    }

    private fun isActiveRouteLoad(loadId: Long): Boolean = activeRouteLoadId == loadId

    private fun cancelStagedTransitEnhancement() {
        if (stagedTransitEnhancementJob != null) {
            activeRouteLoadId += 1L
        }
        stagedTransitEnhancementJob?.cancel()
        stagedTransitEnhancementJob = null
    }

    private fun selectTravelMode(
        mode: RouteTravelMode,
        requestedOption: RouteOption? = null,
    ) {
        if (mutableUiState.value.selectedTravelMode == mode && requestedOption == null) {
            return
        }
        cancelStagedTransitEnhancement()

        viewModelScope.launch {
            val originResolution =
                resolveOrigin(
                    selectedOrigin = destinationSelectionRepository.selectedOrigin.value,
                    locationSnapshot = latestLocationSnapshot,
                )
            val destinationResolution = resolveDestination(destinationSelectionRepository.selectedDestination.value)
            val selectedOption = selectedOptionForMode(mode, requestedOption)

            mutableUiState.update { state ->
                state.copy(
                    isLoading = true,
                    loadErrorMessage = null,
                    loadNoticeMessage = null,
                    loadDebugMessage = buildPendingRouteLoadDebugMessage(mode),
                    originState = originResolution.originState,
                    originStatus = originResolution.originStatus,
                    origin = originResolution.originUiState,
                    destination = destinationResolution.destinationUiState,
                    destinationHandoffState = destinationResolution.handoffState,
                    destinationFallbackMessage = destinationResolution.fallbackMessage,
                    isUsingFallbackDestination = destinationResolution.isUsingFallbackDestination,
                    selectedTravelMode = mode,
                    pendingTravelMode = null,
                    selectedOption = selectedOption,
                    optionCards = emptyList(),
                    selectedRoute = null,
                    routePreviewMap =
                        loadingRoutePreviewMapUiState(
                            originCoordinate = originResolution.routeOrigin.coordinate,
                            destinationResolution = destinationResolution,
                        ),
                    sourceLabel = null,
                    cta = loadingCtaUiState(),
                    ctaAcknowledged = false,
                )
            }

            runCatching {
                fetchSearchData(
                    mode = mode,
                    originResolution = originResolution,
                    destinationResolution = destinationResolution,
                )
            }.onSuccess { searchData ->
                mutableUiState.value =
                    buildUiState(
                        searchData = searchData,
                        originResolution = originResolution,
                        destinationResolution = destinationResolution,
                        selectedTravelMode = mode,
                        requestedOption = selectedOption,
                        ctaAcknowledged = false,
                    )
                rememberSuccessfulAutomaticOrigin(originResolution)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                applyModeLoadFailure(
                    mode = mode,
                    selectedOption = selectedOption,
                    originResolution = originResolution,
                    destinationResolution = destinationResolution,
                    throwable = throwable,
                )
            }
        }
    }

    private fun selectRouteOption(routeOption: RouteOption) {
        val previousState = mutableUiState.value
        cancelStagedTransitEnhancement()
        val selectedTravelMode = routeOption.toTravelMode()
        if (mutableUiState.value.selectedTravelMode != selectedTravelMode) {
            selectTravelMode(
                mode = selectedTravelMode,
                requestedOption = routeOption,
            )
            return
        }

        val searchData = latestSearchDataByMode[selectedTravelMode]
        if (searchData == null) {
            rememberSelectedOption(selectedTravelMode, routeOption)
            mutableUiState.update { state ->
                state.copy(
                    pendingTravelMode = null,
                    selectedOption = routeOption,
                    ctaAcknowledged = false,
                )
            }
            return
        }

        mutableUiState.value =
            buildUiState(
                searchData = searchData,
                originResolution = currentOriginResolution(searchData.result.origin),
                destinationResolution = resolveDestination(destinationSelectionRepository.selectedDestination.value),
                selectedTravelMode = selectedTravelMode,
                requestedOption = routeOption,
                ctaAcknowledged = false,
            ).copy(
                loadNoticeMessage =
                    previousState.loadNoticeMessage?.takeIf {
                        previousState.pendingTravelMode == null
                    },
            )
    }

    private fun swapWaypoints() {
        if (mutableUiState.value.destinationHandoffState != RouteDestinationHandoffState.DIRECT) {
            return
        }
        val currentOrigin = mutableUiState.value.origin.toPlaceDestinationOrNull(originPlaceId())
        val currentDestination = mutableUiState.value.destination.toPlaceDestinationOrNull()
        if (currentOrigin == null || currentDestination == null) {
            return
        }

        destinationSelectionRepository.swapSelections(
            origin = currentDestination,
            destination = currentOrigin,
        )
    }

    private fun openWaypointSearch(editingTarget: RouteEditingTarget) {
        destinationSelectionRepository.setEditingTarget(editingTarget)
        emitUiEvent(RouteSettingUiEvent.NavigateToSearch(editingTarget))
    }

    private fun originPlaceId(): String =
        destinationSelectionRepository.selectedOrigin.value?.placeId
            ?: latestLocationSnapshot?.let { CURRENT_LOCATION_ORIGIN_PLACE_ID }
            ?: FALLBACK_ORIGIN_PLACE_ID

    private fun openRouteDetail(routeOption: RouteOption) {
        selectRouteOption(routeOption)
        emitUiEvent(RouteSettingUiEvent.NavigateToRouteDetail(routeOption))
    }

    private fun startNavigation() {
        cancelStagedTransitEnhancement()
        if (mutableUiState.value.ctaAcknowledged || isStartNavigationInFlight) {
            return
        }
        if (mutableUiState.value.destinationHandoffState != RouteDestinationHandoffState.DIRECT) {
            return
        }
        val selectedDestination = destinationSelectionRepository.selectedDestination.value ?: return
        val selectedTravelMode = mutableUiState.value.selectedTravelMode
        val searchData = latestSearchDataByMode[selectedTravelMode] ?: return
        val selectedRoute =
            searchData.findRoute(uiState.value.selectedOption)
                ?: searchData.primaryRoute
                ?: return
        val searchId = searchData.searchId?.takeIf(String::isNotBlank)
        val routeId = selectedRoute.serverRouteId?.takeIf(String::isNotBlank)
        if (searchId == null || routeId == null) {
            applyNavigationStartFailure()
            return
        }

        isStartNavigationInFlight = true
        viewModelScope.launch {
            runCatching {
                routeRepository.selectRoute(
                    routeId = routeId,
                    searchId = searchId,
                )
            }.onSuccess { sessionData ->
                mutableUiState.update { state ->
                    state.copy(
                        loadErrorMessage = null,
                        cta =
                            buildCtaUiState(
                                selectedRoute = state.selectedRoute,
                                ctaAcknowledged = true,
                                destinationHandoffState = state.destinationHandoffState,
                            ),
                        ctaAcknowledged = true,
                    )
                }
                persistRecentDestination(selectedDestination)
                destinationSelectionRepository.clearSelectedOriginSilently()

                emitUiEvent(
                    RouteSettingUiEvent.StartNavigationRequested(
                        request =
                            RouteNavigationRequest(
                                origin = searchData.result.origin,
                                destination = searchData.result.destination,
                                selectedRoute = selectedRoute,
                                source = searchData.source,
                                selectionHandoff =
                                    RouteNavigationSelectionHandoff(
                                        searchId = searchId,
                                        routeId = routeId,
                                        sessionId = sessionData.sessionId,
                                        initialRemainingDistanceMeters =
                                            sessionData.totalDistanceMeters ?: selectedRoute.summary.distanceMeters,
                                        initialRemainingDurationSeconds =
                                            sessionData.totalDurationSeconds
                                                ?: selectedRoute.summary.durationSeconds
                                                ?: selectedRoute.summary.estimatedTimeMinutes * SECONDS_PER_MINUTE,
                                    ),
                            ),
                    ),
                )
            }.onFailure {
                applyNavigationStartFailure()
            }
            isStartNavigationInFlight = false
        }
    }

    private suspend fun persistRecentDestination(destination: PlaceDestination) {
        val repository = searchRepository ?: return
        runCatching {
            repository.saveRecentDestination(destination.toRecentDestination())
        }
    }

    private suspend fun fetchSearchData(
        mode: RouteTravelMode,
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
    ): RouteSearchData {
        validateRouteSearchRequest(
            originResolution = originResolution,
            destinationResolution = destinationResolution,
        )
        val query =
            buildQuery(
                originResolution = originResolution,
                destinationResolution = destinationResolution,
                mode = mode,
            )
        val searchData =
            when (mode) {
                RouteTravelMode.WALK -> routeRepository.getRouteSearchData(query)
                RouteTravelMode.TRANSIT -> routeRepository.getTransitRouteSearchData(query)
            }
        latestSearchDataByMode = latestSearchDataByMode + (mode to searchData)
        return searchData
    }

    private fun determineDefaultTravelMode(walkSearchData: RouteSearchData): RouteTravelMode {
        val safeWalkRoute = walkSearchData.findRoute(RouteOption.SAFE) ?: return RouteTravelMode.WALK
        return if (safeWalkRoute.summary.distanceMeters > WALK_TO_TRANSIT_THRESHOLD_METERS) {
            RouteTravelMode.TRANSIT
        } else {
            RouteTravelMode.WALK
        }
    }

    private fun applyModeLoadFailure(
        mode: RouteTravelMode,
        selectedOption: RouteOption,
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
        throwable: Throwable,
    ) {
        if (throwable is CancellationException) throw throwable
        latestSearchDataByMode = latestSearchDataByMode - mode
        val errorMessage = throwable.toRouteLoadErrorMessage()
        mutableUiState.update { state ->
            state.copy(
                isLoading = false,
                loadErrorMessage = errorMessage,
                loadNoticeMessage = null,
                loadDebugMessage = throwable.toRouteLoadDebugMessage(mode),
                originState = originResolution.originState,
                originStatus = originResolution.originStatus,
                origin = originResolution.originUiState,
                destination = destinationResolution.destinationUiState,
                destinationHandoffState = destinationResolution.handoffState,
                destinationFallbackMessage = destinationResolution.fallbackMessage,
                isUsingFallbackDestination = destinationResolution.isUsingFallbackDestination,
                selectedTravelMode = mode,
                pendingTravelMode = null,
                selectedOption = selectedOption,
                optionCards = emptyList(),
                selectedRoute = null,
                routePreviewMap =
                    throwable.toRoutePreviewFailureMapUiState(
                        originCoordinate = originResolution.routeOrigin.coordinate,
                        destinationResolution = destinationResolution,
                        message = errorMessage,
                    ),
                sourceLabel = null,
                cta = errorCtaUiState(),
                ctaAcknowledged = false,
            )
        }
    }

    private fun Throwable.toRouteLoadErrorMessage(): String =
        when (this) {
            is RouteRequestValidationException -> message
            is RouteApiException ->
                when {
                    status == ROUTE_STATUS_SAME_ENDPOINT -> ROUTE_SAME_ENDPOINT_ERROR_MESSAGE

                    status == ROUTE_STATUS_NO_ROUTE -> ROUTE_NO_ROUTE_ERROR_MESSAGE

                    status == ROUTE_STATUS_MISSING_SESSION ||
                        status == ROUTE_STATUS_AUTHENTICATION_FAILED ||
                        httpStatusCode == HTTP_UNAUTHORIZED ->
                        ROUTE_AUTH_REQUIRED_ERROR_MESSAGE

                    httpStatusCode == HTTP_REQUEST_TIMEOUT ||
                        httpStatusCode == HTTP_GATEWAY_TIMEOUT ||
                        failureKind == RouteFailureKind.CLIENT_TIMEOUT ->
                        ROUTE_TIMEOUT_ERROR_MESSAGE

                    failureKind == RouteFailureKind.UNKNOWN_HOST ||
                        failureKind == RouteFailureKind.CONNECTION_FAILURE ||
                        failureKind == RouteFailureKind.NETWORK_IO ->
                        ROUTE_NETWORK_ERROR_MESSAGE

                    else -> DEFAULT_ROUTE_LOAD_ERROR_MESSAGE
                }

            else -> DEFAULT_ROUTE_LOAD_ERROR_MESSAGE
        }

    private fun Throwable.toRoutePreviewFailureMapUiState(
        originCoordinate: GeoCoordinate,
        destinationResolution: RouteDestinationResolution,
        message: String,
    ): RoutePreviewMapUiState =
        when {
            this is RouteApiException && status == ROUTE_STATUS_NO_ROUTE ->
                RoutePreviewMapUiState(
                    status = RoutePreviewMapStatus.NO_ROUTE,
                    originCoordinate = originCoordinate,
                    destinationCoordinate = destinationResolution.routeDestination.coordinate,
                    fallbackMessage = message,
                )

            else ->
                errorRoutePreviewMapUiState(
                    originCoordinate = originCoordinate,
                    destinationResolution = destinationResolution,
                    message = message,
                )
        }

    private fun buildPendingRouteLoadDebugMessage(mode: RouteTravelMode): String =
        listOf(
            "mode=${mode.name}",
            "path=${mode.routeSearchPath()}",
            "result=pending",
        ).joinToString(separator = "\n")

    private fun combineRouteLoadDebugMessages(
        primary: String?,
        secondary: String?,
    ): String? =
        listOfNotNull(
            primary?.takeIf(String::isNotBlank),
            secondary?.takeIf(String::isNotBlank),
        ).takeIf { messages -> messages.isNotEmpty() }?.joinToString(separator = "\n\n")

    private fun RouteSearchData.toRouteLoadDebugMessage(mode: RouteTravelMode): String =
        buildList {
            add("mode=${mode.name}")
            add("path=${mode.routeSearchPath()}")
            add("result=success")
            add("layer=${if (source.isFromCache) "CACHE" else "REMOTE"}")
            add("source=${source.type.name}")
            add("fromCache=${source.isFromCache}")
            searchId?.takeIf(String::isNotBlank)?.let { resolvedSearchId ->
                add("searchId=$resolvedSearchId")
            }
        }.joinToString(separator = "\n")

    private fun Throwable.toRouteLoadDebugMessage(mode: RouteTravelMode): String =
        when (this) {
            is RouteRequestValidationException ->
                buildList {
                    add("mode=${mode.name}")
                    add("path=${mode.routeSearchPath()}")
                    add("result=failure")
                    add("layer=CLIENT")
                    add("validation=${validation.name}")
                    add("message=$message")
                }.joinToString(separator = "\n")

            is RouteApiException ->
                buildList {
                    add("mode=${mode.name}")
                    add("path=${mode.routeSearchPath()}")
                    add("result=failure")
                    add("layer=${routeFailureLayer()}")
                    add("failureKind=${failureKind.name}")
                    add("httpStatus=$httpStatusCode")
                    if (status.isNotBlank()) {
                        add("status=$status")
                    }
                    if (message.isNotBlank()) {
                        add("message=$message")
                    }
                }.joinToString(separator = "\n")

            else ->
                listOf(
                    "mode=${mode.name}",
                    "path=${mode.routeSearchPath()}",
                    "result=failure",
                    "layer=UNKNOWN",
                    "throwable=${this::class.simpleName.orEmpty()}",
                    message?.takeIf(String::isNotBlank)?.let { resolvedMessage -> "message=$resolvedMessage" },
                ).filterNotNull().joinToString(separator = "\n")
        }

    private fun RouteApiException.routeFailureLayer(): String =
        if (status == ROUTE_STATUS_MISSING_SESSION || status == ROUTE_STATUS_AUTHENTICATION_FAILED) {
            "AUTH_GATE"
        } else {
            "REMOTE"
        }

    private fun RouteTravelMode.routeSearchPath(): String =
        when (this) {
            RouteTravelMode.WALK -> ROUTE_SEARCH_WALK_PATH
            RouteTravelMode.TRANSIT -> ROUTE_SEARCH_TRANSIT_PATH
        }

    private fun validateRouteSearchRequest(
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
    ) {
        if (destinationResolution.handoffState != RouteDestinationHandoffState.DIRECT) {
            return
        }
        if (originResolution.routeOrigin.coordinate == destinationResolution.routeDestination.coordinate) {
            throw RouteRequestValidationException(
                validation = RouteRequestValidation.SAME_ENDPOINT,
                message = ROUTE_SAME_ENDPOINT_ERROR_MESSAGE,
            )
        }
    }

    private fun rememberSuccessfulAutomaticOrigin(originResolution: RouteOriginResolution) {
        lastSuccessfulAutoOrigin =
            if (destinationSelectionRepository.selectedOrigin.value == null) {
                originResolution.routeOrigin
            } else {
                null
            }
    }

    private fun shouldReloadForAutomaticOriginUpdate(
        previousOrigin: RouteWaypoint,
        currentOrigin: RouteWaypoint,
        snapshot: LocationSnapshot,
    ): Boolean {
        if (previousOrigin.coordinate == currentOrigin.coordinate) {
            return false
        }
        val movementMeters = haversineDistanceMeters(previousOrigin.coordinate, currentOrigin.coordinate)
        val significanceThresholdMeters =
            maxOf(
                AUTOMATIC_ORIGIN_RELOAD_THRESHOLD_METERS,
                snapshot.accuracyMeters?.toDouble() ?: 0.0,
            )
        return movementMeters >= significanceThresholdMeters
    }

    private fun selectedOptionForMode(
        mode: RouteTravelMode,
        requestedOption: RouteOption? = null,
    ): RouteOption {
        val resolvedOption = requestedOption ?: selectedOptionByMode[mode] ?: defaultSelectedOption(mode)
        rememberSelectedOption(mode, resolvedOption)
        return resolvedOption
    }

    private fun rememberSelectedOption(
        mode: RouteTravelMode,
        option: RouteOption,
    ) {
        selectedOptionByMode = selectedOptionByMode + (mode to option)
    }

    private fun currentOriginResolution(routeOrigin: RouteWaypoint): RouteOriginResolution =
        RouteOriginResolution(
            routeOrigin = routeOrigin,
            originUiState = mutableUiState.value.origin,
            originState = mutableUiState.value.originState,
            originStatus = mutableUiState.value.originStatus,
        )

    private fun applyNavigationStartFailure() {
        mutableUiState.update { state ->
            state.copy(
                cta = errorCtaUiState(),
                ctaAcknowledged = false,
            )
        }
    }

    private fun buildUiState(
        searchData: RouteSearchData,
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
        selectedTravelMode: RouteTravelMode,
        requestedOption: RouteOption,
        ctaAcknowledged: Boolean,
    ): RouteSettingUiState {
        val availableRoutes = searchData.routes.sortedBy { route -> route.routeOption.routeSortOrder() }
        val resolvedOrigin = originResolution.originUiState
        val resolvedDestination = destinationLocationUiState(searchData.result.destination)
        val resolvedOption =
            if (searchData.findRoute(requestedOption) != null) {
                requestedOption
            } else {
                availableRoutes.firstOrNull()?.routeOption ?: requestedOption
            }
        rememberSelectedOption(selectedTravelMode, resolvedOption)
        val selectedRoute =
            searchData.findRoute(resolvedOption)
                ?: availableRoutes.firstOrNull()
        val selectedRouteUiState =
            selectedRoute?.toSelectedRouteUiState(
                destination = resolvedDestination,
            )
        val routePreviewMapUiState =
            selectedRoute.toRoutePreviewMapUiState(
                originCoordinate = resolvedOrigin.coordinate ?: searchData.result.origin.coordinate,
                destinationCoordinate = resolvedDestination.coordinate ?: searchData.result.destination.coordinate,
                destinationHandoffState = destinationResolution.handoffState,
            )

        return RouteSettingUiState(
            isLoading = false,
            loadErrorMessage = null,
            loadNoticeMessage = null,
            loadDebugMessage = searchData.toRouteLoadDebugMessage(selectedTravelMode),
            originState = originResolution.originState,
            originStatus = originResolution.originStatus,
            origin = resolvedOrigin,
            destination = resolvedDestination,
            destinationHandoffState = destinationResolution.handoffState,
            destinationFallbackMessage = destinationResolution.fallbackMessage,
            isUsingFallbackDestination = destinationResolution.isUsingFallbackDestination,
            selectedTravelMode = selectedTravelMode,
            pendingTravelMode = null,
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

    private fun buildPendingTransitUiState(
        walkSearchData: RouteSearchData,
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
        selectedOption: RouteOption,
    ): RouteSettingUiState =
        RouteSettingUiState(
            isLoading = true,
            loadErrorMessage = null,
            loadNoticeMessage = TRANSIT_LOADING_NOTICE_MESSAGE,
            loadDebugMessage =
                combineRouteLoadDebugMessages(
                    primary = walkSearchData.toRouteLoadDebugMessage(RouteTravelMode.WALK),
                    secondary = buildPendingRouteLoadDebugMessage(RouteTravelMode.TRANSIT),
                ),
            originState = originResolution.originState,
            originStatus = originResolution.originStatus,
            origin = originResolution.originUiState,
            destination = destinationResolution.destinationUiState,
            destinationHandoffState = destinationResolution.handoffState,
            destinationFallbackMessage = destinationResolution.fallbackMessage,
            isUsingFallbackDestination = destinationResolution.isUsingFallbackDestination,
            selectedTravelMode = RouteTravelMode.TRANSIT,
            pendingTravelMode = RouteTravelMode.TRANSIT,
            selectedOption = selectedOption,
            optionCards = emptyList(),
            selectedRoute = null,
            routePreviewMap =
                loadingRoutePreviewMapUiState(
                    originCoordinate = originResolution.routeOrigin.coordinate,
                    destinationResolution = destinationResolution,
                ),
            sourceLabel = null,
            cta = loadingCtaUiState(),
            ctaAcknowledged = false,
        )

    private fun originLocationUiState(
        origin: RouteWaypoint = DEFAULT_ORIGIN,
        addressFallback: String? = DEFAULT_ORIGIN_SUPPORTING_TEXT,
    ): RouteLocationUiState = origin.toLocationUiState(addressFallback = addressFallback)

    private fun destinationLocationUiState(destination: RouteWaypoint): RouteLocationUiState =
        destination.toLocationUiState(addressFallback = DEFAULT_DESTINATION_ADDRESS_FALLBACK)

    private fun buildQuery(
        originResolution: RouteOriginResolution,
        destinationResolution: RouteDestinationResolution,
        mode: RouteTravelMode,
    ): RouteSearchQuery =
        RouteSearchQuery(
            origin = originResolution.routeOrigin,
            destination = destinationResolution.routeDestination,
            requestedOptions =
                when (mode) {
                    RouteTravelMode.WALK -> WALK_ROUTE_OPTIONS
                    RouteTravelMode.TRANSIT -> TRANSIT_ROUTE_OPTIONS
                },
        )

    private suspend fun resolveOrigin(
        selectedOrigin: PlaceDestination?,
        locationSnapshot: LocationSnapshot?,
    ): RouteOriginResolution =
        selectedOrigin
            ?.toRouteWaypointOrNull()
            ?.let { routeOrigin ->
                RouteOriginResolution(
                    routeOrigin = routeOrigin,
                    originUiState = originLocationUiState(routeOrigin),
                    originState = RouteOriginState.MANUAL_SELECTION,
                    originStatus = null,
                )
            }
            ?: locationSnapshot?.let { snapshot ->
                resolveCurrentLocationOrigin(
                    routeOrigin = snapshot.toRouteWaypoint(),
                )
            }
            ?: if (hasStartedActiveLocationUpdates) {
                RouteOriginResolution(
                    routeOrigin = DEFAULT_ORIGIN,
                    originUiState =
                        RouteLocationUiState(
                            placeId = FALLBACK_ORIGIN_PLACE_ID,
                            name = DEFAULT_ORIGIN_LABEL,
                            supportingText = CURRENT_LOCATION_UNAVAILABLE_SUPPORTING_TEXT,
                            coordinate = DEFAULT_ORIGIN.coordinate,
                            metadataLabel = null,
                        ),
                    originState = RouteOriginState.CURRENT_LOCATION_UNAVAILABLE,
                    originStatus =
                        RouteOriginStatusUiState(
                            label = ORIGIN_STATUS_LOCATION_REQUIRED,
                            tone = RouteOriginStatusTone.WARNING,
                        ),
                )
            } else {
                RouteOriginResolution(
                    routeOrigin = DEFAULT_ORIGIN,
                    originUiState =
                        RouteLocationUiState(
                            placeId = FALLBACK_ORIGIN_PLACE_ID,
                            name = CURRENT_LOCATION_LOADING_LABEL,
                            supportingText = null,
                            coordinate = DEFAULT_ORIGIN.coordinate,
                            metadataLabel = null,
                        ),
                    originState = RouteOriginState.CURRENT_LOCATION_LOADING,
                    originStatus =
                        RouteOriginStatusUiState(
                            label = ORIGIN_STATUS_LOADING,
                            tone = RouteOriginStatusTone.INFO,
                        ),
                )
            }

    private suspend fun resolveCurrentLocationOrigin(
        routeOrigin: RouteWaypoint,
    ): RouteOriginResolution {
        val resolvedOrigin = resolveCurrentLocationDisplay(routeOrigin)
        return RouteOriginResolution(
            routeOrigin = resolvedOrigin,
            originUiState = originLocationUiState(resolvedOrigin),
            originState = RouteOriginState.CURRENT_LOCATION_RESOLVED,
            originStatus =
                RouteOriginStatusUiState(
                    label = ORIGIN_STATUS_CURRENT_LOCATION,
                    tone = RouteOriginStatusTone.INFO,
                ),
        )
    }

    private suspend fun resolveCurrentLocationDisplay(
        routeOrigin: RouteWaypoint,
    ): RouteWaypoint {
        val repository = placesRepository ?: return routeOrigin
        val detail =
            runCatching {
                repository.getMapTappedPlaceDetail(
                    MapPlaceDetailRequest(
                        latitude = routeOrigin.coordinate.latitude,
                        longitude = routeOrigin.coordinate.longitude,
                        clickType = MapPlaceClickType.ADDRESS,
                    ),
                )
            }.getOrNull()

        return detail?.toCurrentLocationRouteWaypoint(fallback = routeOrigin) ?: routeOrigin
    }

    private fun resolveDestination(selectedDestination: PlaceDestination?): RouteDestinationResolution =
        when {
            selectedDestination == null ->
                RouteDestinationResolution(
                    routeDestination = DEFAULT_DESTINATION,
                    destinationUiState = destinationLocationUiState(DEFAULT_DESTINATION),
                    handoffState = RouteDestinationHandoffState.EMPTY,
                    fallbackMessage = DESTINATION_FALLBACK_EMPTY_MESSAGE_USER,
                )

            else -> {
                val routeDestination = selectedDestination.toRouteWaypointOrNull()
                if (routeDestination == null) {
                    RouteDestinationResolution(
                        routeDestination = DEFAULT_DESTINATION,
                        destinationUiState = destinationLocationUiState(DEFAULT_DESTINATION),
                        handoffState = RouteDestinationHandoffState.INVALID_COORDINATE,
                        fallbackMessage = DESTINATION_FALLBACK_INVALID_COORDINATE_MESSAGE_USER,
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

    private fun RouteCandidate.toSelectedRouteUiState(
        destination: RouteLocationUiState,
        reversePreview: Boolean = false,
    ): RouteSelectedRouteUiState {
        val aggregateFlags = aggregateSafetyFlags()
        val hasUsableDetailSteps = segments.hasUsableDetailSteps()
        val previewPoints =
            if (reversePreview) {
                previewPolyline.points.reversed()
            } else {
                previewPolyline.points
            }

        return RouteSelectedRouteUiState(
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
            previewPoints = previewPoints,
            segmentCount = preview.segmentCount,
            renderableSegmentCount = preview.renderableSegmentCount,
            fallbackSegmentCount = preview.fallbackSegmentCount,
            previewFallbackNotice = preview.fallbackNotice(),
            badges = routeBadges(includeSafePriority = true),
            detailAccessibilityChips = buildDetailAccessibilityChips(),
            detailHighlights = buildDetailHighlights(aggregateFlags),
            detailSteps = buildDetailSteps(destinationName = destination.name, hasUsableDetailSteps = hasUsableDetailSteps),
            detailFallbackMessage = if (hasUsableDetailSteps) null else ROUTE_DETAIL_FALLBACK_MESSAGE,
        )
    }

    private fun RouteCandidate.routeBadges(includeSafePriority: Boolean): List<RouteOptionBadge> {
        val aggregateFlags = aggregateSafetyFlags()

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

    private fun RouteCandidate.aggregateSafetyFlags(): RouteSegmentSafetyFlags =
        segments.fold(RouteSegmentSafetyFlags()) { flags, segment ->
            flags.merge(segment.safetyFlags)
        }

    private fun RouteCandidate.reversedForWaypointSwap(): RouteCandidate =
        copy(
            preview =
                preview.copy(
                    polyline = RoutePolyline(points = preview.polyline.points.reversed()),
                ),
            segments =
                segments.asReversed().mapIndexed { index, segment ->
                    segment.copy(
                        sequence = index + 1,
                        polyline = RoutePolyline(points = segment.polyline.points.reversed()),
                    )
                },
        )

    private fun RouteCandidate.buildDetailAccessibilityChips(): List<RouteDetailChipUiState> =
        buildList {
            val routeBadgeKinds =
                routeBadges(includeSafePriority = false)
                    .map(RouteOptionBadge::toDetailChipKind)
                    .toSet()
            val hasElevator = segments.any { segment -> segment.toRouteDetailStepKind() == RouteDetailStepKind.ELEVATOR }
            val hasConstruction = segments.any { segment -> segment.toRouteDetailStepKind() == RouteDetailStepKind.CONSTRUCTION }

            if (hasElevator) {
                add(RouteDetailChipKind.ELEVATOR.toUiState())
            }
            if (RouteDetailChipKind.STEP_FREE in routeBadgeKinds) {
                add(RouteDetailChipKind.STEP_FREE.toUiState())
            }
            if (hasConstruction) {
                add(RouteDetailChipKind.CONSTRUCTION.toUiState())
            }
            if (RouteDetailChipKind.SIGNAL_CROSSWALK in routeBadgeKinds) {
                add(RouteDetailChipKind.SIGNAL_CROSSWALK.toUiState())
            }
            if (RouteDetailChipKind.UNSIGNALIZED_CROSSWALK in routeBadgeKinds) {
                add(RouteDetailChipKind.UNSIGNALIZED_CROSSWALK.toUiState())
            }
            if (RouteDetailChipKind.CURB_GAP in routeBadgeKinds) {
                add(RouteDetailChipKind.CURB_GAP.toUiState())
            }
            if (RouteDetailChipKind.STAIRS in routeBadgeKinds) {
                add(RouteDetailChipKind.STAIRS.toUiState())
            }
            if (RouteDetailChipKind.AUDIO_SIGNAL in routeBadgeKinds) {
                add(RouteDetailChipKind.AUDIO_SIGNAL.toUiState())
            }
            if (RouteDetailChipKind.BRAILLE_BLOCK in routeBadgeKinds) {
                add(RouteDetailChipKind.BRAILLE_BLOCK.toUiState())
            }
            if (isEmpty()) {
                add(RouteDetailChipKind.PENDING.toUiState())
            }
        }.distinctBy(RouteDetailChipUiState::kind).take(MAX_ROUTE_DETAIL_CHIP_COUNT)

    private fun RouteCandidate.buildDetailHighlights(aggregateFlags: RouteSegmentSafetyFlags): List<RouteDetailHighlightUiState> =
        buildList<RouteDetailHighlightUiState> {
            if (aggregateFlags.hasAudioSignal) {
                add(
                    RouteDetailHighlightUiState(
                        title = DETAIL_HIGHLIGHT_AUDIO_SIGNAL_TITLE,
                        description = DETAIL_HIGHLIGHT_AUDIO_SIGNAL_DESCRIPTION,
                        badgeLabel = DETAIL_HIGHLIGHT_BADGE_SUPPORT,
                        tone = RouteDetailTone.INFO,
                    ),
                )
            }
            if (aggregateFlags.hasBrailleBlock) {
                add(
                    RouteDetailHighlightUiState(
                        title = DETAIL_HIGHLIGHT_BRAILLE_TITLE,
                        description = DETAIL_HIGHLIGHT_BRAILLE_DESCRIPTION,
                        badgeLabel = DETAIL_HIGHLIGHT_BADGE_SUPPORT,
                        tone = RouteDetailTone.INFO,
                    ),
                )
            }
            if (aggregateFlags.hasCrosswalk && !aggregateFlags.hasSignal) {
                add(
                    RouteDetailHighlightUiState(
                        title = DETAIL_HIGHLIGHT_UNSIGNALIZED_TITLE,
                        description = DETAIL_HIGHLIGHT_UNSIGNALIZED_DESCRIPTION,
                        badgeLabel = DETAIL_HIGHLIGHT_BADGE_WARNING,
                        tone = RouteDetailTone.WARNING,
                    ),
                )
            }
            if (aggregateFlags.hasCurbGap) {
                add(
                    RouteDetailHighlightUiState(
                        title = DETAIL_HIGHLIGHT_CURB_GAP_TITLE,
                        description = DETAIL_HIGHLIGHT_CURB_GAP_DESCRIPTION,
                        badgeLabel = DETAIL_HIGHLIGHT_BADGE_WARNING,
                        tone = RouteDetailTone.WARNING,
                    ),
                )
            }
            if (aggregateFlags.hasStairs) {
                add(
                    RouteDetailHighlightUiState(
                        title = DETAIL_HIGHLIGHT_STAIRS_TITLE,
                        description = DETAIL_HIGHLIGHT_STAIRS_DESCRIPTION,
                        badgeLabel = DETAIL_HIGHLIGHT_BADGE_WARNING,
                        tone = RouteDetailTone.WARNING,
                    ),
                )
            }
            if (summary.riskLevel == RouteRiskLevel.HIGH && none { highlight -> highlight.tone == RouteDetailTone.WARNING }) {
                add(
                    RouteDetailHighlightUiState(
                        title = DETAIL_HIGHLIGHT_HIGH_RISK_TITLE,
                        description = DETAIL_HIGHLIGHT_HIGH_RISK_DESCRIPTION,
                        badgeLabel = DETAIL_HIGHLIGHT_BADGE_WARNING,
                        tone = RouteDetailTone.WARNING,
                    ),
                )
            }
        }.take(MAX_ROUTE_DETAIL_HIGHLIGHT_COUNT)

    private fun RouteCandidate.buildDetailSteps(
        destinationName: String,
        hasUsableDetailSteps: Boolean,
    ): List<RouteDetailStepUiState> {
        val steps =
            mutableListOf(
                RouteDetailStepUiState(
                    indexLabel = DETAIL_STEP_INDEX_START,
                    title = DETAIL_STEP_START_TITLE,
                    description = DETAIL_STEP_START_DESCRIPTION,
                    badgeLabel = routeOption.toOptionTitle(),
                    badgeTone = RouteDetailTone.INFO,
                    kind = RouteDetailStepKind.START,
                    tone = RouteDetailTone.INFO,
                ),
            )

        if (hasUsableDetailSteps) {
            steps +=
                segments
                    .sortedBy(RouteSegment::sequence)
                    .mapIndexed { index, segment ->
                        segment.toDetailStepUiState(displayIndex = index + 2)
                    }
        } else {
            steps +=
                RouteDetailStepUiState(
                    indexLabel = DETAIL_STEP_INDEX_FALLBACK,
                    title = DETAIL_STEP_FALLBACK_TITLE,
                    description = ROUTE_DETAIL_FALLBACK_MESSAGE,
                    kind = RouteDetailStepKind.FALLBACK,
                    tone = RouteDetailTone.NEUTRAL,
                )
        }

        steps +=
            RouteDetailStepUiState(
                indexLabel = (steps.size + 1).toStepIndexLabel(),
                title = DETAIL_STEP_ARRIVAL_TITLE,
                description = "$destinationName${DETAIL_STEP_ARRIVAL_SUFFIX}",
                kind = RouteDetailStepKind.ARRIVAL,
                tone = RouteDetailTone.INFO,
            )

        return steps
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
            currentLocationManager: CurrentLocationManager,
            locationPermissionManager: LocationPermissionManager,
            placesRepository: PlacesRepository,
            searchRepository: SearchRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(RouteSettingViewModel::class.java)) {
                        return RouteSettingViewModel(
                            routeRepository = routeRepository,
                            destinationSelectionRepository = destinationSelectionRepository,
                            currentLocationManager = currentLocationManager,
                            locationPermissionManager = locationPermissionManager,
                            placesRepository = placesRepository,
                            searchRepository = searchRepository,
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

private fun LocationSnapshot.toRouteWaypoint(): RouteWaypoint =
    RouteWaypoint(
        name = DEFAULT_ORIGIN_LABEL,
        placeId = CURRENT_LOCATION_ORIGIN_PLACE_ID,
        coordinate = GeoCoordinate(latitude = latitude, longitude = longitude),
    )

private fun MapTappedPlaceDetail.toCurrentLocationRouteWaypoint(
    fallback: RouteWaypoint,
): RouteWaypoint =
    fallback.copy(
        name = name.takeIf(String::isNotBlank) ?: fallback.name,
        address = address.takeIf(String::isNotBlank) ?: fallback.address,
        category = category ?: fallback.category,
    )

private fun RouteWaypoint.toLocationUiState(addressFallback: String?): RouteLocationUiState =
    RouteLocationUiState(
        placeId = placeId,
        name = name.orEmpty(),
        supportingText = address?.takeIf { value -> value.isNotBlank() } ?: addressFallback,
        coordinate = coordinate,
        category = category,
        metadataLabel = buildLocationMetadataLabel(placeId = placeId, category = category),
    )

private fun RouteLocationUiState.toPlaceDestinationOrNull(fallbackPlaceId: String? = placeId): PlaceDestination? {
    val resolvedCoordinate = coordinate ?: return null

    return PlaceDestination(
        placeId = fallbackPlaceId ?: return null,
        name = name.ifBlank { DEFAULT_ORIGIN_LABEL },
        address = supportingText,
        latitude = resolvedCoordinate.latitude,
        longitude = resolvedCoordinate.longitude,
        category = category ?: PlaceCategory.OTHER,
    )
}

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
    reversePolyline: Boolean = false,
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
                polyline =
                    if (reversePolyline) {
                        previewPolyline.points.reversed()
                    } else {
                        previewPolyline.points
                    },
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

        RouteOption.RECOMMENDED ->
            RouteOptionCardPresentation(
                title = OPTION_TITLE_RECOMMENDED,
                description = OPTION_DESCRIPTION_RECOMMENDED,
                highlightLabel = OPTION_HIGHLIGHT_RECOMMENDED,
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

        RouteOption.MIN_TRANSFER ->
            RouteOptionCardPresentation(
                title = OPTION_TITLE_MIN_TRANSFER,
                description = OPTION_DESCRIPTION_MIN_TRANSFER,
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

        RouteOption.MIN_WALK ->
            RouteOptionCardPresentation(
                title = OPTION_TITLE_MIN_WALK,
                description = OPTION_DESCRIPTION_MIN_WALK,
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
        RouteOption.RECOMMENDED -> OPTION_TITLE_RECOMMENDED
        RouteOption.MIN_TRANSFER -> OPTION_TITLE_MIN_TRANSFER
        RouteOption.MIN_WALK -> OPTION_TITLE_MIN_WALK
    }

private fun List<RouteSegment>.hasUsableDetailSteps(): Boolean =
    any { segment ->
        segment.distanceMeters > 0 ||
            segment.guidanceMessage.isNotBlank() ||
            segment.safetyFlags != RouteSegmentSafetyFlags()
    }

private fun RouteSegment.toDetailStepUiState(displayIndex: Int): RouteDetailStepUiState =
    toRouteDetailStepKind().let { kind ->
        RouteDetailStepUiState(
            indexLabel = displayIndex.toStepIndexLabel(),
            title = detailStepTitle(kind = kind),
            description = detailStepDescription(kind = kind),
            metaLabel = detailStepMetaLabel(kind = kind),
            badgeLabel = detailStepBadgeLabel(kind = kind),
            badgeTone = detailStepBadgeTone(kind = kind),
            kind = kind,
            tone = detailStepTone(kind = kind),
        )
}

private fun RouteSegment.detailStepTitle(kind: RouteDetailStepKind): String =
    when (kind) {
        RouteDetailStepKind.START -> DETAIL_STEP_START_TITLE
        RouteDetailStepKind.STRAIGHT -> DETAIL_STEP_WALK_TITLE
        RouteDetailStepKind.TURN_LEFT -> "좌회전"
        RouteDetailStepKind.TURN_RIGHT -> "우회전"
        RouteDetailStepKind.TACTILE_GUIDE -> DETAIL_STEP_TACTILE_GUIDE_TITLE
        RouteDetailStepKind.CROSSWALK -> DETAIL_STEP_CROSSWALK_TITLE
        RouteDetailStepKind.ELEVATOR -> DETAIL_STEP_ELEVATOR_TITLE
        RouteDetailStepKind.CONSTRUCTION -> DETAIL_STEP_CONSTRUCTION_TITLE
        RouteDetailStepKind.CURB_GAP -> DETAIL_STEP_CURB_GAP_TITLE
        RouteDetailStepKind.STAIRS -> DETAIL_STEP_STAIRS_TITLE
        RouteDetailStepKind.ARRIVAL -> DETAIL_STEP_ARRIVAL_TITLE
        RouteDetailStepKind.FALLBACK -> DETAIL_STEP_FALLBACK_TITLE
    }

private fun RouteSegment.detailStepDescription(kind: RouteDetailStepKind): String {
    val distanceLabel = distanceMeters.toDistanceLabel()
    val guidanceFallback = guidanceMessage.takeIf { message -> message.hasVisibleHangul() }

	if (guidanceFallback != null && guidanceFallback != DEFAULT_GUIDANCE_MESSAGE && kind != RouteDetailStepKind.CROSSWALK) {
		return guidanceFallback
	}

    return when (kind) {
        RouteDetailStepKind.START -> DETAIL_STEP_START_DESCRIPTION
        RouteDetailStepKind.STRAIGHT ->
            if (distanceMeters > 0) {
                "$distanceLabel 정도 직진으로 이동하세요."
            } else {
                DETAIL_STEP_GENERIC_DESCRIPTION
            }

        RouteDetailStepKind.TURN_LEFT ->
            if (distanceMeters > 0) {
                "$distanceLabel 정도 이동 후 왼쪽 방향으로 이동하세요."
            } else {
                "왼쪽 방향으로 이동하세요."
            }

        RouteDetailStepKind.TURN_RIGHT ->
            if (distanceMeters > 0) {
                "$distanceLabel 정도 이동 후 오른쪽 방향으로 이동하세요."
            } else {
                "오른쪽 방향으로 이동하세요."
            }

        RouteDetailStepKind.TACTILE_GUIDE ->
            "점자블록 유도선을 따라 주변 보행 흐름을 유지하며 이동하세요."

        RouteDetailStepKind.CROSSWALK ->
            when {
                safetyFlags.hasAudioSignal ->
                    "음향신호기 안내를 확인한 뒤 횡단보도를 건너세요."
                safetyFlags.hasSignal ->
                    "신호를 확인한 뒤 횡단보도를 건너세요."
                else ->
                    "주변 차량을 먼저 확인한 뒤 횡단보도를 조심해서 건너세요."
            }

        RouteDetailStepKind.ELEVATOR -> "안내된 엘리베이터를 이용해 다음 구간으로 이동하세요."
        RouteDetailStepKind.CONSTRUCTION -> "공사로 통로가 좁을 수 있어 주변을 확인하며 지나가세요."
        RouteDetailStepKind.CURB_GAP -> "연석 단차가 있어 속도를 줄이고 바퀴 각도를 맞춰 이동하세요."
        RouteDetailStepKind.STAIRS -> "계단이 포함된 구간이어서 보조가 필요할 수 있습니다."
        RouteDetailStepKind.ARRIVAL -> DETAIL_STEP_GENERIC_DESCRIPTION
        RouteDetailStepKind.FALLBACK -> ROUTE_DETAIL_FALLBACK_MESSAGE
    }
}

private fun RouteSegment.detailStepMetaLabel(kind: RouteDetailStepKind): String? =
    when (kind) {
        RouteDetailStepKind.START,
        RouteDetailStepKind.ARRIVAL,
        RouteDetailStepKind.FALLBACK,
            -> null

        else ->
            if (distanceMeters > 0) {
                distanceMeters.toDistanceLabel()
            } else {
                null
            }
    }

private fun RouteSegment.detailStepBadgeLabel(kind: RouteDetailStepKind): String? =
    when (kind) {
        RouteDetailStepKind.START,
        RouteDetailStepKind.STRAIGHT,
        RouteDetailStepKind.TURN_LEFT,
        RouteDetailStepKind.TURN_RIGHT,
        RouteDetailStepKind.ARRIVAL,
        RouteDetailStepKind.FALLBACK,
            -> null

        RouteDetailStepKind.TACTILE_GUIDE -> DETAIL_STEP_BADGE_TACTILE_GUIDE
        RouteDetailStepKind.CROSSWALK ->
            when {
                safetyFlags.hasAudioSignal -> DETAIL_STEP_BADGE_AUDIO_SIGNAL
                !safetyFlags.hasSignal -> DETAIL_STEP_BADGE_WARNING
                else -> DETAIL_STEP_BADGE_CROSSWALK
            }

        RouteDetailStepKind.ELEVATOR -> DETAIL_STEP_BADGE_ELEVATOR
        RouteDetailStepKind.CONSTRUCTION -> DETAIL_STEP_BADGE_CONSTRUCTION
        RouteDetailStepKind.CURB_GAP -> DETAIL_STEP_BADGE_CURB_GAP
        RouteDetailStepKind.STAIRS -> DETAIL_STEP_BADGE_WARNING
    }

private fun RouteSegment.detailStepBadgeTone(kind: RouteDetailStepKind): RouteDetailTone? =
    when (kind) {
        RouteDetailStepKind.START,
        RouteDetailStepKind.STRAIGHT,
        RouteDetailStepKind.TURN_LEFT,
        RouteDetailStepKind.TURN_RIGHT,
        RouteDetailStepKind.ARRIVAL,
        RouteDetailStepKind.FALLBACK,
            -> null

        RouteDetailStepKind.TACTILE_GUIDE,
        RouteDetailStepKind.ELEVATOR,
            -> RouteDetailTone.INFO

        RouteDetailStepKind.CROSSWALK ->
            if (safetyFlags.hasAudioSignal) {
                RouteDetailTone.INFO
            } else {
                RouteDetailTone.WARNING
            }

        RouteDetailStepKind.CONSTRUCTION,
        RouteDetailStepKind.CURB_GAP,
        RouteDetailStepKind.STAIRS,
            -> RouteDetailTone.WARNING
    }

private fun RouteSegment.detailStepTone(kind: RouteDetailStepKind): RouteDetailTone =
    when (kind) {
        RouteDetailStepKind.START,
        RouteDetailStepKind.ELEVATOR,
        RouteDetailStepKind.TACTILE_GUIDE,
        RouteDetailStepKind.ARRIVAL,
            -> RouteDetailTone.INFO

        RouteDetailStepKind.CROSSWALK,
        RouteDetailStepKind.CONSTRUCTION,
        RouteDetailStepKind.CURB_GAP,
        RouteDetailStepKind.STAIRS,
            -> RouteDetailTone.WARNING

        RouteDetailStepKind.STRAIGHT,
        RouteDetailStepKind.TURN_LEFT,
        RouteDetailStepKind.TURN_RIGHT,
        RouteDetailStepKind.FALLBACK,
            -> RouteDetailTone.NEUTRAL
    }

private fun String.hasVisibleHangul(): Boolean = any { character -> character in '\uAC00'..'\uD7A3' }

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

private fun Int.toStepIndexLabel(): String = toString().padStart(2, '0')

private fun loadingCtaUiState(): RouteSettingCtaUiState =
    RouteSettingCtaUiState(
        label = CTA_LABEL_START,
        supportingText = CTA_SUPPORTING_LOADING_USER,
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

private fun RouteOptionBadge.toDetailChipKind(): RouteDetailChipKind =
    when (this) {
        RouteOptionBadge.SAFE_PRIORITY -> RouteDetailChipKind.PENDING
        RouteOptionBadge.STEP_FREE -> RouteDetailChipKind.STEP_FREE
        RouteOptionBadge.AUDIO_SIGNAL -> RouteDetailChipKind.AUDIO_SIGNAL
        RouteOptionBadge.BRAILLE_BLOCK -> RouteDetailChipKind.BRAILLE_BLOCK
        RouteOptionBadge.SIGNAL_CROSSWALK -> RouteDetailChipKind.SIGNAL_CROSSWALK
        RouteOptionBadge.CURB_GAP -> RouteDetailChipKind.CURB_GAP
        RouteOptionBadge.UNSIGNALIZED_CROSSWALK -> RouteDetailChipKind.UNSIGNALIZED_CROSSWALK
    }

private fun RouteDetailChipKind.toUiState(): RouteDetailChipUiState =
    when (this) {
        RouteDetailChipKind.STEP_FREE ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_STEP_FREE,
                kind = this,
                tone = RouteDetailTone.INFO,
            )

        RouteDetailChipKind.ELEVATOR ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_ELEVATOR,
                kind = this,
                tone = RouteDetailTone.INFO,
            )

        RouteDetailChipKind.AUDIO_SIGNAL ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_AUDIO_SIGNAL,
                kind = this,
                tone = RouteDetailTone.INFO,
            )

        RouteDetailChipKind.BRAILLE_BLOCK ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_BRAILLE_BLOCK,
                kind = this,
                tone = RouteDetailTone.INFO,
            )

        RouteDetailChipKind.CONSTRUCTION ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_CONSTRUCTION,
                kind = this,
                tone = RouteDetailTone.WARNING,
            )

        RouteDetailChipKind.SIGNAL_CROSSWALK ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_SIGNAL_CROSSWALK,
                kind = this,
                tone = RouteDetailTone.INFO,
            )

        RouteDetailChipKind.UNSIGNALIZED_CROSSWALK ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_UNSIGNALIZED_CROSSWALK,
                kind = this,
                tone = RouteDetailTone.WARNING,
            )

        RouteDetailChipKind.CURB_GAP ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_CURB_GAP,
                kind = this,
                tone = RouteDetailTone.WARNING,
            )

        RouteDetailChipKind.STAIRS ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_STAIRS,
                kind = this,
                tone = RouteDetailTone.WARNING,
            )

        RouteDetailChipKind.PENDING ->
            RouteDetailChipUiState(
                label = DETAIL_CHIP_PENDING,
                kind = this,
                tone = RouteDetailTone.INFO,
            )
    }

private fun RouteOption.routeSortOrder(): Int =
    when (this) {
        RouteOption.SAFE -> 0
        RouteOption.SHORTEST -> 1
        RouteOption.RECOMMENDED -> 2
        RouteOption.MIN_TRANSFER -> 3
        RouteOption.MIN_WALK -> 4
    }

private fun RouteOption.toTravelMode(): RouteTravelMode =
    when (this) {
        RouteOption.SAFE,
        RouteOption.SHORTEST,
            -> RouteTravelMode.WALK

        RouteOption.RECOMMENDED,
        RouteOption.MIN_TRANSFER,
        RouteOption.MIN_WALK,
            -> RouteTravelMode.TRANSIT
    }

private fun defaultSelectedOptionsByMode(): Map<RouteTravelMode, RouteOption> =
    mapOf(
        RouteTravelMode.WALK to WALK_DEFAULT_SELECTED_OPTION,
        RouteTravelMode.TRANSIT to TRANSIT_DEFAULT_SELECTED_OPTION,
    )

private fun defaultSelectedOption(mode: RouteTravelMode): RouteOption =
    when (mode) {
        RouteTravelMode.WALK -> WALK_DEFAULT_SELECTED_OPTION
        RouteTravelMode.TRANSIT -> TRANSIT_DEFAULT_SELECTED_OPTION
    }

private const val DEFAULT_ORIGIN_LABEL = "현재 위치"
private const val DEFAULT_ORIGIN_SUPPORTING_TEXT = "실시간 위치 연동 전까지 데모 좌표를 출발지로 사용합니다."
private const val CURRENT_LOCATION_ORIGIN_SUPPORTING_TEXT = "GPS 현재 위치를 출발지로 사용 중입니다."
private const val DEFAULT_DESTINATION_ADDRESS_FALLBACK = "주소 정보 없음"
private const val DEFAULT_ROUTE_LOAD_ERROR_MESSAGE = "전체 경로를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
private const val DEFAULT_GUIDANCE_MESSAGE = "선택한 경로를 따라 이동합니다."
private const val CTA_LABEL_START = "길 안내 시작"
private const val CTA_SUPPORTING_READY = "선택한 경로로 길 안내를 시작할 수 있습니다."
private const val CTA_SUPPORTING_ACKNOWLEDGED = "길 안내를 시작하는 중입니다."
private const val CTA_SUPPORTING_WAITING_HANDOFF = "검색 또는 지도에서 목적지를 선택하면 안내 시작을 활성화합니다."
private const val CTA_SUPPORTING_INVALID_HANDOFF = "목적지 좌표를 다시 확인하면 안내 시작을 활성화합니다."
private const val CTA_SUPPORTING_ERROR = "경로 정보를 다시 불러오면 시작 CTA를 활성화할 수 있습니다."
private const val CTA_SUPPORTING_LOADING = "경로 요약을 불러오는 동안 CTA를 잠시 비활성화합니다."
private const val CTA_SUPPORTING_EMPTY = "표시할 경로가 준비되면 시작 CTA를 활성화합니다."
private const val DESTINATION_FALLBACK_EMPTY_MESSAGE = "검색 handoff 전에는 기본 도착지를 유지합니다."
private const val DESTINATION_FALLBACK_INVALID_COORDINATE_MESSAGE = "선택한 목적지 좌표를 확인할 수 없어 기본 도착지로 대체했습니다."
private const val SUMMARY_VALUE_PENDING = "확인 중"
private const val OPTION_TITLE_SAFE = "안전한 길"
private const val OPTION_DESCRIPTION_SAFE = "보행 안전 요소를 우선으로 반영한 추천 경로입니다."
private const val OPTION_TITLE_SHORTEST = "최단거리"
private const val OPTION_DESCRIPTION_SHORTEST = "이동 거리를 줄이는 기준으로 빠른 경로를 비교합니다."
private const val OPTION_TITLE_RECOMMENDED = "추천 경로"
private const val OPTION_DESCRIPTION_RECOMMENDED = "대중교통 기준으로 균형 잡힌 경로를 우선 제공합니다."
private const val OPTION_TITLE_MIN_TRANSFER = "최소 환승"
private const val OPTION_DESCRIPTION_MIN_TRANSFER = "환승 횟수를 줄이는 기준으로 경로를 비교합니다."
private const val OPTION_TITLE_MIN_WALK = "최소 도보"
private const val OPTION_DESCRIPTION_MIN_WALK = "도보 이동을 줄이는 기준으로 경로를 비교합니다."
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
private const val ROUTE_DETAIL_FALLBACK_MESSAGE = "세부 이동 정보는 준비 중입니다. 요약 정보와 주의 구간을 먼저 확인하세요."
private const val DETAIL_CHIP_STEP_FREE = "단차 없음"
private const val DETAIL_CHIP_ELEVATOR = "엘리베이터 있음"
private const val DETAIL_CHIP_AUDIO_SIGNAL = "음향 신호 있음"
private const val DETAIL_CHIP_BRAILLE_BLOCK = "점자블록 있음"
private const val DETAIL_CHIP_CONSTRUCTION = "공사 구간 주의"
private const val DETAIL_CHIP_SIGNAL_CROSSWALK = "신호등 횡단보도"
private const val DETAIL_CHIP_UNSIGNALIZED_CROSSWALK = "무신호 횡단 주의"
private const val DETAIL_CHIP_CURB_GAP = "연석 단차 주의"
private const val DETAIL_CHIP_STAIRS = "계단 포함"
private const val DETAIL_CHIP_PENDING = "상세 정보 확인 중"
private const val DETAIL_HIGHLIGHT_BADGE_SUPPORT = "접근 지원"
private const val DETAIL_HIGHLIGHT_BADGE_WARNING = "주의 구간"
private const val DETAIL_HIGHLIGHT_AUDIO_SIGNAL_TITLE = "음향 신호 횡단보도"
private const val DETAIL_HIGHLIGHT_AUDIO_SIGNAL_DESCRIPTION = "신호와 음성 안내가 있는 횡단보도 구간이 포함되어 있습니다."
private const val DETAIL_HIGHLIGHT_BRAILLE_TITLE = "점자블록 유도 구간"
private const val DETAIL_HIGHLIGHT_BRAILLE_DESCRIPTION = "점자블록 유도선을 따라 이동할 수 있는 구간이 포함되어 있습니다."
private const val DETAIL_HIGHLIGHT_UNSIGNALIZED_TITLE = "무신호 횡단 주의"
private const val DETAIL_HIGHLIGHT_UNSIGNALIZED_DESCRIPTION = "신호등이 없는 횡단보도 구간이 있어 주변 차량을 확인해야 합니다."
private const val DETAIL_HIGHLIGHT_CURB_GAP_TITLE = "연석 단차 주의"
private const val DETAIL_HIGHLIGHT_CURB_GAP_DESCRIPTION = "보도 턱이나 연석 단차가 있어 바퀴와 발끝을 주의해 이동하세요."
private const val DETAIL_HIGHLIGHT_STAIRS_TITLE = "계단 구간 주의"
private const val DETAIL_HIGHLIGHT_STAIRS_DESCRIPTION = "계단이 포함된 구간이 있어 보조가 필요할 수 있습니다."
private const val DETAIL_HIGHLIGHT_HIGH_RISK_TITLE = "주의 구간이 포함된 경로"
private const val DETAIL_HIGHLIGHT_HIGH_RISK_DESCRIPTION = "상대적으로 위험도가 높은 구간이 포함되어 있어 안내를 천천히 확인하세요."
private const val DETAIL_STEP_INDEX_START = "01"
private const val DETAIL_STEP_INDEX_FALLBACK = "02"
private const val DETAIL_STEP_START_TITLE = "출발"
private const val DETAIL_STEP_START_DESCRIPTION = "현재 위치에서 선택한 경로 안내를 시작합니다."
private const val DETAIL_STEP_FALLBACK_TITLE = "세부 경로 확인 중"
private const val DETAIL_STEP_ARRIVAL_TITLE = "도착"
private const val DETAIL_STEP_ARRIVAL_SUFFIX = "에 도착합니다."
private const val DETAIL_STEP_WALK_TITLE = "직진 이동"
private const val DETAIL_STEP_TACTILE_GUIDE_TITLE = "점자블록 따라 이동"
private const val DETAIL_STEP_CROSSWALK_TITLE = "횡단보도 건너기"
private const val DETAIL_STEP_ELEVATOR_TITLE = "엘리베이터 이용"
private const val DETAIL_STEP_CONSTRUCTION_TITLE = "공사 구간 진입"
private const val DETAIL_STEP_CURB_GAP_TITLE = "단차 구간 주의"
private const val DETAIL_STEP_STAIRS_TITLE = "계단 구간 주의"
private const val DETAIL_STEP_GENERIC_DESCRIPTION = "이 구간의 세부 정보는 준비 중입니다."
private const val DETAIL_STEP_META_PENDING = "정보 준비 중"
private const val DETAIL_STEP_BADGE_WARNING = "주의"
private const val DETAIL_STEP_BADGE_AUDIO_SIGNAL = "음향 신호"
private const val DETAIL_STEP_BADGE_TACTILE_GUIDE = "점자블록 유도"
private const val DETAIL_STEP_BADGE_CROSSWALK = "신호 확인"
private const val DETAIL_STEP_BADGE_ELEVATOR = "엘리베이터 있음"
private const val DETAIL_STEP_BADGE_CONSTRUCTION = "공사 구간"
private const val DETAIL_STEP_BADGE_CURB_GAP = "단차 있음"
private const val CURRENT_LOCATION_LOADING_LABEL = "현재 위치 확인 중"
private const val CURRENT_LOCATION_UNAVAILABLE_SUPPORTING_TEXT = "현재 위치를 가져오지 못했어요. 출발지를 직접 선택해 주세요."
private const val ORIGIN_STATUS_MANUAL = "직접 선택"
private const val ORIGIN_STATUS_LOADING = "위치 확인 중"
private const val ORIGIN_STATUS_CURRENT_LOCATION = "현재 위치"
private const val ORIGIN_STATUS_LOCATION_REQUIRED = "위치 확인 필요"
private const val CTA_SUPPORTING_LOADING_USER = "경로 정보를 불러오는 동안 안내 시작 버튼을 잠시 비활성화합니다."
private const val DESTINATION_FALLBACK_EMPTY_MESSAGE_USER = "목적지를 선택하면 경로를 보여드릴게요."
private const val DESTINATION_FALLBACK_INVALID_COORDINATE_MESSAGE_USER = "목적지 정보를 다시 확인한 뒤 경로를 보여드릴게요."
private const val METERS_PER_KILOMETER = 1_000
private const val MAX_ROUTE_BADGE_COUNT = 3
private const val MAX_ROUTE_DETAIL_CHIP_COUNT = 4
private const val MAX_ROUTE_DETAIL_HIGHLIGHT_COUNT = 3
private const val CURRENT_LOCATION_ORIGIN_PLACE_ID = "route-origin-current-location"
private const val FALLBACK_ORIGIN_PLACE_ID = "route-origin-fallback"
private const val WALK_TO_TRANSIT_THRESHOLD_METERS = 750
private const val AUTOMATIC_ORIGIN_RELOAD_THRESHOLD_METERS = 25.0
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_REQUEST_TIMEOUT = 408
private const val HTTP_GATEWAY_TIMEOUT = 504
private const val ROUTE_SEARCH_WALK_PATH = "/routes/search/walk"
private const val ROUTE_SEARCH_TRANSIT_PATH = "/routes/search/transit"
private const val ROUTE_STATUS_SAME_ENDPOINT = "RT4004"
private const val ROUTE_STATUS_NO_ROUTE = "RT4040"
private const val ROUTE_STATUS_MISSING_SESSION = "ROUTE_AUTH_MISSING_SESSION"
private const val ROUTE_STATUS_AUTHENTICATION_FAILED = "ROUTE_AUTHENTICATION_FAILED"
private const val ROUTE_AUTH_REQUIRED_ERROR_MESSAGE = "로그인이 필요해요. 다시 로그인한 뒤 시도해 주세요."
private const val ROUTE_SAME_ENDPOINT_ERROR_MESSAGE = "출발지와 도착지를 다르게 선택해 주세요."
private const val ROUTE_NO_ROUTE_ERROR_MESSAGE = "탐색 가능한 경로가 없어요. 출발지나 도착지를 다시 선택해 주세요."
private const val ROUTE_TIMEOUT_ERROR_MESSAGE = "경로 응답이 늦어지고 있어요. 잠시 후 다시 시도해 주세요."
private const val ROUTE_NETWORK_ERROR_MESSAGE = "네트워크 연결 상태를 확인한 뒤 다시 시도해 주세요."
private const val TRANSIT_LOADING_NOTICE_MESSAGE = "대중교통 경로를 불러오고 있어요."
private const val SECONDS_PER_MINUTE = 60
private val DEFAULT_TRAVEL_MODE = RouteTravelMode.WALK
private val WALK_DEFAULT_SELECTED_OPTION = RouteOption.SAFE
private val TRANSIT_DEFAULT_SELECTED_OPTION = RouteOption.RECOMMENDED
private val WALK_ROUTE_OPTIONS = listOf(RouteOption.SAFE, RouteOption.SHORTEST)
private val TRANSIT_ROUTE_OPTIONS = listOf(RouteOption.RECOMMENDED, RouteOption.MIN_TRANSFER, RouteOption.MIN_WALK)

private data class RouteOptionCardPresentation(
    val title: String,
    val description: String,
    val highlightLabel: String?,
    val metrics: List<RouteOptionCardMetricUiState>,
)

private data class RouteOriginResolution(
    val routeOrigin: RouteWaypoint,
    val originUiState: RouteLocationUiState,
    val originState: RouteOriginState,
    val originStatus: RouteOriginStatusUiState?,
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

private data class RouteReloadRequest(
    val resetSelectedOption: Boolean,
    val force: Boolean,
)

private data class RouteReloadSignature(
    val originPlaceId: String?,
    val originCoordinate: GeoCoordinate,
    val destinationPlaceId: String?,
    val destinationCoordinate: GeoCoordinate,
    val destinationHandoffState: RouteDestinationHandoffState,
)

private enum class RouteRequestValidation {
    SAME_ENDPOINT,
}

private class RouteRequestValidationException(
    val validation: RouteRequestValidation,
    override val message: String,
) : IllegalStateException(message)

private fun RouteReloadRequest?.mergeWith(next: RouteReloadRequest): RouteReloadRequest =
    when (this) {
        null -> next
        else ->
            RouteReloadRequest(
                resetSelectedOption = resetSelectedOption || next.resetSelectedOption,
                force = force || next.force,
            )
    }

private object NoOpCurrentLocationManager : CurrentLocationManager {
    private val mutableLatestLocation = MutableStateFlow<LocationSnapshot?>(null)

    override val latestLocation = mutableLatestLocation.asStateFlow()

    override fun refreshLatestLocation() = Unit

    override fun startLocationUpdates() = Unit

    override fun stopLocationUpdates() = Unit
}
