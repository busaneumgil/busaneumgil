package com.ssafy.e102.eumgil.app.navigation

sealed interface AppRoute {
    val route: String
}

sealed interface OnboardingRoute : AppRoute {
    data object DisabilityType : OnboardingRoute {
        override val route: String = "onboarding/type"
    }

    data object DisabilityLevel : OnboardingRoute {
        const val ARG_DISABILITY_TYPE: String = "disabilityType"

        override val route: String = "onboarding/level/{$ARG_DISABILITY_TYPE}"

        fun createRoute(disabilityType: String): String = "onboarding/level/$disabilityType"
    }

    data object LocationTerms : OnboardingRoute {
        const val ARG_DISABILITY_TYPE: String = "disabilityType"
        const val ARG_DISABILITY_LEVEL: String = "disabilityLevel"

        override val route: String =
            "onboarding/location_terms/{$ARG_DISABILITY_TYPE}/{$ARG_DISABILITY_LEVEL}"

        fun createRoute(
            disabilityType: String,
            disabilityLevel: String,
        ): String = "onboarding/location_terms/$disabilityType/$disabilityLevel"
    }
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

sealed interface SearchRoute : AppRoute {
    data object Search : SearchRoute {
        override val route: String = "search"
    }
}

sealed interface RouteSettingRoute : AppRoute {
    data object Setting : RouteSettingRoute {
        override val route: String = "route_setting"
    }
}

sealed interface ReportRoute : AppRoute {
    data object Report : ReportRoute {
        override val route: String = "report"
    }
}

sealed interface RouteSettingRoute : AppRoute {
    data object Setting : RouteSettingRoute {
        override val route: String = "route_setting"
    }
}
