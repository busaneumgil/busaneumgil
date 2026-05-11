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
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import com.ssafy.e102.eumgil.feature.route.RouteNavigationSelectionHandoff
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    fun `segment tap resolves focused map coordinate even when the segment polyline is missing`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(testSparseSegmentNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 0))
            advanceUntilIdle()

            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.mapOverlay.mapFocusMode)
            assertTrue(viewModel.uiState.value.mapOverlay.focusedSegmentPolyline.isEmpty())
            val focusCoordinate = requireNotNull(viewModel.uiState.value.mapOverlay.focusCoordinate)
            assertEquals(35.1800, focusCoordinate.latitude, 0.0001)
            assertEquals(129.0720, focusCoordinate.longitude, 0.0001)
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
}

private fun createViewModel(
    locationManager: FakeCurrentLocationManager = FakeCurrentLocationManager(),
    bookmarkRepository: BookmarkRepository = FakeBookmarkRepository(),
    routeRepository: RouteRepository = FakeRouteRepository(),
): NavigationViewModel =
    NavigationViewModel(
        currentLocationManager = locationManager,
        bookmarkRepository = bookmarkRepository,
        routeRepository = routeRepository,
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
) : BookmarkRepository {
    val bookmarks = MutableStateFlow(bookmarks)

    override fun observeBookmarks(): Flow<List<BookmarkData>> = bookmarks

    override suspend fun isBookmarked(placeId: String): Boolean =
        bookmarks.value.any { bookmark -> bookmark.placeId == placeId }

    override suspend fun saveBookmark(bookmark: BookmarkData): BookmarkData {
        bookmarks.value = bookmarks.value.filterNot { it.placeId == bookmark.placeId } + bookmark
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
) : RouteRepository {
    val transitRefreshCalls = mutableListOf<Pair<String, Int>>()
    val rerouteCalls = mutableListOf<Pair<String, GeoCoordinate>>()
    val endRouteCalls = mutableListOf<String>()

    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        RouteSearchData(query, RouteSearchResult(query.origin, query.destination), RouteSearchSource.serverApi())

    override suspend fun getTransitRouteSearchData(query: RouteSearchQuery): RouteSearchData =
        RouteSearchData(query, RouteSearchResult(query.origin, query.destination), RouteSearchSource.serverApi())

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
                coordinate = GeoCoordinate(latitude = 35.1800, longitude = 129.0700),
            ),
        destination =
            RouteWaypoint(
                name = "Destination",
                coordinate = GeoCoordinate(latitude = 35.1800, longitude = 129.0780),
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
                                        GeoCoordinate(latitude = 35.1800, longitude = 129.0700),
                                        GeoCoordinate(latitude = 35.1800, longitude = 129.0740),
                                        GeoCoordinate(latitude = 35.1800, longitude = 129.0780),
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
                                            GeoCoordinate(latitude = 35.1800, longitude = 129.0740),
                                            GeoCoordinate(latitude = 35.1800, longitude = 129.0780),
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

private val WALK_START_POINT = GeoCoordinate(latitude = 35.1796, longitude = 129.0756)
private val WALK_MID_POINT = GeoCoordinate(latitude = 35.1796, longitude = 129.0781)
private val WALK_END_POINT = GeoCoordinate(latitude = 35.1796, longitude = 129.0806)
private val OFF_ROUTE_POINT = GeoCoordinate(latitude = 35.1815, longitude = 129.0756)

private val TRANSIT_START_POINT = GeoCoordinate(latitude = 35.1700, longitude = 129.0600)
private val TRANSIT_BOARDING_POINT = GeoCoordinate(latitude = 35.1700, longitude = 129.0625)
private val TRANSIT_END_POINT = GeoCoordinate(latitude = 35.1700, longitude = 129.0670)
