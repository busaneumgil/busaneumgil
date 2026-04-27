package com.ssafy.e102.eumgil.feature.map

import androidx.activity.ComponentActivity
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationGrantAccuracy
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.FacilityBrowseData
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.core.model.FacilitySeedCatalog
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.local.datasource.FacilitySeedLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DefaultFacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `destination selection centers camera on selected place`() =
        runTest {
            val permissionManager = FakeLocationPermissionManager(initialState = LocationPermissionState.Denied)
            val locationManager = FakeCurrentLocationManager()
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = destinationSelectionRepository,
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )
            val destination = testDestination()

            destinationSelectionRepository.updateSelectedDestination(destination)
            advanceUntilIdle()

            assertEquals(destination, viewModel.uiState.value.selectedDestination)
            assertEquals(MapCameraSource.SEARCH_RESULT, viewModel.uiState.value.cameraTarget.source)
            assertEquals(destination.latitude, viewModel.uiState.value.cameraTarget.center.latitude, 0.0)
            assertEquals(destination.longitude, viewModel.uiState.value.cameraTarget.center.longitude, 0.0)
        }

    @Test
    fun `location updates do not override selected destination until recenter`() =
        runTest {
            val initialLocation = testLocationSnapshot(latitude = 35.1000, longitude = 129.1000)
            val updatedLocation = testLocationSnapshot(latitude = 35.2000, longitude = 129.2000)
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = initialLocation)
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = destinationSelectionRepository,
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )
            val destination = testDestination()

            viewModel.onRouteStarted()
            advanceUntilIdle()

            destinationSelectionRepository.updateSelectedDestination(destination)
            advanceUntilIdle()

            locationManager.updateLocation(updatedLocation)
            advanceUntilIdle()

            assertEquals(MapCameraSource.SEARCH_RESULT, viewModel.uiState.value.cameraTarget.source)
            assertEquals(destination.latitude, viewModel.uiState.value.cameraTarget.center.latitude, 0.0)
            assertEquals(destination.longitude, viewModel.uiState.value.cameraTarget.center.longitude, 0.0)

            viewModel.onAction(MapUiAction.LocationActionClicked)
            advanceUntilIdle()

            assertEquals(MapCameraSource.CURRENT_LOCATION, viewModel.uiState.value.cameraTarget.source)
            assertEquals(updatedLocation.latitude, viewModel.uiState.value.cameraTarget.center.latitude, 0.0)
            assertEquals(updatedLocation.longitude, viewModel.uiState.value.cameraTarget.center.longitude, 0.0)
            assertEquals(destination, viewModel.uiState.value.selectedDestination)
        }

    @Test
    fun `reselecting same destination recenters map after current location recenter`() =
        runTest {
            val currentLocation = testLocationSnapshot(latitude = 35.1500, longitude = 129.1500)
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = currentLocation)
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = destinationSelectionRepository,
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )
            val destination = testDestination()

            viewModel.onRouteStarted()
            advanceUntilIdle()

            destinationSelectionRepository.updateSelectedDestination(destination)
            advanceUntilIdle()
            viewModel.onAction(MapUiAction.LocationActionClicked)
            advanceUntilIdle()

            destinationSelectionRepository.updateSelectedDestination(destination)
            advanceUntilIdle()

            assertEquals(MapCameraSource.SEARCH_RESULT, viewModel.uiState.value.cameraTarget.source)
            assertEquals(destination.latitude, viewModel.uiState.value.cameraTarget.center.latitude, 0.0)
            assertEquals(destination.longitude, viewModel.uiState.value.cameraTarget.center.longitude, 0.0)
        }

    @Test
    fun `browse state initializes with all markers visible and category options ready`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            assertEquals(13, viewModel.uiState.value.markerOverlayState.totalMarkerCount)
            assertEquals(13, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertTrue(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories)
            assertEquals(6, viewModel.uiState.value.markerFilterState.categoryOptions.size)
        }

    @Test
    fun `category filter options prioritize accessibility facilities`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            val categoryOrder =
                viewModel.uiState.value.markerFilterState.categoryOptions
                    .map { option -> option.category }

            assertEquals(
                listOf(
                    FacilityCategory.TOILET,
                    FacilityCategory.ELEVATOR,
                    FacilityCategory.CHARGING_STATION,
                    FacilityCategory.BRAILLE_BLOCK,
                    FacilityCategory.TOURIST_ATTRACTION,
                    FacilityCategory.RESTAURANT,
                ),
                categoryOrder,
            )
        }

    @Test
    fun `toggling category filter from all state shows only selected category markers`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MarkerCategoryFilterToggled(FacilityCategory.TOILET))
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertEquals(
                2,
                viewModel.uiState.value.markerOverlayState.markers.count { marker ->
                    marker.displayState == MapMarkerDisplayState.VISIBLE &&
                        marker.categoryType.category == FacilityCategory.TOILET
                },
            )
            assertTrue(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories.not())
            assertTrue(
                viewModel.uiState.value.markerFilterState.categoryOptions
                    .first { option -> option.category == FacilityCategory.TOILET }
                    .isSelected,
            )
        }

    @Test
    fun `toggling another category keeps custom multi select state`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MarkerCategoryFilterToggled(FacilityCategory.TOILET))
            viewModel.onAction(MapUiAction.MarkerCategoryFilterToggled(FacilityCategory.ELEVATOR))
            advanceUntilIdle()

            assertEquals(4, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertEquals(
                setOf(FacilityCategory.TOILET, FacilityCategory.ELEVATOR),
                viewModel.uiState.value.markerFilterState.selection.selectedFacilityCategories,
            )
        }

    @Test
    fun `toggling last selected category returns filter state to all`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MarkerCategoryFilterToggled(FacilityCategory.TOILET))
            viewModel.onAction(MapUiAction.MarkerCategoryFilterToggled(FacilityCategory.TOILET))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories)
            assertEquals(13, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
        }

    @Test
    fun `marker tap toggles selected marker state`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()
            assertEquals(markerId, viewModel.uiState.value.selectedMarkerId)
            assertEquals(markerId, viewModel.uiState.value.facilityDetailSheetState.detail?.facilityId)

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()
            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.detail)
        }

    @Test
    fun `marker tap loads existing bookmark state`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = bookmarkRepository,
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId
            bookmarkRepository.bookmarkedPlaceIds += markerId

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.facilityDetailSheetState.isBookmarked)
            assertFalse(viewModel.uiState.value.facilityDetailSheetState.isBookmarkUpdating)
        }

    @Test
    fun `bookmark toggle saves and removes selected facility bookmark`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = bookmarkRepository,
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId
            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()

            viewModel.onAction(MapUiAction.FacilityBookmarkClicked)
            advanceUntilIdle()

            assertTrue(markerId in bookmarkRepository.bookmarkedPlaceIds)
            assertTrue(viewModel.uiState.value.facilityDetailSheetState.isBookmarked)

            viewModel.onAction(MapUiAction.FacilityBookmarkClicked)
            advanceUntilIdle()

            assertFalse(markerId in bookmarkRepository.bookmarkedPlaceIds)
            assertFalse(viewModel.uiState.value.facilityDetailSheetState.isBookmarked)
        }

    @Test
    fun `bookmark toggle failure rolls back selected facility state`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository(failSave = true)
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = bookmarkRepository,
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId
            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()

            viewModel.onAction(MapUiAction.FacilityBookmarkClicked)
            advanceUntilIdle()

            val sheetState = viewModel.uiState.value.facilityDetailSheetState

            assertFalse(markerId in bookmarkRepository.bookmarkedPlaceIds)
            assertFalse(sheetState.isBookmarked)
            assertFalse(sheetState.isBookmarkUpdating)
            assertEquals("북마크 저장에 실패했습니다. 다시 시도해 주세요.", sheetState.bookmarkErrorMessage)
        }

    @Test
    fun `facility detail dismiss action clears selected marker state`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.facilityDetailSheetState.isVisible)

            viewModel.onAction(MapUiAction.FacilityDetailDismissed)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.detail)
        }

    @Test
    fun `route entry action stores selected facility destination and emits navigation event`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()

            val selectedDetail = checkNotNull(viewModel.uiState.value.facilityDetailSheetState.detail)
            val eventDeferred = async { viewModel.uiEvent.first() }

            viewModel.onAction(MapUiAction.FacilityRouteEntryClicked)
            advanceUntilIdle()

            assertEquals(selectedDetail.toPlaceDestination(), destinationSelectionRepository.selectedDestination.value)
            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.detail)
            assertEquals(MapUiEvent.NavigateToFacilityRouteEntry, eventDeferred.await())
        }

    @Test
    fun `selected marker is cleared when filter hides it`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            val restaurantMarkerId =
                viewModel.uiState.value.markerOverlayState.markers
                    .first { marker -> marker.categoryType.category == FacilityCategory.RESTAURANT }
                    .markerId

            viewModel.onAction(MapUiAction.MarkerTapped(restaurantMarkerId))
            viewModel.onAction(MapUiAction.MarkerCategoryFilterToggled(FacilityCategory.TOILET))
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.detail)
        }

    @Test
    fun `external destination selection clears facility detail state before recentering`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId
            val destination = testDestination()

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.facilityDetailSheetState.isVisible)

            destinationSelectionRepository.updateSelectedDestination(destination)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.detail)
            assertEquals(destination, viewModel.uiState.value.selectedDestination)
            assertEquals(MapCameraSource.SEARCH_RESULT, viewModel.uiState.value.cameraTarget.source)
        }

    @Test
    fun `browse load failure exposes error states`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = FailingFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.markerOverlayState.isLoadFailed)
            assertTrue(viewModel.uiState.value.markerFilterState.isLoadFailed)
            assertEquals(0, viewModel.uiState.value.markerOverlayState.totalMarkerCount)
        }

    @Test
    fun `empty browse data exposes empty states`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = EmptyFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.markerOverlayState.isEmptyData)
            assertTrue(viewModel.uiState.value.markerFilterState.isEmptyData)
            assertEquals(0, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertTrue(viewModel.uiState.value.markerFilterState.categoryOptions.isEmpty())
        }
}

private fun testDestination(): PlaceDestination =
    PlaceDestination(
        placeId = "place-1",
        name = "부산시청",
        address = "부산 연제구 중앙대로 1001",
        latitude = 35.1797,
        longitude = 129.0750,
    )

private fun testLocationSnapshot(
    latitude: Double,
    longitude: Double,
): LocationSnapshot =
    LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 5f,
        recordedAtEpochMillis = 1_000L,
    )

private class FakeLocationPermissionManager(
    initialState: LocationPermissionState,
) : LocationPermissionManager {
    private val mutablePermissionState = MutableStateFlow(initialState)

    override val permissionState: StateFlow<LocationPermissionState> = mutablePermissionState

    override fun refreshPermissionState() = Unit

    override fun requestLocationPermission(activity: ComponentActivity) = Unit
}

private class FakeCurrentLocationManager(
    initialLocation: LocationSnapshot? = null,
) : CurrentLocationManager {
    private val mutableLatestLocation = MutableStateFlow(initialLocation)

    override val latestLocation: StateFlow<LocationSnapshot?> = mutableLatestLocation

    override fun refreshLatestLocation() = Unit

    override fun startLocationUpdates() = Unit

    override fun stopLocationUpdates() = Unit

    fun updateLocation(snapshot: LocationSnapshot?) {
        mutableLatestLocation.value = snapshot
    }
}

private class FakeBookmarkRepository(
    val bookmarkedPlaceIds: MutableSet<String> = mutableSetOf(),
    private val failSave: Boolean = false,
    private val failDelete: Boolean = false,
) : BookmarkRepository {
    override fun observeBookmarks(): Flow<List<BookmarkData>> = flowOf(emptyList())

    override suspend fun isBookmarked(placeId: String): Boolean =
        placeId in bookmarkedPlaceIds

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        if (failSave) error("bookmark save failed")
        bookmarkedPlaceIds += bookmark.placeId
    }

    override suspend fun deleteBookmark(placeId: String) {
        if (failDelete) error("bookmark delete failed")
        bookmarkedPlaceIds -= placeId
    }
}

private fun testFacilitySeedRepository(): FacilitySeedRepository =
    DefaultFacilitySeedRepository(
        localDataSource = FacilitySeedLocalDataSource(),
        mockDataSource = FacilitySeedMockDataSource(),
    )

private class EmptyFacilitySeedRepository : FacilitySeedRepository {
    override suspend fun getSeedCatalog(): FacilitySeedCatalog = FacilitySeedCatalog()

    override suspend fun getFacilityBrowseData(query: FacilitySeedQuery): FacilityBrowseData = FacilityBrowseData()

    override suspend fun getFacilityMarkers(query: FacilitySeedQuery): List<FacilityMarkerSeed> = emptyList()

    override suspend fun getFacilityDetail(facilityId: String): FacilityDetailSeed? = null
}

private class FailingFacilitySeedRepository : FacilitySeedRepository {
    override suspend fun getSeedCatalog(): FacilitySeedCatalog {
        error("browse load failed")
    }

    override suspend fun getFacilityBrowseData(query: FacilitySeedQuery): FacilityBrowseData {
        error("browse load failed")
    }

    override suspend fun getFacilityMarkers(query: FacilitySeedQuery): List<FacilityMarkerSeed> {
        error("browse load failed")
    }

    override suspend fun getFacilityDetail(facilityId: String): FacilityDetailSeed? {
        error("browse load failed")
    }
}
