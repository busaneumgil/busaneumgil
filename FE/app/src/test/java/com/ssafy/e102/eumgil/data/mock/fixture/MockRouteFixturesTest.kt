package com.ssafy.e102.eumgil.data.mock.fixture

import com.ssafy.e102.eumgil.data.route.DefaultRouteGeometryParser
import com.ssafy.e102.eumgil.data.route.RouteGeometryParseStatus
import com.ssafy.e102.eumgil.data.route.RoutePointDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockRouteFixturesTest {
    private val parser = DefaultRouteGeometryParser()

    @Test
    fun `default fixture resolves requested routes into parsable linestring geometry`() {
        val response = MockRouteFixtures.searchRoutes(testRequest())

        assertEquals(listOf("SAFE", "SHORTEST"), response.routes.mapNotNull { route -> route.routeOption })
        assertTrue(response.routes.isNotEmpty())
        assertTrue(response.routes.all { route -> route.segments.isNotEmpty() })
        response.routes
            .flatMap { route -> route.segments }
            .forEach { segment ->
                val geometry = requireNotNull(segment.geometry)

                assertTrue(geometry.startsWith("LINESTRING("))
                assertEquals(RouteGeometryParseStatus.SUCCESS, parser.parse(geometry).status)
            }
    }

    @Test
    fun `default fixture filters response by requested route option`() {
        val response =
            MockRouteFixtures.searchRoutes(
                testRequest(routeOptions = listOf("SAFE")),
            )

        assertEquals(1, response.routes.size)
        assertEquals("SAFE", response.routes.single().routeOption)
    }

    @Test
    fun `resolveSearchRequest exposes fixture metadata together with response payload`() {
        val payload = MockRouteFixtures.resolveSearchRequest(testRequest())

        assertEquals("busan-cityhall-to-station-demo", payload.fixtureId)
        assertEquals("Busan City Hall to Busan Station demo route", payload.fixtureName)
        assertEquals(payload.response, MockRouteFixtures.searchRoutes(payload.request))
    }

    @Test
    fun `default fixture catalog keeps at least one canned route option for downstream parser consumers`() {
        val fixture = MockRouteFixtures.defaultFixture

        assertEquals("busan-cityhall-to-station-demo", fixture.fixtureId)
        assertTrue(fixture.routes.isNotEmpty())
        assertTrue(fixture.routes.any { route -> route.routeOption == "SAFE" })
    }
}

private fun testRequest(routeOptions: List<String> = listOf("SAFE", "SHORTEST")): RouteSearchRequestDto =
    RouteSearchRequestDto(
        startPoint = RoutePointDto(lat = 35.1796, lng = 129.0756),
        endPoint = RoutePointDto(lat = 35.1151, lng = 129.0414),
        routeOptions = routeOptions,
    )
