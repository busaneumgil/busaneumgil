package com.ssafy.e102.eumgil.feature.map

import androidx.activity.ComponentActivity
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationGrantAccuracy
import com.ssafy.e102.eumgil.core.location.LocationPermissionManager
import com.ssafy.e102.eumgil.core.location.LocationPermissionState
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.model.AccessibilityTag
import com.ssafy.e102.eumgil.core.model.FacilityBrowseData
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.core.model.FacilityMarkerSeed
import com.ssafy.e102.eumgil.core.model.FacilitySeed
import com.ssafy.e102.eumgil.core.model.FacilitySeedCatalog
import com.ssafy.e102.eumgil.core.model.FacilitySeedQuery
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.MapPlaceClickType
import com.ssafy.e102.eumgil.core.model.MapPlaceDetailRequest
import com.ssafy.e102.eumgil.core.model.MapPlaceDetailType
import com.ssafy.e102.eumgil.core.model.MapTappedPlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.PlaceFeatureAvailability
import com.ssafy.e102.eumgil.core.model.PlaceFeatureType
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.core.model.PlaceSummary
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.local.datasource.FacilitySeedLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.FacilitySeedMockDataSource
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.DefaultFacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.FacilitySeedRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.feature.map.component.createKakaoCameraRenderState
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapDefaults
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.feature.map.model.MapShortcutFilterKey
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
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
    fun `current location button stays visually inactive on initial auto location sync`() =
        runTest {
            val currentLocation = testLocationSnapshot(latitude = 35.1500, longitude = 129.1500)
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = currentLocation)
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            viewModel.onRouteStarted()
            advanceUntilIdle()

            assertEquals(MapRecenterButtonState.ENABLED, viewModel.uiState.value.recenterButtonState)
            assertFalse(viewModel.uiState.value.isRecenterButtonActive)
        }

    @Test
    fun `stale last known location does not center map on route start`() =
        runTest {
            val staleLocation =
                testLocationSnapshot(
                    latitude = 35.1500,
                    longitude = 129.1500,
                    recordedAtEpochMillis = 1_000L,
                )
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = staleLocation)
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            viewModel.onRouteStarted()
            advanceUntilIdle()

            assertEquals(MapCameraSource.DEFAULT_BUSAN, viewModel.uiState.value.cameraTarget.source)
            assertEquals(MapDefaults.BUSAN_CENTER.latitude, viewModel.uiState.value.cameraTarget.center.latitude, 0.0)
            assertEquals(MapDefaults.BUSAN_CENTER.longitude, viewModel.uiState.value.cameraTarget.center.longitude, 0.0)
            assertEquals(MapRecenterButtonState.LOADING, viewModel.uiState.value.recenterButtonState)
        }

    @Test
    fun `current location button becomes visually active after explicit recenter tap`() =
        runTest {
            val currentLocation = testLocationSnapshot(latitude = 35.1500, longitude = 129.1500)
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = currentLocation)
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            viewModel.onRouteStarted()
            advanceUntilIdle()
            viewModel.onAction(MapUiAction.LocationActionClicked)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isRecenterButtonActive)
        }

    @Test
    fun `programmatic viewport camera change keeps explicit recenter active`() =
        runTest {
            val currentLocation = testLocationSnapshot(latitude = 35.1500, longitude = 129.1500)
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = currentLocation)
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            viewModel.onRouteStarted()
            advanceUntilIdle()
            viewModel.onAction(MapUiAction.LocationActionClicked)
            advanceUntilIdle()

            viewModel.onAction(
                MapUiAction.ViewportCameraChanged(
                    center = MapCoordinate(latitude = 35.1501, longitude = 129.1501),
                    zoomLevel = 17,
                    isUserGesture = false,
                ),
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isRecenterButtonActive)
        }

    @Test
    fun `user viewport camera change clears explicit recenter active`() =
        runTest {
            val currentLocation = testLocationSnapshot(latitude = 35.1500, longitude = 129.1500)
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = currentLocation)
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            viewModel.onRouteStarted()
            advanceUntilIdle()
            viewModel.onAction(MapUiAction.LocationActionClicked)
            advanceUntilIdle()

            viewModel.onAction(
                MapUiAction.ViewportCameraChanged(
                    center = MapCoordinate(latitude = 35.1510, longitude = 129.1510),
                    zoomLevel = 16,
                    isUserGesture = true,
                ),
            )
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRecenterButtonActive)
        }

    @Test
    fun `zoom in action increases camera zoom level and request id`() =
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

            val initialCamera = createKakaoCameraRenderState(viewModel.uiState.value.cameraTarget)

            viewModel.onAction(MapUiAction.ZoomInClicked)
            advanceUntilIdle()

            val updatedCamera = createKakaoCameraRenderState(viewModel.uiState.value.cameraTarget)

            assertEquals(initialCamera.zoomLevel + 1, updatedCamera.zoomLevel)
            assertEquals(initialCamera.requestId + 1L, updatedCamera.requestId)
            assertEquals(initialCamera.latitude, updatedCamera.latitude, 0.0)
            assertEquals(initialCamera.longitude, updatedCamera.longitude, 0.0)
        }

    @Test
    fun `zoom action keeps last viewport center after manual camera move`() =
        runTest {
            val currentLocation = testLocationSnapshot(latitude = 35.1500, longitude = 129.1500)
            val permissionManager =
                FakeLocationPermissionManager(
                    initialState = LocationPermissionState.Granted(LocationGrantAccuracy.PRECISE),
                )
            val locationManager = FakeCurrentLocationManager(initialLocation = currentLocation)
            val viewModel =
                MapViewModel(
                    locationPermissionManager = permissionManager,
                    currentLocationManager = locationManager,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )
            val viewportCenter = MapCoordinate(latitude = 35.1705, longitude = 129.0832)

            viewModel.onRouteStarted()
            advanceUntilIdle()

            viewModel.onAction(
                MapUiAction.ViewportCameraChanged(
                    center = viewportCenter,
                    zoomLevel = 14,
                ),
            )
            advanceUntilIdle()

            viewModel.onAction(MapUiAction.ZoomInClicked)
            advanceUntilIdle()

            val updatedCamera = createKakaoCameraRenderState(viewModel.uiState.value.cameraTarget)

            assertEquals(viewportCenter.latitude, updatedCamera.latitude, 0.0)
            assertEquals(viewportCenter.longitude, updatedCamera.longitude, 0.0)
            assertEquals(15, updatedCamera.zoomLevel)
        }

    @Test
    fun `browse state initializes with no active filters and hides markers by default`() =
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

            assertEquals(17, viewModel.uiState.value.markerOverlayState.totalMarkerCount)
            assertEquals(0, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertFalse(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories)
            assertFalse(viewModel.uiState.value.markerOverlayState.isEmptyResult)
            assertEquals(10, viewModel.uiState.value.markerFilterState.categoryOptions.size)
            assertTrue(viewModel.uiState.value.shortcutFilterState.chips.none { chip -> chip.isSelected })
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
                    FacilityCategory.FOOD_CAFE,
                    FacilityCategory.TOURIST_SPOT,
                    FacilityCategory.ACCOMMODATION,
                    FacilityCategory.HEALTHCARE,
                    FacilityCategory.WELFARE,
                    FacilityCategory.PUBLIC_OFFICE,
                    FacilityCategory.BRAILLE_BLOCK,
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
    fun `toggling last selected category clears the filter selection`() =
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

            assertFalse(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories)
            assertTrue(viewModel.uiState.value.markerFilterState.selection.selectedFacilityCategories.isEmpty())
            assertEquals(0, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertFalse(viewModel.uiState.value.markerOverlayState.isEmptyResult)
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
    fun `map tap drops selected location pin and clears selected facility`() =
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
            val tappedCoordinate = MapCoordinate(latitude = 35.1775, longitude = 129.0771)

            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()
            viewModel.onAction(MapUiAction.MapTapped(MapTapPayload(coordinate = tappedCoordinate)))
            advanceUntilIdle()

            assertEquals(tappedCoordinate, viewModel.uiState.value.selectedMapPinCoordinate)
            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.detail)
        }

    @Test
    fun `blank map tap requests address detail and exposes map tap sheet state`() =
        runTest {
            val tappedCoordinate = MapCoordinate(latitude = 35.1775, longitude = 129.0771)
            val placesRepository =
                FakePlacesRepository(
                    mapTapDetail =
                        testMapTappedDetail(
                            bookmarkTargetId = "external-address:35.1775,129.0771",
                            detailType = MapPlaceDetailType.EXTERNAL_ADDRESS,
                            name = "Selected Address",
                            address = "100 Jungang-daero, Busan",
                            latitude = tappedCoordinate.latitude,
                            longitude = tappedCoordinate.longitude,
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MapTapped(MapTapPayload(coordinate = tappedCoordinate)))
            advanceUntilIdle()

            val request = placesRepository.mapTapDetailRequests.single()
            assertEquals(tappedCoordinate.latitude, request.latitude, 0.0)
            assertEquals(tappedCoordinate.longitude, request.longitude, 0.0)
            assertEquals(MapPlaceClickType.ADDRESS, request.clickType)
            assertEquals(null, request.provider)
            assertEquals(null, request.providerPlaceId)
            assertEquals(null, request.nameHint)
            assertEquals(tappedCoordinate, viewModel.uiState.value.selectedMapPinCoordinate)
            assertEquals("Selected Address", viewModel.uiState.value.facilityDetailSheetState.mapTapDetail?.name)
            assertEquals("100 Jungang-daero, Busan", viewModel.uiState.value.facilityDetailSheetState.mapTapDetail?.address)
            assertFalse(viewModel.uiState.value.facilityDetailSheetState.isMapTapDetailLoading)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.mapTapDetailErrorMessage)
            assertTrue(viewModel.uiState.value.facilityDetailSheetState.isVisible)
        }

    @Test
    fun `external poi tap requests provider detail and preserves provider metadata`() =
        runTest {
            val tappedCoordinate = MapCoordinate(latitude = 35.1799, longitude = 129.0752)
            val placesRepository =
                FakePlacesRepository(
                    mapTapDetail =
                        testMapTappedDetail(
                            bookmarkTargetId = "kakao:poi-123",
                            detailType = MapPlaceDetailType.EXTERNAL_POI,
                            provider = "KAKAO",
                            providerPlaceId = "poi-123",
                            name = "Kakao Cafe",
                            providerCategory = "Cafe",
                            address = "10 Cafe-ro, Busan",
                            latitude = tappedCoordinate.latitude,
                            longitude = tappedCoordinate.longitude,
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(
                MapUiAction.MapTapped(
                    MapTapPayload(
                        coordinate = tappedCoordinate,
                        clickType = MapTapClickType.POI,
                        provider = "KAKAO",
                        providerPlaceId = "poi-123",
                        nameHint = "Cafe Hint",
                    ),
                ),
            )
            advanceUntilIdle()

            val request = placesRepository.mapTapDetailRequests.single()
            assertEquals(MapPlaceClickType.POI, request.clickType)
            assertEquals("KAKAO", request.provider)
            assertEquals("poi-123", request.providerPlaceId)
            assertEquals("Cafe Hint", request.nameHint)
            val detail = requireNotNull(viewModel.uiState.value.facilityDetailSheetState.mapTapDetail)
            assertEquals(MapPlaceDetailType.EXTERNAL_POI, detail.detailType)
            assertEquals("kakao:poi-123", detail.bookmarkTargetId)
            assertEquals("KAKAO", detail.provider)
            assertEquals("poi-123", detail.providerPlaceId)
            assertEquals("Cafe", detail.providerCategory)
            assertEquals("Kakao Cafe", detail.name)
        }

    @Test
    fun `map tap detail failure clears loading and keeps sheet error state`() =
        runTest {
            val tappedCoordinate = MapCoordinate(latitude = 35.1775, longitude = 129.0771)
            val placesRepository =
                FakePlacesRepository(
                    mapTapDetailFailure = IllegalStateException("detail failed"),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MapTapped(MapTapPayload(coordinate = tappedCoordinate)))
            advanceUntilIdle()

            assertEquals(1, placesRepository.mapTapDetailRequests.size)
            assertEquals(tappedCoordinate, viewModel.uiState.value.selectedMapPinCoordinate)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.mapTapDetail)
            assertFalse(viewModel.uiState.value.facilityDetailSheetState.isMapTapDetailLoading)
            assertTrue(viewModel.uiState.value.facilityDetailSheetState.mapTapDetailErrorMessage?.isNotBlank() == true)
            assertTrue(viewModel.uiState.value.facilityDetailSheetState.isVisible)
        }

    @Test
    fun `marker tap clears previous map tap detail before loading internal detail`() =
        runTest {
            val tappedCoordinate = MapCoordinate(latitude = 35.1775, longitude = 129.0771)
            val placesRepository =
                FakePlacesRepository(
                    mapTapDetail =
                        testMapTappedDetail(
                            bookmarkTargetId = "external-address:35.1775,129.0771",
                            detailType = MapPlaceDetailType.EXTERNAL_ADDRESS,
                            name = "Selected Address",
                            latitude = tappedCoordinate.latitude,
                            longitude = tappedCoordinate.longitude,
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MapTapped(MapTapPayload(coordinate = tappedCoordinate)))
            advanceUntilIdle()
            assertEquals("Selected Address", viewModel.uiState.value.facilityDetailSheetState.mapTapDetail?.name)

            val markerId = viewModel.uiState.value.markerOverlayState.markers.first().markerId
            viewModel.onAction(MapUiAction.MarkerTapped(markerId))
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.selectedMapPinCoordinate)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.mapTapDetail)
            assertEquals(markerId, viewModel.uiState.value.selectedMarkerId)
            assertEquals(markerId, viewModel.uiState.value.facilityDetailSheetState.detail?.facilityId)
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

            viewModel.onAction(MapUiAction.FacilitySetDestinationClicked)
            advanceUntilIdle()

            val event =
                withTimeoutOrNull(100) {
                    viewModel.uiEvent.first()
                }

            assertEquals(selectedDetail.toPlaceDestination(), destinationSelectionRepository.selectedDestination.value)
            assertEquals(null, viewModel.uiState.value.selectedMarkerId)
            assertEquals(null, viewModel.uiState.value.facilityDetailSheetState.detail)
            assertEquals(MapUiEvent.NavigateToRouteSetting, event)
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

            val foodCafeMarkerId =
                viewModel.uiState.value.markerOverlayState.markers
                    .first { marker -> marker.categoryType.category == FacilityCategory.FOOD_CAFE }
                    .markerId

            viewModel.onAction(MapUiAction.MarkerTapped(foodCafeMarkerId))
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
    fun `recent destinations are limited to three entries`() =
        runTest {
            val searchRepository =
                FakeSearchRepository(
                    recentDestinations =
                        listOf(
                            recentDestination(placeId = "place-4", searchedAtMillis = 4_000L),
                            recentDestination(placeId = "place-3", searchedAtMillis = 3_000L),
                            recentDestination(placeId = "place-2", searchedAtMillis = 2_000L),
                            recentDestination(placeId = "place-1", searchedAtMillis = 1_000L),
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = testFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    searchRepository = searchRepository,
                )

            advanceUntilIdle()

            assertEquals(
                listOf("place-4", "place-3", "place-2"),
                viewModel.uiState.value.recentDestinations.map { recentDestination -> recentDestination.placeId },
            )
        }

    @Test
    fun `recent destination route click stores destination and emits navigation event`() =
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
                    searchRepository =
                        FakeSearchRepository(
                            recentDestinations =
                                listOf(
                                    recentDestination(placeId = "recent-place-1", searchedAtMillis = 2_000L),
                                ),
                        ),
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.RecentDestinationRouteClicked(placeId = "recent-place-1"))
            advanceUntilIdle()

            val event =
                withTimeoutOrNull(100) {
                    viewModel.uiEvent.first()
                }

            assertEquals("recent-place-1", destinationSelectionRepository.selectedDestination.value?.placeId)
            assertEquals(MapUiEvent.NavigateToRouteSetting, event)
        }

    @Test
    fun `shortcut filter row exposes contract aligned quick filters`() =
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

            assertEquals(
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
                ),
                viewModel.uiState.value.shortcutFilterState.chips.map { chip -> chip.key },
            )
        }

    @Test
    fun `shortcut filter charging station chip narrows visible markers to charging facilities`() =
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

            viewModel.onAction(MapUiAction.ShortcutFilterClicked(MapShortcutFilterKey.CHARGING_STATION))
            advanceUntilIdle()

            val visibleMarkers = viewModel.uiState.value.markerOverlayState.visibleMarkers

            assertTrue(visibleMarkers.isNotEmpty())
            assertTrue(visibleMarkers.size < viewModel.uiState.value.markerOverlayState.totalMarkerCount)
            assertTrue(
                visibleMarkers.all { marker ->
                    marker.categoryType.category == FacilityCategory.CHARGING_STATION
                },
            )
            assertTrue(
                viewModel.uiState.value.shortcutFilterState.chips
                    .first { chip -> chip.key == MapShortcutFilterKey.CHARGING_STATION }
                    .isSelected,
            )
        }

    @Test
    fun `shortcut filter chips allow multi select without clearing earlier selections`() =
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

            viewModel.onAction(MapUiAction.ShortcutFilterClicked(MapShortcutFilterKey.CHARGING_STATION))
            viewModel.onAction(MapUiAction.ShortcutFilterClicked(MapShortcutFilterKey.TOILET))
            advanceUntilIdle()

            val visibleMarkers = viewModel.uiState.value.markerOverlayState.visibleMarkers

            assertTrue(visibleMarkers.isNotEmpty())
            assertTrue(
                visibleMarkers.all { marker ->
                    marker.categoryType.category == FacilityCategory.CHARGING_STATION ||
                        marker.categoryType.category == FacilityCategory.TOILET
                },
            )
            assertEquals(
                setOf(FacilityCategory.CHARGING_STATION, FacilityCategory.TOILET),
                viewModel.uiState.value.markerFilterState.selection.selectedFacilityCategories,
            )
            assertTrue(
                viewModel.uiState.value.shortcutFilterState.chips
                    .first { chip -> chip.key == MapShortcutFilterKey.CHARGING_STATION }
                    .isSelected,
            )
            assertTrue(
                viewModel.uiState.value.shortcutFilterState.chips
                    .first { chip -> chip.key == MapShortcutFilterKey.TOILET }
                    .isSelected,
            )
        }

    @Test
    fun `shortcut filter keeps explicit multi select when all nearby shortcut categories are selected`() =
        runTest {
            val placesRepository =
                FakePlacesRepository(
                    places =
                        listOf(
                            PlaceSummary(
                                placeId = "toilet-1",
                                name = "Accessible Toilet",
                                address = "1 Toilet-ro, Busan",
                                latitude = 35.1796,
                                longitude = 129.0756,
                                category = PlaceCategory.TOILET,
                            ),
                            PlaceSummary(
                                placeId = "elevator-1",
                                name = "Station Elevator",
                                address = "2 Elevator-ro, Busan",
                                latitude = 35.1802,
                                longitude = 129.0762,
                                category = PlaceCategory.ELEVATOR,
                            ),
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = EmptyFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.ShortcutFilterClicked(MapShortcutFilterKey.TOILET))
            viewModel.onAction(MapUiAction.ShortcutFilterClicked(MapShortcutFilterKey.ELEVATOR))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories)
            assertEquals(
                setOf(FacilityCategory.TOILET, FacilityCategory.ELEVATOR),
                viewModel.uiState.value.markerFilterState.selection.selectedFacilityCategories,
            )
            assertTrue(
                viewModel.uiState.value.shortcutFilterState.chips
                    .first { chip -> chip.key == MapShortcutFilterKey.TOILET }
                    .isSelected,
            )
            assertTrue(
                viewModel.uiState.value.shortcutFilterState.chips
                    .first { chip -> chip.key == MapShortcutFilterKey.ELEVATOR }
                    .isSelected,
            )
        }

    @Test
    fun `shortcut filter disabled chip tap shows unavailable snackbar without changing selection`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = facilitySeedRepositoryWithOtherCategory(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.ShortcutFilterClicked(MapShortcutFilterKey.CHARGING_STATION))
            advanceUntilIdle()

            val event =
                withTimeoutOrNull(100) {
                    viewModel.uiEvent.first()
                }

            assertEquals(
                MapUiEvent.ShowSnackbar("근처에 해당 장소가 없어요"),
                event,
            )
            assertFalse(viewModel.uiState.value.markerFilterState.selection.isShowingAllCategories)
            assertEquals(0, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
            assertFalse(
                viewModel.uiState.value.shortcutFilterState.chips
                    .first { chip -> chip.key == MapShortcutFilterKey.CHARGING_STATION }
                    .isSelected,
            )
        }

    @Test
    fun `category filter options exclude other even when browse data contains other markers`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = facilitySeedRepositoryWithOtherCategory(),
                    bookmarkRepository = FakeBookmarkRepository(),
                )

            advanceUntilIdle()

            assertFalse(
                viewModel.uiState.value.markerFilterState.categoryOptions.any { option ->
                    option.category == FacilityCategory.OTHER
                },
            )
            assertEquals(2, viewModel.uiState.value.markerOverlayState.totalMarkerCount)
            assertEquals(0, viewModel.uiState.value.markerOverlayState.visibleMarkerCount)
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

    @Test
    fun `places browse maps feature filter membership without changing facility detail category`() =
        runTest {
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = EmptyFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository =
                        FakePlacesRepository(
                            places =
                                listOf(
                                    PlaceSummary(
                                        placeId = "place-cafe-1",
                                        name = "Accessible Cafe",
                                        address = "1 Jungang-daero, Busan",
                                        latitude = 35.1796,
                                        longitude = 129.0756,
                                        category = PlaceCategory.FOOD_CAFE,
                                        features =
                                            listOf(
                                                PlaceFeatureAvailability(
                                                    featureType = PlaceFeatureType.ACCESSIBLE_TOILET,
                                                    isAvailable = true,
                                                ),
                                                PlaceFeatureAvailability(
                                                    featureType = PlaceFeatureType.ACCESSIBLE_PARKING,
                                                    isAvailable = true,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                )

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.shortcutFilterState.chips
                    .first { chip -> chip.key == MapShortcutFilterKey.TOILET }
                    .isEnabled,
            )

            viewModel.onAction(MapUiAction.ShortcutFilterClicked(MapShortcutFilterKey.TOILET))
            advanceUntilIdle()

            assertEquals(
                listOf("place-cafe-1"),
                viewModel.uiState.value.markerOverlayState.visibleMarkers.map { marker -> marker.markerId },
            )

            viewModel.onAction(MapUiAction.MarkerTapped("place-cafe-1"))
            advanceUntilIdle()

            assertEquals(
                FacilityCategory.FOOD_CAFE,
                viewModel.uiState.value.facilityDetailSheetState.detail?.category,
            )
        }

    @Test
    fun `marker tap upgrades sheet detail from live place detail and keeps destination handoff`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val placesRepository =
                FakePlacesRepository(
                    places =
                        listOf(
                            PlaceSummary(
                                placeId = "101",
                                name = "Accessible Cafe",
                                address = "1 Jungang-daero, Busan",
                                latitude = 35.1796,
                                longitude = 129.0756,
                                category = PlaceCategory.FOOD_CAFE,
                            ),
                        ),
                    placeDetailsById =
                        mapOf(
                            "101" to
                                PlaceDetail(
                                    placeId = "101",
                                    name = "Accessible Cafe",
                                    address = "1 Jungang-daero, Busan",
                                    latitude = 35.1796,
                                    longitude = 129.0756,
                                    category = PlaceCategory.FOOD_CAFE,
                                    features =
                                        listOf(
                                            PlaceFeatureAvailability(
                                                featureType = PlaceFeatureType.ACCESSIBLE_ENTRANCE,
                                                isAvailable = true,
                                            ),
                                            PlaceFeatureAvailability(
                                                featureType = PlaceFeatureType.ACCESSIBLE_TOILET,
                                                isAvailable = true,
                                            ),
                                        ),
                                    isBookmarked = true,
                                    accessibilityTags = listOf("step-free-entrance", "accessible-toilet"),
                                    providerPlaceId = "kakao-101",
                                    description = "East gate is step-free and the accessible toilet is inside.",
                                ),
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    facilitySeedRepository = EmptyFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MarkerTapped("101"))
            advanceUntilIdle()

            assertEquals(listOf("101"), placesRepository.detailRequests)
            val detail = requireNotNull(viewModel.uiState.value.facilityDetailSheetState.detail)
            assertEquals("101", detail.facilityId)
            assertEquals(FacilityCategory.FOOD_CAFE, detail.category)
            assertTrue(AccessibilityTag.STEP_FREE_ENTRANCE in detail.accessibilityTags)
            assertTrue(AccessibilityTag.ACCESSIBLE_TOILET in detail.accessibilityTags)
            assertEquals(
                "East gate is step-free and the accessible toilet is inside.",
                detail.description,
            )
            assertTrue(viewModel.uiState.value.facilityDetailSheetState.isBookmarked)

            viewModel.onAction(MapUiAction.FacilitySetDestinationClicked)
            advanceUntilIdle()

            val event =
                withTimeoutOrNull(100) {
                    viewModel.uiEvent.first()
                }

            assertEquals("101", destinationSelectionRepository.selectedDestination.value?.placeId)
            assertEquals("Accessible Cafe", destinationSelectionRepository.selectedDestination.value?.name)
            assertEquals(MapUiEvent.NavigateToRouteSetting, event)
        }

    @Test
    fun `live places browse uses the fallback busan camera center before current location is ready`() =
        runTest {
            val placesRepository =
                FakePlacesRepository(
                    places =
                        listOf(
                            PlaceSummary(
                                placeId = "fallback-place-1",
                                name = "Fallback Browse Place",
                                address = "1 Busan-daero, Busan",
                                latitude = 35.1802,
                                longitude = 129.0724,
                                category = PlaceCategory.WELFARE,
                            ),
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = EmptyFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            val query = placesRepository.queries.single()
            assertEquals(MapDefaults.BUSAN_CENTER.latitude, query.latitude ?: Double.NaN, 0.0)
            assertEquals(MapDefaults.BUSAN_CENTER.longitude, query.longitude ?: Double.NaN, 0.0)
            assertEquals(1_000, query.radiusMeters)
            assertEquals(MapCameraSource.DEFAULT_BUSAN, viewModel.uiState.value.cameraTarget.source)
        }

    @Test
    fun `marker tap keeps preview detail when live detail returns not found`() =
        runTest {
            val placesRepository =
                FakePlacesRepository(
                    places =
                        listOf(
                            PlaceSummary(
                                placeId = "404",
                                name = "Accessible Hotel",
                                address = "10 Haeundae-ro, Busan",
                                latitude = 35.1587,
                                longitude = 129.1604,
                                category = PlaceCategory.ACCOMMODATION,
                                features =
                                    listOf(
                                        PlaceFeatureAvailability(
                                            featureType = PlaceFeatureType.GUIDANCE_FACILITY,
                                            isAvailable = true,
                                        ),
                                        PlaceFeatureAvailability(
                                            featureType = PlaceFeatureType.ACCESSIBLE_ROOM,
                                            isAvailable = true,
                                        ),
                                    ),
                            ),
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = EmptyFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MarkerTapped("404"))
            advanceUntilIdle()

            val detail = requireNotNull(viewModel.uiState.value.facilityDetailSheetState.detail)
            assertEquals("404", detail.facilityId)
            assertEquals(FacilityCategory.ACCOMMODATION, detail.category)
            assertEquals(
                listOf(
                    AccessibilityTag.GUIDANCE_FACILITY,
                    AccessibilityTag.ACCESSIBLE_ROOM,
                ),
                detail.accessibilityTags,
            )
            assertEquals(listOf("404"), placesRepository.detailRequests)
            assertEquals("404", viewModel.uiState.value.selectedMarkerId)
        }

    @Test
    fun `marker tap keeps preview detail when live detail request fails`() =
        runTest {
            val placesRepository =
                FakePlacesRepository(
                    places =
                        listOf(
                            PlaceSummary(
                                placeId = "failed-detail",
                                name = "Accessible Welfare Center",
                                address = "7 Welfare-ro, Busan",
                                latitude = 35.1777,
                                longitude = 129.0711,
                                category = PlaceCategory.WELFARE,
                                features =
                                    listOf(
                                        PlaceFeatureAvailability(
                                            featureType = PlaceFeatureType.ACCESSIBLE_ENTRANCE,
                                            isAvailable = true,
                                        ),
                                        PlaceFeatureAvailability(
                                            featureType = PlaceFeatureType.ACCESSIBLE_PARKING,
                                            isAvailable = true,
                                        ),
                                    ),
                            ),
                        ),
                    detailFailureById =
                        mapOf(
                            "failed-detail" to IllegalStateException("detail fetch failed"),
                        ),
                )
            val viewModel =
                MapViewModel(
                    locationPermissionManager =
                        FakeLocationPermissionManager(initialState = LocationPermissionState.Denied),
                    currentLocationManager = FakeCurrentLocationManager(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                    facilitySeedRepository = EmptyFacilitySeedRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    placesRepository = placesRepository,
                )

            advanceUntilIdle()

            viewModel.onAction(MapUiAction.MarkerTapped("failed-detail"))
            advanceUntilIdle()

            val detail = requireNotNull(viewModel.uiState.value.facilityDetailSheetState.detail)
            assertEquals("failed-detail", detail.facilityId)
            assertEquals(FacilityCategory.WELFARE, detail.category)
            assertEquals(
                listOf(
                    AccessibilityTag.STEP_FREE_ENTRANCE,
                    AccessibilityTag.ACCESSIBLE_PARKING,
                ),
                detail.accessibilityTags,
            )
            assertEquals("failed-detail", viewModel.uiState.value.selectedMarkerId)
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
    recordedAtEpochMillis: Long = System.currentTimeMillis(),
): LocationSnapshot =
    LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 5f,
        recordedAtEpochMillis = recordedAtEpochMillis,
    )

private fun testMapTappedDetail(
    bookmarkTargetId: String,
    detailType: MapPlaceDetailType,
    provider: String? = null,
    providerPlaceId: String? = null,
    name: String,
    category: PlaceCategory? = null,
    providerCategory: String? = null,
    address: String = "",
    latitude: Double,
    longitude: Double,
    isBookmarked: Boolean = false,
    accessibilityTags: List<String> = emptyList(),
): MapTappedPlaceDetail =
    MapTappedPlaceDetail(
        bookmarkTargetId = bookmarkTargetId,
        detailType = detailType,
        placeId = null,
        provider = provider,
        providerPlaceId = providerPlaceId,
        name = name,
        category = category,
        providerCategory = providerCategory,
        address = address,
        latitude = latitude,
        longitude = longitude,
        isBookmarked = isBookmarked,
        accessibilityTags = accessibilityTags,
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

private class FakeSearchRepository(
    private val recentDestinations: List<RecentDestination> = emptyList(),
) : SearchRepository {
    override suspend fun search(query: com.ssafy.e102.eumgil.core.model.SearchQuery) =
        emptyList<com.ssafy.e102.eumgil.core.model.SearchResult>()

    override suspend fun getRecentSearches() = emptyList<com.ssafy.e102.eumgil.core.model.RecentSearch>()

    override suspend fun saveRecentSearch(keyword: String) = Unit

    override suspend fun getRecentDestinations(): List<RecentDestination> = recentDestinations

    override suspend fun saveRecentDestination(destination: RecentDestination) = Unit
}

private class FakePlacesRepository(
    private val places: List<PlaceSummary> = emptyList(),
    private val placeDetailsById: Map<String, PlaceDetail> = emptyMap(),
    private val mapTapDetail: MapTappedPlaceDetail? = null,
    private val placesFailure: Throwable? = null,
    private val detailFailureById: Map<String, Throwable> = emptyMap(),
    private val mapTapDetailFailure: Throwable? = null,
) : PlacesRepository {
    val queries = mutableListOf<PlaceQuery>()
    val detailRequests = mutableListOf<String>()
    val mapTapDetailRequests = mutableListOf<MapPlaceDetailRequest>()

    override suspend fun getPlaces(query: PlaceQuery): List<PlaceSummary> {
        queries += query
        placesFailure?.let { throw it }
        return places
    }

    override suspend fun getPlaceDetail(placeId: String): PlaceDetail? {
        detailRequests += placeId
        detailFailureById[placeId]?.let { throw it }
        return placeDetailsById[placeId]
    }

    override suspend fun getMapTappedPlaceDetail(request: MapPlaceDetailRequest): MapTappedPlaceDetail? {
        mapTapDetailRequests += request
        mapTapDetailFailure?.let { throw it }
        return mapTapDetail
    }
}

private fun testFacilitySeedRepository(): FacilitySeedRepository =
    DefaultFacilitySeedRepository(
        localDataSource = FacilitySeedLocalDataSource(),
        mockDataSource = FacilitySeedMockDataSource(),
    )

private fun facilitySeedRepositoryWithOtherCategory(): FacilitySeedRepository =
    object : FacilitySeedRepository {
        private val toiletMarker =
            FacilityMarkerSeed(
                facilityId = "facility-toilet",
                name = "장애인 화장실",
                coordinate = GeoCoordinate(latitude = 35.1, longitude = 129.1),
                category = FacilityCategory.TOILET,
            )
        private val otherMarker =
            FacilityMarkerSeed(
                facilityId = "facility-other",
                name = "기타 편의시설",
                coordinate = GeoCoordinate(latitude = 35.2, longitude = 129.2),
                category = FacilityCategory.OTHER,
            )
        private val details =
            listOf(
                FacilityDetailSeed(
                    facilityId = toiletMarker.facilityId,
                    name = toiletMarker.name,
                    address = "부산광역시 테스트구 1",
                    coordinate = toiletMarker.coordinate,
                    category = toiletMarker.category,
                ),
                FacilityDetailSeed(
                    facilityId = otherMarker.facilityId,
                    name = otherMarker.name,
                    address = "부산광역시 테스트구 2",
                    coordinate = otherMarker.coordinate,
                    category = otherMarker.category,
                ),
            )

        override suspend fun getSeedCatalog(): FacilitySeedCatalog =
            FacilitySeedCatalog(
                facilities =
                    details.map { detail ->
                        FacilitySeed(
                            facilityId = detail.facilityId,
                            name = detail.name,
                            address = detail.address,
                            coordinate = detail.coordinate,
                            category = detail.category,
                        )
                    },
            )

        override suspend fun getFacilityBrowseData(query: FacilitySeedQuery): FacilityBrowseData =
            FacilityBrowseData(
                facilityMarkers = listOf(toiletMarker, otherMarker),
                detailsById = details.associateBy { detail -> detail.facilityId },
                availableCategories = listOf(FacilityCategory.TOILET, FacilityCategory.OTHER),
            )

        override suspend fun getFacilityMarkers(query: FacilitySeedQuery): List<FacilityMarkerSeed> =
            listOf(toiletMarker, otherMarker)

        override suspend fun getFacilityDetail(facilityId: String): FacilityDetailSeed? =
            details.firstOrNull { detail -> detail.facilityId == facilityId }
    }

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

private fun recentDestination(
    placeId: String,
    searchedAtMillis: Long,
): RecentDestination =
    RecentDestination(
        placeId = placeId,
        name = "Recent Destination $placeId",
        address = "Busan Address $placeId",
        latitude = 35.1796,
        longitude = 129.0756,
        category = PlaceCategory.RESTAURANT,
        accessibilityTagKeys = listOf("accessible-parking"),
        searchedAtMillis = searchedAtMillis,
    )
