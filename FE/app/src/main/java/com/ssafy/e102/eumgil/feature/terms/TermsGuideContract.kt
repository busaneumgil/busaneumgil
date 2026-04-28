package com.ssafy.e102.eumgil.feature.terms

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ssafy.e102.eumgil.R

/**
 * Five-step terms walkthrough used in the visual-impairment voice-guide flow.
 *
 * Source: Figma file MREqSzkmwhRcXnFS3lzW17 (E102-mockup), nodes
 *   1) 328:486  약관 동의
 *   2) 328:528  민감정보
 *   3) 328:570  위치정보
 *   4) 328:612  14세 이상         (자세히 보기 버튼 없음, 힌트만 노출)
 *   5) 328:652  처리방침
 *
 * Each step renders the same shell (`TermsGuideScreen`) with per-step icon,
 * card label, hint copy, and an optional "자세히 보기" button. Sequence number
 * is the public API: callers navigate by passing `routeValue` and the screen
 * advances on double-tap until [isLast], at which point the final agreement
 * callback fires.
 */
enum class TermsGuideStep(
    val sequence: Int,
    val routeValue: String,
    @DrawableRes val iconRes: Int,
    @StringRes val cardLabelRes: Int,
    @StringRes val hintRes: Int,
    val showMoreButton: Boolean,
) {
    AGREE(
        sequence = 1,
        routeValue = "agree",
        iconRes = R.drawable.ic_terms_document,
        cardLabelRes = R.string.terms_guide_step_agree_card,
        hintRes = R.string.terms_guide_hint_agree,
        showMoreButton = true,
    ),
    SENSITIVE(
        sequence = 2,
        routeValue = "sensitive",
        iconRes = R.drawable.ic_terms_sensitive,
        cardLabelRes = R.string.terms_guide_step_sensitive_card,
        hintRes = R.string.terms_guide_hint_agree,
        showMoreButton = true,
    ),
    LOCATION(
        sequence = 3,
        routeValue = "location",
        iconRes = R.drawable.ic_terms_location,
        cardLabelRes = R.string.terms_guide_step_location_card,
        hintRes = R.string.terms_guide_hint_agree,
        showMoreButton = true,
    ),
    AGE(
        sequence = 4,
        routeValue = "age",
        iconRes = R.drawable.ic_terms_age,
        cardLabelRes = R.string.terms_guide_step_age_card,
        hintRes = R.string.terms_guide_hint_confirm,
        showMoreButton = false,
    ),
    PRIVACY(
        sequence = 5,
        routeValue = "privacy",
        iconRes = R.drawable.ic_terms_privacy,
        cardLabelRes = R.string.terms_guide_step_privacy_card,
        hintRes = R.string.terms_guide_hint_confirm,
        showMoreButton = true,
    ),
    ;

    val isLast: Boolean
        get() = sequence == TOTAL_STEPS

    fun next(): TermsGuideStep? = entries.firstOrNull { it.sequence == sequence + 1 }

    companion object {
        const val TOTAL_STEPS: Int = 5

        fun fromRouteValue(routeValue: String?): TermsGuideStep? =
            entries.firstOrNull { it.routeValue == routeValue }

        fun fromSequence(sequence: Int): TermsGuideStep? =
            entries.firstOrNull { it.sequence == sequence }
    }
}

/**
 * UI state for the terms walkthrough. Driven by [step]; the screen reads everything
 * else (label, hint, icon, button visibility) from the enum so adding a new step
 * is a one-place change.
 */
data class TermsGuideUiState(
    val step: TermsGuideStep,
    val isAgreed: Boolean = false,
) {
    val currentStep: Int get() = step.sequence
    val totalSteps: Int get() = TermsGuideStep.TOTAL_STEPS
    val pageIndex: Int get() = (currentStep - 1).coerceIn(0, totalSteps - 1)
}

/**
 * Bottom navigation tabs rendered at the bottom of the high-contrast walkthrough.
 *
 * Selection here only highlights the tab; tab routing is delegated to the caller because
 * the destinations live under [com.ssafy.e102.eumgil.app.navigation.TopLevelDestination]
 * and are not all reachable while onboarding is incomplete.
 */
enum class TermsBottomTab {
    HOME,
    BOOKMARK,
    CATEGORY,
    MY,
}
