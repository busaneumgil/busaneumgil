package com.ssafy.e102.eumgil.feature.route

import com.ssafy.e102.eumgil.core.model.PlaceCategory
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
    fun `init loads SAFE route by default with preview map state`() =
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
            assertEquals("place-1", uiState.destination.placeId)
            assertEquals(PlaceCategory.RESTAURANT, uiState.destination.category)
            assertEquals("ID place-1 | Category RESTAURANT", uiState.destination.metadataLabel)
            assertEquals("현재 위치", uiState.origin.name)
            assertEquals("카페 온도", uiState.destination.name)
            assertFalse(uiState.isUsingFallbackDestination)
            assertEquals(listOf(RouteOption.SAFE, RouteOption.SHORTEST), uiState.optionCards.map(RouteOptionCardUiState::routeOption))
            val safeCard = uiState.optionCards.first()
            val shortestCard = uiState.optionCards.last()
            assertTrue(safeCard.isSelected)
            assertEquals("안전한 길", safeCard.title)
            assertEquals("보행 안전 요소를 우선으로 반영한 추천 경로입니다.", safeCard.description)
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
            assertEquals("최단거리", shortestCard.title)
            assertEquals("이동 거리를 줄이는 기준으로 빠른 경로를 비교합니다.", shortestCard.description)
            assertEquals("탭하여 선택", shortestCard.selectionLabel)
            assertEquals(
                listOf("예상 시간", "예상 거리"),
                shortestCard.metrics.map(RouteOptionCardMetricUiState::label),
            )
            assertEquals(RouteOption.SAFE, uiState.selectedRoute?.routeOption)
            assertEquals("안전한 길", uiState.selectedRoute?.optionTitle)
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
            assertEquals(uiState.destination, uiState.selectedRoute?.destination)
            assertTrue(uiState.selectedRoute?.previewPoints?.size ?: 0 >= 2)
            assertEquals(null, uiState.selectedRoute?.previewFallbackNotice)
            assertEquals(RoutePreviewMapStatus.READY, uiState.routePreviewMap.status)
            assertEquals(RouteOption.SAFE, uiState.routePreviewMap.routeOption)
            assertEquals(uiState.origin.coordinate, uiState.routePreviewMap.originCoordinate)
            assertEquals(uiState.destination.coordinate, uiState.routePreviewMap.destinationCoordinate)
            assertEquals(uiState.selectedRoute?.previewPoints, uiState.routePreviewMap.polyline)
            assertEquals(null, uiState.routePreviewMap.fallbackMessage)
            assertTrue(uiState.routePreviewMap.isDisplayable)
            assertTrue(uiState.cta.isEnabled)
            assertEquals("길 안내 시작", uiState.cta.label)
            assertEquals("선택한 경로로 길 안내를 시작할 수 있습니다.", uiState.cta.supportingText)
            assertTrue(uiState.isStartEnabled)
            assertEquals(
                listOf("엘리베이터 있음", "공사 구간 주의", "신호등 횡단보도", "연석 단차 주의"),
                uiState.selectedRoute?.detailAccessibilityChips?.map(RouteDetailChipUiState::label),
            )
            assertEquals(
                listOf("연석 단차 주의"),
                uiState.selectedRoute?.detailHighlights?.map(RouteDetailHighlightUiState::title),
            )
            assertEquals(
                listOf("출발", "직진 이동", "엘리베이터 이용", "공사 구간 진입", "횡단보도 건너기", "단차 구간 주의", "직진 이동", "도착"),
                uiState.selectedRoute?.detailSteps?.map(RouteDetailStepUiState::title),
            )
            assertEquals(
                listOf(
                    RouteDetailStepKind.START,
                    RouteDetailStepKind.WALK,
                    RouteDetailStepKind.ELEVATOR,
                    RouteDetailStepKind.CONSTRUCTION,
                    RouteDetailStepKind.CROSSWALK,
                    RouteDetailStepKind.CURB_GAP,
                    RouteDetailStepKind.WALK,
                    RouteDetailStepKind.ARRIVAL,
                ),
                uiState.selectedRoute?.detailSteps?.map(RouteDetailStepUiState::kind),
            )
            assertEquals(
                listOf("150 m", "90 m", "62 m", "128 m", "100 m", "450 m"),
                uiState.selectedRoute?.detailSteps?.drop(1)?.dropLast(1)?.mapNotNull(RouteDetailStepUiState::metaLabel),
            )
            assertEquals(null, uiState.selectedRoute?.detailFallbackMessage)
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
            assertEquals(RouteDestinationHandoffState.EMPTY, uiState.destinationHandoffState)
            assertEquals("검색 handoff 전에는 fixture 목적지를 기본 도착지로 유지합니다.", uiState.destinationFallbackMessage)
            assertEquals(null, uiState.destination.metadataLabel)
            assertEquals("부산역", uiState.destination.name)
            assertEquals("부산 동구 중앙대로 206", uiState.destination.supportingText)
            assertEquals(RouteOption.SAFE, uiState.selectedRoute?.routeOption)
            assertEquals(uiState.destination, uiState.selectedRoute?.destination)
            assertEquals(RoutePreviewMapStatus.NO_DESTINATION, uiState.routePreviewMap.status)
            assertEquals("Destination is required before showing a route preview map.", uiState.routePreviewMap.fallbackMessage)
            assertFalse(uiState.routePreviewMap.isDisplayable)
            assertFalse(uiState.isStartEnabled)
            assertEquals("검색 또는 지도에서 목적지를 선택하면 안내 시작을 활성화합니다.", uiState.cta.supportingText)
        }

    @Test
    fun `selected destination update replaces fallback destination metadata after init`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = testRouteRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isUsingFallbackDestination)
            assertEquals(null, viewModel.uiState.value.destination.metadataLabel)

            destinationSelectionRepository.updateSelectedDestination(testDestination())
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertFalse(uiState.isUsingFallbackDestination)
            assertEquals(RouteDestinationHandoffState.DIRECT, uiState.destinationHandoffState)
            assertEquals(null, uiState.destinationFallbackMessage)
            assertEquals("place-1", uiState.destination.placeId)
            assertEquals(PlaceCategory.RESTAURANT, uiState.destination.category)
            assertEquals("ID place-1 | Category RESTAURANT", uiState.destination.metadataLabel)
            assertEquals("카페 온도", uiState.destination.name)
            assertEquals(uiState.destination, uiState.selectedRoute?.destination)
        }

    @Test
    fun `invalid destination coordinates fall back to fixture destination and disable start action`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    updateSelectedDestination(invalidDestination())
                }
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = testRouteRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertTrue(uiState.isUsingFallbackDestination)
            assertEquals(RouteDestinationHandoffState.INVALID_COORDINATE, uiState.destinationHandoffState)
            assertEquals("선택한 목적지 좌표를 확인할 수 없어 fixture 목적지로 대체했습니다.", uiState.destinationFallbackMessage)
            assertEquals("부산역", uiState.destination.name)
            assertEquals(uiState.destination, uiState.selectedRoute?.destination)
            assertEquals(RoutePreviewMapStatus.INVALID_DESTINATION, uiState.routePreviewMap.status)
            assertEquals("Destination coordinate is invalid.", uiState.routePreviewMap.fallbackMessage)
            assertFalse(uiState.routePreviewMap.isDisplayable)
            assertFalse(uiState.isStartEnabled)
            assertEquals("목적지 좌표를 다시 확인하면 안내 시작을 활성화합니다.", uiState.cta.supportingText)
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
            assertEquals(uiState.destination, selectedRoute.destination)
            assertEquals(
                "일부 구간은 geometry fallback 상태라 preview 없이 요약 정보만 표시합니다.",
                selectedRoute.previewFallbackNotice,
            )
            assertEquals(listOf("상세 정보 확인 중"), selectedRoute.detailAccessibilityChips.map(RouteDetailChipUiState::label))
            assertTrue(selectedRoute.detailHighlights.isEmpty())
            assertEquals(
                listOf("출발", "세부 경로 확인 중", "도착"),
                selectedRoute.detailSteps.map(RouteDetailStepUiState::title),
            )
            assertEquals(
                "세부 이동 정보는 준비 중입니다. 요약 정보와 주의 구간을 먼저 확인하세요.",
                selectedRoute.detailFallbackMessage,
            )
            assertEquals(RoutePreviewMapStatus.POLYLINE_UNAVAILABLE, uiState.routePreviewMap.status)
            assertEquals(RouteOption.SAFE, uiState.routePreviewMap.routeOption)
            assertEquals(uiState.origin.coordinate, uiState.routePreviewMap.originCoordinate)
            assertEquals(uiState.destination.coordinate, uiState.routePreviewMap.destinationCoordinate)
            assertTrue(uiState.routePreviewMap.polyline.isEmpty())
            assertEquals("Selected route preview polyline needs at least two points.", uiState.routePreviewMap.fallbackMessage)
            assertFalse(uiState.routePreviewMap.isDisplayable)
        }

    @Test
    fun `clearing selected destination returns route setting to fallback shell`() =
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
            destinationSelectionRepository.clearSelectedDestination()
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertTrue(uiState.isUsingFallbackDestination)
            assertEquals(RouteDestinationHandoffState.EMPTY, uiState.destinationHandoffState)
            assertEquals("부산역", uiState.destination.name)
            assertEquals(RouteOption.SAFE, uiState.selectedOption)
            assertFalse(uiState.isStartEnabled)
        }

    @Test
    fun `empty route result keeps preview map no route fallback and disables start action`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    updateSelectedDestination(testDestination())
                }
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = emptyRouteRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertTrue(uiState.optionCards.isEmpty())
            assertEquals(null, uiState.selectedRoute)
            assertEquals(RouteDestinationHandoffState.DIRECT, uiState.destinationHandoffState)
            assertEquals(RoutePreviewMapStatus.NO_ROUTE, uiState.routePreviewMap.status)
            assertEquals(uiState.origin.coordinate, uiState.routePreviewMap.originCoordinate)
            assertEquals(uiState.destination.coordinate, uiState.routePreviewMap.destinationCoordinate)
            assertEquals("No selected route is available for the preview map.", uiState.routePreviewMap.fallbackMessage)
            assertFalse(uiState.routePreviewMap.isDisplayable)
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
            assertEquals(RoutePreviewMapStatus.ERROR, uiState.routePreviewMap.status)
            assertEquals("fixture load failed", uiState.routePreviewMap.fallbackMessage)
            assertFalse(uiState.routePreviewMap.isDisplayable)
        }

    @Test
    fun `route option selection swaps summary and preview map to shortest route`() =
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
            assertEquals(RouteOption.SHORTEST, uiState.routePreviewMap.routeOption)
            assertEquals(RoutePreviewMapStatus.READY, uiState.routePreviewMap.status)
            assertEquals(uiState.selectedRoute?.previewPoints, uiState.routePreviewMap.polyline)
            assertEquals(uiState.origin.coordinate, uiState.routePreviewMap.originCoordinate)
            assertEquals(uiState.destination.coordinate, uiState.routePreviewMap.destinationCoordinate)
            assertTrue(uiState.routePreviewMap.isDisplayable)
            assertEquals("최단거리", uiState.selectedRoute?.optionTitle)
            assertEquals("Shortest Route", uiState.selectedRoute?.title)
            assertEquals(RouteRiskLevel.MEDIUM, uiState.selectedRoute?.riskLevel)
            assertEquals(uiState.destination, uiState.selectedRoute?.destination)
            assertEquals(
                listOf("무신호 횡단 주의", "연석 단차 주의"),
                uiState.selectedRoute?.detailAccessibilityChips?.map(RouteDetailChipUiState::label),
            )
            assertEquals(
                listOf("무신호 횡단 주의", "연석 단차 주의"),
                uiState.selectedRoute?.detailHighlights?.map(RouteDetailHighlightUiState::title),
            )
            assertEquals(
                listOf(RouteDetailStepKind.CROSSWALK, RouteDetailStepKind.CURB_GAP),
                uiState.selectedRoute?.detailSteps?.drop(1)?.dropLast(1)?.map(RouteDetailStepUiState::kind),
            )
            assertEquals(
                listOf(RouteDetailTone.WARNING, RouteDetailTone.WARNING),
                uiState.selectedRoute?.detailSteps?.drop(1)?.dropLast(1)?.map(RouteDetailStepUiState::tone),
            )
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
    fun `waypoint swap action swaps displayed endpoints and preview direction`() =
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
            val initialState = viewModel.uiState.value
            val initialOrigin = initialState.origin
            val initialDestination = initialState.destination
            val initialPreviewPoints = initialState.routePreviewMap.polyline

            viewModel.onAction(RouteSettingUiAction.WaypointsSwapClicked)
            advanceUntilIdle()

            val swappedState = viewModel.uiState.value

            assertEquals(initialDestination, swappedState.origin)
            assertEquals(initialOrigin, swappedState.destination)
            assertEquals(initialDestination.coordinate, swappedState.routePreviewMap.originCoordinate)
            assertEquals(initialOrigin.coordinate, swappedState.routePreviewMap.destinationCoordinate)
            assertEquals(initialPreviewPoints.reversed(), swappedState.routePreviewMap.polyline)
            assertEquals(swappedState.destination, swappedState.selectedRoute?.destination)
            assertTrue(swappedState.isStartEnabled)

            viewModel.onAction(RouteSettingUiAction.WaypointsSwapClicked)
            advanceUntilIdle()

            val restoredState = viewModel.uiState.value

            assertEquals(initialOrigin, restoredState.origin)
            assertEquals(initialDestination, restoredState.destination)
            assertEquals(initialPreviewPoints, restoredState.routePreviewMap.polyline)
        }

    @Test
    fun `route detail action selects target option and emits detail navigation event`() =
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

            viewModel.onAction(RouteSettingUiAction.RouteOptionDetailClicked(RouteOption.SHORTEST))
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertEquals(RouteOption.SHORTEST, uiState.selectedOption)
            assertEquals(RouteOption.SHORTEST, uiState.selectedRoute?.routeOption)
            val event = uiEvent.await()
            assertTrue(event is RouteSettingUiEvent.NavigateToRouteDetail)
            assertEquals(RouteOption.SHORTEST, (event as RouteSettingUiEvent.NavigateToRouteDetail).routeOption)
        }

    @Test
    fun `same destination reselection reloads route shell and resets option to SAFE`() =
        runTest {
            val destinationSelectionRepository =
                InMemoryDestinationSelectionRepository().apply {
                    updateSelectedDestination(testDestination())
                }
            val routeRepository = CountingRouteRepository()
            val viewModel =
                RouteSettingViewModel(
                    routeRepository = routeRepository,
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            viewModel.onAction(RouteSettingUiAction.RouteOptionSelected(RouteOption.SHORTEST))
            advanceUntilIdle()

            destinationSelectionRepository.updateSelectedDestination(testDestination())
            advanceUntilIdle()

            val uiState = viewModel.uiState.value

            assertEquals(2, routeRepository.callCount)
            assertEquals(RouteOption.SAFE, uiState.selectedOption)
            assertEquals(RouteOption.SAFE, uiState.selectedRoute?.routeOption)
            assertEquals("Safe Route #2", uiState.selectedRoute?.title)
            assertEquals(uiState.destination, uiState.selectedRoute?.destination)
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
            assertEquals("길 안내를 시작하는 중입니다.", viewModel.uiState.value.cta.supportingText)
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
        category = PlaceCategory.RESTAURANT,
    )

private fun invalidDestination(): PlaceDestination =
    PlaceDestination(
        placeId = "place-invalid",
        name = "좌표 누락 목적지",
        address = "알 수 없는 위치",
        latitude = Double.NaN,
        longitude = 129.0750,
        category = PlaceCategory.OTHER,
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

private class CountingRouteRepository : RouteRepository {
    var callCount: Int = 0
        private set

    override suspend fun getRouteSearchData(query: RouteSearchQuery): RouteSearchData {
        callCount += 1
        val countLabel = callCount
        return RouteSearchData(
            query = query,
            result =
                RouteSearchResult(
                    origin = query.origin,
                    destination = query.destination,
                    routes =
                        listOf(
                            RouteCandidate(
                                routeOption = RouteOption.SAFE,
                                title = "Safe Route #$countLabel",
                                summary =
                                    RouteSummary(
                                        distanceMeters = 900 + countLabel,
                                        estimatedTimeMinutes = 15 + countLabel,
                                        riskLevel = RouteRiskLevel.LOW,
                                    ),
                                preview =
                                    RoutePreviewModel(
                                        polyline =
                                            RoutePolyline(
                                                points =
                                                    listOf(
                                                        query.origin.coordinate,
                                                        query.destination.coordinate,
                                                    ),
                                            ),
                                        segmentCount = 1,
                                        renderableSegmentCount = 1,
                                        fallbackSegmentCount = 0,
                                    ),
                                segments =
                                    listOf(
                                        RouteSegment(
                                            sequence = 1,
                                            guidanceMessage = "Safe guidance #$countLabel",
                                        ),
                                    ),
                            ),
                            RouteCandidate(
                                routeOption = RouteOption.SHORTEST,
                                title = "Shortest Route #$countLabel",
                                summary =
                                    RouteSummary(
                                        distanceMeters = 800 + countLabel,
                                        estimatedTimeMinutes = 13 + countLabel,
                                        riskLevel = RouteRiskLevel.MEDIUM,
                                    ),
                                preview =
                                    RoutePreviewModel(
                                        polyline =
                                            RoutePolyline(
                                                points =
                                                    listOf(
                                                        query.origin.coordinate,
                                                        query.destination.coordinate,
                                                    ),
                                            ),
                                        segmentCount = 1,
                                        renderableSegmentCount = 1,
                                        fallbackSegmentCount = 0,
                                    ),
                                segments =
                                    listOf(
                                        RouteSegment(
                                            sequence = 1,
                                            guidanceMessage = "Shortest guidance #$countLabel",
                                        ),
                                    ),
                            ),
                        ),
                ),
            source =
                RouteSearchSource.mockFixture(
                    fixtureId = "counting-fixture-$countLabel",
                    label = "Counting route fixture #$countLabel",
                ),
        )
    }
}
