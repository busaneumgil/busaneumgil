package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RoutePreviewModel
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
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
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial ui state exposes navigation shell placeholders`() =
        runTest {
            val viewModel = createViewModel()

            assertEquals(NavigationScreenState.Loading, viewModel.uiState.value.screenState)
            assertEquals(0, viewModel.uiState.value.segmentSync.activeSegmentIndex)
            assertEquals(0, viewModel.uiState.value.segmentSync.focusedSegmentIndex)
            assertFalse(viewModel.uiState.value.segmentSync.isInspectingSegments)
            assertEquals(NavigationMapFocusMode.ACTIVE, viewModel.uiState.value.segmentSync.mapFocusMode)
            assertTrue(viewModel.uiState.value.segmentSync.railItems.isEmpty())
            assertNull(viewModel.uiState.value.focusedSegmentCard)
            assertNull(viewModel.uiState.value.pendingActiveChangeLabel)
            assertFalse(viewModel.uiState.value.mapOverlay.isDisplayable)
            assertTrue(viewModel.uiState.value.mapOverlay.shouldUsePlaceholder)
            assertNull(viewModel.uiState.value.mapOverlay.currentLocation)
            assertNull(viewModel.uiState.value.selectedRouteOption)
            assertFalse(viewModel.uiState.value.isExitConfirmDialogVisible)
            assertEquals("다음 안내", viewModel.uiState.value.stepCard.sectionLabel)
            assertEquals("준비 중", viewModel.uiState.value.stepCard.statusLabel)
            assertEquals("확인 중", viewModel.uiState.value.stepCard.metrics[0].value)
            assertEquals("음성 안내를 준비하고 있습니다.", viewModel.uiState.value.tts.fallbackMessage)
        }

    @Test
    fun `binding navigation request maps route handoff into screen state`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel = createViewModel(locationManager = locationManager)
            val request = testNavigationRequest()

            viewModel.bindNavigationRequest(request)
            advanceUntilIdle()

            assertTrue(locationManager.isUpdating)
            assertEquals(NavigationScreenState.Ready, viewModel.uiState.value.screenState)
            assertEquals(RouteOption.SAFE, viewModel.uiState.value.selectedRouteOption)
            assertEquals(0, viewModel.uiState.value.segmentSync.activeSegmentIndex)
            assertEquals(0, viewModel.uiState.value.segmentSync.focusedSegmentIndex)
            assertFalse(viewModel.uiState.value.segmentSync.isInspectingSegments)
            assertEquals(NavigationMapFocusMode.ACTIVE, viewModel.uiState.value.segmentSync.mapFocusMode)
            assertEquals(request.selectedRoute.segments.size, viewModel.uiState.value.segmentSync.railItems.size)
            assertTrue(viewModel.uiState.value.segmentSync.railItems.first().isActive)
            assertTrue(viewModel.uiState.value.segmentSync.railItems.first().isFocused)
            assertFalse(viewModel.uiState.value.segmentSync.railItems.last().isFocused)
            assertEquals(
                request.selectedRoute.segments.first().polyline.points,
                viewModel.uiState.value.mapOverlay.activeSegmentPolyline,
            )
            assertEquals(
                request.selectedRoute.segments.first().polyline.points,
                viewModel.uiState.value.mapOverlay.focusedSegmentPolyline,
            )
            assertEquals(request.origin.coordinate, viewModel.uiState.value.mapOverlay.focusCoordinate)
            assertNotNull(viewModel.uiState.value.focusedSegmentCard)
            assertEquals(
                request.selectedRoute.segments.first().guidanceMessage,
                viewModel.uiState.value.focusedSegmentCard?.instruction,
            )
            assertEquals(NavigationGuidanceAction.TURN_RIGHT, viewModel.uiState.value.stepCard.guidanceAction)
            assertEquals(
                NavigationGuidanceAction.TURN_RIGHT,
                viewModel.uiState.value.focusedSegmentCard?.guidanceAction,
            )
            assertEquals(
                NavigationGuidanceAction.TURN_RIGHT,
                viewModel.uiState.value.segmentSync.railItems.first().guidanceAction,
            )
            assertTrue(viewModel.uiState.value.canOpenRouteDetail)
            assertEquals("안전한 길", viewModel.uiState.value.stepCard.statusLabel)
            assertEquals("350m", viewModel.uiState.value.stepCard.distanceLabel)
            assertEquals("980m", viewModel.uiState.value.stepCard.metrics[0].value)
            assertEquals("16분", viewModel.uiState.value.stepCard.metrics[1].value)
            assertEquals("길 안내 종료", viewModel.uiState.value.exitCta.label)
            assertTrue(viewModel.uiState.value.isExitEnabled)
            assertFalse(viewModel.uiState.value.isExitConfirmDialogVisible)
        }

    @Test
    fun `segment tap updates focused segment without changing active segment`() =
        runTest {
            val request = testNavigationRequest()
            val viewModel = createViewModel()

            viewModel.bindNavigationRequest(request)
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 1))
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.segmentSync.activeSegmentIndex)
            assertEquals(1, viewModel.uiState.value.segmentSync.focusedSegmentIndex)
            assertTrue(viewModel.uiState.value.segmentSync.isInspectingSegments)
            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.segmentSync.mapFocusMode)
            assertEquals(
                request.selectedRoute.segments[1].guidanceMessage,
                viewModel.uiState.value.focusedSegmentCard?.instruction,
            )
            assertEquals(
                NavigationGuidanceAction.STRAIGHT,
                viewModel.uiState.value.focusedSegmentCard?.guidanceAction,
            )
            assertEquals(
                request.selectedRoute.segments[0].polyline.points,
                viewModel.uiState.value.mapOverlay.activeSegmentPolyline,
            )
            assertEquals(
                request.selectedRoute.segments[1].polyline.points,
                viewModel.uiState.value.mapOverlay.focusedSegmentPolyline,
            )
            assertEquals(
                GeoCoordinate(latitude = 35.14255, longitude = 129.0532),
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
            assertTrue(viewModel.uiState.value.mapOverlay.routeSegments[1].isFocused)
            assertFalse(viewModel.uiState.value.mapOverlay.routeSegments[0].isFocused)
        }

    @Test
    fun `location update during inspect mode keeps focused segment while active segment advances`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val request = testNavigationRequest()
            val viewModel = createViewModel(locationManager = locationManager)

            viewModel.bindNavigationRequest(request)
            advanceUntilIdle()
            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 0))
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = 35.1151,
                    longitude = 129.0414,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.segmentSync.activeSegmentIndex)
            assertEquals(0, viewModel.uiState.value.segmentSync.focusedSegmentIndex)
            assertTrue(viewModel.uiState.value.segmentSync.isInspectingSegments)
            assertTrue(viewModel.uiState.value.segmentSync.hasPendingActiveChange)
            assertNotNull(viewModel.uiState.value.pendingActiveChangeLabel)
            assertEquals("2 / 2", viewModel.uiState.value.progressLabel)
            assertEquals(
                request.selectedRoute.segments[1].polyline.points,
                viewModel.uiState.value.mapOverlay.activeSegmentPolyline,
            )
            assertEquals(
                request.selectedRoute.segments[0].polyline.points,
                viewModel.uiState.value.mapOverlay.focusedSegmentPolyline,
            )
            assertEquals(
                GeoCoordinate(latitude = 35.1748, longitude = 129.0703),
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
            assertEquals(NavigationMapFocusMode.FOCUSED, viewModel.uiState.value.mapOverlay.mapFocusMode)
            assertTrue(viewModel.uiState.value.mapOverlay.routeSegments[1].isActive)
            assertTrue(viewModel.uiState.value.mapOverlay.routeSegments[0].isFocused)
        }

    @Test
    fun `return to active action resets focus after inspect mode`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val request = testNavigationRequest()
            val viewModel = createViewModel(locationManager = locationManager)

            viewModel.bindNavigationRequest(request)
            advanceUntilIdle()
            viewModel.onAction(NavigationUiAction.SegmentTapped(index = 0))
            advanceUntilIdle()
            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = 35.1151,
                    longitude = 129.0414,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.ReturnToActiveSegmentClicked)
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.segmentSync.activeSegmentIndex)
            assertEquals(1, viewModel.uiState.value.segmentSync.focusedSegmentIndex)
            assertFalse(viewModel.uiState.value.segmentSync.isInspectingSegments)
            assertFalse(viewModel.uiState.value.segmentSync.hasPendingActiveChange)
            assertEquals(NavigationMapFocusMode.ACTIVE, viewModel.uiState.value.segmentSync.mapFocusMode)
            assertNull(viewModel.uiState.value.pendingActiveChangeLabel)
            assertEquals(
                request.selectedRoute.segments[1].guidanceMessage,
                viewModel.uiState.value.focusedSegmentCard?.instruction,
            )
            assertEquals(
                request.selectedRoute.segments[1].polyline.points,
                viewModel.uiState.value.mapOverlay.activeSegmentPolyline,
            )
            assertEquals(
                request.selectedRoute.segments[1].polyline.points,
                viewModel.uiState.value.mapOverlay.focusedSegmentPolyline,
            )
            assertEquals(
                GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
                viewModel.uiState.value.mapOverlay.focusCoordinate,
            )
            assertEquals(NavigationMapFocusMode.ACTIVE, viewModel.uiState.value.mapOverlay.mapFocusMode)
        }

    @Test
    fun `route detail action emits navigation event for selected route option`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()
            val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.first() }

            viewModel.onAction(NavigationUiAction.RouteDetailClicked)
            advanceUntilIdle()

            val event = eventDeferred.await()
            assertTrue(event is NavigationUiEvent.NavigateToRouteDetail)
            assertEquals(RouteOption.SAFE, (event as NavigationUiEvent.NavigateToRouteDetail).routeOption)
        }

    @Test
    fun `location update refreshes remaining distance and eta`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel = createViewModel(locationManager = locationManager)
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = 35.1700,
                    longitude = 129.0650,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()

            assertEquals("6.5km", viewModel.uiState.value.stepCard.metrics[0].value)
            assertEquals("80분", viewModel.uiState.value.stepCard.metrics[1].value)
            assertEquals(
                GeoCoordinate(latitude = 35.1700, longitude = 129.0650),
                viewModel.uiState.value.mapOverlay.currentLocation?.coordinate,
            )
        }

    @Test
    fun `location updates within one second are ignored`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel = createViewModel(locationManager = locationManager)
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = 35.1700,
                    longitude = 129.0650,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_000L,
                ),
            )
            advanceUntilIdle()
            val firstDistance = viewModel.uiState.value.stepCard.metrics[0].value

            locationManager.emitLocation(
                LocationSnapshot(
                    latitude = 35.1151,
                    longitude = 129.0414,
                    accuracyMeters = 5f,
                    recordedAtEpochMillis = 1_500L,
                ),
            )
            advanceUntilIdle()

            assertEquals(firstDistance, viewModel.uiState.value.stepCard.metrics[0].value)
        }

    @Test
    fun `remaining distance calculator uses cached route tail distance`() {
        val polyline =
            listOf(
                GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
                GeoCoordinate(latitude = 35.1700, longitude = 129.0650),
                GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
            )
        val calculator = RemainingDistanceCalculator(polyline)

            assertEquals(
            6471,
            calculator
                .calculateRemainingDistanceMeters(polyline[1])
                .roundToInt(),
        )
    }

    @Test
    fun `exit action opens confirmation dialog before emitting navigation events`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel = createViewModel(locationManager = locationManager)
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()
            val eventDeferred =
                async {
                    withTimeoutOrNull(100) {
                        viewModel.uiEvent.first()
                    }
                }

            viewModel.onAction(NavigationUiAction.ExitNavigationClicked)
            advanceUntilIdle()

            assertTrue(locationManager.isUpdating)
            assertTrue(viewModel.uiState.value.isExitConfirmDialogVisible)
            assertNull(eventDeferred.await())
        }

    @Test
    fun `dismiss exit confirmation hides dialog without stopping guidance`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel = createViewModel(locationManager = locationManager)
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()

            viewModel.onAction(NavigationUiAction.ExitNavigationClicked)
            advanceUntilIdle()
            viewModel.onAction(NavigationUiAction.ExitNavigationDismissed)
            advanceUntilIdle()

            assertTrue(locationManager.isUpdating)
            assertFalse(viewModel.uiState.value.isExitConfirmDialogVisible)
        }

    @Test
    fun `confirm exit action stops location updates and emits exit events`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel = createViewModel(locationManager = locationManager)
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(2).toList() }

            viewModel.onAction(NavigationUiAction.ExitNavigationClicked)
            advanceUntilIdle()
            viewModel.onAction(NavigationUiAction.ConfirmExitNavigationClicked)
            advanceUntilIdle()

            assertFalse(locationManager.isUpdating)
            assertFalse(viewModel.uiState.value.isExitConfirmDialogVisible)
            assertEquals(
                listOf(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToMap),
                eventsDeferred.await(),
            )
        }

    @Test
    fun `save bookmark action stores destination and opens saved route list`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel =
                NavigationViewModel(
                    currentLocationManager = locationManager,
                    bookmarkRepository = bookmarkRepository,
                )
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(2).toList() }

            viewModel.onAction(NavigationUiAction.SaveBookmarkClicked)
            advanceUntilIdle()

            assertFalse(locationManager.isUpdating)
            assertEquals(
                BookmarkData(
                    placeId = "destination-place",
                    placeName = "목적지",
                    address = "부산광역시 해운대구",
                    latitude = 35.1151,
                    longitude = 129.0414,
                    category = null,
                ),
                bookmarkRepository.bookmarks.value.single(),
            )
            assertEquals(
                listOf(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToSavedRoute),
                eventsDeferred.await(),
            )
        }

    @Test
    fun `complete action stops guidance and opens arrival screen`() =
        runTest {
            val locationManager = FakeCurrentLocationManager()
            val viewModel =
                NavigationViewModel(
                    currentLocationManager = locationManager,
                    bookmarkRepository = FakeBookmarkRepository(),
                )
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(2).toList() }

            viewModel.onAction(NavigationUiAction.NavigationCompleteClicked)
            advanceUntilIdle()

            assertFalse(locationManager.isUpdating)
            assertEquals(
                listOf(NavigationUiEvent.StopBriefing, NavigationUiEvent.NavigateToArrival),
                eventsDeferred.await(),
            )
        }

    @Test
    fun `exit action is ignored while navigation request is not ready`() =
        runTest {
            val viewModel = createViewModel()
            val eventDeferred =
                async {
                    withTimeoutOrNull(100) {
                        viewModel.uiEvent.first()
                    }
                }

            viewModel.onAction(NavigationUiAction.ExitNavigationClicked)
            advanceUntilIdle()

            assertNull(eventDeferred.await())
        }

    @Test
    fun `complete action is ignored while navigation request is not ready`() =
        runTest {
            val viewModel = createViewModel()
            val eventDeferred =
                async {
                    withTimeoutOrNull(100) {
                        viewModel.uiEvent.first()
                    }
                }

            viewModel.onAction(NavigationUiAction.NavigationCompleteClicked)
            advanceUntilIdle()

            assertNull(eventDeferred.await())
        }

    @Test
    fun `tts ready update clears fallback and allows briefing request`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.bindNavigationRequest(testNavigationRequest())

            viewModel.updateTextToSpeechState(
                isEnabled = true,
                canSpeak = true,
                status = NavigationTtsStatus.Ready,
            )

            assertTrue(viewModel.uiState.value.tts.canSpeak)
            assertEquals(NavigationTtsStatus.Ready, viewModel.uiState.value.tts.status)
            assertEquals("", viewModel.uiState.value.tts.fallbackMessage)
            assertTrue(viewModel.uiState.value.tts.canRequestBriefing)
    }
}

private fun createViewModel(
    locationManager: FakeCurrentLocationManager = FakeCurrentLocationManager(),
    bookmarkRepository: BookmarkRepository = FakeBookmarkRepository(),
): NavigationViewModel =
    NavigationViewModel(
        currentLocationManager = locationManager,
        bookmarkRepository = bookmarkRepository,
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

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        bookmarks.value = bookmarks.value.filterNot { it.placeId == bookmark.placeId } + bookmark
    }

    override suspend fun deleteBookmark(placeId: String) {
        bookmarks.value = bookmarks.value.filterNot { bookmark -> bookmark.placeId == placeId }
    }
}

private fun testNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "현재 위치",
                coordinate = GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
            ),
        destination =
            RouteWaypoint(
                name = "목적지",
                placeId = "destination-place",
                address = "부산광역시 해운대구",
                coordinate = GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
            ),
        selectedRoute =
            RouteCandidate(
                routeOption = RouteOption.SAFE,
                title = "Safe Route",
                summary =
                    RouteSummary(
                        distanceMeters = 980,
                        estimatedTimeMinutes = 16,
                        riskLevel = RouteRiskLevel.LOW,
                    ),
                preview =
                    RoutePreviewModel(
                        polyline =
                            RoutePolyline(
                                points =
                                    listOf(
                                        GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
                                        GeoCoordinate(latitude = 35.1700, longitude = 129.0650),
                                        GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
                                    ),
                            ),
                        segmentCount = 2,
                        renderableSegmentCount = 2,
                    ),
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            polyline =
                                RoutePolyline(
                                    points =
                                        listOf(
                                            GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
                                            GeoCoordinate(latitude = 35.1700, longitude = 129.0650),
                                        ),
                                ),
                            distanceMeters = 350,
                            guidanceMessage = "350m 앞에서 오른쪽 방향으로 이동하세요",
                        ),
                        RouteSegment(
                            sequence = 2,
                            polyline =
                                RoutePolyline(
                                    points =
                                        listOf(
                                            GeoCoordinate(latitude = 35.1700, longitude = 129.0650),
                                            GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
                                        ),
                                ),
                            distanceMeters = 630,
                            guidanceMessage = "목적지까지 계속 이동하세요",
                        ),
                    ),
            ),
        source =
            RouteSearchSource.serverApi(
                label = "Navigation test route",
            ),
    )
