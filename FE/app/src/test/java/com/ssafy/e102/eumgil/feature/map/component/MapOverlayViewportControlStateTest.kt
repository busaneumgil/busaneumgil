package com.ssafy.e102.eumgil.feature.map.component

import com.ssafy.e102.eumgil.feature.map.model.MapCameraSource
import com.ssafy.e102.eumgil.feature.map.model.MapCameraTarget
import com.ssafy.e102.eumgil.feature.map.model.MapCoordinate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapOverlayViewportControlStateTest {
    @Test
    fun `programmatic camera callback aligned with requested target does not count as user gesture`() {
        val requestedTarget =
            MapCameraTarget(
                center = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                source = MapCameraSource.CURRENT_LOCATION,
                requestId = 42L,
                zoomLevel = 17,
            )

        assertFalse(
            shouldTreatViewportCameraMoveAsUserGesture(
                requestedTarget = requestedTarget,
                center = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                zoomLevel = 17,
                reportedUserGesture = true,
            ),
        )
    }

    @Test
    fun `camera callback away from requested target still counts as user gesture`() {
        val requestedTarget =
            MapCameraTarget(
                center = MapCoordinate(latitude = 35.1796, longitude = 129.0756),
                source = MapCameraSource.CURRENT_LOCATION,
                requestId = 42L,
                zoomLevel = 17,
            )

        assertTrue(
            shouldTreatViewportCameraMoveAsUserGesture(
                requestedTarget = requestedTarget,
                center = MapCoordinate(latitude = 35.1816, longitude = 129.0796),
                zoomLevel = 17,
                reportedUserGesture = true,
            ),
        )
    }
}
