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
import com.ssafy.e102.eumgil.data.local.datasource.FacilitySeedLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import com.ssafy.e102.eumgil.data.repository.DefaultFacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
                )

            advanceUntilIdle()

            assertEquals(13, viewModel.uiState.value.markerOverlayState.totalMarkerCount)
            assertEquals(13, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertTrue(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories)
            assertEquals(6, viewModel.uiState.value.markerFilterState.categoryOptions.size)
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
                )

            advanceUntilIdle()

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()
            assertEquals(markerId, viewModel.uiState.value.selectedMarkerId)

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()
            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
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
