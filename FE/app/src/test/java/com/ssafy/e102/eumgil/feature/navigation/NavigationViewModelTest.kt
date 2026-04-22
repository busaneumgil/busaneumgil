package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.RouteCandidate
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteRiskLevel
import com.ssafy.e102.eumgil.core.model.RouteSearchSource
import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSummary
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial ui state exposes navigation shell placeholders`() =
        runTest {
            val viewModel = NavigationViewModel()

            assertEquals(NavigationScreenState.Loading, viewModel.uiState.value.screenState)
            assertEquals("다음 안내", viewModel.uiState.value.stepCard.sectionLabel)
            assertEquals("준비 중", viewModel.uiState.value.stepCard.statusLabel)
            assertEquals("경로 확인", viewModel.uiState.value.stepCard.emphasisLabel)
            assertEquals("확인 중", viewModel.uiState.value.stepCard.distanceLabel)
            assertEquals("경로 안내를 불러오는 중입니다", viewModel.uiState.value.stepCard.instruction)
            assertEquals(
                "선택한 경로 정보를 확인한 뒤 첫 안내 메시지를 표시합니다.",
                viewModel.uiState.value.stepCard.supportingText,
            )
            assertEquals(3, viewModel.uiState.value.stepCard.metrics.size)
            assertEquals("남은 거리", viewModel.uiState.value.stepCard.metrics[0].label)
            assertEquals("확인 중", viewModel.uiState.value.stepCard.metrics[0].value)
            assertEquals("예상 시간", viewModel.uiState.value.stepCard.metrics[1].label)
            assertEquals("확인 중", viewModel.uiState.value.stepCard.metrics[1].value)
            assertEquals("진행 단계", viewModel.uiState.value.stepCard.metrics[2].label)
            assertEquals("-", viewModel.uiState.value.stepCard.metrics[2].value)
            assertEquals("안내 준비 중", viewModel.uiState.value.exitCta.label)
            assertEquals("경로 정보가 준비되면 종료 버튼이 활성화됩니다.", viewModel.uiState.value.exitCta.supportingText)
            assertFalse(viewModel.uiState.value.isExitEnabled)
        }

    @Test
    fun `back action emits navigate back event`() =
        runTest {
            val viewModel = NavigationViewModel()
            val eventDeferred = async { viewModel.uiEvent.first() }

            viewModel.onAction(NavigationUiAction.BackClicked)
            advanceUntilIdle()

            assertEquals(NavigationUiEvent.NavigateBack, eventDeferred.await())
        }

    @Test
    fun `exit action emits navigate to map event after navigation is ready`() =
        runTest {
            val viewModel = NavigationViewModel()
            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()

            val eventDeferred = async { viewModel.uiEvent.first() }

            viewModel.onAction(NavigationUiAction.ExitNavigationClicked)
            advanceUntilIdle()

            assertEquals(NavigationUiEvent.NavigateToMap, eventDeferred.await())
        }

    @Test
    fun `binding navigation request maps route handoff into step card summary`() =
        runTest {
            val viewModel = NavigationViewModel()

            viewModel.bindNavigationRequest(testNavigationRequest())
            advanceUntilIdle()

            assertEquals(NavigationScreenState.Ready, viewModel.uiState.value.screenState)
            assertEquals("SAFE 우선", viewModel.uiState.value.stepCard.statusLabel)
            assertEquals("350m", viewModel.uiState.value.stepCard.distanceLabel)
            assertEquals("350m 앞에서 좌회전 후 횡단보도를 건너세요", viewModel.uiState.value.stepCard.instruction)
            assertEquals("부산역 방향으로 Safe Route 경로를 따라 이동합니다.", viewModel.uiState.value.stepCard.supportingText)
            assertEquals("남은 거리", viewModel.uiState.value.stepCard.metrics[0].label)
            assertEquals("980m", viewModel.uiState.value.stepCard.metrics[0].value)
            assertEquals("예상 시간", viewModel.uiState.value.stepCard.metrics[1].label)
            assertEquals("16분", viewModel.uiState.value.stepCard.metrics[1].value)
            assertEquals("진행 단계", viewModel.uiState.value.stepCard.metrics[2].label)
            assertEquals("1 / 2", viewModel.uiState.value.stepCard.metrics[2].value)
            assertEquals("내비게이션 종료", viewModel.uiState.value.exitCta.label)
            assertEquals("안내를 종료하고 지도로 돌아갑니다.", viewModel.uiState.value.exitCta.supportingText)
        }

    @Test
    fun `binding summary only request exposes empty fallback guidance`() =
        runTest {
            val viewModel = NavigationViewModel()

            viewModel.bindNavigationRequest(summaryOnlyNavigationRequest())
            advanceUntilIdle()

            assertEquals(NavigationScreenState.Empty, viewModel.uiState.value.screenState)
            assertEquals("840m", viewModel.uiState.value.stepCard.distanceLabel)
            assertEquals("현재 안내 메시지를 준비하지 못했습니다", viewModel.uiState.value.stepCard.instruction)
            assertEquals("부산역 방향으로 거리와 예상 시간 요약만 먼저 표시합니다.", viewModel.uiState.value.stepCard.supportingText)
            assertEquals("남은 거리", viewModel.uiState.value.stepCard.metrics[0].label)
            assertEquals("840m", viewModel.uiState.value.stepCard.metrics[0].value)
            assertEquals("예상 시간", viewModel.uiState.value.stepCard.metrics[1].label)
            assertEquals("14분", viewModel.uiState.value.stepCard.metrics[1].value)
            assertEquals("진행 단계", viewModel.uiState.value.stepCard.metrics[2].label)
            assertEquals("안내 없음", viewModel.uiState.value.stepCard.metrics[2].value)
            assertEquals("내비게이션 종료", viewModel.uiState.value.exitCta.label)
            assertEquals("안내를 종료하고 지도로 돌아갑니다.", viewModel.uiState.value.exitCta.supportingText)
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
                name = "부산역",
                address = "부산 동구 중앙대로 206",
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
                segments =
                    listOf(
                        RouteSegment(
                            sequence = 1,
                            distanceMeters = 350,
                            guidanceMessage = "350m 앞에서 좌회전 후 횡단보도를 건너세요",
                        ),
                        RouteSegment(
                            sequence = 2,
                            distanceMeters = 630,
                            guidanceMessage = "목적지까지 직진하세요",
                        ),
                    ),
            ),
        source =
            RouteSearchSource.mockFixture(
                fixtureId = "navigation-test",
                label = "Navigation test fixture",
            ),
    )

private fun summaryOnlyNavigationRequest(): RouteNavigationRequest =
    RouteNavigationRequest(
        origin =
            RouteWaypoint(
                name = "현재 위치",
                coordinate = GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
            ),
        destination =
            RouteWaypoint(
                name = "부산역",
                address = "부산 동구 중앙대로 206",
                coordinate = GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
            ),
        selectedRoute =
            RouteCandidate(
                routeOption = RouteOption.SHORTEST,
                title = "Summary Only Route",
                summary =
                    RouteSummary(
                        distanceMeters = 840,
                        estimatedTimeMinutes = 14,
                        riskLevel = RouteRiskLevel.MEDIUM,
                    ),
                segments = emptyList(),
            ),
        source =
            RouteSearchSource.mockFixture(
                fixtureId = "navigation-summary-only",
                label = "Navigation summary-only fixture",
            ),
    )
