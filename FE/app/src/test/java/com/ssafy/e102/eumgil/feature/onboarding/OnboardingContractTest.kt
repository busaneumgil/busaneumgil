package com.ssafy.e102.eumgil.feature.onboarding

import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.model.InitSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingContractTest {
    @Test
    fun `mobility subtype entries stay aligned with ONB-003 options`() {
        assertEquals(
            listOf(
                MobilitySubtype.ELECTRIC_WHEELCHAIR,
                MobilitySubtype.MANUAL_WHEELCHAIR,
                MobilitySubtype.OTHER,
            ),
            MobilitySubtype.entries,
        )
        assertEquals(
            listOf(
                "electric_wheelchair",
                "manual_wheelchair",
                "other_mobility_impaired",
            ),
            MobilitySubtype.entries.map { it.routeValue },
        )
        assertEquals(
            listOf(
                R.drawable.ic_user_wheelchair,
                R.drawable.ic_user_wheelchair,
                R.drawable.ic_user_check,
            ),
            MobilitySubtype.entries.map { it.iconRes },
        )
    }

    @Test
    fun `primary user type entries stay aligned with ONB-001 options`() {
        assertEquals(
            listOf(
                PrimaryUserType.LOW_VISION,
                PrimaryUserType.MOBILITY_IMPAIRED,
            ),
            PrimaryUserType.entries,
        )
        assertEquals(
            listOf(
                R.drawable.ic_user_visual_impairment,
                R.drawable.ic_user_wheelchair,
            ),
            PrimaryUserType.entries.map { it.iconRes },
        )
        assertEquals(
            listOf(true, false),
            PrimaryUserType.entries.map { it.usesHighContrastCard },
        )
    }

    @Test
    fun `mobility subtype lookup returns null for unknown route value`() {
        assertNull(MobilitySubtype.fromRouteValue("legacy_disability_level"))
    }

    @Test
    fun `mobility impaired onboarding stays incomplete until subtype is saved`() {
        val initSettings =
            InitSettings(
                selectedPrimaryUserType = PrimaryUserType.MOBILITY_IMPAIRED.routeValue,
                isLocationTermsAgreed = true,
            )

        assertFalse(initSettings.isOnboardingCompleted)
    }

    @Test
    fun `mobility impaired onboarding completes when subtype and required terms are saved`() {
        val initSettings =
            InitSettings(
                selectedPrimaryUserType = PrimaryUserType.MOBILITY_IMPAIRED.routeValue,
                selectedMobilitySubtype = MobilitySubtype.MANUAL_WHEELCHAIR.routeValue,
                isLocationTermsAgreed = true,
            )

        assertTrue(initSettings.isOnboardingCompleted)
    }
}
