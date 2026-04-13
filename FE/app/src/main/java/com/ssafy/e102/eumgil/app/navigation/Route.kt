package com.ssafy.e102.eumgil.app.navigation

sealed interface AppRoute {
    val route: String
}

sealed interface TopLevelRoute : AppRoute {
    data object Map : TopLevelRoute {
        override val route: String = "map"
    }

    data object SavedRoute : TopLevelRoute {
        override val route: String = "saved_route"
    }

    data object MyPage : TopLevelRoute {
        override val route: String = "my_page"
    }
}
