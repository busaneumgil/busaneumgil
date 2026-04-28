package com.ssafy.e102.eumgil.app.navigation

sealed interface AppRoute {
    val route: String
}

sealed interface AuthRoute : AppRoute {
    data object Login : AuthRoute {
        override val route: String = "auth/login"
    }

    data object ProfileSetup : AuthRoute {
        override val route: String = "auth/profile_setup"
    }
}

sealed interface OnboardingRoute : AppRoute {
    data object UserTypePrimary : OnboardingRoute {
        override val route: String = "onboarding/user_type_primary"
    }

    data object LowVisionFollowUp : OnboardingRoute {
        override val route: String = "onboarding/low_vision_followup"
    }

    data object MobilityTypeSecondary : OnboardingRoute {
        override val route: String = "onboarding/mobility_type_secondary"
    }

    data object Terms : OnboardingRoute {
        override val route: String = "onboarding/terms"
    }

    /**
     * High-contrast terms walkthrough screen (Figma node 328:486).
     * Used in the visual-impairment voice-guide flow.
     */
    data object TermsGuide : OnboardingRoute {
        override val route: String = "onboarding/terms_guide"
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

sealed interface NavigationRoute : AppRoute {
    data object Guidance : NavigationRoute {
        override val route: String = "navigation_guidance"
    }
}
