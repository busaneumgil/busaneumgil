package com.ssafy.e102.eumgil.feature.navigation

import com.ssafy.e102.eumgil.core.model.RouteSegment
import com.ssafy.e102.eumgil.core.model.RouteSegmentSafetyFlags
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationGuidanceActionTest {
    @Test
    fun `guidance action reuses route detail step classification`() {
        assertEquals(
            NavigationGuidanceAction.CROSSWALK,
            RouteSegment(
                sequence = 1,
                guidanceMessage = "신호를 확인한 뒤 횡단보도를 건너세요.",
                safetyFlags = RouteSegmentSafetyFlags(hasCrosswalk = true),
            ).toNavigationGuidanceAction(),
        )
        assertEquals(
            NavigationGuidanceAction.TURN_LEFT,
            RouteSegment(
                sequence = 2,
                guidanceMessage = "120m 앞에서 좌회전하세요.",
            ).toNavigationGuidanceAction(),
        )
        assertEquals(
            NavigationGuidanceAction.TURN_RIGHT,
            RouteSegment(
                sequence = 3,
                guidanceMessage = "350m 앞에서 오른쪽 방향으로 이동하세요.",
            ).toNavigationGuidanceAction(),
        )
        assertEquals(
            NavigationGuidanceAction.STRAIGHT,
            RouteSegment(
                sequence = 4,
                guidanceMessage = "엘리베이터를 타고 이동하세요.",
            ).toNavigationGuidanceAction(),
        )
        assertEquals(
            NavigationGuidanceAction.STRAIGHT,
            RouteSegment(
                sequence = 5,
                guidanceMessage = "목적지까지 계속 이동하세요.",
            ).toNavigationGuidanceAction(),
        )
    }

    @Test
    fun `hero detail mirrors route detail style copy for english turn guidance`() {
        val detail =
            RouteSegment(
                sequence = 1,
                distanceMeters = 120,
                guidanceMessage = "turn right after the crosswalk",
            ).toNavigationHeroDetail()

        assertEquals("우회전", detail.title)
        assertEquals("120 m 정도 이동 후 오른쪽 방향으로 이동하세요.", detail.description)
        assertEquals(NavigationGuidanceAction.TURN_RIGHT, detail.guidanceAction)
    }

    @Test
    fun `hero detail keeps generated crosswalk copy instead of raw segment text`() {
        val detail =
            RouteSegment(
                sequence = 2,
                distanceMeters = 80,
                guidanceMessage = "횡단보도를 건너 엘리베이터 방향으로 이동하세요.",
                safetyFlags = RouteSegmentSafetyFlags(hasCrosswalk = true),
            ).toNavigationHeroDetail()

        assertEquals("횡단보도 건너기", detail.title)
        assertEquals("주변 차량이 멈췄는지 확인한 뒤 횡단보도를 조심해서 건너세요.", detail.description)
        assertEquals(NavigationGuidanceAction.CROSSWALK, detail.guidanceAction)
    }
}
