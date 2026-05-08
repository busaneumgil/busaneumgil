package com.ssafy.e102.eumgil.feature.lowvision

import com.ssafy.e102.eumgil.R
import org.junit.Assert.assertEquals
import org.junit.Test

class LowVisionPlaceCardDefaultsTest {
    @Test
    fun `place action icons use bookmark and route navigation assets`() {
        assertEquals(R.drawable.ic_nav_bookmark_outline, LowVisionPlaceCardDefaults.saveIconRes)
        assertEquals(R.drawable.ic_route_start_navigation, LowVisionPlaceCardDefaults.routeIconRes)
    }

    @Test
    fun `brief address keeps the most specific address segments`() {
        assertEquals(
            "중구 중앙대로 206",
            lowVisionBriefAddress("부산광역시 중구 중앙대로 206"),
        )
    }

    @Test
    fun `detail address includes full address and gps text`() {
        assertEquals(
            "상세 주소: 부산광역시 중구 중앙대로 206\nGPS 위치: 위도 35.11510, 경도 129.04150",
            lowVisionDetailAddress(
                address = "부산광역시 중구 중앙대로 206",
                latitude = 35.1151,
                longitude = 129.0415,
            ),
        )
    }

    @Test
    fun `detail address falls back to gps when address is missing`() {
        assertEquals(
            "상세 주소: GPS 기반 위치\nGPS 위치: 위도 35.11510, 경도 129.04150",
            lowVisionDetailAddress(
                address = null,
                latitude = 35.1151,
                longitude = 129.0415,
            ),
        )
    }
}
