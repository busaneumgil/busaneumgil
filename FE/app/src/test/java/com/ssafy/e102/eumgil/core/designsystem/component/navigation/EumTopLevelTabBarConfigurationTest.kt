package com.ssafy.e102.eumgil.core.designsystem.component.navigation

import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.navigation.TopLevelDestination
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import org.junit.Assert.assertEquals
import org.junit.Test

class EumTopLevelTabBarConfigurationTest {
    @Test
    fun `top level tab bar uses compact layout spec`() {
        val spec = topLevelTabBarLayoutSpec()

        assertEquals(12, spec.containerHorizontalPaddingDp)
        assertEquals(8, spec.itemVerticalPaddingDp)
        assertEquals(2, spec.itemSpacingDp)
    }

    @Test
    fun `top level tab bar keeps same icon resource for selected state`() {
        assertEquals(
            R.drawable.ic_nav_home,
            topLevelTabIconRes(destination = TopLevelDestination.Map, selected = true),
        )
        assertEquals(
            R.drawable.ic_nav_home,
            topLevelTabIconRes(destination = TopLevelDestination.Map, selected = false),
        )
        assertEquals(
            R.drawable.ic_nav_bookmark_outline,
            topLevelTabIconRes(destination = TopLevelDestination.SavedRoute, selected = true),
        )
    }

    @Test
    fun `selected top level tab uses bright blue tint`() {
        assertEquals(EumPrimary600, topLevelTabContentColor(selected = true))
    }
}
