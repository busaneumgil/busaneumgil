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
    fun `selected map pin renders inside kakao marker layer instead of compose overlay`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/KakaoMapViewport.kt")
                .readText()

        assertFalse(
            "Selected map pin should not be rendered as a separate Compose overlay on top of the map.",
            source.contains("MapSelectedPinOverlay("),
        )
        assertFalse(
            "Selected map pin should not depend on screen-point sync state once it is rendered inside the Kakao marker layer.",
            source.contains("selectedMapPinScreenPoint"),
        )
    }
}
