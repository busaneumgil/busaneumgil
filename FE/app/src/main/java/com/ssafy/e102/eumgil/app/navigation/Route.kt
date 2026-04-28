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
     * High-contrast 5-step terms walkthrough screen
     * (Figma file MREqSzkmwhRcXnFS3lzW17, nodes 328:486 / 328:528 / 328:570 /
     * 328:612 / 328:652 — 약관 동의 / 민감정보 / 위치정보 / 14세 이상 / 처리방침).
     *
     * `step` 인자는 deep-link로 특정 단계에서 다시 시작하는 분기를 허용한다
     * ("각 화면이 분기 시작점"). 기본값은 첫 단계(agree)이며 step 라우트 값은
     * [com.ssafy.e102.eumgil.feature.terms.TermsGuideStep.routeValue]에서 정의한다.
     */
    data object TermsGuide : OnboardingRoute {
        const val ARG_STEP: String = "step"
        const val DEFAULT_STEP: String = "agree"

        override val route: String = "onboarding/terms_guide/{$ARG_STEP}"

        fun createRoute(stepRouteValue: String = DEFAULT_STEP): String =
            "onboarding/terms_guide/$stepRouteValue"
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
