package com.ssafy.e102.eumgil.feature.map.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MapShortcutFilterRowConfigurationTest {
    @Test
    fun `map shortcut filter row uses dedicated elevator asset for top chip`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/map/component/MapShortcutFilterRow.kt")
                .readText()

        assertTrue(
            "Top shortcut elevator chip should use its dedicated drawable resource.",
            source.contains("MapShortcutFilterKey.ELEVATOR -> R.drawable.ic_map_shortcut_elevator"),
        )
        assertTrue(
            "Dedicated elevator shortcut icon resource should exist.",
            File("src/main/res/drawable/ic_map_shortcut_elevator.png").exists(),
        )
    }
}
