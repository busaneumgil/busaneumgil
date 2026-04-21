package com.ssafy.e102.eumgil.data.mock.datasource

import com.ssafy.e102.eumgil.data.mock.fixture.MockRouteFixtures
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto

class RouteMockDataSource(
    private val fixtureProvider: (RouteSearchRequestDto) -> RouteSearchResponseDto = MockRouteFixtures::searchRoutes,
) {
    suspend fun searchRoutes(request: RouteSearchRequestDto): RouteSearchResponseDto = fixtureProvider(request)
}
