package com.ssafy.e102.eumgil.data.remote.datasource

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceFeatureAvailability
import com.ssafy.e102.eumgil.core.model.PlaceFeatureType
import com.ssafy.e102.eumgil.core.model.PlaceQuery
import com.ssafy.e102.eumgil.data.remote.HttpJsonResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacesRemoteDataSourceTest {
    @Test
    fun `getPlaces maps browse response and sends expected query filters`() =
        runBlocking {
            var capturedPath: String? = null
            var capturedQueryParams: Map<String, String> = emptyMap()
            var capturedHeaders: Map<String, String> = emptyMap()
            val dataSource =
                PlacesRemoteDataSource(
                    requestExecutor = { path, queryParams, headers ->
                        capturedPath = path
                        capturedQueryParams = queryParams
                        capturedHeaders = headers
                        HttpJsonResponse(
                            statusCode = 200,
                            body =
                                """
                                {
                                  "status": "S2000",
                                  "data": {
                                    "places": [
                                      {
                                        "placeId": 101,
                                        "name": "Accessible Cafe",
                                        "category": "FOOD_CAFE",
                                        "address": "1 Jungang-daero, Busan",
                                        "point": {
                                          "lat": 35.1796,
                                          "lng": 129.0756
                                        },
                                        "accessibilityFeatures": [
                                          {
                                            "featureType": "accessibleToilet",
                                            "isAvailable": true
                                          },
                                          {
                                            "featureType": "accessibleParking",
                                            "isAvailable": false
                                          }
                                        ],
                                        "isBookmarked": true
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

            val places =
                dataSource.getPlaces(
                    PlaceQuery(
                        latitude = 35.1796,
                        longitude = 129.0756,
                        radiusMeters = 1500,
                        categories = setOf(PlaceCategory.FOOD_CAFE),
                        featureTypes =
                            setOf(
                                PlaceFeatureType.ACCESSIBLE_TOILET,
                                PlaceFeatureType.ELEVATOR,
                            ),
                    ),
                )

            assertEquals("/places", capturedPath)
            assertEquals("35.1796", capturedQueryParams["lat"])
            assertEquals("129.0756", capturedQueryParams["lng"])
            assertEquals("1500", capturedQueryParams["radius"])
            assertEquals("FOOD_CAFE", capturedQueryParams["category"])
            assertEquals("accessibleToilet,elevator", capturedQueryParams["featureType"])
            assertEquals("Bearer access-token", capturedHeaders["Authorization"])

            assertEquals(1, places.size)
            assertEquals("101", places.first().placeId)
            assertEquals(PlaceCategory.FOOD_CAFE, places.first().category)
            assertTrue(
                places.first().features.any { feature ->
                    feature.featureType == PlaceFeatureType.ACCESSIBLE_TOILET && feature.isAvailable
                },
            )
            assertTrue(places.first().isBookmarked)
        }

    @Test
    fun `getPlaceDetail maps detail response and sends expected auth header`() =
        runBlocking {
            var capturedPath: String? = null
            var capturedQueryParams: Map<String, String> = emptyMap()
            var capturedHeaders: Map<String, String> = emptyMap()
            val dataSource =
                PlacesRemoteDataSource(
                    requestExecutor = { path, queryParams, headers ->
                        capturedPath = path
                        capturedQueryParams = queryParams
                        capturedHeaders = headers
                        HttpJsonResponse(
                            statusCode = 200,
                            body =
                                """
                                {
                                  "status": "S2000",
                                  "data": {
                                    "placeId": 101,
                                    "name": "Accessible Cafe",
                                    "category": "FOOD_CAFE",
                                    "address": "1 Jungang-daero, Busan",
                                    "point": {
                                      "lat": 35.1796,
                                      "lng": 129.0756
                                    },
                                    "providerPlaceId": "kakao-101",
                                    "accessibilityFeatures": [
                                      {
                                        "featureType": "accessibleEntrance",
                                        "isAvailable": true
                                      },
                                      {
                                        "featureType": "accessibleToilet",
                                        "isAvailable": true
                                      }
                                    ],
                                    "isBookmarked": true
                                  },
                                  "message": "ok"
                                }
                                """.trimIndent(),
                        )
                    },
                    accessTokenProvider = { "access-token" },
                )

            val detail = dataSource.getPlaceDetail("101")

            assertEquals("/places/101", capturedPath)
            assertTrue(capturedQueryParams.isEmpty())
            assertEquals("Bearer access-token", capturedHeaders["Authorization"])
            assertEquals("101", detail?.placeId)
            assertEquals(PlaceCategory.FOOD_CAFE, detail?.category)
            assertEquals("kakao-101", detail?.providerPlaceId)
            assertEquals(
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
                detail?.features,
            )
            assertEquals(listOf("step-free-entrance", "accessible-toilet"), detail?.accessibilityTags)
            assertTrue(detail?.isBookmarked == true)
            assertNull(detail?.description)
        }
}
