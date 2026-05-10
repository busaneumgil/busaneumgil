package com.ssafy.e102.eumgil.feature.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.LocationPermissionUnavailableReason as PermissionUnavailableReason
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.location.isFreshCurrentLocation
import com.ssafy.e102.eumgil.core.model.FacilityBrowseData
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.MapPlaceClickType
import com.ssafy.e102.eumgil.core.model.MapPlaceDetailRequest
import com.ssafy.e102.eumgil.core.model.MapTappedPlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.RouteSelectionRequestReason
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.data.repository.toBookmarkData
import com.ssafy.e102.eumgil.feature.map.model.KAKAO_MAP_MAX_ZOOM_LEVEL
import com.ssafy.e102.eumgil.feature.map.model.KAKAO_MAP_MIN_ZOOM_LEVEL
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapDefaults
import com.ssafy.e102.eumgil.feature.map.model.MapFilterSelectionState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterChipState
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterKey
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterRowState
import com.ssafy.e102.eumgil.feature.map.model.defaultZoomLevel
import com.ssafy.e102.eumgil.feature.map.model.resolvedZoomLevel
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
import java.util.Locale

class MapViewModel(
    private val locationPermissionManager: LocationPermissionManager,
    private val currentLocationManager: CurrentLocationManager,
    private val destinationSelectionRepository: DestinationSelectionRepository,
    private val facilitySeedRepository: FacilitySeedRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val searchRepository: SearchRepository = NoOpSearchRepository,
    private val placesRepository: PlacesRepository? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = Channel<MapUiEvent>(capacity = Channel.BUFFERED)
    val uiEvent: Flow<MapUiEvent> = mutableUiEvent.receiveAsFlow()

    private var latestPermissionState: LocationPermissionState = locationPermissionManager.permissionState.value
    private var latestLocation: LocationSnapshot? = currentLocationManager.latestLocation.value.toFreshCurrentLocationOrNull()
    private var selectedDestination: PlaceDestination? = destinationSelectionRepository.selectedDestination.value
    private var selectedMarkerId: String? = null
    private var selectedMapPinCoordinate: MapCoordinate? = null
    private var selectedFacilityDetail: FacilityDetailSeed? = null
    private var selectedMapTapDetail: MapTappedPlaceDetail? = null
    private var selectedMapTapNameHint: String? = null
    private var isMapTapDetailLoading = false
    private var mapTapDetailErrorMessage: String? = null
    private var mapTapDetailRequestId: Long = 0L
    private var mapTapDetailLookupJob: Job? = null
    private var selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
    private var facilityBrowseData: FacilityBrowseData? = null
    private var markerFilterSelectionState: MapFilterSelectionState = MapFilterSelectionState()
    private var recentDestinations: List<RecentDestination> = emptyList()
    private var isRouteStarted = false
    private var locationLookupState: LocationLookupState = LocationLookupState.Idle
    private var locationLookupTimeoutJob: Job? = null
    private var isRecenterButtonActive = false
    private var lastPlacesBrowseAnchorSource: PlacesBrowseAnchorSource? = null
    private var lastMarkerOverlayLogSnapshot: String? = null

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
        isRecenterButtonActive = false
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
        isRecenterButtonActive = false
        stopLocationLookup()
        currentLocationManager.stopLocationUpdates()
    }

    fun onAction(action: MapUiAction) {
        when (action) {
            MapUiAction.FacilityBookmarkClicked -> toggleSelectedFacilityBookmark()
            MapUiAction.FacilityDetailDismissed -> dismissFacilityDetailSheet()
            MapUiAction.FacilitySetDestinationClicked -> handleFacilitySetDestinationClicked()
            MapUiAction.LocationActionClicked -> handleLocationAction()
            MapUiAction.ZoomInClicked -> handleZoomAction(delta = 1)
            MapUiAction.ZoomOutClicked -> handleZoomAction(delta = -1)
            is MapUiAction.MarkerTapped -> handleMarkerTapped(action.markerId)
            is MapUiAction.MapTapped -> handleMapTapped(action.payload)
            is MapUiAction.ViewportCameraChanged ->
                handleViewportCameraChanged(
                    center = action.center,
                    zoomLevel = action.zoomLevel,
                    isUserGesture = action.isUserGesture,
                )
            MapUiAction.MarkerCategoryFilterReset -> resetMarkerCategoryFilter()
            is MapUiAction.MarkerCategoryFilterToggled -> toggleMarkerCategoryFilter(action.category)
            is MapUiAction.ShortcutFilterClicked -> handleShortcutFilterClicked(action.key)
            is MapUiAction.RecentDestinationRouteClicked -> handleRecentDestinationRouteClicked(action.placeId)
            MapUiAction.SearchEntryClicked -> emitUiEvent(MapUiEvent.NavigateToSearch)
        }
    }

    override fun onCleared() {
        stopLocationLookup()
        mapTapDetailLookupJob?.cancel()
        currentLocationManager.stopLocationUpdates()
        mutableUiEvent.close()
        super.onCleared()
    }

    private fun loadMarkerBrowseState() {
        val placesRepository = placesRepository
        if (placesRepository != null) {
            safeLogInfo(
                MAP_VIEW_MODEL_LOG_TAG,
                "Loading live places browse state source=${currentPlacesBrowseAnchorSource().name}",
            )
            loadLivePlaceBrowseState(
                anchorSource = currentPlacesBrowseAnchorSource(),
            )
            return
        }

        safeLogInfo(MAP_VIEW_MODEL_LOG_TAG, "Loading facility seed browse state")
        viewModelScope.launch {
            runCatching {
                facilitySeedRepository.getFacilityBrowseData()
            }.onSuccess { browseData ->
                facilityBrowseData = browseData
                safeLogInfo(
                    MAP_VIEW_MODEL_LOG_TAG,
                    "Seed browse success markers=${browseData.facilityMarkers.size} categories=${browseData.availableCategories.size}",
                )
                markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
                renderMarkerBrowseState()
            }.onFailure {
                safeLogError(MAP_VIEW_MODEL_LOG_TAG, "Seed browse load failed", it)
                facilityBrowseData = null
                updateSelectedFacility(markerId = null)
                markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
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

    private fun loadLivePlaceBrowseState(
        anchorSource: PlacesBrowseAnchorSource,
        force: Boolean = false,
    ) {
        val placesRepository = placesRepository ?: return
        if (!force && lastPlacesBrowseAnchorSource == anchorSource && facilityBrowseData != null) {
            safeLogDebug(
                MAP_VIEW_MODEL_LOG_TAG,
                "Skipping live places browse reload source=${anchorSource.name} reason=cached",
            )
            return
        }

        val anchor = currentPlacesBrowseAnchor()
        safeLogInfo(
            MAP_VIEW_MODEL_LOG_TAG,
            "Requesting places browse source=${anchorSource.name} force=$force lat=${anchor.latitude.toLogCoordinate()} lng=${anchor.longitude.toLogCoordinate()} radius=$MAP_BROWSE_RADIUS_METERS",
        )
        viewModelScope.launch {
            runCatching {
                placesRepository.getPlaces(
                    com.ssafy.e102.eumgil.core.model.PlaceQuery(
                        latitude = anchor.latitude,
                        longitude = anchor.longitude,
                        radiusMeters = MAP_BROWSE_RADIUS_METERS,
                    ),
                )
            }.onSuccess { places ->
                val browseData = MapPlaceBrowseDataMapper.toBrowseData(places)
                facilityBrowseData = browseData
                lastPlacesBrowseAnchorSource = anchorSource
                safeLogInfo(
                    MAP_VIEW_MODEL_LOG_TAG,
                    "Places browse success source=${anchorSource.name} places=${places.size} markers=${browseData.facilityMarkers.size} categories=${browseData.availableCategories.size}",
                )
                if (places.isEmpty()) {
                    safeLogWarn(
                        MAP_VIEW_MODEL_LOG_TAG,
                        "Places browse returned no data source=${anchorSource.name} lat=${anchor.latitude.toLogCoordinate()} lng=${anchor.longitude.toLogCoordinate()} radius=$MAP_BROWSE_RADIUS_METERS",
                    )
                }
                markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
                renderMarkerBrowseState()
            }.onFailure {
                safeLogError(
                    MAP_VIEW_MODEL_LOG_TAG,
                    "Places browse failed source=${anchorSource.name} lat=${anchor.latitude.toLogCoordinate()} lng=${anchor.longitude.toLogCoordinate()} radius=$MAP_BROWSE_RADIUS_METERS",
                    it,
                )
                facilityBrowseData = null
                lastPlacesBrowseAnchorSource = null
                updateSelectedFacility(markerId = null)
                markerFilterSelectionState = MapBrowseStateFactory.resetSelection()
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
            renderSelectedFacilityState()
            return
        }

        val previewDetail = facilityBrowseData?.detailFor(markerId)
        val placesRepository = placesRepository
        if (placesRepository != null) {
            if (previewDetail != null) {
                updateSelectedFacility(
                    markerId = markerId,
                    detail = previewDetail,
                    loadBookmarkState = false,
                )
            } else {
                clearMapTapSelectionState(clearPin = true)
                selectedMarkerId = markerId
                selectedFacilityDetail = null
                selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
            }
            renderSelectedFacilityState()
            fetchSelectedFacilityDetail(
                markerId = markerId,
                fallbackDetail = previewDetail,
            )
            return
        }

        updateSelectedFacility(markerId = markerId)
        renderSelectedFacilityState()
    }

    private fun handleMapTapped(payload: MapTapPayload) {
        val coordinate = payload.coordinate
        safeLogInfo(
            MAP_VIEW_MODEL_LOG_TAG,
            "Map tapped lat=${coordinate.latitude.toLogCoordinate()} lng=${coordinate.longitude.toLogCoordinate()} clickType=${payload.clickType.name} provider=${payload.provider.orEmpty()} providerPlaceId=${payload.providerPlaceId.orEmpty()}",
        )
        mapTapDetailRequestId += 1L
        val requestId = mapTapDetailRequestId
        selectedMapPinCoordinate = coordinate
        clearSelectedFacilitySelection(clearMapTapSelection = false)
        selectedMapTapDetail = null
        selectedMapTapNameHint = payload.nameHint?.takeIf { it.isNotBlank() }
        mapTapDetailErrorMessage = null
        isMapTapDetailLoading = placesRepository != null
        renderSelectedFacilityState()

        val placesRepository = placesRepository ?: return
        mapTapDetailLookupJob?.cancel()
        mapTapDetailLookupJob =
            viewModelScope.launch {
                runCatching {
                    placesRepository.getMapTappedPlaceDetail(payload.toMapPlaceDetailRequest())
                }.onSuccess { detail ->
                    if (requestId != mapTapDetailRequestId) return@onSuccess
                    isMapTapDetailLoading = false
                    selectedMapTapDetail = detail
                    mapTapDetailErrorMessage =
                        if (detail == null) {
                            MAP_TAP_DETAIL_EMPTY_MESSAGE
                        } else {
                            null
                        }
                    renderSelectedFacilityState()
                }.onFailure {
                    if (requestId != mapTapDetailRequestId) return@onFailure
                    isMapTapDetailLoading = false
                    selectedMapTapDetail = null
                    mapTapDetailErrorMessage = MAP_TAP_DETAIL_LOAD_FAILURE_MESSAGE
                    renderSelectedFacilityState()
                }
            }
    }

    private fun fetchSelectedFacilityDetail(
        markerId: String,
        fallbackDetail: FacilityDetailSeed?,
    ) {
        val placesRepository = placesRepository ?: return
        viewModelScope.launch {
            runCatching { placesRepository.getPlaceDetail(markerId) }
                .onSuccess { placeDetail ->
                    if (selectedMarkerId != markerId) return@onSuccess
                    if (placeDetail == null) {
                        fallbackDetail?.let(::loadSelectedFacilityBookmarkState)
                        return@onSuccess
                    }

                    updateSelectedFacility(
                        markerId = markerId,
                        detail = MapPlaceBrowseDataMapper.toFacilityDetailSeed(placeDetail),
                        loadBookmarkState = false,
                    )
                    selectedFacilityBookmarkState =
                        SelectedFacilityBookmarkState(
                            facilityId = markerId,
                            isBookmarked = placeDetail.isBookmarked,
                        )
                    renderSelectedFacilityState()
                }.onFailure {
                    if (selectedMarkerId != markerId) return@onFailure
                    fallbackDetail?.let(::loadSelectedFacilityBookmarkState)
                }
        }
    }

    private fun dismissFacilityDetailSheet() {
        if (!clearSelectedFacilitySelection()) return
        renderSelectedFacilityState()
    }

    private fun handleFacilitySetDestinationClicked() {
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
        renderMarkerBrowseState()
    }

    private fun handleShortcutFilterClicked(key: MapShortcutFilterKey) {
        val browseData = facilityBrowseData ?: return

        val category = key.toFacilityCategory(browseData.availableCategories.toSet())
        if (category == null) {
            emitUiEvent(MapUiEvent.ShowSnackbar(SHORTCUT_FILTER_UNAVAILABLE_MESSAGE))
            return
        }
        markerFilterSelectionState =
            MapBrowseStateFactory.toggleCategory(
                selection = markerFilterSelectionState,
                browseData = browseData,
                category = category,
            )
        renderMarkerBrowseState()
    }

    private fun renderMarkerBrowseState() {
        val browseData = facilityBrowseData ?: return
        val availableCategories = browseData.availableCategories.toSet()
        val baseOverlayState =
            MapBrowseStateFactory.createMarkerOverlayState(
                browseData = browseData,
                selection = markerFilterSelectionState,
            )
        val overlayState = baseOverlayState
        val filterState =
            MapBrowseStateFactory.createFilterUiState(
                browseData = browseData,
                selection = markerFilterSelectionState,
                overlayState = overlayState,
            )
        val overlayLogSnapshot =
            buildString {
                append("total=")
                append(overlayState.totalMarkerCount)
                append(" visible=")
                append(overlayState.visibleMarkerCount)
                append(" emptyData=")
                append(overlayState.isEmptyData)
                append(" emptyResult=")
                append(overlayState.isEmptyResult)
                append(" shortcut=")
                append(
                    SHORTCUT_FILTER_ORDER
                        .filter { key -> key.isSelected(selection = filterState.selection, availableCategories = availableCategories) }
                        .joinToString(",") { key -> key.name }
                        .ifEmpty {
                            if (filterState.selection.isShowingAllCategories) {
                                "ALL"
                            } else {
                                "NONE"
                            }
                        },
                )
                append(" categories=")
                append(filterState.selection.selectedFacilityCategories.joinToString(",") { category -> category.name }.ifEmpty {
                    if (filterState.selection.isShowingAllCategories) {
                        "ALL"
                    } else {
                        "NONE"
                    }
                })
            }
        if (lastMarkerOverlayLogSnapshot != overlayLogSnapshot) {
            lastMarkerOverlayLogSnapshot = overlayLogSnapshot
            safeLogInfo(MAP_VIEW_MODEL_LOG_TAG, "Marker overlay state $overlayLogSnapshot")
        }

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
                if (destination != null) {
                    isRecenterButtonActive = false
                }

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
            destinationSelectionRepository.selectionRequests.collectLatest { request ->
                if (
                    request.reason != RouteSelectionRequestReason.DESTINATION_UPDATED &&
                    request.reason != RouteSelectionRequestReason.DESTINATION_CLEARED &&
                    request.reason != RouteSelectionRequestReason.SWAPPED
                ) {
                    return@collectLatest
                }
                // Any destination handoff should close stale facility detail state before the map recenters.
                if (clearSelectedFacilitySelection()) {
                    renderSelectedFacilityState()
                }
                val destination = request.state.selectedDestination
                isRecenterButtonActive = false
                if (destination == null) {
                    applyFallbackCameraTarget()
                } else {
                    syncCameraToSelectedDestination(
                        destination = destination,
                        incrementRequestId = true,
                    )
                }
                renderUiState()
            }
        }
    }

    private fun observeLocationUpdates() {
        viewModelScope.launch {
            currentLocationManager.latestLocation.collectLatest { snapshot ->
                val hadLocation = latestLocation != null
                latestLocation = snapshot.toFreshCurrentLocationOrNull()
                val freshLocation = latestLocation

                if (freshLocation == null) {
                    isRecenterButtonActive = false
                    if (latestPermissionState is LocationPermissionState.Granted && isRouteStarted) {
                        startLocationLookup(forceRestart = false)
                    }
                    applyFallbackCameraTarget()
                } else {
                    stopLocationLookup()
                    if (placesRepository != null && lastPlacesBrowseAnchorSource != PlacesBrowseAnchorSource.CURRENT_LOCATION) {
                        loadLivePlaceBrowseState(
                            anchorSource = PlacesBrowseAnchorSource.CURRENT_LOCATION,
                            force = true,
                        )
                    }
                    if (shouldSyncCameraToCurrentLocation()) {
                        val shouldIncrementRequestId =
                            !hadLocation ||
                                mutableUiState.value.cameraTarget.source != MapCameraSource.CURRENT_LOCATION

                        syncCameraToCurrentLocation(
                            snapshot = freshLocation,
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
        isRecenterButtonActive = false
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
                isRecenterButtonActive = true
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
            LocationPermissionState.Denied -> {
                isRecenterButtonActive = false
                emitUiEvent(MapUiEvent.RequestLocationPermission)
            }
            is LocationPermissionState.Unavailable -> {
                isRecenterButtonActive = false
                applyFallbackCameraTarget()
                renderUiState()
            }
        }
    }

    private fun handleZoomAction(delta: Int) {
        mutableUiState.update { state ->
            val currentZoomLevel = state.cameraTarget.resolvedZoomLevel()
            val nextZoomLevel =
                (currentZoomLevel + delta)
                    .coerceIn(KAKAO_MAP_MIN_ZOOM_LEVEL, KAKAO_MAP_MAX_ZOOM_LEVEL)
            if (nextZoomLevel == currentZoomLevel) {
                state
            } else {
                state.copy(
                    cameraTarget =
                        state.cameraTarget.copy(
                            requestId = state.cameraTarget.requestId + 1L,
                            zoomLevel = nextZoomLevel,
                        ),
                )
            }
        }
    }

    private fun handleViewportCameraChanged(
        center: MapCoordinate,
        zoomLevel: Int,
        isUserGesture: Boolean,
    ) {
        mutableUiState.update { state ->
            val currentTarget = state.cameraTarget
            val hasCameraChanged =
                currentTarget.center != center ||
                    currentTarget.resolvedZoomLevel() != zoomLevel

            if (!hasCameraChanged) {
                state
            } else {
                if (isUserGesture) {
                    isRecenterButtonActive = false
                }
                state.copy(
                    cameraTarget =
                        currentTarget.copy(
                            center = center,
                            zoomLevel = zoomLevel,
                        ),
                    isRecenterButtonActive = if (isUserGesture) false else state.isRecenterButtonActive,
                )
            }
        }
    }

    private fun startLocationTracking(forceLookupRestart: Boolean) {
        if (!isRouteStarted) return

        currentLocationManager.startLocationUpdates()
        currentLocationManager.refreshLatestLocation()
        latestLocation = currentLocationManager.latestLocation.value.toFreshCurrentLocationOrNull()

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
            val nextZoomLevel =
                if (shouldIncrement) {
                    MapCameraSource.CURRENT_LOCATION.defaultZoomLevel()
                } else {
                    state.cameraTarget.zoomLevel
                }

            state.copy(
                cameraTarget =
                    MapCameraTarget(
                        center = coordinate,
                        source = MapCameraSource.CURRENT_LOCATION,
                        requestId = nextRequestId,
                        zoomLevel = nextZoomLevel,
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
            val nextZoomLevel =
                if (shouldIncrement) {
                    MapCameraSource.SEARCH_RESULT.defaultZoomLevel()
                } else {
                    state.cameraTarget.zoomLevel
                }

            state.copy(
                cameraTarget =
                    MapCameraTarget(
                        center = coordinate,
                        source = MapCameraSource.SEARCH_RESULT,
                        requestId = nextRequestId,
                        zoomLevel = nextZoomLevel,
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
                            zoomLevel = MapCameraSource.DEFAULT_BUSAN.defaultZoomLevel(),
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

        if (recenterButtonState != MapRecenterButtonState.ENABLED) {
            isRecenterButtonActive = false
        }

        mutableUiState.update { state ->
            state.copy(
                selectedDestination = selectedDestination,
                locationStatus = locationStatus,
                recenterButtonState = recenterButtonState,
                isRecenterButtonActive = isRecenterButtonActive,
            )
        }
    }

    private fun renderSelectedFacilityState() {
        mutableUiState.update { state ->
            state.copy(
                selectedMarkerId = selectedMarkerId,
                selectedMapPinCoordinate = selectedMapPinCoordinate,
                facilityDetailSheetState = currentFacilityDetailSheetState(),
            )
        }
    }

    private fun clearSelectedFacilitySelection(clearMapTapSelection: Boolean = true): Boolean {
        val hadSelection =
            selectedMarkerId != null ||
                selectedFacilityDetail != null ||
                (
                    clearMapTapSelection &&
                        (
                            selectedMapPinCoordinate != null ||
                                selectedMapTapDetail != null ||
                                isMapTapDetailLoading ||
                                mapTapDetailErrorMessage != null
                        )
                )
        if (!hadSelection) return false

        selectedMarkerId = null
        selectedFacilityDetail = null
        selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
        if (clearMapTapSelection) {
            clearMapTapSelectionState(clearPin = true)
        }
        return true
    }

    private fun clearMapTapSelectionState(clearPin: Boolean) {
        mapTapDetailLookupJob?.cancel()
        mapTapDetailLookupJob = null
        mapTapDetailRequestId += 1L
        if (clearPin) {
            selectedMapPinCoordinate = null
        }
        selectedMapTapDetail = null
        selectedMapTapNameHint = null
        isMapTapDetailLoading = false
        mapTapDetailErrorMessage = null
    }

    private fun updateSelectedFacility(
        markerId: String?,
        detail: FacilityDetailSeed? = markerId?.let { facilityBrowseData?.detailFor(it) },
        loadBookmarkState: Boolean = true,
    ) {
        if (markerId != null && detail == null) {
            selectedMarkerId = null
            selectedFacilityDetail = null
            selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
            return
        }

        val previousFacilityId = selectedFacilityDetail?.facilityId
        if (markerId != null) {
            clearMapTapSelectionState(clearPin = true)
        }
        selectedMarkerId = markerId
        selectedFacilityDetail = detail
        if (detail == null) {
            selectedFacilityBookmarkState = SelectedFacilityBookmarkState()
        } else if (previousFacilityId != detail.facilityId) {
            selectedFacilityBookmarkState =
                if (loadBookmarkState) {
                    SelectedFacilityBookmarkState(
                        facilityId = detail.facilityId,
                        isUpdating = true,
                    )
                } else {
                    SelectedFacilityBookmarkState(
                        facilityId = detail.facilityId,
                    )
                }
            if (loadBookmarkState) {
                loadSelectedFacilityBookmarkState(detail)
            }
        }
    }

    private fun currentFacilityDetailSheetState(): MapFacilityDetailSheetState =
        MapFacilityDetailSheetState(
            detail = selectedFacilityDetail,
            mapTapDetail = selectedMapTapDetail,
            mapTapNameHint = selectedMapTapNameHint,
            isMapTapDetailLoading = isMapTapDetailLoading,
            mapTapDetailErrorMessage = mapTapDetailErrorMessage,
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

    private fun createShortcutFilterState(
        browseData: FacilityBrowseData? = facilityBrowseData,
    ): MapShortcutFilterRowState {
        val availableCategories = browseData?.availableCategories?.toSet().orEmpty()
        val selection = markerFilterSelectionState

        return MapShortcutFilterRowState(
            chips =
                SHORTCUT_FILTER_ORDER.map { key ->
                    MapShortcutFilterChipState(
                        key = key,
                        isSelected = key.isSelected(selection = selection, availableCategories = availableCategories),
                        isEnabled = key.toFacilityCategory(availableCategories) != null,
                    )
                },
        )
    }

    private fun emitUiEvent(event: MapUiEvent) {
        mutableUiEvent.trySend(event)
    }

    private fun currentPlacesBrowseAnchor(): com.ssafy.e102.eumgil.feature.map.model.MapCoordinate =
        latestLocation?.toMapCoordinate() ?: mutableUiState.value.cameraTarget.center

    private fun currentPlacesBrowseAnchorSource(): PlacesBrowseAnchorSource =
        if (latestLocation != null) {
            PlacesBrowseAnchorSource.CURRENT_LOCATION
        } else {
            PlacesBrowseAnchorSource.FALLBACK
        }

    private fun LocationSnapshot?.toFreshCurrentLocationOrNull(): LocationSnapshot? =
        this?.takeIf { snapshot -> snapshot.isFreshCurrentLocation() }

    private enum class LocationLookupState {
        Idle,
        Searching,
        TimedOut,
    }

    private enum class PlacesBrowseAnchorSource {
        FALLBACK,
        CURRENT_LOCATION,
    }

    companion object {
        private const val MAP_VIEW_MODEL_LOG_TAG = "MapViewModel"
        private val SHORTCUT_FILTER_ORDER =
            listOf(
                MapShortcutFilterKey.TOILET,
                MapShortcutFilterKey.ELEVATOR,
                MapShortcutFilterKey.CHARGING_STATION,
                MapShortcutFilterKey.FOOD_CAFE,
                MapShortcutFilterKey.TOURIST_SPOT,
                MapShortcutFilterKey.ACCOMMODATION,
                MapShortcutFilterKey.HEALTHCARE,
                MapShortcutFilterKey.WELFARE,
                MapShortcutFilterKey.PUBLIC_OFFICE,
            )
        private const val LOCATION_LOOKUP_TIMEOUT_MILLIS = 5_000L
        private const val MAP_BROWSE_RADIUS_METERS = 1_000
        private const val MAX_MAP_HOME_RECENT_DESTINATIONS = 3
        private const val BOOKMARK_LOAD_ERROR_MESSAGE = "북마크 상태를 확인하지 못했습니다."
        private const val BOOKMARK_SAVE_SUCCESS_MESSAGE = "북마크에 저장했습니다."
        private const val BOOKMARK_DELETE_SUCCESS_MESSAGE = "북마크를 해제했습니다."
        private const val BOOKMARK_SAVE_FAILURE_MESSAGE = "북마크 저장에 실패했습니다. 다시 시도해 주세요."
        private const val SHORTCUT_FILTER_UNAVAILABLE_MESSAGE = "근처에 해당 장소가 없어요"

        private const val MAP_TAP_DETAIL_LOAD_FAILURE_MESSAGE = "선택한 위치의 상세 정보를 불러오지 못했습니다."
        private const val MAP_TAP_DETAIL_EMPTY_MESSAGE = "선택한 위치의 상세 정보가 없습니다."

        fun provideFactory(
            locationPermissionManager: LocationPermissionManager,
            currentLocationManager: CurrentLocationManager,
            destinationSelectionRepository: DestinationSelectionRepository,
            facilitySeedRepository: FacilitySeedRepository,
            bookmarkRepository: BookmarkRepository,
            searchRepository: SearchRepository,
            placesRepository: PlacesRepository? = null,
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
                            placesRepository = placesRepository,
                        ) as T
                    }

                    error("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

private fun Double.toLogCoordinate(): String = String.format(Locale.US, "%.6f", this)

private fun safeLogDebug(
    tag: String,
    message: String,
) {
    runCatching { Log.d(tag, message) }
}

private fun safeLogInfo(
    tag: String,
    message: String,
) {
    runCatching { Log.i(tag, message) }
}

private fun safeLogWarn(
    tag: String,
    message: String,
) {
    runCatching { Log.w(tag, message) }
}

private fun safeLogError(
    tag: String,
    message: String,
    throwable: Throwable,
) {
    runCatching { Log.e(tag, message, throwable) }
}

private object NoOpSearchRepository : SearchRepository {
    override suspend fun search(query: com.ssafy.e102.eumgil.core.model.SearchQuery) = emptyList<com.ssafy.e102.eumgil.core.model.SearchResult>()

    override suspend fun getRecentSearches() = emptyList<com.ssafy.e102.eumgil.core.model.RecentSearch>()

    override suspend fun saveRecentSearch(keyword: String) = Unit

    override suspend fun getRecentDestinations(): List<RecentDestination> = emptyList()

    override suspend fun saveRecentDestination(destination: RecentDestination) = Unit
}

private fun MapTapPayload.toMapPlaceDetailRequest(): MapPlaceDetailRequest =
    MapPlaceDetailRequest(
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
        clickType =
            when (clickType) {
                MapTapClickType.POI -> MapPlaceClickType.POI
                MapTapClickType.ADDRESS -> MapPlaceClickType.ADDRESS
            },
        provider = provider,
        providerPlaceId = providerPlaceId,
        nameHint = nameHint,
    )

private data class SelectedFacilityBookmarkState(
    val facilityId: String? = null,
    val isBookmarked: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
)

private fun MapShortcutFilterKey.toFacilityCategory(availableCategories: Set<FacilityCategory>): FacilityCategory? =
    when (this) {
        MapShortcutFilterKey.TOILET -> FacilityCategory.TOILET.takeIf(availableCategories::contains)
        MapShortcutFilterKey.ELEVATOR -> FacilityCategory.ELEVATOR.takeIf(availableCategories::contains)
        MapShortcutFilterKey.CHARGING_STATION -> FacilityCategory.CHARGING_STATION.takeIf(availableCategories::contains)
        MapShortcutFilterKey.FOOD_CAFE ->
            when {
                FacilityCategory.FOOD_CAFE in availableCategories -> FacilityCategory.FOOD_CAFE
                FacilityCategory.RESTAURANT in availableCategories -> FacilityCategory.RESTAURANT
                else -> null
            }

        MapShortcutFilterKey.TOURIST_SPOT ->
            when {
                FacilityCategory.TOURIST_SPOT in availableCategories -> FacilityCategory.TOURIST_SPOT
                FacilityCategory.TOURIST_ATTRACTION in availableCategories -> FacilityCategory.TOURIST_ATTRACTION
                else -> null
            }

        MapShortcutFilterKey.ACCOMMODATION -> FacilityCategory.ACCOMMODATION.takeIf(availableCategories::contains)
        MapShortcutFilterKey.HEALTHCARE -> FacilityCategory.HEALTHCARE.takeIf(availableCategories::contains)
        MapShortcutFilterKey.WELFARE -> FacilityCategory.WELFARE.takeIf(availableCategories::contains)
        MapShortcutFilterKey.PUBLIC_OFFICE -> FacilityCategory.PUBLIC_OFFICE.takeIf(availableCategories::contains)
    }

private fun MapShortcutFilterKey.isSelected(
    selection: MapFilterSelectionState,
    availableCategories: Set<FacilityCategory>,
): Boolean {
    if (selection.isShowingAllCategories) return false
    val category = toFacilityCategory(availableCategories) ?: return false
    return category in selection.selectedFacilityCategories
}
