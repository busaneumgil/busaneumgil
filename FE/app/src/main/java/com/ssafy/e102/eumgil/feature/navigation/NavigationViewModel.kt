package com.ssafy.e102.eumgil.feature.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteBookmarkDraft
import com.ssafy.e102.eumgil.core.model.RouteLeg
import com.ssafy.e102.eumgil.core.model.RouteLegRole
import com.ssafy.e102.eumgil.core.model.RouteLegType
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.repository.RouteTransitRefreshData
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import com.ssafy.e102.eumgil.feature.route.RouteTransitOptionLabelUiState
import java.util.Locale
import kotlinx.coroutines.CancellationException
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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationViewModel(
    private val currentLocationManager: CurrentLocationManager,
    private val bookmarkRepository: BookmarkRepository,
    private val routeRepository: RouteRepository = NoOpRouteRepository,
    initialLowVisionMode: Boolean = false,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<NavigationUiEvent>()
    val uiEvent: SharedFlow<NavigationUiEvent> = mutableUiEvent.asSharedFlow()

    private var initialBriefingRequested = false
    private var hasPendingBriefingPlayback = false
    private var hasPendingInitialBriefing = false
    private var hasAutoPlayedInitialBriefing = false
    private var navigationRequest: RouteNavigationRequest? = null
    private var routeSession: NavigationRouteSession? = null
    private var lastProcessedLocationEpochMillis: Long? = null
    private var deviationState = NavigationDeviationState()
    private var activeSegmentIndex: Int = 0
    private var focusedSegmentIndex: Int = 0
    private var isInspectingSegments: Boolean = false
    private var hasPendingActiveChange: Boolean = false
    private var isExitConfirmDialogVisible: Boolean = false
    private var isTransitRefreshInFlight: Boolean = false
    private var isRerouteInFlight: Boolean = false
    private var isEndNavigationInFlight: Boolean = false
    private var latestLocationCoordinate: GeoCoordinate? = null
    private var latestProgress: NavigationProgressSnapshot? = null
    private var latestRemainingDistanceMeters: Int? = null
    private var latestEstimatedMinutes: Int? = null
    private var latestRemainingMetricsSource: NavigationRemainingMetricsSource =
        NavigationRemainingMetricsSource.ProjectedRoute
    private var latestTransitPresentation: NavigationTransitPresentation? = null
    private var lastSegmentMarkerDebugSummary: String? = null
    private var isLowVisionMode: Boolean = initialLowVisionMode
    private var lowVisionActualMetricsCacheKey: LowVisionActualMetricsKey? = null
    private var lowVisionActualMetricsCache: NavigationRemainingMetrics? = null
    private var lowVisionActualMetricsCacheCoordinate: GeoCoordinate? = null
    private var lowVisionActualMetricsCacheRecordedAtMillis: Long? = null
    private var lowVisionActualMetricsInFlightKey: LowVisionActualMetricsKey? = null
    private var lowVisionActualMetricsFailedKey: LowVisionActualMetricsKey? = null
    private var lowVisionActualMetricsLastAttemptCoordinate: GeoCoordinate? = null
    private var lowVisionActualMetricsLastAttemptRecordedAtMillis: Long? = null
    private var lastLowVisionRouteChangeAlertSegmentIndex: Int? = null
    init {
        collectLocationUpdates()
    }

    fun setLowVisionMode(enabled: Boolean) {
        if (isLowVisionMode == enabled) return
        isLowVisionMode = enabled
        if (!enabled) {
            lowVisionActualMetricsCacheKey = null
            lowVisionActualMetricsCache = null
            lowVisionActualMetricsCacheCoordinate = null
            lowVisionActualMetricsCacheRecordedAtMillis = null
            lowVisionActualMetricsInFlightKey = null
            lowVisionActualMetricsFailedKey = null
            lowVisionActualMetricsLastAttemptCoordinate = null
            lowVisionActualMetricsLastAttemptRecordedAtMillis = null
            lastLowVisionRouteChangeAlertSegmentIndex = null
        }
        publishNavigationState()
    }

    fun bindNavigationRequest(request: RouteNavigationRequest) {
        navigationRequest = request
        routeSession =
            NavigationRouteSession(
                route = request.selectedRoute,
                routeId = request.selectionHandoff?.routeId ?: request.selectedRoute.serverRouteId,
                sessionId = request.selectionHandoff?.sessionId,
            )
        lastProcessedLocationEpochMillis = null
        deviationState = NavigationDeviationState()
        activeSegmentIndex = 0
        focusedSegmentIndex = 0
        isInspectingSegments = false
        hasPendingActiveChange = false
        isExitConfirmDialogVisible = false
        isTransitRefreshInFlight = false
        isRerouteInFlight = false
        isEndNavigationInFlight = false
        latestLocationCoordinate = request.origin.coordinate
        latestProgress = routeSession?.route?.evaluateProgress(request.origin.coordinate)
        val initialRemainingMetrics =
            latestProgress?.let { progress ->
                routeSession?.route?.resolveRemainingMetrics(
                    progress = progress,
                    destination = request.destination.coordinate,
                    useLowVisionWalkingPace = isLowVisionMode,
                )
            }
        latestRemainingDistanceMeters =
            request.selectionHandoff?.initialRemainingDistanceMeters
                ?.takeIf { distanceMeters -> distanceMeters >= 0 }
                ?: initialRemainingMetrics?.distanceMeters
                ?: request.selectedRoute.totalDistanceMeters()
        latestEstimatedMinutes =
            request.selectionHandoff?.initialRemainingDurationSeconds
                ?.takeIf { durationSeconds -> durationSeconds >= 0 }
                ?.toEtaMinutes()
                ?: initialRemainingMetrics?.estimatedMinutes
                ?: request.selectedRoute.summary.estimatedTimeMinutes
        latestRemainingMetricsSource =
            if (request.selectionHandoff?.initialRemainingDistanceMeters != null ||
                request.selectionHandoff?.initialRemainingDurationSeconds != null
            ) {
                NavigationRemainingMetricsSource.ProjectedRoute
            } else {
                initialRemainingMetrics?.source ?: NavigationRemainingMetricsSource.ProjectedRoute
            }
        lowVisionActualMetricsCacheKey = null
        lowVisionActualMetricsCache = null
        lowVisionActualMetricsCacheCoordinate = null
        lowVisionActualMetricsCacheRecordedAtMillis = null
        lowVisionActualMetricsInFlightKey = null
        lowVisionActualMetricsFailedKey = null
        lowVisionActualMetricsLastAttemptCoordinate = null
        lowVisionActualMetricsLastAttemptRecordedAtMillis = null
        lastLowVisionRouteChangeAlertSegmentIndex = null
        latestTransitPresentation =
            routeSession?.resolveTransitPresentation(latestProgress?.activeLegIndex ?: 0)
        initialBriefingRequested = false
        hasPendingBriefingPlayback = false
        hasPendingInitialBriefing = false
        hasAutoPlayedInitialBriefing = false
        syncActiveSegment(latestProgress?.activeSegmentIndex ?: 0)
        publishNavigationState()

        currentLocationManager.startLocationUpdates()
    }

    fun currentRouteDetailRequest(): RouteNavigationRequest? {
        val request = navigationRequest ?: return null
        val currentSession = routeSession
        val currentRoute = currentSession?.route ?: request.selectedRoute
        val currentSelectionHandoff =
            request.selectionHandoff?.let { selectionHandoff ->
                selectionHandoff.copy(
                    routeId = currentSession?.routeId ?: selectionHandoff.routeId,
                    sessionId = currentSession?.sessionId ?: selectionHandoff.sessionId,
                )
            }
        return if (currentRoute == request.selectedRoute && currentSelectionHandoff == request.selectionHandoff) {
            request
        } else {
            request.copy(
                selectedRoute = currentRoute,
                selectionHandoff = currentSelectionHandoff,
            )
        }
    }

    fun currentRouteBookmarkDraft(): RouteBookmarkDraft? =
        navigationRequest?.toRouteBookmarkDraft(routeSession?.route)

    fun currentRatingSessionId(): String? =
        routeSession?.latestEndedSessionId ?: routeSession?.sessionId

    fun onAction(action: NavigationUiAction) {
        when (action) {
            NavigationUiAction.NavigationEntered -> requestInitialBriefingIfNeeded()
            NavigationUiAction.BackClicked -> requestExitNavigationConfirmation()
            NavigationUiAction.RouteDetailClicked -> {
                uiState.value.selectedRouteOption?.let { routeOption ->
                    if (uiState.value.canOpenRouteDetail) {
                        emitUiEvent(NavigationUiEvent.NavigateToRouteDetail(routeOption))
                    }
                }
            }
            NavigationUiAction.ExitNavigationClicked -> requestExitNavigationConfirmation()
            NavigationUiAction.ExitNavigationDismissed -> {
                if (isExitConfirmDialogVisible) {
                    isExitConfirmDialogVisible = false
                    publishNavigationState()
                }
            }
            NavigationUiAction.ConfirmExitNavigationClicked -> {
                if (uiState.value.isExitEnabled) {
                    isExitConfirmDialogVisible = false
                    publishNavigationState()
                    finishNavigation(NavigationUiEvent.NavigateToArrival)
                }
            }
            NavigationUiAction.SaveBookmarkClicked -> {
                if (uiState.value.isExitEnabled) {
                    saveDestinationBookmarkAndNavigate()
                }
            }
            NavigationUiAction.NavigationCompleteClicked -> {
                if (uiState.value.isExitEnabled) {
                    finishNavigation(NavigationUiEvent.NavigateToArrival)
                }
            }
            is NavigationUiAction.SegmentTapped -> onSegmentTapped(action.index)
            NavigationUiAction.ReturnToActiveSegmentClicked -> returnToActiveSegment()
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
        playPendingBriefingIfPossible()
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
        val currentSession = routeSession ?: return

        val currentCoordinate = GeoCoordinate(latitude = snapshot.latitude, longitude = snapshot.longitude)
        latestLocationCoordinate = currentCoordinate
        latestProgress = currentSession.route.evaluateProgress(currentCoordinate)
        val destinationCoordinate = navigationRequest?.destination?.coordinate ?: currentCoordinate

        val progress = latestProgress
        if (progress != null) {
            if (isLowVisionMode && !progress.shouldUseProjectedRemainingMetrics()) {
                applyLowVisionActualRemainingMetrics(
                    current = currentCoordinate,
                    destination = destinationCoordinate,
                    recordedAtEpochMillis = snapshot.recordedAtEpochMillis,
                )
            } else {
                val remainingMetrics =
                    currentSession.route.resolveRemainingMetrics(
                        progress = progress,
                        destination = destinationCoordinate,
                        useLowVisionWalkingPace = isLowVisionMode,
                    )
                latestRemainingDistanceMeters = remainingMetrics.distanceMeters
                latestEstimatedMinutes = remainingMetrics.estimatedMinutes
                latestRemainingMetricsSource = remainingMetrics.source
            }
            maybePlayLowVisionRouteChangeAlert(currentSession.route, progress)
            syncActiveSegment(progress.activeSegmentIndex)
            latestTransitPresentation = currentSession.resolveTransitPresentation(progress.activeLegIndex)
            maybeRefreshTransit(currentSession, progress, snapshot)
            maybeRequestReroute(currentSession, progress, snapshot)
        } else {
            latestRemainingDistanceMeters = currentSession.route.totalDistanceMeters()
            latestEstimatedMinutes = currentSession.route.summary.estimatedTimeMinutes
            latestRemainingMetricsSource = NavigationRemainingMetricsSource.ProjectedRoute
            latestTransitPresentation = currentSession.resolveTransitPresentation(activeLegIndex = 0)
        }

        publishNavigationState()
    }

    private fun applyLowVisionActualRemainingMetrics(
        current: GeoCoordinate,
        destination: GeoCoordinate,
        recordedAtEpochMillis: Long,
    ) {
        val key = LowVisionActualMetricsKey.from(current = current, destination = destination)
        val cachedMetrics = resolveReusableLowVisionActualMetrics(key, current, recordedAtEpochMillis)
        if (cachedMetrics != null) {
            latestRemainingDistanceMeters = cachedMetrics.distanceMeters
            latestEstimatedMinutes = cachedMetrics.estimatedMinutes
            latestRemainingMetricsSource = cachedMetrics.source
            return
        }

        latestRemainingDistanceMeters = null
        latestEstimatedMinutes = null
        latestRemainingMetricsSource = NavigationRemainingMetricsSource.Unavailable

        if (lowVisionActualMetricsInFlightKey == key) {
            return
        }
        if (!shouldRequestLowVisionActualMetrics(current = current, recordedAtEpochMillis = recordedAtEpochMillis)) {
            return
        }
        if (lowVisionActualMetricsFailedKey == key) {
            lowVisionActualMetricsFailedKey = null
        }

        requestLowVisionActualRemainingMetrics(
            key = key,
            current = current,
            destination = destination,
            recordedAtEpochMillis = recordedAtEpochMillis,
        )
    }

    private fun requestLowVisionActualRemainingMetrics(
        key: LowVisionActualMetricsKey,
        current: GeoCoordinate,
        destination: GeoCoordinate,
        recordedAtEpochMillis: Long,
    ) {
        lowVisionActualMetricsInFlightKey = key
        lowVisionActualMetricsLastAttemptCoordinate = current
        lowVisionActualMetricsLastAttemptRecordedAtMillis = recordedAtEpochMillis
        viewModelScope.launch {
            val metrics =
                try {
                    resolveLowVisionActualRemainingMetrics(
                        current = current,
                        destination = destination,
                    )
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    null
                }

            if (lowVisionActualMetricsInFlightKey != key) {
                return@launch
            }
            lowVisionActualMetricsInFlightKey = null

            if (metrics == null) {
                lowVisionActualMetricsFailedKey = key
                publishNavigationState()
                return@launch
            }

            lowVisionActualMetricsFailedKey = null
            lowVisionActualMetricsCacheKey = key
            lowVisionActualMetricsCache = metrics
            lowVisionActualMetricsCacheCoordinate = current
            lowVisionActualMetricsCacheRecordedAtMillis = recordedAtEpochMillis

            val latestCoordinate = latestLocationCoordinate ?: return@launch
            val latestKey = LowVisionActualMetricsKey.from(current = latestCoordinate, destination = destination)
            if (isLowVisionMode && latestKey == key && latestProgress?.shouldUseProjectedRemainingMetrics() == false) {
                latestRemainingDistanceMeters = metrics.distanceMeters
                latestEstimatedMinutes = metrics.estimatedMinutes
                latestRemainingMetricsSource = metrics.source
                publishNavigationState()
            }
        }
    }

    private suspend fun resolveLowVisionActualRemainingMetrics(
        current: GeoCoordinate,
        destination: GeoCoordinate,
    ): NavigationRemainingMetrics? {
        val originWaypoint =
            RouteWaypoint(
                name = "\uD604\uC7AC \uC704\uCE58",
                address = "\uD604\uC7AC \uC704\uCE58",
                coordinate = current,
            )
        val destinationWaypoint =
            RouteWaypoint(
                name = navigationRequest?.destination?.name.orEmpty().ifBlank { "\uBAA9\uC801\uC9C0" },
                placeId = navigationRequest?.destination?.placeId,
                address = navigationRequest?.destination?.address,
                coordinate = destination,
            )
        val walkRoute =
            try {
                routeRepository
                    .getFreshRouteSearchData(
                        RouteSearchQuery(
                            origin = originWaypoint,
                            destination = destinationWaypoint,
                            requestedOptions = LOW_VISION_ACTUAL_WALK_OPTIONS,
                        ),
                    ).selectLowVisionActualRoute(preferredOption = RouteOption.SAFE)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                null
            }
        if (walkRoute != null && walkRoute.summary.distanceMeters <= LOW_VISION_WALKABLE_DISTANCE_THRESHOLD_METERS) {
            return walkRoute.toActualRouteSearchMetrics()
        }

        val transitRoute =
            try {
                routeRepository
                    .getFreshTransitRouteSearchData(
                        RouteSearchQuery(
                            origin = originWaypoint,
                            destination = destinationWaypoint,
                            requestedOptions = LOW_VISION_ACTUAL_TRANSIT_OPTIONS,
                        ),
                    ).selectLowVisionActualRoute(preferredOption = RouteOption.RECOMMENDED)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                null
            }
        return transitRoute?.toActualRouteSearchMetrics()
    }

    private fun resolveReusableLowVisionActualMetrics(
        key: LowVisionActualMetricsKey,
        current: GeoCoordinate,
        recordedAtEpochMillis: Long,
    ): NavigationRemainingMetrics? {
        val cacheKey = lowVisionActualMetricsCacheKey ?: return null
        val cachedMetrics = lowVisionActualMetricsCache ?: return null
        if (!cacheKey.hasSameDestinationAs(key)) return null

        val cachedCoordinate = lowVisionActualMetricsCacheCoordinate ?: return null
        val cachedAt = lowVisionActualMetricsCacheRecordedAtMillis ?: return null
        if (recordedAtEpochMillis - cachedAt > LOW_VISION_ACTUAL_METRICS_CACHE_MAX_AGE_MILLIS) {
            return null
        }
        if (haversineDistanceMeters(cachedCoordinate, current) > LOW_VISION_ACTUAL_METRICS_REUSE_DISTANCE_METERS) {
            return null
        }
        return cachedMetrics
    }

    private fun shouldRequestLowVisionActualMetrics(
        current: GeoCoordinate,
        recordedAtEpochMillis: Long,
    ): Boolean {
        val lastAttemptAt = lowVisionActualMetricsLastAttemptRecordedAtMillis ?: return true
        val lastAttemptCoordinate = lowVisionActualMetricsLastAttemptCoordinate ?: return true
        val elapsedMillis = recordedAtEpochMillis - lastAttemptAt
        if (elapsedMillis >= LOW_VISION_ACTUAL_METRICS_MIN_REQUEST_INTERVAL_MILLIS) {
            return true
        }
        return haversineDistanceMeters(lastAttemptCoordinate, current) >= LOW_VISION_ACTUAL_METRICS_MIN_REQUEST_DISTANCE_METERS
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

    private fun onSegmentTapped(index: Int) {
        val request = routeSession?.route ?: navigationRequest?.selectedRoute ?: return
        if (index !in request.segments.indices) return

        focusedSegmentIndex = index
        isInspectingSegments = true
        hasPendingActiveChange = false
        publishNavigationState()
    }

    private fun returnToActiveSegment() {
        if (navigationRequest == null) return

        focusedSegmentIndex = activeSegmentIndex
        isInspectingSegments = false
        hasPendingActiveChange = false
        publishNavigationState()
    }

    private fun publishNavigationState() {
        val request = navigationRequest ?: return
        val runtimeRequest = request.withSelectedRoute(routeSession?.route ?: request.selectedRoute)
        val screenState = runtimeRequest.toScreenState()
        val maxSegmentIndex = runtimeRequest.selectedRoute.segments.lastIndex.coerceAtLeast(0)

        activeSegmentIndex = activeSegmentIndex.coerceIn(0, maxSegmentIndex)
        focusedSegmentIndex = focusedSegmentIndex.coerceIn(0, maxSegmentIndex)

        val mapFocusMode =
            if (isInspectingSegments) {
                NavigationMapFocusMode.FOCUSED
            } else {
                NavigationMapFocusMode.ACTIVE
            }
        val stepCard =
            runtimeRequest.toStepCardUiState(
                screenState = screenState,
                activeSegmentIndex = activeSegmentIndex,
                remainingDistanceMeters = latestRemainingDistanceMeters,
                estimatedMinutes = latestEstimatedMinutes,
                remainingMetricsSource = latestRemainingMetricsSource,
                transitPresentation = latestTransitPresentation,
            )
        val briefingText =
            runtimeRequest.toNavigationBriefingText(
                activeSegmentIndex = activeSegmentIndex,
                fallback = stepCard.toNavigationBriefingText(),
            )
        val mapOverlay =
            runtimeRequest.toMapOverlayUiState(
                currentLocationCoordinate = latestLocationCoordinate,
                activeSegmentIndex = activeSegmentIndex,
                focusedSegmentIndex = focusedSegmentIndex,
                mapFocusMode = mapFocusMode,
            )
        logSegmentMarkerDebugSummary(mapOverlay)

        mutableUiState.update { state ->
            state.copy(
                screenState = screenState,
                selectedRouteOption = runtimeRequest.selectedRoute.routeOption,
                mapPlaceholderTitle =
                    runtimeRequest.selectedRoute.title.toNavigationRouteTitle(
                        runtimeRequest.selectedRoute.routeOption,
                    ),
                mapPlaceholderDescription = runtimeRequest.toMapPlaceholderDescription(screenState),
                mapOverlay = mapOverlay,
                segmentSync =
                    runtimeRequest.toSegmentSyncUiState(
                        activeSegmentIndex = activeSegmentIndex,
                        focusedSegmentIndex = focusedSegmentIndex,
                        isInspectingSegments = isInspectingSegments,
                        hasPendingActiveChange = hasPendingActiveChange,
                        mapFocusMode = mapFocusMode,
                        transitPresentation = latestTransitPresentation,
                    ),
                focusedSegmentCard =
                    runtimeRequest.toFocusedSegmentCardUiState(
                        focusedSegmentIndex = focusedSegmentIndex,
                        transitPresentation = latestTransitPresentation,
                    ),
                pendingActiveChangeLabel =
                    if (hasPendingActiveChange) {
                        PENDING_ACTIVE_SEGMENT_LABEL
                    } else {
                        null
                    },
                stepCard = stepCard,
                exitCta = screenState.toExitCtaUiState(),
                isExitConfirmDialogVisible = isExitConfirmDialogVisible,
                tts =
                    state.tts.copy(
                        briefingText = briefingText,
                        fallbackMessage = state.tts.toFallbackMessage(),
                    ),
            )
        }
    }

    private fun logSegmentMarkerDebugSummary(mapOverlay: NavigationMapOverlayUiState) {
        val summary = createNavigationSegmentMarkerDebugSummary(mapOverlay)
        if (summary == lastSegmentMarkerDebugSummary) return
        lastSegmentMarkerDebugSummary = summary
        println("SegmentMarkerTrace[NavigationViewModel] $summary")
    }

    private fun syncActiveSegment(nextActiveSegmentIndex: Int) {
        if (nextActiveSegmentIndex == activeSegmentIndex) return

        activeSegmentIndex = nextActiveSegmentIndex
        if (isInspectingSegments) {
            hasPendingActiveChange = focusedSegmentIndex != activeSegmentIndex
        } else {
            focusedSegmentIndex = activeSegmentIndex
            hasPendingActiveChange = false
        }
    }

    private fun maybePlayLowVisionRouteChangeAlert(
        route: RouteCandidate,
        progress: NavigationProgressSnapshot,
    ) {
        if (!isLowVisionMode) return
        val nextSegmentIndex = progress.activeSegmentIndex + 1
        if (nextSegmentIndex !in route.segments.indices) return
        if (lastLowVisionRouteChangeAlertSegmentIndex == nextSegmentIndex) return
        if (!uiState.value.tts.isEnabled) return

        val distanceToBoundaryMeters = route.distanceToNextSegmentBoundaryMeters(progress) ?: return
        if (distanceToBoundaryMeters !in 0..LOW_VISION_ROUTE_CHANGE_ALERT_DISTANCE_METERS) return

        lastLowVisionRouteChangeAlertSegmentIndex = nextSegmentIndex
        emitUiEvent(NavigationUiEvent.PlayRouteChangeAlert)
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
        hasPendingInitialBriefing = true
        requestBriefingPlayback()
    }

    private fun requestExitNavigationConfirmation() {
        if (!uiState.value.isExitEnabled) return
        isExitConfirmDialogVisible = true
        publishNavigationState()
    }

    private fun onVoiceGuidanceToggled(enabled: Boolean) {
        mutableUiState.update { state ->
            val nextTts = state.tts.copy(isEnabled = enabled)
            state.copy(tts = nextTts.copy(fallbackMessage = nextTts.toFallbackMessage()))
        }

        if (enabled) {
            requestBriefingPlayback()
            val briefingText = consumePendingBriefingTextIfPossible()
            if (briefingText != null) {
                emitUiEvents(
                    NavigationUiEvent.SetVoiceGuidanceEnabled(enabled = true),
                    NavigationUiEvent.SpeakBriefing(briefingText),
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
        requestBriefingPlayback()
        val briefingText = consumePendingBriefingTextIfPossible() ?: return
        emitUiEvent(NavigationUiEvent.SpeakBriefing(briefingText))
    }

    private fun requestBriefingPlayback() {
        hasPendingBriefingPlayback = true
    }

    private fun playPendingBriefingIfPossible() {
        val briefingText = consumePendingBriefingTextIfPossible() ?: return
        emitUiEvent(NavigationUiEvent.SpeakBriefing(briefingText))
    }

    private fun consumePendingBriefingTextIfPossible(): String? {
        val tts = uiState.value.tts
        if (!tts.canRequestBriefing) return null
        val shouldAutoPlayInitialBriefing = hasPendingInitialBriefing && !hasAutoPlayedInitialBriefing
        if (!hasPendingBriefingPlayback && !shouldAutoPlayInitialBriefing) return null

        val briefingText =
            tts.briefingText
                .trim()
                .takeIf(String::isNotEmpty)
                ?: return null
        if (shouldAutoPlayInitialBriefing) {
            hasPendingInitialBriefing = false
            hasAutoPlayedInitialBriefing = true
        }
        hasPendingBriefingPlayback = false
        return briefingText
    }

    private fun saveDestinationBookmarkAndNavigate() {
        viewModelScope.launch {
            val bookmark = navigationRequest?.toDestinationBookmarkData()
            val saveResult =
                runCatching {
                    bookmark?.let { pendingBookmark ->
                        bookmarkRepository.saveBookmark(pendingBookmark)
                    }
                }

            saveResult
                .onSuccess {
                    println(
                        "BookmarkSaveTrace[NavigationViewModel] result=success placeId=${bookmark?.placeId.orEmpty()}",
                    )
                    completeNavigation(NavigationUiEvent.NavigateToSavedRoute)
                }.onFailure { throwable ->
                    println(
                        "BookmarkSaveTrace[NavigationViewModel] result=failure placeId=${bookmark?.placeId.orEmpty()} message=${throwable.message.orEmpty()}",
                    )
                }
        }
    }

    private fun finishNavigation(event: NavigationUiEvent) {
        viewModelScope.launch {
            completeNavigation(event)
        }
    }

    private suspend fun completeNavigation(event: NavigationUiEvent) {
        if (isEndNavigationInFlight) return
        isEndNavigationInFlight = true
        currentLocationManager.stopLocationUpdates()

        routeSession
            ?.routeId
            ?.let { routeId ->
                runCatching {
                    routeRepository.endRoute(routeId)
                }.onSuccess { sessionData ->
                    routeSession = routeSession?.withEndedSession(sessionData.sessionId)
                }
            }

        isEndNavigationInFlight = false
        publishNavigationState()
        mutableUiEvent.emit(NavigationUiEvent.StopBriefing)
        mutableUiEvent.emit(event)
    }

    private fun maybeRefreshTransit(
        currentSession: NavigationRouteSession,
        progress: NavigationProgressSnapshot,
        snapshot: LocationSnapshot,
    ) {
        if (isTransitRefreshInFlight) return
        val trigger =
            currentSession.resolveTransitRefreshTrigger(
                progress = progress,
                recordedAtEpochMillis = snapshot.recordedAtEpochMillis,
            ) ?: return

        isTransitRefreshInFlight = true
        viewModelScope.launch {
            runCatching {
                routeRepository.refreshTransit(
                    routeId = trigger.routeId,
                    legSequence = trigger.legSequence,
                )
            }.onSuccess { refreshData ->
                routeSession =
                    routeSession?.withTransitRefresh(
                        legSequence = trigger.legSequence,
                        data = refreshData,
                        requestedAtMillis = snapshot.recordedAtEpochMillis,
                    )
                latestTransitPresentation =
                    routeSession?.resolveTransitPresentation(progress.activeLegIndex)
                publishNavigationState()
            }.onFailure {
                routeSession =
                    routeSession?.copy(
                        lastTransitRefreshAtMillisByLegSequence =
                            currentSession.lastTransitRefreshAtMillisByLegSequence +
                                (trigger.legSequence to snapshot.recordedAtEpochMillis),
                    )
            }
            isTransitRefreshInFlight = false
        }
    }

    private fun maybeRequestReroute(
        currentSession: NavigationRouteSession,
        progress: NavigationProgressSnapshot,
        snapshot: LocationSnapshot,
    ) {
        val accuracyMeters = snapshot.accuracyMeters
        val isOffRoute =
            accuracyMeters != null &&
                accuracyMeters <= REROUTE_MAX_GPS_ACCURACY_METERS &&
                progress.distanceToRouteMeters >= REROUTE_DEVIATION_DISTANCE_METERS
        deviationState =
            deviationState.next(
                isOffRoute = isOffRoute,
                recordedAtEpochMillis = snapshot.recordedAtEpochMillis,
            )
        if (!deviationState.shouldTriggerReroute(snapshot.recordedAtEpochMillis) || isRerouteInFlight) {
            return
        }

        val routeId = currentSession.routeId ?: return
        val currentCoordinate = progress.coordinate
        deviationState = NavigationDeviationState()
        isRerouteInFlight = true
        viewModelScope.launch {
            runCatching {
                routeRepository.reroute(
                    routeId = routeId,
                    currentPoint = currentCoordinate,
                )
            }.onSuccess { rerouteData ->
                rerouteData.route?.let { reroutedRoute ->
                    routeSession = routeSession?.withReroutedRoute(reroutedRoute)
                    latestProgress = reroutedRoute.evaluateProgress(currentCoordinate)
                    val remainingMetrics =
                        latestProgress?.let { progress ->
                            reroutedRoute.resolveRemainingMetrics(
                                progress = progress,
                                destination = navigationRequest?.destination?.coordinate ?: currentCoordinate,
                                useLowVisionWalkingPace = isLowVisionMode,
                            )
                        }
                    latestRemainingDistanceMeters =
                        remainingMetrics?.distanceMeters ?: reroutedRoute.totalDistanceMeters()
                    latestEstimatedMinutes =
                        remainingMetrics?.estimatedMinutes ?: reroutedRoute.summary.estimatedTimeMinutes
                    latestRemainingMetricsSource =
                        remainingMetrics?.source ?: NavigationRemainingMetricsSource.ProjectedRoute
                    latestTransitPresentation =
                        routeSession?.resolveTransitPresentation(latestProgress?.activeLegIndex ?: 0)
                    val reroutedSegmentIndex = latestProgress?.activeSegmentIndex ?: 0
                    activeSegmentIndex = reroutedSegmentIndex
                    focusedSegmentIndex = reroutedSegmentIndex
                    isInspectingSegments = false
                    hasPendingActiveChange = false
                    lastLowVisionRouteChangeAlertSegmentIndex = null
                }
                publishNavigationState()
            }
            isRerouteInFlight = false
        }
    }

    companion object {
        fun provideFactory(
            currentLocationManager: CurrentLocationManager,
            bookmarkRepository: BookmarkRepository,
            routeRepository: RouteRepository,
            isLowVisionMode: Boolean = false,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NavigationViewModel(
                        currentLocationManager = currentLocationManager,
                        bookmarkRepository = bookmarkRepository,
                        routeRepository = routeRepository,
                        initialLowVisionMode = isLowVisionMode,
                    ) as T
            }
    }
}

private data class NavigationRouteSession(
    val route: RouteCandidate,
    val routeId: String? = route.serverRouteId,
    val sessionId: String? = null,
    val latestEndedSessionId: String? = null,
    val transitRefreshByLegSequence: Map<Int, RouteTransitRefreshData> = emptyMap(),
    val lastTransitRefreshAtMillisByLegSequence: Map<Int, Long> = emptyMap(),
) {
    fun withEndedSession(sessionId: String): NavigationRouteSession =
        copy(latestEndedSessionId = sessionId)

    fun withTransitRefresh(
        legSequence: Int,
        data: RouteTransitRefreshData,
        requestedAtMillis: Long,
    ): NavigationRouteSession =
        copy(
            transitRefreshByLegSequence = transitRefreshByLegSequence + (legSequence to data),
            lastTransitRefreshAtMillisByLegSequence =
                lastTransitRefreshAtMillisByLegSequence + (legSequence to requestedAtMillis),
        )

    fun withReroutedRoute(reroutedRoute: RouteCandidate): NavigationRouteSession =
        copy(
            route = reroutedRoute,
            routeId = reroutedRoute.serverRouteId ?: routeId,
            transitRefreshByLegSequence = emptyMap(),
            lastTransitRefreshAtMillisByLegSequence = emptyMap(),
        )

    fun resolveTransitRefreshTrigger(
        progress: NavigationProgressSnapshot,
        recordedAtEpochMillis: Long,
    ): NavigationTransitRefreshTrigger? {
        val routeId = routeId ?: return null
        val activeLeg = route.legs.getOrNull(progress.activeLegIndex) ?: return null
        if (activeLeg.role != RouteLegRole.WALK_TO_TRANSIT) return null

        val transitLeg =
            route.legs
                .drop(progress.activeLegIndex + 1)
                .firstOrNull { leg -> leg.type == RouteLegType.BUS || leg.type == RouteLegType.SUBWAY }
                ?: return null
        val boardingStop = transitLeg.boardingStop ?: return null
        val thresholdMeters =
            when (transitLeg.type) {
                RouteLegType.BUS -> BUS_STOP_REFRESH_DISTANCE_METERS
                RouteLegType.SUBWAY -> SUBWAY_ELEVATOR_REFRESH_DISTANCE_METERS
                RouteLegType.WALK -> return null
            }
        val lastRefreshAt = lastTransitRefreshAtMillisByLegSequence[transitLeg.sequence]
        if (
            lastRefreshAt != null &&
            recordedAtEpochMillis - lastRefreshAt < TRANSIT_REFRESH_COOLDOWN_MILLIS
        ) {
            return null
        }
        val distanceToBoardingStopMeters =
            haversineDistanceMeters(progress.coordinate, boardingStop.coordinate)
        if (distanceToBoardingStopMeters > thresholdMeters) return null

        return NavigationTransitRefreshTrigger(
            routeId = routeId,
            legSequence = transitLeg.sequence,
        )
    }

    fun resolveTransitPresentation(activeLegIndex: Int): NavigationTransitPresentation? {
        val activeLeg = route.legs.getOrNull(activeLegIndex) ?: return null
        val targetLeg =
            when {
                activeLeg.role == RouteLegRole.WALK_TO_TRANSIT ->
                    route.legs
                        .drop(activeLegIndex + 1)
                        .firstOrNull { leg -> leg.type == RouteLegType.BUS || leg.type == RouteLegType.SUBWAY }

                activeLeg.role == RouteLegRole.TRANSIT &&
                    (activeLeg.type == RouteLegType.BUS || activeLeg.type == RouteLegType.SUBWAY) ->
                    activeLeg

                else -> null
            } ?: return null

        val refreshData = transitRefreshByLegSequence[targetLeg.sequence]
        val refreshedArrival =
            refreshData
                ?.transits
                ?.firstOrNull { arrival -> arrival.routeNo == targetLeg.routeNo }
                ?: refreshData?.transits?.firstOrNull()
        val fallbackLaneOption =
            targetLeg.laneOptions.firstOrNull { option -> option.routeNo == targetLeg.routeNo }
                ?: targetLeg.laneOptions.firstOrNull()
        val waitMinutes = refreshedArrival?.remainingMinute ?: fallbackLaneOption?.remainingMinute
        val transitLabel =
            when (targetLeg.type) {
                RouteLegType.BUS -> targetLeg.routeNo?.let { routeNo -> "${routeNo}번 버스" } ?: "버스"
                RouteLegType.SUBWAY -> targetLeg.routeNo ?: "지하철"
                RouteLegType.WALK -> "대중교통"
            }
        val statusLabel =
            when (refreshData?.arrivalStatus) {
                "REALTIME_AVAILABLE" -> "실시간 도착"
                "SCHEDULE_BASED" -> "시간표 기준"
                "NO_CURRENT_ARRIVAL",
                "ARRIVAL_UNKNOWN",
                    -> "도착 정보 없음"

                else -> "도착 정보"
            }
        val supportingText =
            when {
                waitMinutes != null -> "${arrivalBasis(refreshData?.arrivalStatus)} $transitLabel ${waitMinutes}분 후 도착 예정"
                targetLeg.boardingStop != null -> "${targetLeg.boardingStop.name} 승차 정보 없음"
                else -> "승차 정보 없음"
            }

        return NavigationTransitPresentation(
            statusLabel = statusLabel,
            supportingText = supportingText,
            info = targetLeg.toNavigationTransitInfo(arrivalRouteNo = refreshedArrival?.routeNo, arrivalMinutes = waitMinutes),
        )
    }
}

private fun arrivalBasis(arrivalStatus: String?): String =
    when (arrivalStatus) {
        "REALTIME_AVAILABLE" -> "실시간 기준"
        "SCHEDULE_BASED" -> "시간표 기준"
        else -> "도착 정보 기준"
    }

private data class NavigationProgressSnapshot(
    val coordinate: GeoCoordinate,
    val activeSegmentIndex: Int,
    val activeLegIndex: Int,
    val distanceToRouteMeters: Double,
    val distanceAlongRouteMeters: Int,
    val remainingRouteDistanceMeters: Int,
    val remainingDurationSeconds: Int,
)

private data class NavigationRemainingMetrics(
    val distanceMeters: Int,
    val estimatedMinutes: Int,
    val source: NavigationRemainingMetricsSource,
)

private enum class NavigationRemainingMetricsSource {
    ProjectedRoute,
    ActualDirect,
    ActualRouteSearch,
    Unavailable,
}

private data class NavigationDeviationState(
    val consecutiveOffRouteCount: Int = 0,
    val firstOffRouteEpochMillis: Long? = null,
) {
    fun next(
        isOffRoute: Boolean,
        recordedAtEpochMillis: Long,
    ): NavigationDeviationState =
        if (!isOffRoute) {
            NavigationDeviationState()
        } else {
            NavigationDeviationState(
                consecutiveOffRouteCount = consecutiveOffRouteCount + 1,
                firstOffRouteEpochMillis = firstOffRouteEpochMillis ?: recordedAtEpochMillis,
            )
        }

    fun shouldTriggerReroute(recordedAtEpochMillis: Long): Boolean {
        if (consecutiveOffRouteCount >= REROUTE_OFF_ROUTE_CONSECUTIVE_COUNT) {
            return true
        }
        val firstOffRouteAt = firstOffRouteEpochMillis ?: return false
        return recordedAtEpochMillis - firstOffRouteAt >= REROUTE_OFF_ROUTE_DURATION_MILLIS
    }
}

private data class NavigationTransitRefreshTrigger(
    val routeId: String,
    val legSequence: Int,
)

private data class NavigationTransitPresentation(
    val statusLabel: String,
    val supportingText: String,
    val info: NavigationTransitInfoUiState? = null,
)

private fun RouteLeg.toNavigationTransitInfo(
    arrivalRouteNo: String?,
    arrivalMinutes: Int?,
): NavigationTransitInfoUiState? {
    if (type != RouteLegType.BUS && type != RouteLegType.SUBWAY) return null
    val guidanceAction =
        when (type) {
            RouteLegType.BUS -> NavigationGuidanceAction.BUS
            RouteLegType.SUBWAY -> NavigationGuidanceAction.SUBWAY
            RouteLegType.WALK -> return null
        }
    val startName =
        boardingStop?.name?.takeIf(String::isNotBlank)
            ?: if (type == RouteLegType.SUBWAY) "출발역" else "출발 정류장"
    val endName =
        alightingStop?.name?.takeIf(String::isNotBlank)
            ?: if (type == RouteLegType.SUBWAY) "도착역" else "도착 정류장"
    val durationLabel =
        estimatedTimeMinutes?.takeIf { minute -> minute > 0 }?.let { minute -> "${minute}분" }
            ?: durationSeconds?.takeIf { seconds -> seconds > 0 }?.let { seconds -> "${((seconds + 59) / 60).coerceAtLeast(1)}분" }
    val routeNumbers =
        buildList {
            routeNo?.takeIf(String::isNotBlank)?.let(::add)
            addAll(laneOptions.mapNotNull { option -> option.routeNo?.takeIf(String::isNotBlank) })
            arrivalRouteNo?.takeIf(String::isNotBlank)?.let(::add)
        }.distinct()
    val arrivalByRouteNo =
        laneOptions
            .mapNotNull { option ->
                val optionRouteNo = option.routeNo?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val arrivalLabel =
                    option.remainingMinute?.let { minute -> "${minute}분" }
                        ?: option.estimatedTimeMinutes?.let { minute -> "${minute}분" }
                optionRouteNo to arrivalLabel
            }.toMap()
    val options =
        routeNumbers.map { routeNo ->
            RouteTransitOptionLabelUiState(
                typeLabel =
                    when (type) {
                        RouteLegType.SUBWAY -> "지하철"
                        else -> if (isLowFloor == true || laneOptions.any { option -> option.routeNo == routeNo && option.isLowFloor == true }) "저상" else "일반"
                    },
                routeNo = routeNo,
                arrivalLabel =
                    if (routeNo == arrivalRouteNo && arrivalMinutes != null) {
                        "${arrivalMinutes}분"
                    } else {
                        arrivalByRouteNo[routeNo]
                    },
            )
        }.take(NAVIGATION_TRANSIT_OPTION_LABEL_LIMIT)

    return NavigationTransitInfoUiState(
        guidanceAction = guidanceAction,
        startName = startName,
        endName = endName,
        durationLabel = durationLabel,
        optionLabels = options,
    )
}

private data class RoutePolylineProjection(
    val distanceToPolylineMeters: Double,
    val distanceAlongPolylineMeters: Double,
    val totalPolylineDistanceMeters: Double,
)

private data class RouteSegmentProjection(
    val distanceToSegmentMeters: Double,
    val distanceAlongSegmentMeters: Double,
)

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

private fun RouteNavigationRequest.toRouteBookmarkDraft(
    route: RouteCandidate? = selectedRoute,
): RouteBookmarkDraft =
    RouteBookmarkDraft(
        routeId = route?.serverRouteId,
        startLabel = origin.name.orEmpty().ifBlank { "출발지" },
        endLabel = destination.name.orEmpty().ifBlank { "목적지" },
        startPoint = origin.coordinate,
        endPoint = destination.coordinate,
        routeOption = route?.routeOption ?: selectedRoute.routeOption,
        distanceMeters = route?.summary?.distanceMeters?.takeIf { distance -> distance > 0 },
        durationMinutes = route?.summary?.estimatedTimeMinutes?.takeIf { duration -> duration > 0 },
        routeSnapshot = route,
    )

private fun RouteWaypoint.toNavigationDestinationPlaceId(): String =
    "navigation-destination:${coordinate.latitude},${coordinate.longitude}"

private fun RouteNavigationRequest.withSelectedRoute(route: RouteCandidate): RouteNavigationRequest =
    copy(selectedRoute = route)

private fun RouteCandidate.totalDistanceMeters(): Int =
    summary.distanceMeters
        .takeIf { distanceMeters -> distanceMeters > 0 }
        ?: legs.sumOf { leg -> leg.distanceMeters ?: 0 }.takeIf { distanceMeters -> distanceMeters > 0 }
        ?: segments.sumOf(RouteSegment::distanceMeters).takeIf { distanceMeters -> distanceMeters > 0 }
        ?: navigationPolylinePoints().totalPolylineDistanceMeters().roundToInt()

private fun RouteCandidate.totalDurationSeconds(): Int =
    summary.durationSeconds
        ?.takeIf { durationSeconds -> durationSeconds > 0 }
        ?: legs.sumOf { leg -> leg.durationSeconds ?: 0 }.takeIf { durationSeconds -> durationSeconds > 0 }
        ?: summary.estimatedTimeMinutes.takeIf { estimatedTimeMinutes -> estimatedTimeMinutes > 0 }?.times(60)
        ?: 0

private fun RouteCandidate.evaluateProgress(current: GeoCoordinate): NavigationProgressSnapshot? {
    val routePoints = navigationPolylinePoints()
    val projection = projectOntoPolylineMeters(current = current, polyline = routePoints) ?: return null
    val progressRatio =
        if (projection.totalPolylineDistanceMeters <= 0.0) {
            0.0
        } else {
            (projection.distanceAlongPolylineMeters / projection.totalPolylineDistanceMeters).coerceIn(0.0, 1.0)
        }
    val totalDistanceMeters = totalDistanceMeters()
    val totalDurationSeconds = totalDurationSeconds()
    val remainingRatio = (1.0 - progressRatio).coerceIn(0.0, 1.0)

    return NavigationProgressSnapshot(
        coordinate = current,
        activeSegmentIndex = resolveActiveSegmentIndex(progressRatio),
        activeLegIndex = resolveActiveLegIndex(progressRatio),
        distanceToRouteMeters = projection.distanceToPolylineMeters,
        distanceAlongRouteMeters = (totalDistanceMeters * progressRatio).roundToInt().coerceAtLeast(0),
        remainingRouteDistanceMeters =
            (totalDistanceMeters * remainingRatio).roundToInt().coerceAtLeast(0),
        remainingDurationSeconds =
            (totalDurationSeconds * remainingRatio).roundToInt().coerceAtLeast(0),
    )
}

private fun RouteCandidate.resolveRemainingMetrics(
    progress: NavigationProgressSnapshot,
    destination: GeoCoordinate,
    useLowVisionWalkingPace: Boolean,
): NavigationRemainingMetrics =
    resolveRemainingMetrics(
        progress = progress,
        destination = destination,
        directDistanceMeters = haversineDistanceMeters(progress.coordinate, destination).roundToInt().coerceAtLeast(0),
        useLowVisionWalkingPace = useLowVisionWalkingPace,
    )

private fun RouteCandidate.resolveRemainingMetrics(
    progress: NavigationProgressSnapshot,
    destination: GeoCoordinate,
    directDistanceMeters: Int,
    useLowVisionWalkingPace: Boolean,
): NavigationRemainingMetrics {
    if (!progress.shouldUseProjectedRemainingMetrics()) {
        return resolveActualRemainingMetrics(
            current = progress.coordinate,
            destination = destination,
            directDistanceMeters = directDistanceMeters,
            useLowVisionWalkingPace = useLowVisionWalkingPace,
        )
    }

    val projectedDistanceMeters =
        (progress.remainingRouteDistanceMeters + progress.distanceToRouteMeters.roundToInt())
            .coerceAtLeast(0)
    val adjustedDistanceMeters = maxOf(projectedDistanceMeters, directDistanceMeters)
    val adjustedDurationSeconds =
        maxOf(
            progress.remainingDurationSeconds,
            estimateDurationSeconds(
                distanceMeters = adjustedDistanceMeters,
                minimumDurationSeconds = progress.remainingDurationSeconds,
            ),
        )

    return NavigationRemainingMetrics(
        distanceMeters = adjustedDistanceMeters,
        estimatedMinutes = adjustedDurationSeconds.toEtaMinutes(),
        source = NavigationRemainingMetricsSource.ProjectedRoute,
    )
}

private fun RouteCandidate.resolveActualRemainingMetrics(
    current: GeoCoordinate,
    destination: GeoCoordinate,
    directDistanceMeters: Int =
        haversineDistanceMeters(current, destination)
            .roundToInt()
            .coerceAtLeast(0),
    useLowVisionWalkingPace: Boolean = false,
): NavigationRemainingMetrics {
    if (directDistanceMeters <= 0) {
        return NavigationRemainingMetrics(
            distanceMeters = 0,
            estimatedMinutes = 0,
            source = NavigationRemainingMetricsSource.ActualDirect,
        )
    }

    return NavigationRemainingMetrics(
        distanceMeters = directDistanceMeters,
        estimatedMinutes =
            estimateDirectDurationSeconds(
                distanceMeters = directDistanceMeters,
                useLowVisionWalkingPace = useLowVisionWalkingPace,
            ).toEtaMinutes(),
        source = NavigationRemainingMetricsSource.ActualDirect,
    )
}

private fun NavigationProgressSnapshot.shouldUseProjectedRemainingMetrics(): Boolean =
    distanceToRouteMeters <= PROJECTED_PROGRESS_MAX_DISTANCE_METERS

private fun RouteCandidate.estimateDurationSeconds(
    distanceMeters: Int,
    minimumDurationSeconds: Int = MIN_NAVIGATION_DURATION_SECONDS,
): Int {
    val totalDistanceMeters = totalDistanceMeters()
    val totalDurationSeconds = totalDurationSeconds()
    val estimatedDurationSeconds =
        if (totalDistanceMeters > 0 && totalDurationSeconds > 0) {
            ((distanceMeters.toDouble() / totalDistanceMeters) * totalDurationSeconds).roundToInt()
        } else {
            ceil(distanceMeters / FALLBACK_NAVIGATION_SPEED_METERS_PER_SECOND).toInt()
    }
    return estimatedDurationSeconds.coerceAtLeast(minimumDurationSeconds)
}

private fun estimateDirectDurationSeconds(
    distanceMeters: Int,
    useLowVisionWalkingPace: Boolean,
): Int {
    val walkingSpeedMetersPerSecond =
        if (useLowVisionWalkingPace) {
            LOW_VISION_FALLBACK_WALKING_SPEED_METERS_PER_SECOND
        } else {
            FALLBACK_NAVIGATION_SPEED_METERS_PER_SECOND
    }
    return ceil(distanceMeters / walkingSpeedMetersPerSecond).toInt().coerceAtLeast(MIN_NAVIGATION_DURATION_SECONDS)
}

private fun RouteSearchData.selectLowVisionActualRoute(preferredOption: RouteOption): RouteCandidate? =
    findRoute(preferredOption) ?: primaryRoute ?: routes.firstOrNull()

private fun RouteCandidate.toActualRouteSearchMetrics(): NavigationRemainingMetrics {
    val distanceMeters = summary.distanceMeters.coerceAtLeast(0)
    val estimatedMinutes =
        summary.durationSeconds
            ?.takeIf { durationSeconds -> durationSeconds > 0 }
            ?.toEtaMinutes()
            ?: summary.estimatedTimeMinutes.coerceAtLeast(0)
    return NavigationRemainingMetrics(
        distanceMeters = distanceMeters,
        estimatedMinutes = estimatedMinutes,
        source = NavigationRemainingMetricsSource.ActualRouteSearch,
    )
}

private fun RouteCandidate.resolveActiveSegmentIndex(progressRatio: Double): Int =
    resolveActiveIndex(
        progressRatio = progressRatio,
        weights =
            segments.map { segment ->
                segment.polyline.totalDistanceWeight()
                    ?: segment.distanceMeters.toDouble().takeIf { distanceMeters -> distanceMeters > 0 }
                    ?: 1.0
            },
        advanceAtBoundary = true,
    )

private fun RouteCandidate.resolveActiveLegIndex(progressRatio: Double): Int =
    resolveActiveIndex(
        progressRatio = progressRatio,
        weights =
            legs.map { leg ->
                leg.polyline.totalDistanceWeight()
                    ?: leg.distanceMeters?.toDouble()?.takeIf { distanceMeters -> distanceMeters > 0 }
                    ?: 1.0
            },
        advanceAtBoundary = false,
    )

private fun resolveActiveIndex(
    progressRatio: Double,
    weights: List<Double>,
    advanceAtBoundary: Boolean,
): Int {
    if (weights.isEmpty()) return 0

    val sanitizedWeights =
        weights.map { weight ->
            if (weight > 0.0) {
                weight
            } else {
                1.0
            }
        }
    val totalWeight = sanitizedWeights.sum().takeIf { total -> total > 0.0 } ?: sanitizedWeights.size.toDouble()
    var cumulativeRatio = 0.0
    sanitizedWeights.forEachIndexed { index, weight ->
        cumulativeRatio += weight / totalWeight
        if (advanceAtBoundary) {
            if (progressRatio < cumulativeRatio || index == sanitizedWeights.lastIndex) return index
        } else {
            if (progressRatio <= cumulativeRatio || index == sanitizedWeights.lastIndex) return index
        }
    }
    return sanitizedWeights.lastIndex
}

private fun RouteCandidate.navigationPolylinePoints(): List<GeoCoordinate> {
    if (previewPolyline.isRenderable) return previewPolyline.points
    if (geometry.isRenderable) return geometry.points

    return buildList {
        segments.forEach { segment ->
            val segmentPoints = segment.polyline.points
            if (segmentPoints.isEmpty()) return@forEach
            if (isEmpty()) {
                addAll(segmentPoints)
            } else if (last() == segmentPoints.first()) {
                addAll(segmentPoints.drop(1))
            } else {
                addAll(segmentPoints)
            }
        }
    }
}

private fun RouteCandidate.resolveSegmentTravelKind(
    segment: RouteSegment?,
    hasTransitLeg: Boolean,
): NavigationSegmentTravelKind {
    val legType =
        legs.firstOrNull { leg ->
            leg.sequence == segment?.sourceLegSequence
        }?.type

    return when (legType) {
        RouteLegType.BUS,
        RouteLegType.SUBWAY,
            -> NavigationSegmentTravelKind.TRANSIT

        else ->
            if (hasTransitLeg) {
                NavigationSegmentTravelKind.TRANSIT_WALK
            } else {
                NavigationSegmentTravelKind.WALK
            }
    }
}

private fun RouteCandidate.hasTransitLeg(): Boolean =
    legs.any { leg -> leg.type == RouteLegType.BUS || leg.type == RouteLegType.SUBWAY }

private fun RoutePolyline.totalDistanceWeight(): Double? =
    points.totalPolylineDistanceMeters().takeIf { distanceMeters -> distanceMeters > 0.0 }

private fun List<GeoCoordinate>.totalPolylineDistanceMeters(): Double =
    if (size < 2) {
        0.0
    } else {
        zipWithNext().sumOf { (start, end) -> haversineDistanceMeters(start, end) }
    }

private fun projectOntoPolylineMeters(
    current: GeoCoordinate,
    polyline: List<GeoCoordinate>,
): RoutePolylineProjection? {
    val totalPolylineDistanceMeters = polyline.totalPolylineDistanceMeters()
    if (polyline.isEmpty()) return null
    if (polyline.size == 1) {
        return RoutePolylineProjection(
            distanceToPolylineMeters = haversineDistanceMeters(current, polyline.single()),
            distanceAlongPolylineMeters = 0.0,
            totalPolylineDistanceMeters = 0.0,
        )
    }

    var cumulativeDistanceMeters = 0.0
    var bestDistanceToPolylineMeters = Double.POSITIVE_INFINITY
    var bestDistanceAlongPolylineMeters = 0.0
    polyline.zipWithNext().forEach { (start, end) ->
        val segmentLengthMeters = haversineDistanceMeters(start, end)
        val projection =
            projectOntoSegmentMeters(
                point = current,
                start = start,
                end = end,
                segmentLengthMeters = segmentLengthMeters,
            )
        val projectedDistanceAlongPolyline = cumulativeDistanceMeters + projection.distanceAlongSegmentMeters
        val isCloser = projection.distanceToSegmentMeters < bestDistanceToPolylineMeters
        val isTieButFurtherAlong =
            !isCloser &&
                kotlin.math.abs(projection.distanceToSegmentMeters - bestDistanceToPolylineMeters) < 0.01 &&
                projectedDistanceAlongPolyline >= bestDistanceAlongPolylineMeters
        if (isCloser || isTieButFurtherAlong) {
            bestDistanceToPolylineMeters = projection.distanceToSegmentMeters
            bestDistanceAlongPolylineMeters = projectedDistanceAlongPolyline
        }
        cumulativeDistanceMeters += segmentLengthMeters
    }

    return RoutePolylineProjection(
        distanceToPolylineMeters = bestDistanceToPolylineMeters,
        distanceAlongPolylineMeters = bestDistanceAlongPolylineMeters.coerceIn(0.0, totalPolylineDistanceMeters),
        totalPolylineDistanceMeters = totalPolylineDistanceMeters,
    )
}

private fun projectOntoSegmentMeters(
    point: GeoCoordinate,
    start: GeoCoordinate,
    end: GeoCoordinate,
    segmentLengthMeters: Double,
): RouteSegmentProjection {
    if (segmentLengthMeters <= 0.0) {
        return RouteSegmentProjection(
            distanceToSegmentMeters = haversineDistanceMeters(point, start),
            distanceAlongSegmentMeters = 0.0,
        )
    }

    val referenceLatitudeRadians = Math.toRadians((start.latitude + end.latitude + point.latitude) / 3.0)
    fun GeoCoordinate.toLocalPoint(origin: GeoCoordinate): Pair<Double, Double> {
        val deltaLongitudeRadians = Math.toRadians(longitude - origin.longitude)
        val deltaLatitudeRadians = Math.toRadians(latitude - origin.latitude)
        val x = deltaLongitudeRadians * EARTH_RADIUS_METERS * cos(referenceLatitudeRadians)
        val y = deltaLatitudeRadians * EARTH_RADIUS_METERS
        return x to y
    }

    val (segmentEndX, segmentEndY) = end.toLocalPoint(start)
    val (pointX, pointY) = point.toLocalPoint(start)
    val segmentMagnitudeSquared = segmentEndX * segmentEndX + segmentEndY * segmentEndY
    val projectionRatio =
        if (segmentMagnitudeSquared <= 0.0) {
            0.0
        } else {
            ((pointX * segmentEndX + pointY * segmentEndY) / segmentMagnitudeSquared).coerceIn(0.0, 1.0)
        }
    val projectedX = segmentEndX * projectionRatio
    val projectedY = segmentEndY * projectionRatio
    val deltaX = pointX - projectedX
    val deltaY = pointY - projectedY

    return RouteSegmentProjection(
        distanceToSegmentMeters = sqrt(deltaX * deltaX + deltaY * deltaY),
        distanceAlongSegmentMeters = segmentLengthMeters * projectionRatio,
    )
}

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
private const val MIN_LOCATION_UPDATE_INTERVAL_MILLIS = 1_000L
private const val TRANSIT_REFRESH_COOLDOWN_MILLIS = 30_000L
private const val BUS_STOP_REFRESH_DISTANCE_METERS = 30.0
private const val SUBWAY_ELEVATOR_REFRESH_DISTANCE_METERS = 25.0
private const val REROUTE_DEVIATION_DISTANCE_METERS = 10.0
private const val REROUTE_MAX_GPS_ACCURACY_METERS = 20f
private const val REROUTE_OFF_ROUTE_CONSECUTIVE_COUNT = 2
private const val REROUTE_OFF_ROUTE_DURATION_MILLIS = 3_000L
private const val PROJECTED_PROGRESS_MAX_DISTANCE_METERS = 75.0
private const val FALLBACK_NAVIGATION_SPEED_METERS_PER_SECOND = 1.4
private const val LOW_VISION_FALLBACK_WALKING_SPEED_METERS_PER_SECOND = 1.0
private const val LOW_VISION_WALKABLE_DISTANCE_THRESHOLD_METERS = 750
private const val MIN_NAVIGATION_DURATION_SECONDS = 60
private const val LOW_VISION_ACTUAL_METRICS_BUCKET_SCALE = 10_000.0
private const val LOW_VISION_ROUTE_CHANGE_ALERT_DISTANCE_METERS = 50
private const val IMMEDIATE_GUIDANCE_CAMERA_DISTANCE_THRESHOLD_METERS = 500
private const val PENDING_ACTIVE_SEGMENT_LABEL = "Current segment updated"

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
        NavigationScreenState.Loading -> "현재 위치와 경로 안내를 준비하고 있습니다."
        NavigationScreenState.Ready -> "$destinationName 방향 경로 안내를 시작합니다."
        NavigationScreenState.Empty -> "$destinationName 방향 요약 정보만 먼저 표시합니다."
    }
}

private fun RouteNavigationRequest.toMapOverlayUiState(
    currentLocationCoordinate: GeoCoordinate?,
    activeSegmentIndex: Int,
    focusedSegmentIndex: Int,
    mapFocusMode: NavigationMapFocusMode,
): NavigationMapOverlayUiState {
    val selectedRoutePolyline = selectedRoute.previewPolyline.points
    val activeSegment = selectedRoute.segments.getOrNull(activeSegmentIndex)
    val focusedSegment = selectedRoute.segments.getOrNull(focusedSegmentIndex)
    val activeSegmentPolyline = activeSegment?.polyline?.points.orEmpty()
    val focusedSegmentPolyline = focusedSegment?.polyline?.points.orEmpty()
    val hasTransitLeg = selectedRoute.hasTransitLeg()
    val activeFocusCoordinate =
        currentLocationCoordinate
            ?: selectedRoute.resolveSegmentStartCoordinate(activeSegmentIndex)
            ?: selectedRoute.resolveSegmentFocusCoordinate(activeSegmentIndex)
            ?: origin.coordinate
    val inspectedFocusCoordinate =
        selectedRoute.resolveSegmentStartCoordinate(focusedSegmentIndex)
            ?: selectedRoute.resolveSegmentFocusCoordinate(focusedSegmentIndex)
            ?: activeFocusCoordinate
    val segmentRouteSegments =
        selectedRoute.segments.mapIndexed { index, segment ->
            NavigationMapSegmentUiState(
                sequence = segment.sequence,
                polyline = segment.polyline.points,
                segmentStartCoordinate = selectedRoute.resolveSegmentStartCoordinate(index),
                distanceMeters = segment.distanceMeters,
                riskLevel = segment.riskLevel,
                guidanceMessage = segment.guidanceMessage,
                travelKind = selectedRoute.resolveSegmentTravelKind(segment, hasTransitLeg),
                isActive = index == activeSegmentIndex,
                isFocused = index == focusedSegmentIndex,
                isCompleted = index < activeSegmentIndex,
                isRiskUpcoming = index > activeSegmentIndex && segment.riskLevel != RouteRiskLevel.LOW,
            )
        }
    val routeSegments = segmentRouteSegments + selectedRoute.toFallbackWalkingLegMapSegments(segmentRouteSegments, hasTransitLeg)
    val originPoint = origin.toNavigationMapPointUiState(fallbackLabel = "출발지")
    val destinationPoint = destination.toNavigationMapPointUiState(fallbackLabel = "목적지")

    return NavigationMapOverlayUiState(
        isDisplayable = selectedRoute.previewPolyline.isRenderable || routeSegments.any(NavigationMapSegmentUiState::isRenderable),
        currentLocation =
            NavigationMapPointUiState(
                label = originPoint.label,
                coordinate = currentLocationCoordinate ?: origin.coordinate,
            ),
        origin = originPoint,
        destination = destinationPoint,
        selectedRoutePolyline = selectedRoutePolyline,
        activeSegmentPolyline = activeSegmentPolyline,
        focusedSegmentPolyline = focusedSegmentPolyline,
        activeSegmentTravelKind = selectedRoute.resolveSegmentTravelKind(activeSegment, hasTransitLeg),
        focusedSegmentTravelKind = selectedRoute.resolveSegmentTravelKind(focusedSegment, hasTransitLeg),
        focusCoordinate =
            when (mapFocusMode) {
                NavigationMapFocusMode.ACTIVE -> activeFocusCoordinate
                NavigationMapFocusMode.FOCUSED -> inspectedFocusCoordinate
            },
        routeSegments = routeSegments,
        mapFocusMode = mapFocusMode,
        shouldAnimateCameraTransition =
            mapFocusMode != NavigationMapFocusMode.FOCUSED ||
                (focusedSegment?.distanceMeters ?: 0) < IMMEDIATE_GUIDANCE_CAMERA_DISTANCE_THRESHOLD_METERS,
    )
}

private fun RouteCandidate.toFallbackWalkingLegMapSegments(
    existingSegments: List<NavigationMapSegmentUiState>,
    hasTransitLeg: Boolean,
): List<NavigationMapSegmentUiState> {
    val renderableWalkingLegs =
        legs.filter { leg ->
            leg.type == RouteLegType.WALK && leg.polyline.isRenderable
        }
    if (renderableWalkingLegs.isEmpty()) return emptyList()

    val hasRenderableWalkingSegment =
        existingSegments.any { segment ->
            (segment.travelKind == NavigationSegmentTravelKind.WALK ||
                segment.travelKind == NavigationSegmentTravelKind.TRANSIT_WALK) &&
                segment.isRenderable
        }
    val hasLegScopedSegments = segments.any { segment -> segment.sourceLegSequence != null }
    val fallbackWalkingLegs =
        if (hasTransitLeg && hasLegScopedSegments) {
            renderableWalkingLegs.filterNot { leg ->
                segments.any { segment ->
                    segment.sourceLegSequence == leg.sequence && segment.polyline.isRenderable
                }
            }
        } else if (hasRenderableWalkingSegment) {
            emptyList()
        } else {
            renderableWalkingLegs
        }

    return fallbackWalkingLegs
        .map { leg ->
            NavigationMapSegmentUiState(
                sequence = leg.sequence,
                polyline = leg.polyline.points,
                segmentStartCoordinate = leg.polyline.points.firstOrNull(),
                distanceMeters = leg.distanceMeters ?: 0,
                riskLevel = RouteRiskLevel.LOW,
                guidanceMessage = leg.instruction,
                travelKind =
                    if (hasTransitLeg) {
                        NavigationSegmentTravelKind.TRANSIT_WALK
                    } else {
                        NavigationSegmentTravelKind.WALK
                    },
                showJunctionMarker = false,
            )
        }
}

internal fun createNavigationSegmentMarkerDebugSummary(
    mapOverlay: NavigationMapOverlayUiState,
): String {
    val activeIndex = mapOverlay.routeSegments.indexOfFirst(NavigationMapSegmentUiState::isActive)
    val focusedIndex = mapOverlay.routeSegments.indexOfFirst(NavigationMapSegmentUiState::isFocused)
    return buildString {
        append("focusMode=")
        append(mapOverlay.mapFocusMode.name)
        append(" count=")
        append(mapOverlay.routeSegments.size)
        append(" active=")
        append(activeIndex)
        append(" focused=")
        append(focusedIndex)
        append(" routePolyline=")
        append(mapOverlay.selectedRoutePolyline.size)
        append(" activePolyline=")
        append(mapOverlay.activeSegmentPolyline.size)
        append(" focusedPolyline=")
        append(mapOverlay.focusedSegmentPolyline.size)
        append(" details=[")
        append(
            mapOverlay.routeSegments.mapIndexed { index, segment ->
                buildString {
                    append("idx=")
                    append(index)
                    append(" seq=")
                    append(segment.sequence)
                    append(" kind=")
                    append(segment.travelKind.name)
                    append(" polyline=")
                    append(segment.polyline.size)
                    append(" first=")
                    append(segment.polyline.firstOrNull().toDebugCoordinate())
                    append(" start=")
                    append(segment.segmentStartCoordinate.toDebugCoordinate())
                    append(" active=")
                    append(segment.isActive)
                    append(" focused=")
                    append(segment.isFocused)
                }
            }.joinToString(separator = "; "),
        )
        append("]")
    }
}

private fun RouteNavigationRequest.toSegmentSyncUiState(
    activeSegmentIndex: Int,
    focusedSegmentIndex: Int,
    isInspectingSegments: Boolean,
    hasPendingActiveChange: Boolean,
    mapFocusMode: NavigationMapFocusMode,
    transitPresentation: NavigationTransitPresentation?,
): NavigationSegmentSyncUiState =
    NavigationSegmentSyncUiState(
        activeSegmentIndex = activeSegmentIndex,
        focusedSegmentIndex = focusedSegmentIndex,
        isInspectingSegments = isInspectingSegments,
        mapFocusMode = mapFocusMode,
        hasPendingActiveChange = hasPendingActiveChange,
        railItems =
            selectedRoute.segments.mapIndexed { index, segment ->
                NavigationSegmentRailItemUiState(
                    index = index,
                    sequence = segment.sequence,
                    instruction = segment.guidanceMessage,
                    distanceLabel = segment.distanceMeters.toNavigationDistanceLabel(),
                    riskLabel = segment.riskLevel.toRiskLabel(),
                    guidanceAction = selectedRoute.toNavigationGuidanceAction(segment),
                    isActive = index == activeSegmentIndex,
                    isFocused = index == focusedSegmentIndex,
                    isCompleted = index < activeSegmentIndex,
                    isRiskUpcoming = index > activeSegmentIndex && segment.riskLevel != RouteRiskLevel.LOW,
                    transitInfo = selectedRoute.resolveFocusedSegmentTransitInfo(segment, transitPresentation),
                )
            },
    )

private fun RouteNavigationRequest.toFocusedSegmentCardUiState(
    focusedSegmentIndex: Int,
    transitPresentation: NavigationTransitPresentation?,
): NavigationFocusedSegmentCardUiState? {
    val focusedSegment = selectedRoute.segments.getOrNull(focusedSegmentIndex) ?: return null
    val heroDetail = selectedRoute.toNavigationHeroDetail(focusedSegment)

    return NavigationFocusedSegmentCardUiState(
        sequenceLabel = "${focusedSegment.sequence} / ${selectedRoute.segments.size.coerceAtLeast(1)}",
        instruction = focusedSegment.guidanceMessage,
        heroTitle = heroDetail.title,
        heroDescription = heroDetail.description,
        distanceLabel = focusedSegment.distanceMeters.toNavigationDistanceLabel(),
        riskLabel = focusedSegment.riskLevel.toRiskLabel(),
        supportingText = selectedRoute.title.toNavigationRouteTitle(selectedRoute.routeOption),
        guidanceAction = heroDetail.guidanceAction,
        transitInfo = selectedRoute.resolveFocusedSegmentTransitInfo(focusedSegment, transitPresentation),
    )
}

private fun RouteCandidate.resolveFocusedSegmentTransitInfo(
    focusedSegment: RouteSegment,
    transitPresentation: NavigationTransitPresentation?,
): NavigationTransitInfoUiState? {
    val sourceLeg = focusedSegment.resolveSourceLeg(legs = legs) ?: return null
    if (sourceLeg.type != RouteLegType.BUS && sourceLeg.type != RouteLegType.SUBWAY) return null

    val activeTransitInfo = transitPresentation?.info
    if (
        activeTransitInfo != null &&
        activeTransitInfo.guidanceAction ==
        when (sourceLeg.type) {
            RouteLegType.BUS -> NavigationGuidanceAction.BUS
            RouteLegType.SUBWAY -> NavigationGuidanceAction.SUBWAY
            RouteLegType.WALK -> NavigationGuidanceAction.STRAIGHT
        }
    ) {
        return activeTransitInfo
    }

    return sourceLeg.toNavigationTransitInfo(
        arrivalRouteNo = null,
        arrivalMinutes = sourceLeg.laneOptions.firstOrNull()?.remainingMinute,
    )
}

private fun RouteWaypoint.toNavigationMapPointUiState(fallbackLabel: String): NavigationMapPointUiState =
    NavigationMapPointUiState(
        label = name.orEmpty().ifBlank { fallbackLabel },
        coordinate = coordinate,
    )

private fun RouteCandidate.resolveSegmentStartCoordinate(segmentIndex: Int): GeoCoordinate? {
    val segment = segments.getOrNull(segmentIndex) ?: return null
    val sourceLeg = segment.resolveSourceLeg(legs = legs)

    segment.polyline
        .takeIf(RoutePolyline::isRenderable)
        ?.points
        ?.firstOrNull()
        ?.let { return it }
    segment.anchorCoordinate?.let { return it }
    segment.resolveSourceLegStartCoordinate(
        segmentIndex = segmentIndex,
        segments = segments,
        sourceLeg = sourceLeg,
    )?.let { return it }
    resolveSparseSegmentBoundaryCoordinate(segmentIndex)?.let { return it }

    val fallbackPolyline = navigationPolylinePoints()
    if (fallbackPolyline.isNotEmpty()) {
        val progressRatio = resolveSegmentStartProgressRatio(segmentIndex = segmentIndex, weights = segmentWeights())
        fallbackPolyline.coordinateAtProgressRatio(progressRatio)?.let { return it }
    }

    return sourceLeg?.polyline?.points?.firstOrNull()
}

private fun RouteCandidate.resolveSegmentFocusCoordinate(segmentIndex: Int): GeoCoordinate? {
    val segment = segments.getOrNull(segmentIndex) ?: return null
    val sourceLeg = segment.resolveSourceLeg(legs = legs)

    segment.polyline
        .takeIf(RoutePolyline::isRenderable)
        ?.points
        ?.toNavigationFocusCoordinate()
        ?.let { return it }
    segment.anchorCoordinate?.let { return it }
    segment.resolveSourceLegFocusCoordinate(
        segmentIndex = segmentIndex,
        segments = segments,
        sourceLeg = sourceLeg,
    )?.let { return it }
    resolveSparseSegmentBoundaryCoordinate(segmentIndex)?.let { return it }

    val fallbackPolyline = navigationPolylinePoints()
    if (fallbackPolyline.isEmpty()) return null

    val progressRatio = resolveSegmentMidProgressRatio(segmentIndex = segmentIndex, weights = segmentWeights())
    return fallbackPolyline.coordinateAtProgressRatio(progressRatio)
}

private fun RouteCandidate.segmentWeights(): List<Double> =
    segments.map { candidateSegment ->
        candidateSegment.polyline.totalDistanceWeight()
            ?: candidateSegment.distanceMeters.toDouble().takeIf { distanceMeters -> distanceMeters > 0 }
            ?: 1.0
    }

private fun RouteCandidate.distanceToNextSegmentBoundaryMeters(progress: NavigationProgressSnapshot): Int? {
    if (progress.activeSegmentIndex >= segments.lastIndex) return null
    val sanitizedWeights =
        segmentWeights().map { weight ->
            if (weight > 0.0) {
                weight
            } else {
                1.0
            }
        }
    if (sanitizedWeights.isEmpty()) return null

    val totalWeight = sanitizedWeights.sum().takeIf { weight -> weight > 0.0 } ?: return null
    val boundaryRatio =
        sanitizedWeights
            .take(progress.activeSegmentIndex + 1)
            .sum()
            .div(totalWeight)
            .coerceIn(0.0, 1.0)
    val boundaryDistanceMeters = totalDistanceMeters() * boundaryRatio
    return (boundaryDistanceMeters - progress.distanceAlongRouteMeters)
        .roundToInt()
        .coerceAtLeast(0)
}

private fun RouteSegment.resolveSourceLeg(legs: List<RouteLeg>): RouteLeg? =
    sourceLegSequence?.let { sourceLegSequence ->
        legs.firstOrNull { leg -> leg.sequence == sourceLegSequence }
    }

private fun RouteSegment.isFirstSegmentOfSourceLeg(
    segmentIndex: Int,
    segments: List<RouteSegment>,
): Boolean {
    val sourceLegSequence = sourceLegSequence ?: return false
    return segments.indexOfFirst { candidateSegment -> candidateSegment.sourceLegSequence == sourceLegSequence } == segmentIndex
}

private fun RouteSegment.resolveSourceLegStartCoordinate(
    segmentIndex: Int,
    segments: List<RouteSegment>,
    sourceLeg: RouteLeg?,
): GeoCoordinate? {
    val resolvedSourceLeg = sourceLeg ?: return null
    return if (
        resolvedSourceLeg.type != RouteLegType.WALK ||
        isFirstSegmentOfSourceLeg(segmentIndex = segmentIndex, segments = segments)
    ) {
        resolvedSourceLeg.polyline.points.firstOrNull()
    } else {
        null
    }
}

private fun RouteCandidate.resolveSparseSegmentBoundaryCoordinate(segmentIndex: Int): GeoCoordinate? {
    val fallbackPolyline = navigationPolylinePoints()
    if (fallbackPolyline.size < segments.size + 1) return null
    return fallbackPolyline.getOrNull(segmentIndex)
}

private fun RouteSegment.resolveSourceLegFocusCoordinate(
    segmentIndex: Int,
    segments: List<RouteSegment>,
    sourceLeg: RouteLeg?,
): GeoCoordinate? {
    val resolvedSourceLeg = sourceLeg ?: return null
    return if (
        resolvedSourceLeg.type != RouteLegType.WALK ||
        isFirstSegmentOfSourceLeg(segmentIndex = segmentIndex, segments = segments)
    ) {
        resolvedSourceLeg.toNavigationFocusCoordinate()
    } else {
        null
    }
}

private fun RouteLeg.toNavigationFocusCoordinate(): GeoCoordinate? =
    polyline.points.toNavigationFocusCoordinate()
        ?: listOfNotNull(boardingStop?.coordinate, alightingStop?.coordinate).toNavigationFocusCoordinate()

private fun List<GeoCoordinate>.toNavigationFocusCoordinate(): GeoCoordinate? {
    if (isEmpty()) return null
    if (size == 1) return single()
    return coordinateAtProgressRatio(progressRatio = 0.5)
}

private fun resolveSegmentMidProgressRatio(
    segmentIndex: Int,
    weights: List<Double>,
): Double {
    if (weights.isEmpty()) return 0.5

    val sanitizedWeights =
        weights.map { weight ->
            if (weight > 0.0) {
                weight
            } else {
                1.0
            }
        }
    val safeSegmentIndex = segmentIndex.coerceIn(0, sanitizedWeights.lastIndex)
    val totalWeight = sanitizedWeights.sum().takeIf { total -> total > 0.0 } ?: sanitizedWeights.size.toDouble()
    val accumulatedWeightBefore = sanitizedWeights.take(safeSegmentIndex).sum()
    val targetWeight = sanitizedWeights[safeSegmentIndex]
    return ((accumulatedWeightBefore + (targetWeight / 2.0)) / totalWeight).coerceIn(0.0, 1.0)
}

private fun resolveSegmentStartProgressRatio(
    segmentIndex: Int,
    weights: List<Double>,
): Double {
    if (weights.isEmpty()) return 0.0

    val sanitizedWeights =
        weights.map { weight ->
            if (weight > 0.0) {
                weight
            } else {
                1.0
            }
        }
    val safeSegmentIndex = segmentIndex.coerceIn(0, sanitizedWeights.lastIndex)
    val totalWeight = sanitizedWeights.sum().takeIf { total -> total > 0.0 } ?: sanitizedWeights.size.toDouble()
    val accumulatedWeightBefore = sanitizedWeights.take(safeSegmentIndex).sum()
    return (accumulatedWeightBefore / totalWeight).coerceIn(0.0, 1.0)
}

private fun List<GeoCoordinate>.coordinateAtProgressRatio(progressRatio: Double): GeoCoordinate? {
    if (isEmpty()) return null
    if (size == 1) return single()

    val clampedRatio = progressRatio.coerceIn(0.0, 1.0)
    val totalDistanceMeters = totalPolylineDistanceMeters()
    if (totalDistanceMeters <= 0.0) {
        return first().interpolateTo(last(), 0.5)
    }

    val targetDistanceMeters = totalDistanceMeters * clampedRatio
    var cumulativeDistanceMeters = 0.0
    zipWithNext().forEach { (start, end) ->
        val segmentDistanceMeters = haversineDistanceMeters(start, end)
        val nextCumulativeDistanceMeters = cumulativeDistanceMeters + segmentDistanceMeters
        if (segmentDistanceMeters <= 0.0) {
            cumulativeDistanceMeters = nextCumulativeDistanceMeters
            return@forEach
        }
        if (targetDistanceMeters <= nextCumulativeDistanceMeters) {
            val segmentRatio = ((targetDistanceMeters - cumulativeDistanceMeters) / segmentDistanceMeters).coerceIn(0.0, 1.0)
            return start.interpolateTo(end, segmentRatio)
        }
        cumulativeDistanceMeters = nextCumulativeDistanceMeters
    }

    return last()
}

private fun GeoCoordinate.interpolateTo(
    other: GeoCoordinate,
    progressRatio: Double,
): GeoCoordinate =
    GeoCoordinate(
        latitude = latitude + ((other.latitude - latitude) * progressRatio),
        longitude = longitude + ((other.longitude - longitude) * progressRatio),
    )

private fun RouteNavigationRequest.toStepCardUiState(
    screenState: NavigationScreenState,
    activeSegmentIndex: Int,
    remainingDistanceMeters: Int?,
    estimatedMinutes: Int?,
    remainingMetricsSource: NavigationRemainingMetricsSource,
    transitPresentation: NavigationTransitPresentation?,
): NavigationStepCardUiState =
    when (screenState) {
        NavigationScreenState.Loading -> NavigationStepCardUiState()
        NavigationScreenState.Ready ->
            toReadyStepCardUiState(
                activeSegmentIndex = activeSegmentIndex,
                remainingDistanceMeters = remainingDistanceMeters,
                estimatedMinutes = estimatedMinutes,
                remainingMetricsSource = remainingMetricsSource,
                transitPresentation = transitPresentation,
            )
        NavigationScreenState.Empty -> toEmptyStepCardUiState()
    }

private fun NavigationStepCardUiState.toNavigationBriefingText(): String =
    listOf(
        supportingText,
        "$distanceLabel 후 $instruction",
    ).joinToString(separator = " ")

private fun RouteNavigationRequest.toNavigationBriefingText(
    activeSegmentIndex: Int,
    fallback: String,
): String =
    selectedRoute.segments
        .getOrNull(activeSegmentIndex)
        ?.toCompactNavigationInstruction()
        ?: fallback

private fun NavigationTtsUiState.toFallbackMessage(): String =
    when {
        !isEnabled -> NAVIGATION_TTS_DISABLED_MESSAGE
        status == NavigationTtsStatus.Unavailable -> NAVIGATION_TTS_UNAVAILABLE_MESSAGE
        status == NavigationTtsStatus.Initializing -> NAVIGATION_TTS_PREPARING_MESSAGE
        else -> ""
    }

private fun RouteNavigationRequest.toReadyStepCardUiState(
    activeSegmentIndex: Int,
    remainingDistanceMeters: Int?,
    estimatedMinutes: Int?,
    remainingMetricsSource: NavigationRemainingMetricsSource,
    transitPresentation: NavigationTransitPresentation?,
): NavigationStepCardUiState {
    val primarySegment =
        selectedRoute.segments.getOrNull(activeSegmentIndex)
            ?: selectedRoute.segments.firstOrNull { segment ->
                segment.guidanceMessage.isNotBlank()
            }
            ?: selectedRoute.segments.firstOrNull()
    val heroDetail = primarySegment?.let(selectedRoute::toNavigationHeroDetail)
    val remainingMetricPresentation =
        navigationRemainingMetricPresentation(
            distanceMeters = remainingDistanceMeters ?: selectedRoute.summary.distanceMeters,
            estimatedMinutes = estimatedMinutes ?: selectedRoute.summary.estimatedTimeMinutes,
            source = remainingMetricsSource,
        )

    return NavigationStepCardUiState(
        sectionLabel = "다음 안내",
        statusLabel = selectedRoute.routeOption.toRouteOptionLabel(),
        emphasisLabel = (primarySegment?.riskLevel ?: selectedRoute.summary.riskLevel).toRiskLabel(),
        distanceLabel =
            primarySegment?.distanceMeters?.toNavigationDistanceLabel()
                ?: selectedRoute.summary.distanceMeters.toNavigationDistanceLabel(),
        heroTitle = heroDetail?.title ?: "경로 안내",
        heroDescription =
            heroDetail?.description ?: "${destination.name.orEmpty().ifBlank { "목적지" }} 방향으로 경로 안내를 준비하고 있습니다.",
        instruction =
            primarySegment?.guidanceMessage
                ?.trim()
                ?.takeIf { guidanceMessage -> guidanceMessage.isNotEmpty() }
                ?: "목적지 방향으로 계속 이동해 주세요.",
        supportingText =
            transitPresentation?.supportingText ?: "${destination.name.orEmpty().ifBlank { "목적지" }} 방향으로 " +
                "${selectedRoute.title.toNavigationRouteTitle(selectedRoute.routeOption)} 경로를 따라 이동합니다.",
        guidanceAction = heroDetail?.guidanceAction ?: NavigationGuidanceAction.STRAIGHT,
        transitInfo = null,
        metrics =
            listOf(
                NavigationStepMetricUiState(
                    label = "남은 거리",
                    value = remainingMetricPresentation.distanceLabel,
                ),
                NavigationStepMetricUiState(
                    label = "남은 시간",
                    value = remainingMetricPresentation.etaLabel,
                ),
                NavigationStepMetricUiState(
                    label = "진행 단계",
                    value = "${activeSegmentIndex + 1} / ${selectedRoute.segments.size.coerceAtLeast(1)}",
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
        heroTitle = "경로 안내",
        heroDescription = "현재 안내 메시지를 준비하지 못했습니다.",
        instruction = "현재 안내 메시지를 준비하지 못했습니다.",
        supportingText =
            "${destination.name.orEmpty().ifBlank { "목적지" }} 방향으로 거리와 예상 시간 요약만 먼저 표시합니다.",
        guidanceAction =
            selectedRoute.segments.firstOrNull()?.let(selectedRoute::toNavigationGuidanceAction)
                ?: NavigationGuidanceAction.STRAIGHT,
        metrics =
            listOf(
                NavigationStepMetricUiState(
                    label = "남은 거리",
                    value = selectedRoute.summary.distanceMeters.toNavigationDistanceLabel(),
                ),
                NavigationStepMetricUiState(
                    label = "남은 시간",
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
        NavigationScreenState.Empty,
            -> NavigationCtaUiState(
                label = "안내 종료",
                supportingText = "안내를 종료하고 지도로 돌아갑니다.",
                isEnabled = true,
            )
    }

private fun String.toNavigationRouteTitle(routeOption: RouteOption): String =
    trim().ifBlank { routeOption.toRouteOptionLabel() }

private fun RouteOption.toRouteOptionLabel(): String =
    when (this) {
        RouteOption.SAFE -> "안전한 길"
        RouteOption.SHORTEST -> "최단거리"
        RouteOption.RECOMMENDED -> "추천 경로"
        RouteOption.MIN_TRANSFER -> "최소 환승"
        RouteOption.MIN_WALK -> "최소 도보"
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

private data class NavigationRemainingMetricPresentation(
    val distanceLabel: String,
    val etaLabel: String,
)

private fun navigationRemainingMetricPresentation(
    distanceMeters: Int,
    estimatedMinutes: Int,
    source: NavigationRemainingMetricsSource,
): NavigationRemainingMetricPresentation {
    if (source == NavigationRemainingMetricsSource.Unavailable) {
        return NavigationRemainingMetricPresentation(
            distanceLabel = "-",
            etaLabel = "-",
        )
    }

    return NavigationRemainingMetricPresentation(
        distanceLabel = distanceMeters.toNavigationDistanceLabel(),
        etaLabel = estimatedMinutes.toNavigationEtaLabel(),
    )
}

private fun Int.toEtaMinutes(): Int =
    if (this <= 0) {
        0
    } else {
        ceil(this / 60.0).toInt()
    }

private data class LowVisionActualMetricsKey(
    val currentLatitudeBucket: Int,
    val currentLongitudeBucket: Int,
    val destinationLatitudeBucket: Int,
    val destinationLongitudeBucket: Int,
) {
    companion object {
        fun from(
            current: GeoCoordinate,
            destination: GeoCoordinate,
        ): LowVisionActualMetricsKey =
            LowVisionActualMetricsKey(
                currentLatitudeBucket = current.latitude.toLowVisionMetricsBucket(),
                currentLongitudeBucket = current.longitude.toLowVisionMetricsBucket(),
                destinationLatitudeBucket = destination.latitude.toLowVisionMetricsBucket(),
                destinationLongitudeBucket = destination.longitude.toLowVisionMetricsBucket(),
            )
    }
}

private fun LowVisionActualMetricsKey.hasSameDestinationAs(other: LowVisionActualMetricsKey): Boolean =
    destinationLatitudeBucket == other.destinationLatitudeBucket &&
        destinationLongitudeBucket == other.destinationLongitudeBucket

private fun Double.toLowVisionMetricsBucket(): Int =
    (this * LOW_VISION_ACTUAL_METRICS_BUCKET_SCALE).roundToInt()

private val LOW_VISION_ACTUAL_WALK_OPTIONS = listOf(RouteOption.SAFE, RouteOption.SHORTEST)
private val LOW_VISION_ACTUAL_TRANSIT_OPTIONS =
    listOf(
        RouteOption.RECOMMENDED,
        RouteOption.MIN_TRANSFER,
        RouteOption.MIN_WALK,
    )
private const val LOW_VISION_ACTUAL_METRICS_MIN_REQUEST_INTERVAL_MILLIS = 5_000L
private const val LOW_VISION_ACTUAL_METRICS_MIN_REQUEST_DISTANCE_METERS = 20.0
private const val LOW_VISION_ACTUAL_METRICS_REUSE_DISTANCE_METERS = 25.0
private const val LOW_VISION_ACTUAL_METRICS_CACHE_MAX_AGE_MILLIS = 10_000L
private const val NAVIGATION_TRANSIT_OPTION_LABEL_LIMIT = 4

private object NoOpRouteRepository : RouteRepository {
    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("NavigationViewModel requires a RouteRepository for route search.")

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        error("NavigationViewModel requires a RouteRepository for transit route search.")

    override suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ) = error("NavigationViewModel requires a RouteRepository for route selection.")

    override suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ) = error("NavigationViewModel requires a RouteRepository for transit refresh.")

    override suspend fun reroute(
        routeId: String,
        currentPoint: GeoCoordinate,
    ) = error("NavigationViewModel requires a RouteRepository for reroute.")

    override suspend fun endRoute(routeId: String) =
        error("NavigationViewModel requires a RouteRepository for route end.")

    override suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ) = error("NavigationViewModel does not submit ratings.")
}

private fun GeoCoordinate?.toDebugCoordinate(): String =
    this?.let { coordinate ->
        String.format(Locale.US, "%.6f,%.6f", coordinate.latitude, coordinate.longitude)
    } ?: "null"
