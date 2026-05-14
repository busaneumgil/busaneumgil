package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteLeg
import com.ssafy.e102.eumgil.core.model.RouteLegRole
import com.ssafy.e102.eumgil.core.model.RouteLegType
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RoutePreviewModel
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.core.model.RouteTransitStop
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.RouteRatingData
import com.ssafy.e102.eumgil.data.repository.RouteRerouteData
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.data.repository.RouteSessionData
import com.ssafy.e102.eumgil.data.repository.RouteTransitArrivalData
import com.ssafy.e102.eumgil.data.repository.RouteTransitRefreshData
import com.ssafy.e102.eumgil.feature.map.component.MapViewportPointKind
import com.ssafy.e102.eumgil.feature.map.component.createNavigationViewportOverlayState
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import com.ssafy.e102.eumgil.feature.route.RouteNavigationSelectionHandoff
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `location updates rebuild runtime remaining distance and eta from route progress`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel = createViewModel(locationManager = locationManager)

            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = WALK_MID_POINT.latitude,
                    longitude = WALK_MID_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals(NavigationScreenState.Ready, viewModel.uiState.value.screenState)
            assertEquals(1, viewModel.uiState.value.segmentSync.activeSegmentIndex)
            assertEquals(1, viewModel.uiState.value.segmentSync.focusedSegmentIndex)
            assertEquals("450m", viewModel.uiState.value.remainingDistanceLabel)
            assertEquals("8분", viewModel.uiState.value.remainingEtaLabel)
        }

    @Test
    fun `selection handoff remaining metrics seed initial navigation summary`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(
                testWalkNavigationRequest().copy(
                    selectionHandoff =
                        RouteNavigationSelectionHandoff(
                            searchId = "search-1",
                            routeId = "walk-route-1",
                            sessionId = "session-1",
                            initialRemainingDistanceMeters = 720,
                            initialRemainingDurationSeconds = 540,
                        ),
                ),
            )
            advanceUntilIdle()

            assertEquals("720m", viewModel.uiState.value.remainingDistanceLabel)
            assertEquals("9분", viewModel.uiState.value.remainingEtaLabel)
        }

    @Test
    fun `walk to transit leg triggers transit refresh near boarding stop`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository = FakeRouteRepository()
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                )

            viewModel.bindNavigationRequest(testTransitNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = TRANSIT_BOARDING_POINT.latitude,
                    longitude = TRANSIT_BOARDING_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals(listOf("transit-route-1" to 2), routeRepository.transitRefreshCalls)
            assertTrue(viewModel.uiState.value.stepCard.supportingText.contains("6 min"))
        }

    @Test
    fun `segment tap focuses map on the tapped segment start coordinate`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 1))
            advanceUntilIdle()

            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.mapOverlay.mapFocusMode)
            assertEquals(
                WALK_MID_POINT,
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
        }

    @Test
    fun `segment tap falls back to source leg start coordinate when the segment polyline is missing`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testLegPolylineFallbackNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 0))
            advanceUntilIdle()

            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.mapOverlay.mapFocusMode)
            assertTrue(viewModel.uiState.value.mapOverlay.focusedSegmentPolyline.isEmpty())
            assertEquals(
                LEG_FALLBACK_START_POINT,
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
        }

    @Test
    fun `segment tap prefers anchor coordinate over source leg start when walk segment polyline is missing`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testPointAnchorNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 0))
            advanceUntilIdle()

            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.mapOverlay.mapFocusMode)
            assertEquals(
                POINT_ANCHOR_EVENT_POINT,
                viewModel.uiState.value.mapOverlay.routeSegments.first().segmentStartCoordinate,
            )
            assertEquals(
                POINT_ANCHOR_EVENT_POINT,
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
        }

    @Test
    fun `segment tap resolves the missing segment focus to its route start coordinate`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testSparseSegmentNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 0))
            advanceUntilIdle()

            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.mapOverlay.mapFocusMode)
            assertTrue(viewModel.uiState.value.mapOverlay.focusedSegmentPolyline.isEmpty())
            assertEquals(
                SPARSE_ROUTE_START_POINT,
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
        }

    @Test
    fun `segment tap resolves later sparse segments to each branch start coordinate`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testMultiSparseSegmentNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 1))
            advanceUntilIdle()

            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.mapOverlay.mapFocusMode)
            assertTrue(viewModel.uiState.value.mapOverlay.focusedSegmentPolyline.isEmpty())
            assertEquals(
                SPARSE_ROUTE_BRANCH_POINT_1,
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 2))
            advanceUntilIdle()

            assertEquals(
                SPARSE_ROUTE_BRANCH_POINT_2,
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
        }

    @Test
    fun `navigation segment marker debug summary reports segment sequence kind and first coordinate`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testLegPolylineFallbackNavigationRequest())
            advanceUntilIdle()

            val summary = createNavigationSegmentMarkerDebugSummary(viewModel.uiState.value.mapOverlay)

            assertTrue(summary.contains("focusMode=ACTIVE"))
            assertTrue(summary.contains("count=2"))
            assertTrue(summary.contains("idx=0 seq=1 kind=WALK polyline=0 first=null"))
            assertTrue(summary.contains("idx=1 seq=2 kind=TRANSIT polyline=2 first=35.180600,129.073500"))
        }

    @Test
    fun `navigation overlay creates fallback junction markers for sparse later segments`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testMultiSparseSegmentNavigationRequest())
            advanceUntilIdle()

            val overlayState = createNavigationViewportOverlayState(viewModel.uiState.value.mapOverlay)
            val junctionCoordinates =
                overlayState.points
                    .filter { point -> point.kind == MapViewportPointKind.SEGMENT_JUNCTION }
                    .map { it.coordinate }

            assertEquals(
                listOf(
                    MapCoordinate(
                        latitude = SPARSE_ROUTE_BRANCH_POINT_1.latitude,
                        longitude = SPARSE_ROUTE_BRANCH_POINT_1.longitude,
                    ),
                    MapCoordinate(
                        latitude = SPARSE_ROUTE_BRANCH_POINT_2.latitude,
                        longitude = SPARSE_ROUTE_BRANCH_POINT_2.longitude,
                    ),
                ),
                junctionCoordinates,
            )
        }

    @Test
    fun `confirming exit navigation routes to arrival flow`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository = FakeRouteRepository(endSessionId = "ended-session")
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                )
            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            advanceUntilIdle()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(2).toList() }

            viewModel.onAction(NavigationUiAction.ExitNavigationClicked)
            viewModel.onAction(NavigationUiAction.ConfirmExitNavigationClicked)
            advanceUntilIdle()

            assertEquals(listOf("walk-route-1"), routeRepository.endRouteCalls)
            assertEquals("ended-session", viewModel.currentRatingSessionId())
            assertEquals(
                listOf(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToArrival),
                eventsDeferred.await(),
            )
        }

    @Test
    fun `back click opens exit confirmation before completing navigation`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository = FakeRouteRepository(endSessionId = "ended-session")
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                )
            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            advanceUntilIdle()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(2).toList() }

            viewModel.onAction(NavigationUiAction.BackClicked)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isExitConfirmDialogVisible)
            assertTrue(routeRepository.endRouteCalls.isEmpty())

            viewModel.onAction(NavigationUiAction.ConfirmExitNavigationClicked)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isExitConfirmDialogVisible)
            assertEquals(listOf("walk-route-1"), routeRepository.endRouteCalls)
            assertEquals(
                listOf(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToArrival),
                eventsDeferred.await(),
            )
        }

    @Test
    fun `saving destination bookmark completes navigation into saved route when repository save succeeds`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val bookmarkRepository = FakeBookmarkRepository()
            val routeRepository = FakeRouteRepository(endSessionId = "ended-session")
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    bookmarkRepository = bookmarkRepository,
                    routeRepository = routeRepository,
                )
            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            advanceUntilIdle()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(2).toList() }

            viewModel.onAction(NavigationUiAction.SaveBookmarkClicked)
            advanceUntilIdle()

            assertEquals(1, bookmarkRepository.savedBookmarks.size)
            assertEquals(listOf("walk-route-1"), routeRepository.endRouteCalls)
            assertEquals(
                listOf(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToSavedRoute),
                eventsDeferred.await(),
            )
        }

    @Test
    fun `saving destination bookmark failure keeps navigation active and suppresses bookmark navigation`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val bookmarkRepository = FakeBookmarkRepository(failSave = true)
            val routeRepository = FakeRouteRepository(endSessionId = "ended-session")
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    bookmarkRepository = bookmarkRepository,
                    routeRepository = routeRepository,
                )
            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SaveBookmarkClicked)
            advanceUntilIdle()

            assertTrue(locationManager.isUpdating)
            assertTrue(bookmarkRepository.savedBookmarks.isEmpty())
            assertTrue(routeRepository.endRouteCalls.isEmpty())
        }

    @Test
    fun `accurate repeated off route updates reroute and end latest route id on completion`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository =
                FakeRouteRepository(
                    rerouteRoute = reroutedWalkRoute(),
                    endSessionId = "ended-session",
                )
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                )
            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            advanceUntilIdle()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(2).toList() }

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = OFF_ROUTE_POINT.latitude,
                    longitude = OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()
            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = OFF_ROUTE_POINT.latitude,
                    longitude = OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 2_000L,
                ),
            )
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.NavigationCompleteClicked)
            advanceUntilIdle()

            assertEquals(1, routeRepository.rerouteCalls.size)
            assertEquals(listOf("rerouted-route-1"), routeRepository.endRouteCalls)
            assertEquals("ended-session", viewModel.currentRatingSessionId())
            assertEquals(
                listOf(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToArrival),
                eventsDeferred.await(),
            )
        }

    @Test
    fun `low vision mode plays route change alert before the next segment boundary`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    initialLowVisionMode = true,
                )
            viewModel.bindNavigationRequest(testWalkNavigationRequest())
            viewModel.updateTextToSpeechState(
                isEnabled = true,
                canSpeak = true,
                status = NavigationTtsStatus.Ready,
            )
            advanceUntilIdle()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = WALK_PRE_TURN_POINT.latitude,
                    longitude = WALK_PRE_TURN_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals(NavigationUiEvent.PlayRouteChangeAlert, eventDeferred.await())
        }

    @Test
    fun `low vision far off route uses fresh walk search metrics when destination is walkable`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository =
                FakeRouteRepository(
                    freshWalkSearchData =
                        lowVisionRemainingSearchData(
                            routeId = "actual-walk-route",
                            routeOption = RouteOption.SAFE,
                            distanceMeters = 710,
                            estimatedTimeMinutes = 13,
                            durationSeconds = 780,
                        ),
                )
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                    initialLowVisionMode = true,
                )

            viewModel.bindNavigationRequest(testFarOffRouteNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = NEAR_OFF_ROUTE_POINT.latitude,
                    longitude = NEAR_OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals("710m", viewModel.uiState.value.remainingDistanceLabel)
            assertEquals("13\uBD84", viewModel.uiState.value.remainingEtaLabel)
            assertEquals(1, routeRepository.freshWalkQueries.size)
            assertTrue(routeRepository.freshTransitQueries.isEmpty())
        }

    @Test
    fun `low vision far off route uses fresh transit search metrics when destination is beyond walking range`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository =
                FakeRouteRepository(
                    freshWalkSearchData =
                        lowVisionRemainingSearchData(
                            routeId = "actual-walk-route",
                            routeOption = RouteOption.SAFE,
                            distanceMeters = 1_200,
                            estimatedTimeMinutes = 21,
                            durationSeconds = 1_260,
                        ),
                    freshTransitSearchData =
                        lowVisionRemainingSearchData(
                            routeId = "actual-transit-route",
                            routeOption = RouteOption.RECOMMENDED,
                            distanceMeters = 3_600,
                            estimatedTimeMinutes = 34,
                            durationSeconds = 2_040,
                        ),
                )
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                    initialLowVisionMode = true,
                )

            viewModel.bindNavigationRequest(testFarOffRouteNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = FAR_OFF_ROUTE_POINT.latitude,
                    longitude = FAR_OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals("3.6km", viewModel.uiState.value.remainingDistanceLabel)
            assertEquals("34\uBD84", viewModel.uiState.value.remainingEtaLabel)
            assertEquals(1, routeRepository.freshWalkQueries.size)
            assertEquals(1, routeRepository.freshTransitQueries.size)
        }

    @Test
    fun `low vision far off route shows dashes when fresh remaining route search is unavailable`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository =
                FakeRouteRepository(
                    failFreshWalkSearch = true,
                    failFreshTransitSearch = true,
                )
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                    initialLowVisionMode = true,
                )

            viewModel.bindNavigationRequest(testFarOffRouteNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = FAR_OFF_ROUTE_POINT.latitude,
                    longitude = FAR_OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals("-", viewModel.uiState.value.remainingDistanceLabel)
            assertEquals("-", viewModel.uiState.value.remainingEtaLabel)
        }

    @Test
    fun `low vision far off route throttles fresh remaining route search for small movement`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val routeRepository =
                FakeRouteRepository(
                    freshWalkSearchData =
                        lowVisionRemainingSearchData(
                            routeId = "actual-walk-route",
                            routeOption = RouteOption.SAFE,
                            distanceMeters = 710,
                            estimatedTimeMinutes = 13,
                            durationSeconds = 780,
                        ),
                )
            val viewModel =
                createViewModel(
                    locationManager = locationManager,
                    routeRepository = routeRepository,
                    initialLowVisionMode = true,
                )

            viewModel.bindNavigationRequest(testFarOffRouteNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = NEAR_OFF_ROUTE_POINT.latitude,
                    longitude = NEAR_OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = SLIGHTLY_SHIFTED_OFF_ROUTE_POINT.latitude,
                    longitude = SLIGHTLY_SHIFTED_OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 3_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals(1, routeRepository.freshWalkQueries.size)
            assertEquals("710m", viewModel.uiState.value.remainingDistanceLabel)
            assertEquals("13\uBD84", viewModel.uiState.value.remainingEtaLabel)

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = FARTHER_SHIFTED_OFF_ROUTE_POINT.latitude,
                    longitude = FARTHER_SHIFTED_OFF_ROUTE_POINT.longitude,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 8_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals(2, routeRepository.freshWalkQueries.size)
        }
}

private fun createViewModel(
    locationManager: FakeCurrentLocationManager = FakeCurrentLocationManager(),
    bookmarkRepository: BookmarkRepository = FakeBookmarkRepository(),
    routeRepository: RouteRepository = FakeRouteRepository(),
    initialLowVisionMode: Boolean = false,
): NavigationViewModel =
    NavigationViewModel(
        currentLocationManager = locationManager,
        bookmarkRepository = bookmarkRepository,
        routeRepository = routeRepository,
        initialLowVisionMode = initialLowVisionMode,
    )

private class FakeCurrentLocationManager : CurrentLocationManager {
    private val mutableLatestLocation = MutableStateFlow<LocationSnapshot?>(null)

    override val latestLocation: StateFlow<LocationSnapshot?> = mutableLatestLocation

    var isUpdating: Boolean = false
        private set

    override fun refreshLatestLocation() = Unit

    override fun startLocationUpdates() {
        isUpdating = true
    }

    override fun stopLocationUpdates() {
        isUpdating = false
    }

    fun emitLocation(snapshot: LocationSnapshot) {
        mutableLatestLocation.value = snapshot
    }
}

private class FakeBookmarkRepository(
    bookmarks: List<BookmarkData> = emptyList(),
    private val failSave: Boolean = false,
) : BookmarkRepository {
    val bookmarks = MutableStateFlow(bookmarks)
    val savedBookmarks = mutableListOf<BookmarkData>()

    override fun observeBookmarks(): Flow<List<BookmarkData>> = bookmarks

    override suspend fun isBookmarked(placeId: String): Boolean =
        bookmarks.value.any { bookmark -> bookmark.placeId == placeId }

    override suspend fun saveBookmark(bookmark: BookmarkData): BookmarkData {
        if (failSave) error("bookmark save failed")
        bookmarks.value = bookmarks.value.filterNot { it.placeId == bookmark.placeId } + bookmark
        savedBookmarks += bookmark
        return bookmark
    }

    override suspend fun deleteBookmark(placeId: String) {
        bookmarks.value = bookmarks.value.filterNot { bookmark -> bookmark.placeId == placeId }
    }
}

private class FakeRouteRepository(
    private val transitRefreshData: RouteTransitRefreshData =
        RouteTransitRefreshData(
            type = "BUS",
            arrivalStatus = "REALTIME_AVAILABLE",
            transits = listOf(RouteTransitArrivalData(routeNo = "100", remainingMinute = 6, isLowFloor = true)),
        ),
    private val rerouteRoute: RouteCandidate? = null,
    private val endSessionId: String = "ended-session",
    private val freshWalkSearchData: RouteSearchData? = null,
    private val freshTransitSearchData: RouteSearchData? = null,
    private val failFreshWalkSearch: Boolean = false,
    private val failFreshTransitSearch: Boolean = false,
) : RouteRepository {
    val transitRefreshCalls = mutableListOf<Pair<String, Int>>()
    val rerouteCalls = mutableListOf<Pair<String, GeoCoordinate>>()
    val endRouteCalls = mutableListOf<String>()
    val freshWalkQueries = mutableListOf<RouteSearchQuery>()
    val freshTransitQueries = mutableListOf<RouteSearchQuery>()

    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        RouteSearchData(query, RouteSearchResult(query.origin, query.destination), RouteSearchSource.serverApi())

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        RouteSearchData(query, RouteSearchResult(query.origin, query.destination), RouteSearchSource.serverApi())

    override suspend fun getFreshRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        freshWalkQueries += query
        if (failFreshWalkSearch) error("fresh walk search failed")
        return freshWalkSearchData?.copy(query = query) ?: getRouteSearchData(query)
    }

    override suspend fun getFreshTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        freshTransitQueries += query
        if (failFreshTransitSearch) error("fresh transit search failed")
        return freshTransitSearchData?.copy(query = query) ?: getTransitRouteSearchData(query)
    }

    override suspend fun selectRoute(
        routeId: String,
        searchId: String,
    ): RouteSessionData = RouteSessionData(sessionId = "selected-session")

    override suspend fun refreshTransit(
        routeId: String,
        legSequence: Int,
    ): RouteTransitRefreshData {
        transitRefreshCalls += routeId to legSequence
        return transitRefreshData
    }

    override suspend fun reroute(
        routeId: String,
        currentPoint: GeoCoordinate,
    ): RouteRerouteData {
        rerouteCalls += routeId to currentPoint
        return RouteRerouteData(route = rerouteRoute)
    }

    override suspend fun endRoute(routeId: String): RouteSessionData {
        endRouteCalls += routeId
        return RouteSessionData(sessionId = endSessionId)
    }

    override suspend fun rateRoute(
        sessionId: String,
        score: Int,
    ): RouteRatingData = RouteRatingData(ratingId = 1L)
}

private fun testWalkNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "현재 위치",
                coordinate = WALK_START_POINT,
            ),
        destination =
            RouteWaypoint(
                name = "목적지",
                placeId = "destination-place",
                coordinate = WALK_END_POINT,
            ),
        selectedRoute =
            RouteCandidate(
                serverRouteId = "walk-route-1",
                routeOption = RouteOption.SAFE,
                title = "Safe Route",
                summary =
                    RouteSummary(
                        distanceMeters = 900,
                        estimatedTimeMinutes = 15,
                        riskLevel = RouteRiskLevel.LOW,
                        durationSeconds = 900,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline = RoutePolyline(points = listOf(WALK_START_POINT, WALK_MID_POINT, WALK_END_POINT)),
                        segmentCount = 2,
                        renderableSegmentCount = 2,
                    ),
                legs =
                    listOf(
                        RouteLeg(
                            sequence = 1,
                            role = RouteLegRole.WALK_ONLY,
                            distanceMeters = 900,
                            durationSeconds = 900,
                        ),
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline = RoutePolyline(points = listOf(WALK_START_POINT, WALK_MID_POINT)),
                            distanceMeters = 300,
                            guidanceMessage = "직진",
                        ),
                        RouteSegment(
                            sequence = 2,
                            polyline = RoutePolyline(points = listOf(WALK_MID_POINT, WALK_END_POINT)),
                            distanceMeters = 600,
                            guidanceMessage = "우회전",
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Navigation test route"),
        selectionHandoff =
            RouteNavigationSelectionHandoff(
                searchId = "search-1",
                routeId = "walk-route-1",
                sessionId = "session-1",
            ),
    )

private fun reroutedWalkRoute(): RouteCandidate =
    testWalkNavigationRequest()
        .selectedRoute
        .copy(
            serverRouteId = "rerouted-route-1",
            title = "Rerouted Route",
        )

private fun testTransitNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "현재 위치",
                coordinate = TRANSIT_START_POINT,
            ),
        destination =
            RouteWaypoint(
                name = "목적지",
                placeId = "destination-place",
                coordinate = TRANSIT_END_POINT,
            ),
        selectedRoute =
            RouteCandidate(
                serverRouteId = "transit-route-1",
                routeOption = RouteOption.RECOMMENDED,
                title = "Transit Route",
                summary =
                    RouteSummary(
                        distanceMeters = 900,
                        estimatedTimeMinutes = 20,
                        riskLevel = RouteRiskLevel.MEDIUM,
                        durationSeconds = 1_200,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline =
                            RoutePolyline(
                                points =
                                    listOf(
                                        TRANSIT_START_POINT,
                                        TRANSIT_BOARDING_POINT,
                                        TRANSIT_END_POINT,
                                    ),
                            ),
                        segmentCount = 2,
                        renderableSegmentCount = 2,
                    ),
                legs =
                    listOf(
                        RouteLeg(
                            sequence = 1,
                            role = RouteLegRole.WALK_TO_TRANSIT,
                            distanceMeters = 500,
                            durationSeconds = 300,
                        ),
                        RouteLeg(
                            sequence = 2,
                            type = RouteLegType.BUS,
                            role = RouteLegRole.TRANSIT,
                            distanceMeters = 400,
                            durationSeconds = 900,
                            routeNo = "100",
                            boardingStop =
                                RouteTransitStop(
                                    name = "Bus Stop",
                                    coordinate = TRANSIT_BOARDING_POINT,
                                ),
                        ),
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline = RoutePolyline(points = listOf(TRANSIT_START_POINT, TRANSIT_BOARDING_POINT)),
                            distanceMeters = 500,
                            guidanceMessage = "정류장까지 이동",
                            sourceLegSequence = 1,
                        ),
                        RouteSegment(
                            sequence = 2,
                            polyline = RoutePolyline(points = listOf(TRANSIT_BOARDING_POINT, TRANSIT_END_POINT)),
                            distanceMeters = 400,
                            guidanceMessage = "100번 버스 탑승",
                            sourceLegSequence = 2,
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Transit test route"),
        selectionHandoff =
            RouteNavigationSelectionHandoff(
                searchId = "search-2",
                routeId = "transit-route-1",
                sessionId = "session-2",
            ),
    )

private fun testSparseSegmentNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "Origin",
                coordinate = SPARSE_ROUTE_START_POINT,
            ),
        destination =
            RouteWaypoint(
                name = "Destination",
                coordinate = SPARSE_ROUTE_END_POINT,
            ),
        selectedRoute =
            RouteCandidate(
                serverRouteId = "sparse-route-1",
                routeOption = RouteOption.RECOMMENDED,
                title = "Sparse Route",
                summary =
                    RouteSummary(
                        distanceMeters = 800,
                        estimatedTimeMinutes = 12,
                        riskLevel = RouteRiskLevel.LOW,
                        durationSeconds = 720,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline =
                            RoutePolyline(
                                points =
                                    listOf(
                                        SPARSE_ROUTE_START_POINT,
                                        SPARSE_ROUTE_BRANCH_POINT_1,
                                        SPARSE_ROUTE_END_POINT,
                                    ),
                            ),
                        segmentCount = 2,
                        renderableSegmentCount = 1,
                    ),
                legs =
                    listOf(
                        RouteLeg(
                            sequence = 1,
                            role = RouteLegRole.WALK_TO_TRANSIT,
                            distanceMeters = 400,
                            durationSeconds = 300,
                        ),
                        RouteLeg(
                            sequence = 2,
                            role = RouteLegRole.TRANSIT,
                            type = RouteLegType.BUS,
                            distanceMeters = 400,
                            durationSeconds = 420,
                        ),
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline = RoutePolyline(),
                            distanceMeters = 400,
                            guidanceMessage = "Walk to boarding stop",
                            sourceLegSequence = 1,
                        ),
                        RouteSegment(
                            sequence = 2,
                            polyline =
                                RoutePolyline(
                                    points =
                                        listOf(
                                            SPARSE_ROUTE_BRANCH_POINT_1,
                                            SPARSE_ROUTE_END_POINT,
                                        ),
                                ),
                            distanceMeters = 400,
                            guidanceMessage = "Ride the bus",
                            sourceLegSequence = 2,
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Sparse navigation test route"),
        selectionHandoff =
            RouteNavigationSelectionHandoff(
                searchId = "search-3",
                routeId = "sparse-route-1",
                sessionId = "session-3",
            ),
    )

private fun testMultiSparseSegmentNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "Origin",
                coordinate = SPARSE_ROUTE_START_POINT,
            ),
        destination =
            RouteWaypoint(
                name = "Destination",
                coordinate = SPARSE_ROUTE_END_POINT,
            ),
        selectedRoute =
            RouteCandidate(
                serverRouteId = "multi-sparse-route-1",
                routeOption = RouteOption.RECOMMENDED,
                title = "Multi Sparse Route",
                summary =
                    RouteSummary(
                        distanceMeters = 900,
                        estimatedTimeMinutes = 14,
                        riskLevel = RouteRiskLevel.LOW,
                        durationSeconds = 840,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline =
                            RoutePolyline(
                                points =
                                    listOf(
                                        SPARSE_ROUTE_START_POINT,
                                        SPARSE_ROUTE_BRANCH_POINT_1,
                                        SPARSE_ROUTE_BRANCH_POINT_2,
                                        SPARSE_ROUTE_END_POINT,
                                    ),
                            ),
                        segmentCount = 3,
                        renderableSegmentCount = 0,
                    ),
                legs =
                    listOf(
                        RouteLeg(
                            sequence = 1,
                            role = RouteLegRole.WALK_ONLY,
                            distanceMeters = 900,
                            durationSeconds = 840,
                            polyline =
                                RoutePolyline(
                                    points =
                                        listOf(
                                            SPARSE_ROUTE_START_POINT,
                                            SPARSE_ROUTE_BRANCH_POINT_1,
                                        ),
                                ),
                        ),
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline = RoutePolyline(),
                            distanceMeters = 300,
                            guidanceMessage = "Start walking",
                            sourceLegSequence = 1,
                        ),
                        RouteSegment(
                            sequence = 2,
                            polyline = RoutePolyline(),
                            distanceMeters = 300,
                            guidanceMessage = "Turn at the first branch",
                            sourceLegSequence = 1,
                        ),
                        RouteSegment(
                            sequence = 3,
                            polyline = RoutePolyline(),
                            distanceMeters = 300,
                            guidanceMessage = "Turn at the second branch",
                            sourceLegSequence = 1,
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Multi sparse navigation test route"),
        selectionHandoff =
            RouteNavigationSelectionHandoff(
                searchId = "search-3b",
                routeId = "multi-sparse-route-1",
                sessionId = "session-3b",
            ),
    )

private fun testLegPolylineFallbackNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "Origin",
                coordinate = GeoCoordinate(latitude = 35.1800, longitude = 129.0700),
            ),
        destination =
            RouteWaypoint(
                name = "Destination",
                coordinate = GeoCoordinate(latitude = 35.1810, longitude = 129.0780),
            ),
        selectedRoute =
            RouteCandidate(
                serverRouteId = "leg-fallback-route-1",
                routeOption = RouteOption.RECOMMENDED,
                title = "Leg Fallback Route",
                summary =
                    RouteSummary(
                        distanceMeters = 800,
                        estimatedTimeMinutes = 12,
                        riskLevel = RouteRiskLevel.LOW,
                        durationSeconds = 720,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline =
                            RoutePolyline(
                                points =
                                    listOf(
                                        GeoCoordinate(latitude = 35.1800, longitude = 129.0700),
                                        LEG_FALLBACK_START_POINT,
                                        GeoCoordinate(latitude = 35.1810, longitude = 129.0780),
                                    ),
                            ),
                        segmentCount = 2,
                        renderableSegmentCount = 1,
                    ),
                legs =
                    listOf(
                        RouteLeg(
                            sequence = 1,
                            role = RouteLegRole.WALK_TO_TRANSIT,
                            distanceMeters = 400,
                            durationSeconds = 300,
                            polyline =
                                RoutePolyline(
                                    points =
                                        listOf(
                                            LEG_FALLBACK_START_POINT,
                                            GeoCoordinate(latitude = 35.1806, longitude = 129.0735),
                                        ),
                                ),
                        ),
                        RouteLeg(
                            sequence = 2,
                            role = RouteLegRole.TRANSIT,
                            type = RouteLegType.BUS,
                            distanceMeters = 400,
                            durationSeconds = 420,
                        ),
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline = RoutePolyline(),
                            distanceMeters = 400,
                            guidanceMessage = "Walk to transit",
                            sourceLegSequence = 1,
                        ),
                        RouteSegment(
                            sequence = 2,
                            polyline =
                                RoutePolyline(
                                    points =
                                        listOf(
                                            GeoCoordinate(latitude = 35.1806, longitude = 129.0735),
                                            GeoCoordinate(latitude = 35.1810, longitude = 129.0780),
                                        ),
                                ),
                            distanceMeters = 400,
                            guidanceMessage = "Ride transit",
                            sourceLegSequence = 2,
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Leg fallback navigation test route"),
        selectionHandoff =
            RouteNavigationSelectionHandoff(
                searchId = "search-4",
                routeId = "leg-fallback-route-1",
                sessionId = "session-4",
            ),
    )

private fun testPointAnchorNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "Origin",
                coordinate = POINT_ANCHOR_ROUTE_START_POINT,
            ),
        destination =
            RouteWaypoint(
                name = "Destination",
                coordinate = POINT_ANCHOR_ROUTE_END_POINT,
            ),
        selectedRoute =
            RouteCandidate(
                serverRouteId = "point-anchor-route-1",
                routeOption = RouteOption.SAFE,
                title = "Point Anchor Route",
                summary =
                    RouteSummary(
                        distanceMeters = 320,
                        estimatedTimeMinutes = 5,
                        riskLevel = RouteRiskLevel.LOW,
                        durationSeconds = 300,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline =
                            RoutePolyline(
                                points =
                                    listOf(
                                        POINT_ANCHOR_ROUTE_START_POINT,
                                        POINT_ANCHOR_EVENT_POINT,
                                        POINT_ANCHOR_ROUTE_END_POINT,
                                    ),
                            ),
                        segmentCount = 1,
                        renderableSegmentCount = 0,
                    ),
                legs =
                    listOf(
                        RouteLeg(
                            sequence = 1,
                            role = RouteLegRole.WALK_ONLY,
                            distanceMeters = 320,
                            durationSeconds = 300,
                            polyline =
                                RoutePolyline(
                                    points =
                                        listOf(
                                            POINT_ANCHOR_ROUTE_START_POINT,
                                            POINT_ANCHOR_ROUTE_END_POINT,
                                        ),
                                ),
                        ),
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline = RoutePolyline(),
                            anchorCoordinate = POINT_ANCHOR_EVENT_POINT,
                            distanceMeters = 320,
                            guidanceMessage = "Crosswalk ahead",
                            sourceLegSequence = 1,
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Point anchor navigation test route"),
        selectionHandoff =
            RouteNavigationSelectionHandoff(
                searchId = "search-5",
                routeId = "point-anchor-route-1",
                sessionId = "session-5",
            ),
    )

private fun testFarOffRouteNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "Origin",
                coordinate = FAR_ROUTE_START_POINT,
            ),
        destination =
            RouteWaypoint(
                name = "Destination",
                coordinate = FAR_ROUTE_END_POINT,
            ),
        selectedRoute =
            RouteCandidate(
                serverRouteId = "far-off-route-1",
                routeOption = RouteOption.RECOMMENDED,
                title = "Far Off Route",
                summary =
                    RouteSummary(
                        distanceMeters = 2_200,
                        estimatedTimeMinutes = 22,
                        riskLevel = RouteRiskLevel.LOW,
                        durationSeconds = 1_320,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline =
                            RoutePolyline(
                                points =
                                    listOf(
                                        FAR_ROUTE_START_POINT,
                                        FAR_ROUTE_MID_POINT,
                                        FAR_ROUTE_END_POINT,
                                    ),
                            ),
                        segmentCount = 2,
                        renderableSegmentCount = 2,
                    ),
                legs =
                    listOf(
                        RouteLeg(
                            sequence = 1,
                            role = RouteLegRole.WALK_ONLY,
                            distanceMeters = 2_200,
                            durationSeconds = 1_320,
                        ),
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline = RoutePolyline(points = listOf(FAR_ROUTE_START_POINT, FAR_ROUTE_MID_POINT)),
                            distanceMeters = 1_100,
                            guidanceMessage = "Head east",
                        ),
                        RouteSegment(
                            sequence = 2,
                            polyline = RoutePolyline(points = listOf(FAR_ROUTE_MID_POINT, FAR_ROUTE_END_POINT)),
                            distanceMeters = 1_100,
                            guidanceMessage = "Keep going",
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Far off navigation test route"),
        selectionHandoff =
            RouteNavigationSelectionHandoff(
                searchId = "search-6",
                routeId = "far-off-route-1",
                sessionId = "session-6",
            ),
    )

private fun lowVisionRemainingSearchData(
    routeId: String,
    routeOption: RouteOption,
    distanceMeters: Int,
    estimatedTimeMinutes: Int,
    durationSeconds: Int,
): RouteSearchData =
    RouteSearchData(
        query =
            RouteSearchQuery(
                origin =
                    RouteWaypoint(
                        name = "Current",
                        coordinate = FAR_OFF_ROUTE_POINT,
                    ),
                destination =
                    RouteWaypoint(
                        name = "Destination",
                        coordinate = FAR_ROUTE_END_POINT,
                    ),
                requestedOptions = listOf(routeOption),
            ),
        result =
            RouteSearchResult(
                origin =
                    RouteWaypoint(
                        name = "Current",
                        coordinate = FAR_OFF_ROUTE_POINT,
                    ),
                destination =
                    RouteWaypoint(
                        name = "Destination",
                        coordinate = FAR_ROUTE_END_POINT,
                    ),
                routes =
                    listOf(
                        RouteCandidate(
                            serverRouteId = routeId,
                            routeOption = routeOption,
                            title = "Actual Remaining Route",
                            summary =
                                RouteSummary(
                                    distanceMeters = distanceMeters,
                                    estimatedTimeMinutes = estimatedTimeMinutes,
                                    riskLevel = RouteRiskLevel.LOW,
                                    durationSeconds = durationSeconds,
                                ),
                            preview = RoutePreviewModel(),
                        ),
                    ),
            ),
        source = RouteSearchSource.serverApi(label = "Low vision remaining route test"),
    )

private val WALK_START_POINT = GeoCoordinate(latitude = 35.1796, longitude = 129.0756)
private val WALK_PRE_TURN_POINT = GeoCoordinate(latitude = 35.1796, longitude = 129.077905)
private val WALK_MID_POINT = GeoCoordinate(latitude = 35.1796, longitude = 129.0781)
private val WALK_END_POINT = GeoCoordinate(latitude = 35.1796, longitude = 129.0806)
private val OFF_ROUTE_POINT = GeoCoordinate(latitude = 35.1815, longitude = 129.0756)
private val LEG_FALLBACK_START_POINT = GeoCoordinate(latitude = 35.1802, longitude = 129.0718)
private val POINT_ANCHOR_ROUTE_START_POINT = GeoCoordinate(latitude = 35.1804, longitude = 129.0710)
private val POINT_ANCHOR_EVENT_POINT = GeoCoordinate(latitude = 35.1805, longitude = 129.0722)
private val POINT_ANCHOR_ROUTE_END_POINT = GeoCoordinate(latitude = 35.1808, longitude = 129.0745)
private val SPARSE_ROUTE_START_POINT = GeoCoordinate(latitude = 35.1800, longitude = 129.0700)
private val SPARSE_ROUTE_BRANCH_POINT_1 = GeoCoordinate(latitude = 35.1800, longitude = 129.0740)
private val SPARSE_ROUTE_BRANCH_POINT_2 = GeoCoordinate(latitude = 35.1800, longitude = 129.0760)
private val SPARSE_ROUTE_END_POINT = GeoCoordinate(latitude = 35.1800, longitude = 129.0780)
private val FAR_ROUTE_START_POINT = GeoCoordinate(latitude = 35.1000, longitude = 129.0000)
private val FAR_ROUTE_MID_POINT = GeoCoordinate(latitude = 35.1000, longitude = 129.0100)
private val FAR_ROUTE_END_POINT = GeoCoordinate(latitude = 35.1000, longitude = 129.0200)
private val NEAR_OFF_ROUTE_POINT = GeoCoordinate(latitude = 35.1060, longitude = 129.0200)
private val SLIGHTLY_SHIFTED_OFF_ROUTE_POINT = GeoCoordinate(latitude = 35.10611, longitude = 129.0200)
private val FARTHER_SHIFTED_OFF_ROUTE_POINT = GeoCoordinate(latitude = 35.10636, longitude = 129.0200)
private val FAR_OFF_ROUTE_POINT = GeoCoordinate(latitude = 35.1100, longitude = 129.0200)

private val TRANSIT_START_POINT = GeoCoordinate(latitude = 35.1700, longitude = 129.0600)
private val TRANSIT_BOARDING_POINT = GeoCoordinate(latitude = 35.1700, longitude = 129.0625)
private val TRANSIT_END_POINT = GeoCoordinate(latitude = 35.1700, longitude = 129.0670)
