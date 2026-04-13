package com.ssafy.e102.eumgil.app.navigation

import androidx.annotation.StringRes
import com.ssafy.e102.eumgil.R

sealed class TopLevelDestination(
    val route: TopLevelRoute,
    @StringRes val labelRes: Int,
) {
    data object Map : TopLevelDestination(
        route = TopLevelRoute.Map,
        labelRes = R.string.route_map,
    )

    data object SavedRoute : TopLevelDestination(
        route = TopLevelRoute.SavedRoute,
        labelRes = R.string.route_saved_route,
    )

    data object MyPage : TopLevelDestination(
        route = TopLevelRoute.MyPage,
        labelRes = R.string.route_my_page,
    )

    companion object {
        val entries: List<TopLevelDestination> = listOf(
            Map,
            SavedRoute,
            MyPage,
        )
    }
}
