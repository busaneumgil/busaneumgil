package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.repository.DefaultRouteRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads SAFE route by default with selected destination summary`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    updateSelectedDestination(testDestination())
                }
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = testRouteRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertFalse(uiState.isLoading)
            assertEquals(RouteOption.SAFE, uiState.selectedOption)
            assertEquals("현재 위치", uiState.origin.name)
            assertEquals("카페 온도", uiState.destination.name)
            assertFalse(uiState.isUsingFallbackDestination)
            assertEquals(listOf(RouteOption.SAFE, RouteOption.SHORTEST), uiState.optionCards.map(RouteOptionCardUiState::routeOption))
            assertTrue(uiState.optionCards.first().isSelected)
            assertEquals(RouteOption.SAFE, uiState.selectedRoute?.routeOption)
            assertEquals("Safe Route", uiState.selectedRoute?.title)
            assertEquals(980, uiState.selectedRoute?.distanceMeters)
            assertEquals(16, uiState.selectedRoute?.estimatedTimeMinutes)
            assertEquals(RouteRiskLevel.LOW, uiState.selectedRoute?.riskLevel)
            assertTrue(uiState.selectedRoute?.previewPoints?.size ?: 0 >= 2)
            assertTrue(uiState.isStartEnabled)
        }

    @Test
    fun `empty destination falls back to fixture destination and still renders summary`() =
        runTest {
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = testRouteRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertTrue(uiState.isUsingFallbackDestination)
            assertEquals("부산역", uiState.destination.name)
            assertEquals(RouteOption.SAFE, uiState.selectedRoute?.routeOption)
            assertTrue(uiState.isStartEnabled)
        }

    @Test
    fun `route option selection swaps summary to shortest route`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    updateSelectedDestination(testDestination())
                }
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = testRouteRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            viewModel.onAction(RouteSettingUiAction.RouteOptionSelected(RouteOption.SHORTEST))
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(RouteOption.SHORTEST, uiState.selectedOption)
            assertEquals(RouteOption.SHORTEST, uiState.selectedRoute?.routeOption)
            assertEquals("Shortest Route", uiState.selectedRoute?.title)
            assertEquals(RouteRiskLevel.MEDIUM, uiState.selectedRoute?.riskLevel)
            assertTrue(uiState.optionCards.single { card -> card.routeOption == RouteOption.SHORTEST }.isSelected)
        }

    @Test
    fun `start action acknowledges pending handoff and emits navigation request`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    updateSelectedDestination(testDestination())
                }
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = testRouteRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = async { viewModel.uiEvent.first() }

            viewModel.onAction(RouteSettingUiAction.StartNavigationClicked)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.ctaAcknowledged)
            val event = uiEvent.await()
            assertTrue(event is RouteSettingUiEvent.StartNavigationRequested)
            assertEquals(
                RouteOption.SAFE,
                (event as RouteSettingUiEvent.StartNavigationRequested).request.selectedRoute.routeOption,
            )
        }
}

private fun testRouteRepository() =
    DefaultRouteRepository(
        localDataSource = RouteLocalDataSource(),
        mockDataSource = RouteMockDataSource(),
    )

private fun testDestination(): PlaceDestination =
    PlaceDestination(
        placeId = "place-1",
        name = "카페 온도",
        address = "부산 부산진구 중앙대로 1001",
        latitude = 35.1797,
        longitude = 129.0750,
    )
