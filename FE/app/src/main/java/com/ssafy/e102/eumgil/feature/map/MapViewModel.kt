package com.ssafy.e102.eumgil.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.LocationPermissionUnavailableReason as PermissionUnavailableReason
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.AccessibilityTag
import com.ssafy.e102.eumgil.core.model.FacilityBrowseData
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.data.repository.toBookmarkData
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapDefaults
import com.ssafy.e102.eumgil.feature.map.model.MapFilterSelectionState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterChipState
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterKey
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterRowState
import com.ssafy.e102.eumgil.feature.map.model.toMapCoordinate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel(
    private val locationPermissionManager: LocationPermissionManager,
    private val currentLocationManager: CurrentLocationManager,
    private val destinationSelectionRepository: DestinationSelectionRepository,
    private val facilitySeedRepository: FacilitySeedRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val searchRepository: SearchRepository = NoOpSearchRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = Channel<MapUiEvent>(capacity = Channel.BUFFERED)
    val uiEvent: Flow<MapUiEvent> = mutableUiEvent.receiveAsFlow()

    private var latestPermissionState: LocationPermissionState = locationPermissionManager.permissionState.value
    private var latestLocation: LocationSnapshot? = currentLocationManager.latestLocation.value
    private var selectedDestination: PlaceDestination? = destinationSelectionRepository.selectedDestination.value
    private var selectedMarkerId: String? = null
    private var selectedFacilityDetail: FacilityDetailSeed? = null
    private var selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
    private var facilityBrowseData: FacilityBrowseData? = null
    private var markerFilterSelectionState: MapFilterSelectionState = MapFilterSelectionState()
    private var selectedShortcutFilterKey: MapShortcutFilterKey? = null
    private var recentDestinations: List<RecentDestination> = emptyList()
    private var isRouteStarted = false
    private var locationLookupState: LocationLookupState = LocationLookupState.Idle
    private var locationLookupTimeoutJob: Job? = null

    init {
        mutableUiState.update { state ->
            state.copy(selectedDestination = selectedDestination)
        }
        selectedDestination?.let { destination ->
            syncCameraToSelectedDestination(
                destination = destination,
                incrementRequestId = true,
            )
        }
        observeSelectedDestination()
        observeSelectionRequests()
        observePermissionState()
        observeLocationUpdates()
        loadMarkerBrowseState()
        refreshRecentDestinations()
        renderUiState()
    }

    fun onRouteStarted() {
        if (isRouteStarted) {
            locationPermissionManager.refreshPermissionState()
            return
        }

        isRouteStarted = true
        locationPermissionManager.refreshPermissionState()
        latestPermissionState = locationPermissionManager.permissionState.value

        when (latestPermissionState) {
            is LocationPermissionState.Granted -> startLocationTracking(forceLookupRestart = false)
            LocationPermissionState.Denied -> applyFallbackCameraTarget()
            is LocationPermissionState.Unavailable -> applyFallbackCameraTarget()
        }

        refreshRecentDestinations()
        renderUiState()
    }

    fun onRouteStopped() {
        if (!isRouteStarted) return

        isRouteStarted = false
        stopLocationLookup()
        currentLocationManager.stopLocationUpdates()
    }

    fun onAction(action: MapUiAction) {
        when (action) {
            MapUiAction.FacilityBookmarkClicked -> toggleSelectedFacilityBookmark()
            MapUiAction.FacilityDetailDismissed -> dismissFacilityDetailSheet()
            MapUiAction.FacilityRouteEntryClicked -> handleFacilityRouteEntryClicked()
            MapUiAction.LocationActionClicked -> handleLocationAction()
            is MapUiAction.MarkerTapped -> handleMarkerTapped(action.markerId)
            MapUiAction.MarkerCategoryFilterReset -> resetMarkerCategoryFilter()
            is MapUiAction.MarkerCategoryFilterToggled -> toggleMarkerCategoryFilter(action.category)
            is MapUiAction.ShortcutFilterClicked -> handleShortcutFilterClicked(action.key)
            is MapUiAction.RecentDestinationRouteClicked -> handleRecentDestinationRouteClicked(action.placeId)
            MapUiAction.SearchEntryClicked -> emitUiEvent(MapUiEvent.NavigateToSearch)
        }
    }

    override fun onCleared() {
        stopLocationLookup()
        currentLocationManager.stopLocationUpdates()
        mutableUiEvent.close()
        super.onCleared()
    }

    private fun loadMarkerBrowseState() {
        viewModelScope.launch {
            runCatching {
                facilitySeedRepository.getFacilityBrowseData()
            }.onSuccess { browseData ->
                facilityBrowseData = browseData
                markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
                selectedShortcutFilterKey = null
                renderMarkerBrowseState()
            }.onFailure {
                facilityBrowseData = null
                updateSelectedFacility(markerId = null)
                markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
                selectedShortcutFilterKey = null
                mutableUiState.update { state ->
                    state.copy(
                        selectedMarkerId = null,
                        facilityDetailSheetState = currentFacilityDetailSheetState(),
                        markerOverlayState = MapBrowseStateFactory.createErrorMarkerOverlayState(),
                        markerFilterState = MapBrowseStateFactory.createErrorFilterUiState(),
                        shortcutFilterState = createShortcutFilterState(),
                    )
                }
            }
        }
    }

    private fun handleMarkerTapped(markerId: String) {
        if (selectedMarkerId == markerId) {
            updateSelectedFacility(markerId = null)
        } else {
            updateSelectedFacility(markerId = markerId)
        }
        renderSelectedFacilityState()
    }

    private fun dismissFacilityDetailSheet() {
        if (!clearSelectedFacilitySelection()) return
        renderSelectedFacilityState()
    }

    private fun handleFacilityRouteEntryClicked() {
        val destination = selectedFacilityDetail?.toPlaceDestination() ?: return
        clearSelectedFacilitySelection()
        renderSelectedFacilityState()
        destinationSelectionRepository.updateSelectedDestination(destination)
        emitUiEvent(MapUiEvent.NavigateToRouteSetting)
    }

    private fun handleRecentDestinationRouteClicked(placeId: String) {
        val destination =
            recentDestinations
                .firstOrNull { recentDestination -> recentDestination.placeId == placeId }
                ?.toPlaceDestination()
                ?: return

        if (clearSelectedFacilitySelection()) {
            renderSelectedFacilityState()
        }
        destinationSelectionRepository.updateSelectedDestination(destination)
        emitUiEvent(MapUiEvent.NavigateToRouteSetting)
    }

    private fun resetMarkerCategoryFilter() {
        if (facilityBrowseData == null) return
        markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
        selectedShortcutFilterKey = null
        renderMarkerBrowseState()
    }

    private fun toggleMarkerCategoryFilter(category: FacilityCategory) {
        val browseData = facilityBrowseData ?: return
        markerFilterSelectionState =
            MapBrowseStateFactory.toggleCategory(
                selection = markerFilterSelectionState,
                browseData = browseData,
                category = category,
            )
        selectedShortcutFilterKey = shortcutFilterKeyForSelection(markerFilterSelectionState)
        renderMarkerBrowseState()
    }

    private fun handleShortcutFilterClicked(key: MapShortcutFilterKey) {
        val browseData = facilityBrowseData ?: return
        if (key == MapShortcutFilterKey.MORE) return

        if (key == MapShortcutFilterKey.ACCESSIBLE_PARKING) {
            selectedShortcutFilterKey =
                if (selectedShortcutFilterKey == key) {
                    null
                } else {
                    key
                }
            markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
            renderMarkerBrowseState()
            return
        }

        val category = key.toFacilityCategory() ?: return
        val isSameShortcut = selectedShortcutFilterKey == key
        selectedShortcutFilterKey = if (isSameShortcut) null else key
        markerFilterSelectionState =
            if (isSameShortcut) {
                MapBrowseStateFactory.resetSelection()
            } else {
                MapFilterSelectionState(
                    isShowingAllCategories = false,
                    selectedFacilityCategories = setOf(category),
                )
            }
        renderMarkerBrowseState()
    }

    private fun renderMarkerBrowseState() {
        val browseData = facilityBrowseData ?: return
        val baseOverlayState =
            MapBrowseStateFactory.createMarkerOverlayState(
                browseData = browseData,
                selection = markerFilterSelectionState,
            )
        val overlayState = applyShortcutFilter(baseOverlayState)
        val filterState =
            MapBrowseStateFactory.createFilterUiState(
                browseData = browseData,
                selection = markerFilterSelectionState,
                overlayState = overlayState,
            )

        val nextSelectedMarkerId =
            selectedMarkerId?.takeIf { markerId ->
                overlayState.markers.any { marker ->
                    marker.markerId == markerId
                        && marker.displayState == MapMarkerDisplayState.VISIBLE
                }
            }
        val nextSelectedFacilityDetail = nextSelectedMarkerId?.let(browseData::detailFor)
        updateSelectedFacility(
            markerId = nextSelectedMarkerId,
            detail = nextSelectedFacilityDetail,
        )
        markerFilterSelectionState = filterState.selection

        mutableUiState.update { state ->
            state.copy(
                selectedMarkerId = selectedMarkerId,
                facilityDetailSheetState = currentFacilityDetailSheetState(),
                markerOverlayState = overlayState,
                markerFilterState = filterState,
                shortcutFilterState = createShortcutFilterState(browseData),
            )
        }
    }

    private fun observePermissionState() {
        viewModelScope.launch {
            locationPermissionManager.permissionState.collectLatest { permissionState ->
                latestPermissionState = permissionState

                when (permissionState) {
                    is LocationPermissionState.Granted -> handleGrantedPermission()
                    LocationPermissionState.Denied -> handlePermissionBlocked()
                    is LocationPermissionState.Unavailable -> handlePermissionBlocked()
                }

                renderUiState()
            }
        }
    }

    private fun observeSelectedDestination() {
        viewModelScope.launch {
            destinationSelectionRepository.selectedDestination.collectLatest { destination ->
                val previousDestination = selectedDestination
                selectedDestination = destination

                mutableUiState.update { state ->
                    state.copy(selectedDestination = destination)
                }

                if (
                    destination == null &&
                    previousDestination != null &&
                    mutableUiState.value.cameraTarget.source == MapCameraSource.SEARCH_RESULT
                ) {
                    applyFallbackCameraTarget()
                    renderUiState()
                }
            }
        }
    }

    private fun observeSelectionRequests() {
        viewModelScope.launch {
            destinationSelectionRepository.selectionRequests.collectLatest { destination ->
                // Any destination handoff should close stale facility detail state before the map recenters.
                if (clearSelectedFacilitySelection()) {
                    renderSelectedFacilityState()
                }
                syncCameraToSelectedDestination(
                    destination = destination,
                    incrementRequestId = true,
                )
                renderUiState()
            }
        }
    }

    private fun observeLocationUpdates() {
        viewModelScope.launch {
            currentLocationManager.latestLocation.collectLatest { snapshot ->
                val hadLocation = latestLocation != null
                latestLocation = snapshot

                if (snapshot == null) {
                    if (latestPermissionState is LocationPermissionState.Granted && isRouteStarted) {
                        startLocationLookup(forceRestart = false)
                    }
                    applyFallbackCameraTarget()
                } else {
                    stopLocationLookup()
                    if (shouldSyncCameraToCurrentLocation()) {
                        val shouldIncrementRequestId =
                            !hadLocation ||
                                mutableUiState.value.cameraTarget.source != MapCameraSource.CURRENT_LOCATION

                        syncCameraToCurrentLocation(
                            snapshot = snapshot,
                            incrementRequestId = shouldIncrementRequestId,
                        )
                    }
                }

                renderUiState()
            }
        }
    }

    private fun handleGrantedPermission() {
        if (!isRouteStarted) return
        startLocationTracking(forceLookupRestart = false)
    }

    private fun handlePermissionBlocked() {
        stopLocationLookup()
        currentLocationManager.stopLocationUpdates()
        applyFallbackCameraTarget()
    }

    private fun handleLocationAction() {
        when (mutableUiState.value.recenterButtonState) {
            MapRecenterButtonState.REQUEST_PERMISSION ->
                emitUiEvent(MapUiEvent.RequestLocationPermission)

            MapRecenterButtonState.LOADING -> Unit
            MapRecenterButtonState.DISABLED -> Unit

            MapRecenterButtonState.RETRY -> retryLocationResolution()

            MapRecenterButtonState.ENABLED -> {
                val snapshot = latestLocation ?: return retryLocationResolution()
                syncCameraToCurrentLocation(
                    snapshot = snapshot,
                    incrementRequestId = true,
                )
                renderUiState()
            }
        }
    }

    private fun retryLocationResolution() {
        locationPermissionManager.refreshPermissionState()
        latestPermissionState = locationPermissionManager.permissionState.value

        when (latestPermissionState) {
            is LocationPermissionState.Granted -> startLocationTracking(forceLookupRestart = true)
            LocationPermissionState.Denied -> emitUiEvent(MapUiEvent.RequestLocationPermission)
            is LocationPermissionState.Unavailable -> {
                applyFallbackCameraTarget()
                renderUiState()
            }
        }
    }

    private fun startLocationTracking(forceLookupRestart: Boolean) {
        if (!isRouteStarted) return

        currentLocationManager.startLocationUpdates()
        currentLocationManager.refreshLatestLocation()
        latestLocation = currentLocationManager.latestLocation.value

        val snapshot = latestLocation
        if (snapshot == null) {
            startLocationLookup(forceRestart = forceLookupRestart)
            applyFallbackCameraTarget()
            return
        }

        stopLocationLookup()
        if (shouldSyncCameraToCurrentLocation()) {
            syncCameraToCurrentLocation(
                snapshot = snapshot,
                incrementRequestId = mutableUiState.value.cameraTarget.source != MapCameraSource.CURRENT_LOCATION,
            )
        }
    }

    private fun startLocationLookup(forceRestart: Boolean) {
        if (!isRouteStarted || latestPermissionState !is LocationPermissionState.Granted || latestLocation != null) {
            return
        }
        if (!forceRestart && locationLookupState == LocationLookupState.Searching) return

        locationLookupTimeoutJob?.cancel()
        locationLookupState = LocationLookupState.Searching
        locationLookupTimeoutJob =
            viewModelScope.launch {
                delay(LOCATION_LOOKUP_TIMEOUT_MILLIS)
                if (
                    isRouteStarted &&
                    latestPermissionState is LocationPermissionState.Granted &&
                    latestLocation == null
                ) {
                    locationLookupState = LocationLookupState.TimedOut
                    renderUiState()
                }
            }
    }

    private fun stopLocationLookup() {
        locationLookupTimeoutJob?.cancel()
        locationLookupTimeoutJob = null
        locationLookupState = LocationLookupState.Idle
    }

    private fun shouldSyncCameraToCurrentLocation(): Boolean =
        when {
            mutableUiState.value.cameraTarget.source == MapCameraSource.CURRENT_LOCATION -> true
            selectedDestination != null -> false
            else -> true
        }

    private fun syncCameraToCurrentLocation(
        snapshot: LocationSnapshot,
        incrementRequestId: Boolean,
    ) {
        val coordinate = snapshot.toMapCoordinate()

        mutableUiState.update { state ->
            val shouldIncrement =
                incrementRequestId || state.cameraTarget.source != MapCameraSource.CURRENT_LOCATION
            val nextRequestId =
                if (shouldIncrement) {
                    state.cameraTarget.requestId + 1L
                } else {
                    state.cameraTarget.requestId
                }

            state.copy(
                cameraTarget =
                    MapCameraTarget(
                        center = coordinate,
                        source = MapCameraSource.CURRENT_LOCATION,
                        requestId = nextRequestId,
                    ),
            )
        }
    }

    private fun syncCameraToSelectedDestination(
        destination: PlaceDestination,
        incrementRequestId: Boolean,
    ) {
        val coordinate =
            com.ssafy.e102.eumgil.feature.map.model.MapCoordinate(
                latitude = destination.latitude,
                longitude = destination.longitude,
            )

        mutableUiState.update { state ->
            val shouldIncrement =
                incrementRequestId || state.cameraTarget.source != MapCameraSource.SEARCH_RESULT
            val nextRequestId =
                if (shouldIncrement) {
                    state.cameraTarget.requestId + 1L
                } else {
                    state.cameraTarget.requestId
                }

            state.copy(
                cameraTarget =
                    MapCameraTarget(
                        center = coordinate,
                        source = MapCameraSource.SEARCH_RESULT,
                        requestId = nextRequestId,
                    ),
                selectedDestination = destination,
            )
        }
    }

    private fun applyFallbackCameraTarget() {
        selectedDestination?.let { destination ->
            syncCameraToSelectedDestination(
                destination = destination,
                incrementRequestId = mutableUiState.value.cameraTarget.source != MapCameraSource.SEARCH_RESULT,
            )
            return
        }

        val snapshot = latestLocation
        if (snapshot != null && latestPermissionState is LocationPermissionState.Granted) {
            syncCameraToCurrentLocation(
                snapshot = snapshot,
                incrementRequestId = mutableUiState.value.cameraTarget.source != MapCameraSource.CURRENT_LOCATION,
            )
            return
        }

        applyDefaultCameraTarget()
    }

    private fun applyDefaultCameraTarget() {
        mutableUiState.update { state ->
            if (
                state.cameraTarget.source == MapCameraSource.DEFAULT_BUSAN &&
                state.cameraTarget.center == MapDefaults.BUSAN_CENTER
            ) {
                state
            } else {
                state.copy(
                    cameraTarget =
                        MapCameraTarget(
                            center = MapDefaults.BUSAN_CENTER,
                            source = MapCameraSource.DEFAULT_BUSAN,
                            requestId = state.cameraTarget.requestId + 1L,
                        ),
                )
            }
        }
    }

    private fun renderUiState() {
        val locationStatus =
            when (val permissionState = latestPermissionState) {
                is LocationPermissionState.Granted ->
                    latestLocation?.let { snapshot ->
                        MapLocationStatus.Ready(
                            location = snapshot.toMapCoordinate(),
                            accuracyMeters = snapshot.accuracyMeters,
                        )
                    } ?: if (locationLookupState == LocationLookupState.TimedOut) {
                        MapLocationStatus.Unavailable(
                            reason = MapLocationUnavailableReason.CURRENT_LOCATION_UNAVAILABLE,
                        )
                    } else {
                        MapLocationStatus.Loading
                    }

                LocationPermissionState.Denied -> MapLocationStatus.PermissionDenied

                is LocationPermissionState.Unavailable -> {
                    val reason =
                        when (permissionState.reason) {
                            PermissionUnavailableReason.LOCATION_SERVICES_DISABLED ->
                                MapLocationUnavailableReason.LOCATION_SERVICES_DISABLED

                            PermissionUnavailableReason.NO_LOCATION_FEATURE ->
                                MapLocationUnavailableReason.NO_LOCATION_FEATURE
                        }

                    MapLocationStatus.Unavailable(reason = reason)
                }
            }

        val recenterButtonState =
            when (locationStatus) {
                MapLocationStatus.PermissionDenied -> MapRecenterButtonState.REQUEST_PERMISSION
                MapLocationStatus.Loading -> MapRecenterButtonState.LOADING
                is MapLocationStatus.Ready -> MapRecenterButtonState.ENABLED
                is MapLocationStatus.Unavailable ->
                    when (locationStatus.reason) {
                        MapLocationUnavailableReason.NO_LOCATION_FEATURE -> MapRecenterButtonState.DISABLED
                        MapLocationUnavailableReason.CURRENT_LOCATION_UNAVAILABLE -> MapRecenterButtonState.RETRY
                        MapLocationUnavailableReason.LOCATION_SERVICES_DISABLED -> MapRecenterButtonState.RETRY
                    }
            }

        mutableUiState.update { state ->
            state.copy(
                selectedDestination = selectedDestination,
                locationStatus = locationStatus,
                recenterButtonState = recenterButtonState,
            )
        }
    }

    private fun renderSelectedFacilityState() {
        mutableUiState.update { state ->
            state.copy(
                selectedMarkerId = selectedMarkerId,
                facilityDetailSheetState = currentFacilityDetailSheetState(),
            )
        }
    }

    private fun clearSelectedFacilitySelection(): Boolean {
        if (selectedMarkerId == null && selectedFacilityDetail == null) return false

        selectedMarkerId = null
        selectedFacilityDetail = null
        selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
        return true
    }

    private fun updateSelectedFacility(
        markerId: String?,
        detail: FacilityDetailSeed? = markerId?.let { facilityBrowseData?.detailFor(it) },
    ) {
        if (markerId != null && detail == null) {
            selectedMarkerId = null
            selectedFacilityDetail = null
            selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
            return
        }

        val previousFacilityId = selectedFacilityDetail?.facilityId
        selectedMarkerId = markerId
        selectedFacilityDetail = detail
        if (detail == null) {
            selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
        } else if (previousFacilityId != detail.facilityId) {
            selectedFacilityBookmarkState =
                SelectedFacilityBookmarkState(
                    facilityId = detail.facilityId,
                    isUpdating = true,
                )
            loadSelectedFacilityBookmarkState(detail)
        }
    }

    private fun currentFacilityDetailSheetState(): MapFacilityDetailSheetState =
        MapFacilityDetailSheetState(
            detail = selectedFacilityDetail,
            isBookmarked = selectedFacilityBookmarkState.isBookmarked,
            isBookmarkUpdating = selectedFacilityBookmarkState.isUpdating,
            bookmarkErrorMessage = selectedFacilityBookmarkState.errorMessage,
        )

    private fun loadSelectedFacilityBookmarkState(detail: FacilityDetailSeed) {
        viewModelScope.launch {
            runCatching { bookmarkRepository.isBookmarked(detail.facilityId) }
                .onSuccess { isBookmarked ->
                    if (selectedFacilityDetail?.facilityId != detail.facilityId) return@onSuccess
                    selectedFacilityBookmarkState =
                        SelectedFacilityBookmarkState(
                            facilityId = detail.facilityId,
                            isBookmarked = isBookmarked,
                        )
                    renderSelectedFacilityState()
                }
                .onFailure {
                    if (selectedFacilityDetail?.facilityId != detail.facilityId) return@onFailure
                    selectedFacilityBookmarkState =
                        SelectedFacilityBookmarkState(
                            facilityId = detail.facilityId,
                            errorMessage = BOOKMARK_LOAD_ERROR_MESSAGE,
                        )
                    renderSelectedFacilityState()
                }
        }
    }

    private fun toggleSelectedFacilityBookmark() {
        val detail = selectedFacilityDetail ?: return
        val currentBookmarkState = selectedFacilityBookmarkState
        if (currentBookmarkState.isUpdating) return

        val nextBookmarked = !currentBookmarkState.isBookmarked
        selectedFacilityBookmarkState =
            currentBookmarkState.copy(
                isBookmarked = nextBookmarked,
                isUpdating = true,
                errorMessage = null,
            )
        renderSelectedFacilityState()

        viewModelScope.launch {
            runCatching {
                if (nextBookmarked) {
                    bookmarkRepository.saveBookmark(detail.toBookmarkData())
                } else {
                    bookmarkRepository.deleteBookmark(detail.facilityId)
                }
            }.onSuccess {
                if (selectedFacilityDetail?.facilityId != detail.facilityId) return@onSuccess
                selectedFacilityBookmarkState =
                    selectedFacilityBookmarkState.copy(
                        isBookmarked = nextBookmarked,
                        isUpdating = false,
                        errorMessage = null,
                    )
                renderSelectedFacilityState()
                emitUiEvent(
                    MapUiEvent.ShowSnackbar(
                        if (nextBookmarked) {
                            BOOKMARK_SAVE_SUCCESS_MESSAGE
                        } else {
                            BOOKMARK_DELETE_SUCCESS_MESSAGE
                        },
                    ),
                )
            }
            .onFailure {
                if (selectedFacilityDetail?.facilityId != detail.facilityId) return@onFailure
                selectedFacilityBookmarkState =
                    currentBookmarkState.copy(
                        isUpdating = false,
                        errorMessage = BOOKMARK_SAVE_FAILURE_MESSAGE,
                    )
                renderSelectedFacilityState()
                emitUiEvent(MapUiEvent.ShowSnackbar(BOOKMARK_SAVE_FAILURE_MESSAGE))
            }
        }
    }

    private fun refreshRecentDestinations() {
        viewModelScope.launch {
            recentDestinations =
                runCatching {
                    searchRepository.getRecentDestinations()
                }.getOrDefault(emptyList())
            mutableUiState.update { state ->
                state.copy(
                    recentDestinations = recentDestinations.take(MAX_MAP_HOME_RECENT_DESTINATIONS),
                )
            }
        }
    }

    private fun applyShortcutFilter(
        overlayState: com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState,
    ): com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState {
        if (selectedShortcutFilterKey != MapShortcutFilterKey.ACCESSIBLE_PARKING) {
            return overlayState
        }

        val markers =
            overlayState.markers.map { marker ->
                if (
                    marker.displayState == MapMarkerDisplayState.VISIBLE &&
                    AccessibilityTag.ACCESSIBLE_PARKING in marker.accessibilityTags
                ) {
                    marker
                } else {
                    marker.copy(displayState = MapMarkerDisplayState.HIDDEN_BY_FILTER)
                }
            }

        return overlayState.copy(
            markers = markers,
            visibleMarkerCount = markers.count { marker -> marker.displayState == MapMarkerDisplayState.VISIBLE },
        )
    }

    private fun createShortcutFilterState(
        browseData: FacilityBrowseData? = facilityBrowseData,
    ): MapShortcutFilterRowState {
        val availableCategories = browseData?.availableCategories?.toSet().orEmpty()
        val hasAccessibleParking =
            browseData?.allMarkers?.any { marker ->
                AccessibilityTag.ACCESSIBLE_PARKING in marker.accessibilityTags
            } == true

        return MapShortcutFilterRowState(
            chips =
                SHORTCUT_FILTER_ORDER.map { key ->
                    MapShortcutFilterChipState(
                        key = key,
                        isSelected = selectedShortcutFilterKey == key,
                        isEnabled =
                            when (key) {
                                MapShortcutFilterKey.TOILET -> FacilityCategory.TOILET in availableCategories
                                MapShortcutFilterKey.ELEVATOR -> FacilityCategory.ELEVATOR in availableCategories
                                MapShortcutFilterKey.ACCESSIBLE_PARKING -> hasAccessibleParking
                                MapShortcutFilterKey.MORE -> true
                                MapShortcutFilterKey.CHARGING_STATION ->
                                    FacilityCategory.CHARGING_STATION in availableCategories

                                MapShortcutFilterKey.BRAILLE_BLOCK -> FacilityCategory.BRAILLE_BLOCK in availableCategories
                                MapShortcutFilterKey.TOURIST_ATTRACTION ->
                                    FacilityCategory.TOURIST_ATTRACTION in availableCategories

                                MapShortcutFilterKey.RESTAURANT -> FacilityCategory.RESTAURANT in availableCategories
                            },
                    )
                },
        )
    }

    private fun shortcutFilterKeyForSelection(selection: MapFilterSelectionState): MapShortcutFilterKey? {
        if (selection.isShowingAllCategories) return null
        if (selection.selectedBrailleBlockTypes.isNotEmpty()) return null
        if (selection.selectedFacilityCategories.size != 1) return null
        return selection.selectedFacilityCategories.single().toShortcutFilterKey()
    }

    private fun emitUiEvent(event: MapUiEvent) {
        mutableUiEvent.trySend(event)
    }

    private enum class LocationLookupState {
        Idle,
        Searching,
        TimedOut,
    }

    companion object {
        private val SHORTCUT_FILTER_ORDER =
            listOf(
                MapShortcutFilterKey.TOILET,
                MapShortcutFilterKey.ELEVATOR,
                MapShortcutFilterKey.ACCESSIBLE_PARKING,
                MapShortcutFilterKey.MORE,
                MapShortcutFilterKey.CHARGING_STATION,
                MapShortcutFilterKey.BRAILLE_BLOCK,
                MapShortcutFilterKey.TOURIST_ATTRACTION,
                MapShortcutFilterKey.RESTAURANT,
            )
        private const val LOCATION_LOOKUP_TIMEOUT_MILLIS = 5_000L
        private const val MAX_MAP_HOME_RECENT_DESTINATIONS = 3
        private const val BOOKMARK_LOAD_ERROR_MESSAGE = "북마크 상태를 확인하지 못했습니다."
        private const val BOOKMARK_SAVE_SUCCESS_MESSAGE = "북마크에 저장했습니다."
        private const val BOOKMARK_DELETE_SUCCESS_MESSAGE = "북마크를 해제했습니다."
        private const val BOOKMARK_SAVE_FAILURE_MESSAGE = "북마크 저장에 실패했습니다. 다시 시도해 주세요."

        fun provideFactory(
            locationPermissionManager: LocationPermissionManager,
            currentLocationManager: CurrentLocationManager,
            destinationSelectionRepository: DestinationSelectionRepository,
            facilitySeedRepository: FacilitySeedRepository,
            bookmarkRepository: BookmarkRepository,
            searchRepository: SearchRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
                        return MapViewModel(
                            locationPermissionManager = locationPermissionManager,
                            currentLocationManager = currentLocationManager,
                            destinationSelectionRepository = destinationSelectionRepository,
                            facilitySeedRepository = facilitySeedRepository,
                            bookmarkRepository = bookmarkRepository,
                            searchRepository = searchRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private object NoOpSearchRepository : SearchRepository {
    override suspend fun search(query: com.ssafy.e102.eumgil.core.model.SearchQuery) = emptyList<com.ssafy.e102.eumgil.core.model.SearchResult>()

    override suspend fun getRecentSearches() = emptyList<com.ssafy.e102.eumgil.core.model.RecentSearch>()

    override suspend fun saveRecentSearch(keyword: String) = Unit

    override suspend fun getRecentDestinations(): List<RecentDestination> = emptyList()

    override suspend fun saveRecentDestination(destination: RecentDestination) = Unit
}

private data class SelectedFacilityBookmarkState(
    val facilityId: String? = null,
    val isBookmarked: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
)

private fun MapShortcutFilterKey.toFacilityCategory(): FacilityCategory? =
    when (this) {
        MapShortcutFilterKey.TOILET -> FacilityCategory.TOILET
        MapShortcutFilterKey.ELEVATOR -> FacilityCategory.ELEVATOR
        MapShortcutFilterKey.CHARGING_STATION -> FacilityCategory.CHARGING_STATION
        MapShortcutFilterKey.BRAILLE_BLOCK -> FacilityCategory.BRAILLE_BLOCK
        MapShortcutFilterKey.TOURIST_ATTRACTION -> FacilityCategory.TOURIST_ATTRACTION
        MapShortcutFilterKey.RESTAURANT -> FacilityCategory.RESTAURANT
        MapShortcutFilterKey.ACCESSIBLE_PARKING -> null
        MapShortcutFilterKey.MORE -> null
    }

private fun FacilityCategory.toShortcutFilterKey(): MapShortcutFilterKey? =
    when (this) {
        FacilityCategory.TOILET -> MapShortcutFilterKey.TOILET
        FacilityCategory.ELEVATOR -> MapShortcutFilterKey.ELEVATOR
        FacilityCategory.CHARGING_STATION -> MapShortcutFilterKey.CHARGING_STATION
        FacilityCategory.BRAILLE_BLOCK -> MapShortcutFilterKey.BRAILLE_BLOCK
        FacilityCategory.TOURIST_ATTRACTION -> MapShortcutFilterKey.TOURIST_ATTRACTION
        FacilityCategory.RESTAURANT -> MapShortcutFilterKey.RESTAURANT
        FacilityCategory.OTHER -> null
    }
