package com.ssafy.e102.eumgil.data.local.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ssafy.e102.eumgil.feature.onboarding.MobilitySubtype
import com.ssafy.e102.eumgil.feature.onboarding.PrimaryUserType
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class InitSettingsLocalDataSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `changing primary user type preserves accepted terms and clears type specific progress`() =
        runTest {
            val dataSource = createDataSource()

            dataSource.savePrimaryUserType(PrimaryUserType.MOBILITY_IMPAIRED.routeValue)
            dataSource.saveMobilitySubtype(MobilitySubtype.MANUAL_WHEELCHAIR.routeValue)
            dataSource.saveLowVisionFollowUpCompleted(isCompleted = true)
            dataSource.saveLocationTermsAgreement(
                isLocationTermsAgreed = true,
                isPrivacyPolicyAgreed = true,
            )

            dataSource.savePrimaryUserType(PrimaryUserType.LOW_VISION.routeValue)

            val settings = dataSource.getInitSettings()
            assertEquals(PrimaryUserType.LOW_VISION.routeValue, settings.selectedPrimaryUserType)
            assertNull(settings.selectedMobilitySubtype)
            assertFalse(settings.isLowVisionFollowUpCompleted)
            assertTrue(settings.isLocationTermsAgreed)
            assertTrue(settings.isPrivacyPolicyAgreed)
        }

    @Test
    fun `saving low vision type again clears stale mobility subtype and keeps follow up flag`() =
        runTest {
            val dataSource = createDataSource()

            dataSource.savePrimaryUserType(PrimaryUserType.LOW_VISION.routeValue)
            dataSource.saveMobilitySubtype(MobilitySubtype.MANUAL_WHEELCHAIR.routeValue)
            dataSource.saveLowVisionFollowUpCompleted(isCompleted = true)

            dataSource.savePrimaryUserType(PrimaryUserType.LOW_VISION.routeValue)

            val settings = dataSource.getInitSettings()
            assertEquals(PrimaryUserType.LOW_VISION.routeValue, settings.selectedPrimaryUserType)
            assertNull(settings.selectedMobilitySubtype)
            assertTrue(settings.isLowVisionFollowUpCompleted)
        }

    @Test
    fun `clearing init settings removes onboarding progress and agreement flags`() =
        runTest {
            val dataSource = createDataSource()

            dataSource.savePrimaryUserType(PrimaryUserType.MOBILITY_IMPAIRED.routeValue)
            dataSource.saveMobilitySubtype(MobilitySubtype.MANUAL_WHEELCHAIR.routeValue)
            dataSource.saveLowVisionFollowUpCompleted(isCompleted = true)
            dataSource.saveLocationTermsAgreement(
                isLocationTermsAgreed = true,
                isPrivacyPolicyAgreed = true,
            )

            dataSource.clearInitSettings()

            val settings = dataSource.getInitSettings()
            assertNull(settings.selectedPrimaryUserType)
            assertNull(settings.selectedMobilitySubtype)
            assertFalse(settings.isLowVisionFollowUpCompleted)
            assertFalse(settings.isLocationTermsAgreed)
            assertFalse(settings.isPrivacyPolicyAgreed)
        }

    private fun TestScope.createDataSource(): InitSettingsLocalDataSource {
        val file = File(temporaryFolder.newFolder(), "init_settings.preferences_pb")
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = this,
                produceFile = { file },
            )

        return InitSettingsLocalDataSource(dataStore)
    }
}
