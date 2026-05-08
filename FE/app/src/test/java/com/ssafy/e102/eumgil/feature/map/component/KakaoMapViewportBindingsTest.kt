package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.FacilityCategory
import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerCategoryType
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerDisplayState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerOverlayState
import com.ssafy.e102.eumgil.feature.map.model.MapMarkerUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoMapViewportBindingsTest {
    @Test
    fun `integration state binds only when kakao key is configured outside inspection`() {
        assertEquals(
            MapIntegrationState.Unbound,
            resolveMapIntegrationState(
                hasNativeAppKey = false,
                isInspectionMode = false,
            ),
        )
        assertEquals(
            MapIntegrationState.Unbound,
            resolveMapIntegrationState(
                hasNativeAppKey = true,
                isInspectionMode = true,
            ),
        )
        assertEquals(
            MapIntegrationState.Bound(providerName = KAKAO_MAP_PROVIDER_NAME),
            resolveMapIntegrationState(
                hasNativeAppKey = true,
                isInspectionMode = false,
            ),
        )
    }

    @Test
    fun `camera render state keeps coordinates and uses source aligned zoom level`() {
        val currentLocationCamera =
            createKakaoCameraRenderState(
                MapCameraTarget(
                    center = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                    source = MapCameraSource.CURRENT_LOCATION,
                    requestId = 7L,
                ),
            )
        val destinationCamera =
            createKakaoCameraRenderState(
                MapCameraTarget(
                    center = MapCoordinate(latitude = 35.1544, longitude = 129.0596),
                    source = MapCameraSource.SEARCH_RESULT,
                    requestId = 8L,
                ),
            )
        val defaultCamera =
            createKakaoCameraRenderState(
                MapCameraTarget(
                    center = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                    source = MapCameraSource.DEFAULT_BUSAN,
                    requestId = 9L,
                ),
            )

        assertEquals(35.1796, currentLocationCamera.latitude, 0.0)
        assertEquals(129.0756, currentLocationCamera.longitude, 0.0)
        assertEquals(17, currentLocationCamera.zoomLevel)
        assertEquals(7L, currentLocationCamera.requestId)

        assertEquals(16, destinationCamera.zoomLevel)
        assertEquals(8L, destinationCamera.requestId)

        assertEquals(15, defaultCamera.zoomLevel)
        assertEquals(9L, defaultCamera.requestId)
    }

    @Test
    fun `camera debug summary keeps request source and formatted coordinate details`() {
        val summary =
            createKakaoCameraDebugSummary(
                MapCameraTarget(
                    center = MapCoordinate(latitude = 35.1796123, longitude = 129.0756412),
                    source = MapCameraSource.SEARCH_RESULT,
                    requestId = 11L,
                ),
            )

        assertEquals(
            "requestId=11 source=SEARCH_RESULT lat=35.179612 lng=129.075641 zoom=16",
            summary,
        )
    }

    @Test
    fun `lifecycle command resumes immediately after start when lifecycle is resumed`() {
        assertEquals(
            KakaoMapLifecycleCommand.NONE,
            resolveKakaoMapLifecycleCommand(
                isLifecycleResumed = true,
                hasMapView = true,
                isAttachedToWindow = true,
                isStarted = true,
                isFinished = false,
                hasResumedLifecycle = true,
            ),
        )
        assertEquals(
            KakaoMapLifecycleCommand.RESUME,
            resolveKakaoMapLifecycleCommand(
                isLifecycleResumed = true,
                hasMapView = true,
                isAttachedToWindow = true,
                isStarted = true,
                isFinished = false,
                hasResumedLifecycle = false,
            ),
        )
        assertEquals(
            KakaoMapLifecycleCommand.PAUSE,
            resolveKakaoMapLifecycleCommand(
                isLifecycleResumed = false,
                hasMapView = true,
                isAttachedToWindow = true,
                isStarted = true,
                isFinished = false,
                hasResumedLifecycle = true,
            ),
        )
        assertEquals(
            KakaoMapLifecycleCommand.NONE,
            resolveKakaoMapLifecycleCommand(
                isLifecycleResumed = false,
                hasMapView = true,
                isAttachedToWindow = true,
                isStarted = true,
                isFinished = false,
                hasResumedLifecycle = false,
            ),
        )
    }

    @Test
    fun `marker render state keeps visible markers and lifts selected marker rank`() {
        val hiddenMarker =
            MapMarkerUiModel(
                markerId = "hidden",
                name = "Hidden marker",
                coordinate = MapCoordinate(latitude = 35.1, longitude = 129.1),
                categoryType = MapMarkerCategoryType(category = FacilityCategory.OTHER),
                displayState = MapMarkerDisplayState.HIDDEN_BY_FILTER,
            )
        val visibleToilet =
            MapMarkerUiModel(
                markerId = "toilet",
                name = "Accessible toilet",
                coordinate = MapCoordinate(latitude = 35.2, longitude = 129.2),
                categoryType = MapMarkerCategoryType(category = FacilityCategory.TOILET),
            )
        val visibleElevator =
            MapMarkerUiModel(
                markerId = "elevator",
                name = "Elevator",
                coordinate = MapCoordinate(latitude = 35.3, longitude = 129.3),
                categoryType = MapMarkerCategoryType(category = FacilityCategory.ELEVATOR),
            )

        val markerStates =
            createKakaoMarkerRenderStates(
                markerOverlayState =
                    MapMarkerOverlayState(
                        loadStatus = com.ssafy.e102.eumgil.feature.map.model.MapMarkerLoadStatus.READY,
                        markers = listOf(hiddenMarker, visibleToilet, visibleElevator),
                        visibleMarkerCount = 2,
                        totalMarkerCount = 3,
                    ),
                selectedMarkerId = "elevator",
            )

        assertEquals(listOf("toilet", "elevator"), markerStates.map { it.markerId })
        assertEquals(R.drawable.ic_place_restroom, markerStates.first().iconResId)
        assertEquals(R.drawable.ic_place_elevator, markerStates.last().iconResId)
        assertEquals(0L, markerStates.first().rank)
        assertEquals(1L, markerStates.last().rank)
        assertTrue(markerStates.none { it.markerId == "hidden" })
    }

    @Test
    fun `renderer failure keeps exception type and sanitized message for fallback`() {
        val failure =
            createKakaoRendererFailure(
                IllegalStateException("native engine unavailable"),
            )

        assertEquals("IllegalStateException", failure.reasonLabel)
        assertEquals("native engine unavailable", failure.detailMessage)
        assertEquals(
            "IllegalStateException: native engine unavailable",
            failure.debugSummary,
        )
    }

    @Test
    fun `renderer failure falls back when sdk error message is blank`() {
        val failure =
            createKakaoRendererFailure(
                IllegalArgumentException("   "),
            )

        assertEquals("IllegalArgumentException", failure.reasonLabel)
        assertEquals(KAKAO_RENDERER_ERROR_DETAIL_FALLBACK, failure.detailMessage)
        assertEquals(
            "IllegalArgumentException: $KAKAO_RENDERER_ERROR_DETAIL_FALLBACK",
            failure.debugSummary,
        )
    }

    @Test
    fun `marker debug summary keeps overlay counts and current selection`() {
        val markerStates =
            createKakaoMarkerRenderStates(
                markerOverlayState =
                    MapMarkerOverlayState(
                        loadStatus = com.ssafy.e102.eumgil.feature.map.model.MapMarkerLoadStatus.READY,
                        markers =
                            listOf(
                                MapMarkerUiModel(
                                    markerId = "toilet",
                                    name = "Accessible toilet",
                                    coordinate = MapCoordinate(latitude = 35.2, longitude = 129.2),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.TOILET),
                                ),
                                MapMarkerUiModel(
                                    markerId = "elevator",
                                    name = "Elevator",
                                    coordinate = MapCoordinate(latitude = 35.3, longitude = 129.3),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.ELEVATOR),
                                    displayState = MapMarkerDisplayState.HIDDEN_BY_FILTER,
                                ),
                            ),
                        visibleMarkerCount = 1,
                        totalMarkerCount = 2,
                    ),
                selectedMarkerId = "toilet",
            )

        val summary =
            createKakaoMarkerDebugSummary(
                markerOverlayState =
                    MapMarkerOverlayState(
                        loadStatus = com.ssafy.e102.eumgil.feature.map.model.MapMarkerLoadStatus.READY,
                        markers =
                            listOf(
                                MapMarkerUiModel(
                                    markerId = "toilet",
                                    name = "Accessible toilet",
                                    coordinate = MapCoordinate(latitude = 35.2, longitude = 129.2),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.TOILET),
                                ),
                                MapMarkerUiModel(
                                    markerId = "elevator",
                                    name = "Elevator",
                                    coordinate = MapCoordinate(latitude = 35.3, longitude = 129.3),
                                    categoryType = MapMarkerCategoryType(category = FacilityCategory.ELEVATOR),
                                    displayState = MapMarkerDisplayState.HIDDEN_BY_FILTER,
                                ),
                            ),
                        visibleMarkerCount = 1,
                        totalMarkerCount = 2,
                    ),
                renderedMarkers = markerStates,
                selectedMarkerId = "toilet",
            )

        assertEquals("total=2 visible=1 rendered=1 selected=toilet", summary)
    }
}
