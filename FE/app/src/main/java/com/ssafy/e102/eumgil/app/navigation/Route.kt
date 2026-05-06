package com.ssafy.e102.eumgil.app.navigation

import android.net.Uri
import com.ssafy.e102.eumgil.core.model.RouteOption
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

    data object ProfileUserTypePrimary : OnboardingRoute {
        override val route: String = "onboarding/profile_user_type_primary"
    }

    data object LowVisionFollowUp : OnboardingRoute {
        override val route: String = "onboarding/low_vision_followup"
    }

    data object MobilityTypeSecondary : OnboardingRoute {
        override val route: String = "onboarding/mobility_type_secondary"
    }

    data object ProfileMobilityTypeSecondary : OnboardingRoute {
        override val route: String = "onboarding/profile_mobility_type_secondary"
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

/**
 * 시각지원(저시력/시각장애) 모드 풀스크린 셸 화면들의 라우트.
 *
 * 약관 walkthrough([OnboardingRoute.TermsGuide]) 이후 진입하는 음성 안내 메인 흐름.
 * 출처: Figma file MREqSzkmwhRcXnFS3lzW17, nodes 371:105 / 371:300.
 */
sealed interface LowVisionRoute : AppRoute {
    /** 시각지원 모드 메인 홈 (Figma node 371:105). */
    data object Home : LowVisionRoute {
        override val route: String = "low_vision/home"
    }

    /** 시각지원 모드 음성 입력 진행 화면 (Figma node 371:300). */
    data object VoiceInput : LowVisionRoute {
        override val route: String = "low_vision/voice_input"
    }

    data object Bookmark : LowVisionRoute {
        override val route: String = "low_vision/bookmark"
    }

    data object Search : LowVisionRoute {
        override val route: String = "low_vision/search"
    }

    data object CategorySearch : LowVisionRoute {
        override val route: String = "low_vision/category_search"
    }

    data object CategoryResult : LowVisionRoute {
        const val ARG_CATEGORY: String = "category"

        override val route: String = "low_vision/category_result/{$ARG_CATEGORY}"

        fun createRoute(category: String): String = "low_vision/category_result/${category.navArgEncode()}"
    }

    data object RouteBriefing : LowVisionRoute {
        override val route: String = "low_vision/route_briefing"
    }

    data object Guidance : LowVisionRoute {
        override val route: String = "low_vision/guidance"
    }

    data object NavigationComplete : LowVisionRoute {
        override val route: String = "low_vision/navigation_complete"
    }

    data object MyPage : LowVisionRoute {
        override val route: String = "low_vision/my_page"
    }

    data object AppInfo : LowVisionRoute {
        override val route: String = "low_vision/app_info"
    }
}

sealed interface SearchRoute : AppRoute {
    data object Entry : SearchRoute {
        override val route: String = "search"
    }

    data object Results : SearchRoute {
        const val ARG_QUERY: String = "query"

        override val route: String = "search/results/{$ARG_QUERY}"

        fun createRoute(query: String): String = "search/results/${Uri.encode(query)}"
    }
}

sealed interface RouteSettingRoute : AppRoute {
    data object Setting : RouteSettingRoute {
        const val ARG_AUTO_START_NAVIGATION: String = "autoStartNavigation"
        const val ARG_INITIAL_ROUTE_OPTION: String = "initialRouteOption"

        override val route: String =
            "$ROUTE_SETTING_BASE_ROUTE?$ARG_AUTO_START_NAVIGATION={$ARG_AUTO_START_NAVIGATION}" +
                "&$ARG_INITIAL_ROUTE_OPTION={$ARG_INITIAL_ROUTE_OPTION}"

        fun createRoute(
            autoStartNavigation: Boolean = false,
            initialRouteOption: RouteOption? = null,
        ): String {
            val queryParameters =
                buildList {
                    if (autoStartNavigation) {
                        add("$ARG_AUTO_START_NAVIGATION=true")
                    }
                    initialRouteOption?.let { routeOption ->
                        add("$ARG_INITIAL_ROUTE_OPTION=${Uri.encode(routeOption.name)}")
                    }
                }

            return if (queryParameters.isEmpty()) {
                ROUTE_SETTING_BASE_ROUTE
            } else {
                "$ROUTE_SETTING_BASE_ROUTE?${queryParameters.joinToString(separator = "&")}"
            }
        }
    }

    data object Detail : RouteSettingRoute {
        const val ARG_ROUTE_OPTION: String = "routeOption"

        override val route: String = "$ROUTE_SETTING_BASE_ROUTE/detail/{$ARG_ROUTE_OPTION}"

        fun createRoute(routeOption: RouteOption): String =
            "$ROUTE_SETTING_BASE_ROUTE/detail/${routeOption.name.navArgEncode()}"
    }
}

sealed interface ReportRoute : AppRoute {
    data object Report : ReportRoute {
        override val route: String = "report"
    }
}

sealed interface MyPageSubRoute : AppRoute {
    data object ReportHistory : MyPageSubRoute {
        override val route: String = "my_page/report_history"
    }

    data object AppInfo : MyPageSubRoute {
        override val route: String = "my_page/app_info"
    }
}

sealed interface NavigationRoute : AppRoute {
    data object Guidance : NavigationRoute {
        override val route: String = "navigation_guidance"
    }
}

private fun String.navArgEncode(): String =
    URLEncoder
        .encode(this, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")

private const val ROUTE_SETTING_BASE_ROUTE: String = "route_setting"

sealed interface ArrivalRoute : AppRoute {
    data object Entry : ArrivalRoute {
        override val route: String = "arrival"
    }
}
