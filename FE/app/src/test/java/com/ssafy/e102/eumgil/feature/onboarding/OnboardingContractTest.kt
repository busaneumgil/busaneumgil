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
                R.drawable.ic_user_electric_wheelchair,
                R.drawable.ic_user_wheelchair,
                R.drawable.ic_user_walking_aid,
            ),
            MobilitySubtype.entries.map { it.iconRes },
        )
        assertEquals(
            listOf(64, 56, 64),
            MobilitySubtype.entries.map { it.iconSizeDp },
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
                R.drawable.ic_user_low_vision,
                R.drawable.ic_user_wheelchair,
            ),
            PrimaryUserType.entries.map { it.iconRes },
        )
    }

    @Test
    fun `mobility subtype lookup returns null for unknown route value`() {
        assertNull(MobilitySubtype.fromRouteValue("legacy_disability_level"))
    }

    @Test
    fun `location terms require all four mandatory agreements before proceeding`() {
        val missingAgeConfirmation =
            LocationTermsUiState(
                isServiceTermsChecked = true,
                isSensitiveInfoTermsChecked = true,
                isPersonalLocationInfoTermsChecked = true,
            )

        assertFalse(missingAgeConfirmation.canProceed)
        assertFalse(missingAgeConfirmation.isAllTermsChecked)

        val requiredTermsCompleted =
            missingAgeConfirmation.copy(
                isOverFourteenChecked = true,
            )

        assertTrue(requiredTermsCompleted.canProceed)
        assertFalse(requiredTermsCompleted.isAllTermsChecked)
    }

    @Test
    fun `location terms all agreement only completes when privacy policy confirmation is included`() {
        val uiState =
            LocationTermsUiState(
                isServiceTermsChecked = true,
                isSensitiveInfoTermsChecked = true,
                isPersonalLocationInfoTermsChecked = true,
                isOverFourteenChecked = true,
                isPrivacyPolicyChecked = true,
            )

        assertTrue(uiState.canProceed)
        assertTrue(uiState.isAllTermsChecked)
    }

    @Test
    fun `location terms agreement keeps persisted contract focused on required terms and privacy confirmation`() {
        val uiState =
            LocationTermsUiState(
                isServiceTermsChecked = true,
                isSensitiveInfoTermsChecked = true,
                isPersonalLocationInfoTermsChecked = true,
                isOverFourteenChecked = true,
                isPrivacyPolicyChecked = false,
            )

        assertEquals(
            LocationTermsAgreement(
                isLocationTermsAgreed = true,
                isPrivacyPolicyAgreed = false,
            ),
            uiState.toAgreement(),
        )
    }

    @Test
    fun `location terms items stay aligned with ONB-004 required order`() {
        assertEquals(
            listOf(
                LocationTermsItem.SERVICE_AND_LOCATION_BASED_SERVICE,
                LocationTermsItem.SENSITIVE_INFO,
                LocationTermsItem.PERSONAL_LOCATION_INFO,
                LocationTermsItem.OVER_FOURTEEN,
                LocationTermsItem.PRIVACY_POLICY_CONFIRMATION,
            ),
            LocationTermsItem.entries,
        )
        assertEquals(
            listOf(true, true, true, true, false),
            LocationTermsItem.entries.map { it.required },
        )
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
