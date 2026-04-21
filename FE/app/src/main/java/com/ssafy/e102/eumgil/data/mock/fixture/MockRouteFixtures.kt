package com.ssafy.e102.eumgil.data.mock.fixture

import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import com.ssafy.e102.eumgil.data.route.RouteSearchResponseDto

object MockRouteFixtures {
    val defaultFixture: RouteFixtureTemplate
        get() = MockRouteFixtureCatalog.defaultFixture

    fun searchRoutes(request: RouteSearchRequestDto): RouteSearchResponseDto =
        defaultFixture.resolve(request)
}
