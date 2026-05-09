package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import com.ssafy.e102.eumgil.data.route.RoutePointDto
import com.ssafy.e102.eumgil.data.route.RouteSearchRequestDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RouteRemoteDataSourceTest {
    @Test
    fun `searchWalkRoutes posts walk request with bearer token and parses data envelope`() =
        runBlocking {
            var capturedPath: String? = null
            var capturedBody: String? = null
            var capturedHeaders: Map<String, String> = emptyMap()
            val dataSource =
                RouteRemoteDataSource(
                    postRequestExecutor = { path, body, headers ->
                        capturedPath = path
                        capturedBody = body
                        capturedHeaders = headers
                        HttpJsonResponse(
                            statusCode = 200,
                            body =
                                """
                                {
                                  "status": "S2000",
                                  "data": {
                                    "searchId": "rs_walk_server_001",
                                    "routes": [
                                      {
                                        "routeId": "walk_rt_safe_001",
                                        "transportMode": "WALK",
                                        "routeOption": "SAFE",
                                        "title": "Accessible Walk",
                                        "distanceMeter": 120.0,
                                        "durationSecond": 150,
                                        "estimatedTimeMinute": 2,
                                        "badges": ["LOW_SLOPE"],
                                        "geometry": "LINESTRING(129.075600 35.179600, 129.076800 35.180600)",
                                        "legs": [
                                          {
                                            "sequence": 1,
                                            "type": "WALK",
                                            "role": "WALK_ONLY",
                                            "instruction": "Walk to destination",
                                            "distanceMeter": 120.0,
                                            "durationSecond": 150,
                                            "estimatedTimeMinute": 2,
                                            "geometry": "LINESTRING(129.075600 35.179600, 129.076800 35.180600)",
                                            "steps": [
                                              {
                                                "sequence": 1,
                                                "instruction": "Go straight",
                                                "distanceMeter": 80.0,
                                                "durationSecond": 90,
                                                "geometry": "LINESTRING(129.075600 35.179600, 129.076000 35.179900)",
                                                "alert": {
                                                  "type": "CROSSWALK",
                                                  "distanceMeter": 15.0
                                                }
                                              }
                                            ]
                                          }
                                        ]
                                      }
                                    ]
                                  },
                                  "message": "ok"
                                }
                                """.trimIndent(),
                        )
                    },
                    accessTokenProvider = { "access-token" },
                )

            val response =
                dataSource.searchWalkRoutes(
                    RouteSearchRequestDto(
                        startPoint = RoutePointDto(lat = 35.1796, lng = 129.0756),
                        endPoint = RoutePointDto(lat = 35.1151, lng = 129.0414),
                        routeOptions = listOf("SAFE", "SHORTEST"),
                    ),
                )

            assertEquals("/routes/search/walk", capturedPath)
            assertEquals("Bearer access-token", capturedHeaders["Authorization"])
            assertTrue(capturedBody.orEmpty().contains("\"lat\":35.1796"))
            assertTrue(capturedBody.orEmpty().contains("\"lng\":129.0414"))
            assertTrue(capturedBody.orEmpty().contains("\"routeOptions\":[\"SAFE\",\"SHORTEST\"]"))
            assertEquals("rs_walk_server_001", response.searchId)
            assertEquals(1, response.routes.size)
            assertEquals("walk_rt_safe_001", response.routes.single().routeId)
            assertEquals(150, response.routes.single().durationSecond)
            assertEquals(150, response.routes.single().legs.single().durationSecond)
            assertEquals(90, response.routes.single().legs.single().steps.single().durationSecond)
            assertEquals("CROSSWALK", response.routes.single().legs.single().steps.single().alert?.type)
        }

    @Test
    fun `searchWalkRoutes surfaces route api errors without fallback`() =
        runBlocking {
            val dataSource =
                RouteRemoteDataSource(
                    postRequestExecutor = { _, _, _ ->
                        HttpJsonResponse(
                            statusCode = 404,
                            body =
                                """
                                {
                                  "status": "RT4040",
                                  "data": null,
                                  "message": "탐색 가능한 경로가 없습니다."
                                }
                                """.trimIndent(),
                        )
                    },
                )

            try {
                dataSource.searchWalkRoutes(
                    RouteSearchRequestDto(
                        startPoint = RoutePointDto(lat = 35.1796, lng = 129.0756),
                        endPoint = RoutePointDto(lat = 35.1151, lng = 129.0414),
                        routeOptions = listOf("SAFE"),
                    ),
                )
                fail("RouteApiException was expected")
            } catch (error: RouteApiException) {
                assertEquals(404, error.httpStatusCode)
                assertEquals("RT4040", error.status)
                assertEquals("탐색 가능한 경로가 없습니다.", error.message)
            }
        }
}
