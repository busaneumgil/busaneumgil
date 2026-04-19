package com.ssafy.e102.eumgil.feature.map

import androidx.activity.ComponentActivity
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationGrantAccuracy
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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
