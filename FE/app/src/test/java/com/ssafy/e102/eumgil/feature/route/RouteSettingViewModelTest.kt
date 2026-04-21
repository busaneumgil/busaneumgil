package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RoutePolyline
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchData
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteSearchResult
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.core.model.RoutePreviewModel
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.data.local.datasource.RouteLocalDataSource
import com.ssafy.e102.eumgil.data.mock.datasource.RouteMockDataSource
import com.ssafy.e102.eumgil.data.repository.DefaultRouteRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
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
            val safeCard = uiState.optionCards.first()
            val shortestCard = uiState.optionCards.last()
            assertTrue(safeCard.isSelected)
            assertEquals("SAFE 우선", safeCard.title)
            assertEquals("안전 요소와 보행 위험을 함께 고려해 우선 제안하는 경로입니다.", safeCard.description)
            assertEquals("현재 선택됨", safeCard.selectionLabel)
            assertEquals("추천", safeCard.highlightLabel)
            assertEquals(
                listOf("위험도", "예상 시간"),
                safeCard.metrics.map(RouteOptionCardMetricUiState::label),
            )
            assertEquals(
                listOf("낮음", "16분"),
                safeCard.metrics.map(RouteOptionCardMetricUiState::value),
            )
            assertEquals("최단 거리", shortestCard.title)
            assertEquals("이동 시간을 줄이는 기준으로 빠른 경로를 비교합니다.", shortestCard.description)
            assertEquals("탭하여 선택", shortestCard.selectionLabel)
            assertEquals(
                listOf("예상 시간", "예상 거리"),
                shortestCard.metrics.map(RouteOptionCardMetricUiState::label),
            )
            assertEquals(RouteOption.SAFE, uiState.selectedRoute?.routeOption)
            assertEquals("SAFE 우선", uiState.selectedRoute?.optionTitle)
            assertEquals("Safe Route", uiState.selectedRoute?.title)
            assertEquals(980, uiState.selectedRoute?.distanceMeters)
            assertEquals(16, uiState.selectedRoute?.estimatedTimeMinutes)
            assertEquals(RouteRiskLevel.LOW, uiState.selectedRoute?.riskLevel)
            assertEquals("16분", uiState.selectedRoute?.estimatedTimeLabel)
            assertEquals("980 m", uiState.selectedRoute?.distanceLabel)
            assertEquals("위험도 낮음", uiState.selectedRoute?.riskLabel)
            assertEquals("3/3", uiState.selectedRoute?.renderableSegmentLabel)
            assertEquals(
                listOf("예상 시간", "예상 거리", "위험도", "렌더링 구간"),
                uiState.selectedRoute?.summaryMetrics?.map(RouteSummaryMetricUiState::label),
            )
            assertTrue(uiState.selectedRoute?.previewPoints?.size ?: 0 >= 2)
            assertEquals(null, uiState.selectedRoute?.previewFallbackNotice)
            assertTrue(uiState.cta.isEnabled)
            assertEquals("선택한 경로로 안내 시작", uiState.cta.label)
            assertEquals("201 작업에서 route setting handoff를 navigation 진행 화면으로 연결합니다.", uiState.cta.supportingText)
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
            assertEquals("부산 동구 중앙대로 206", uiState.destination.supportingText)
            assertEquals(RouteOption.SAFE, uiState.selectedRoute?.routeOption)
            assertTrue(uiState.isStartEnabled)
        }

    @Test
    fun `partial route data exposes fallback summary labels and address placeholder`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    updateSelectedDestination(
                        PlaceDestination(
                            placeId = "place-2",
                            name = "주소 없는 목적지",
                            address = null,
                            latitude = 35.1799,
                            longitude = 129.0762,
                        ),
                    )
                }
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = partialRouteRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            val selectedRoute = requireNotNull(uiState.selectedRoute)

            assertEquals("주소 정보 없음", uiState.destination.supportingText)
            assertEquals("확인 중", selectedRoute.estimatedTimeLabel)
            assertEquals("확인 중", selectedRoute.distanceLabel)
            assertEquals("위험도 보통", selectedRoute.riskLabel)
            assertEquals("0/2", selectedRoute.renderableSegmentLabel)
            assertEquals("선택한 경로를 따라 이동합니다.", selectedRoute.guidanceMessage)
            assertEquals(
                "일부 구간은 geometry fallback 상태라 preview 없이 요약 정보만 표시합니다.",
                selectedRoute.previewFallbackNotice,
            )
        }

    @Test
    fun `empty route result keeps summary empty and disables start action`() =
        runTest {
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = emptyRouteRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertTrue(uiState.optionCards.isEmpty())
            assertEquals(null, uiState.selectedRoute)
            assertFalse(uiState.cta.isEnabled)
            assertEquals("표시할 경로가 준비되면 시작 CTA를 활성화합니다.", uiState.cta.supportingText)
            assertFalse(uiState.isStartEnabled)
        }

    @Test
    fun `route load failure exposes disabled CTA and error supporting text`() =
        runTest {
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = failingRouteRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertFalse(uiState.cta.isEnabled)
            assertEquals("경로 정보를 다시 불러오면 시작 CTA를 활성화할 수 있습니다.", uiState.cta.supportingText)
            assertEquals("fixture load failed", uiState.loadErrorMessage)
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
            assertEquals("최단 거리", uiState.selectedRoute?.optionTitle)
            assertEquals("Shortest Route", uiState.selectedRoute?.title)
            assertEquals(RouteRiskLevel.MEDIUM, uiState.selectedRoute?.riskLevel)
            assertTrue(uiState.optionCards.single { card -> card.routeOption == RouteOption.SHORTEST }.isSelected)
            assertEquals(
                "현재 선택됨",
                uiState.optionCards.single { card -> card.routeOption == RouteOption.SHORTEST }.selectionLabel,
            )
            assertEquals(
                "탭하여 선택",
                uiState.optionCards.single { card -> card.routeOption == RouteOption.SAFE }.selectionLabel,
            )
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
            assertFalse(viewModel.uiState.value.cta.isEnabled)
            assertEquals("내비게이션 진행 화면 연결은 다음 스레드에서 마무리합니다.", viewModel.uiState.value.cta.supportingText)
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

private fun partialRouteRepository(): RouteRepository =
    object : RouteRepository {
        override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
            RouteSearchData(
                query = query,
                result =
                    RouteSearchResult(
                        origin = query.origin,
                        destination = query.destination,
                        routes =
                            listOf(
                                RouteCandidate(
                                    routeOption = RouteOption.SAFE,
                                    title = "Safe Route",
                                    summary =
                                        RouteSummary(
                                            distanceMeters = 0,
                                            estimatedTimeMinutes = 0,
                                            riskLevel = RouteRiskLevel.MEDIUM,
                                        ),
                                    preview =
                                        RoutePreviewModel(
                                            polyline = RoutePolyline(),
                                            segmentCount = 2,
                                            renderableSegmentCount = 0,
                                            fallbackSegmentCount = 2,
                                        ),
                                    segments =
                                        listOf(
                                            RouteSegment(
                                                sequence = 1,
                                                guidanceMessage = "",
                                            ),
                                            RouteSegment(
                                                sequence = 2,
                                                guidanceMessage = " ",
                                            ),
                                        ),
                                ),
                            ),
                    ),
                source =
                    RouteSearchSource.mockFixture(
                        fixtureId = "partial-fixture",
                        label = "Partial route fixture",
                    ),
            )
    }

private fun emptyRouteRepository(): RouteRepository =
    object : RouteRepository {
        override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
            RouteSearchData(
                query = query,
                result =
                    RouteSearchResult(
                        origin = RouteWaypoint(name = "현재 위치", coordinate = GeoCoordinate(35.1796, 129.0756)),
                        destination = RouteWaypoint(name = "부산역", coordinate = GeoCoordinate(35.1151, 129.0414)),
                        routes = emptyList(),
                    ),
                source =
                    RouteSearchSource.mockFixture(
                        fixtureId = "empty-fixture",
                        label = "Empty route fixture",
                    ),
            )
    }

private fun failingRouteRepository(): RouteRepository =
    object : RouteRepository {
        override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData =
            error("fixture load failed")
    }
