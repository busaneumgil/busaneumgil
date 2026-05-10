package com.ssafy.e102.eumgil.feature.map.component

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KakaoMapViewportConfigurationTest {
    @Test
    fun `renderer fallback uses dedicated overlay instead of mock map surface`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertTrue(
            "Kakao map renderer fallback should use a dedicated centered overlay instead of reusing the mock map surface.",
            source.contains("MapRendererFallbackOverlay("),
        )
        assertFalse(
            "Kakao map renderer fallback should not reuse the mock/fake map surface UI.",
            source.contains("MapFallbackSurface("),
        )
    }

    @Test
    fun `renderer fallback exposes explicit retry call to action`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertTrue(
            "Kakao map renderer fallback should show a dedicated retry CTA for reloading the map renderer.",
            source.contains("map_viewport_retry"),
        )
        assertTrue(
            "Kakao map renderer fallback retry CTA should trigger a local renderer reload.",
            source.contains("reloadGeneration += 1"),
        )
        assertTrue(
            "Kakao map renderer retry should tear down the current renderer session before requesting a new one.",
            source.contains("controller.finish()"),
        )
        assertTrue(
            "Kakao map renderer retry should keep the map subtree unmounted briefly so restart behaves closer to a real pause/resume cycle.",
            source.contains("delay(KAKAO_RENDERER_RESTART_DELAY_MILLIS)"),
        )
    }

    @Test
    fun `renderer startup timeout returns endless loading back to error state`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertTrue(
            "Kakao map renderer should convert endless initializing state back to an error state after a startup timeout.",
            source.contains("markRendererTimedOut()"),
        )
        assertTrue(
            "Kakao map renderer timeout should wait a bounded amount of time before surfacing the error state.",
            source.contains("delay(KAKAO_RENDERER_READY_TIMEOUT_MILLIS)"),
        )
    }

    @Test
    fun `special map markers keep a compose overlay backup anchored by screen point`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertTrue(
            "Special markers should have a Compose overlay path so selected pins, current location, and bookmarked destinations stay visible even when the Kakao label layer is unreliable.",
            source.contains("MapProjectedMarkerOverlay("),
        )
        assertTrue(
            "Projected marker overlays should be anchored from the map screen-point projection.",
            source.contains("projectedMarkerOverlays"),
        )
        assertTrue(
            "Projected marker overlays should include the selected destination marker so bookmarked places are not camera-only state.",
            source.contains("selectedDestinationCoordinate"),
        )
    }

    @Test
    fun `blank map taps bind to terrain click listener so selected pin can be dropped`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertTrue(
            "Blank-area taps should use Kakao's terrain click callback so the dropped pin action is triggered on empty map space.",
            source.contains("setOnTerrainClickListener"),
        )
        assertTrue(
            "Blank-area taps should also listen to the generic map click callback so non-terrain surfaces can still drop a pin.",
            source.contains("setOnMapClickListener"),
        )
    }

    @Test
    fun `kakao marker styles apply dp scale for visible map pin rendering`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertTrue(
            "Kakao marker styles should opt into dp scaling so vector pin assets render at an intended on-screen size.",
            source.contains("setApplyDpScale(true)"),
        )
        assertTrue(
            "Custom map markers should not compete with base map labels, otherwise the dropped pin can be hidden even after it is rendered.",
            source.contains("setCompetitionType(CompetitionType.None)"),
        )
        assertTrue(
            "Marker ordering should follow rank so the dropped pin can stay above lower-priority markers in the same layer.",
            source.contains("setOrderingType(OrderingType.Rank)"),
        )
    }
}
