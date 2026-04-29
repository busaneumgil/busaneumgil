package com.ssafy.e102.eumgil.feature.mypage

import com.ssafy.e102.eumgil.core.model.AuthGateState
import com.ssafy.e102.eumgil.core.model.AuthSession
import com.ssafy.e102.eumgil.core.model.InitSettings
import com.ssafy.e102.eumgil.core.model.RepositoryDebugSettings
import com.ssafy.e102.eumgil.data.repository.AuthSessionRepository
import com.ssafy.e102.eumgil.data.repository.SettingsRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `debug toggle action updates repository source setting`() =
        runTest {
            val settingsRepository =
                FakeSettingsRepository(
                    debugSettings =
                        RepositoryDebugSettings(
                            isRuntimeToggleAvailable = true,
                            isRuntimeToggleEnabled = true,
                            isForceMockEnabled = false,
                        ),
                )
            val viewModel =
                MyPageViewModel(
                    settingsRepository = settingsRepository,
                    authSessionRepository = FakeAuthSessionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(MyPageUiAction.ForceMockToggled(isEnabled = true))
            advanceUntilIdle()

            assertEquals(true, settingsRepository.lastForceMockEnabled)
        }

    @Test
    fun `user type change action emits onboarding navigation event`() =
        runTest {
            val viewModel =
                MyPageViewModel(
                    settingsRepository = FakeSettingsRepository(),
                    authSessionRepository = FakeAuthSessionRepository(),
                )

            viewModel.onAction(MyPageUiAction.UserTypeChangeClicked)
            advanceUntilIdle()

            val event =
                withTimeoutOrNull(100) {
                    viewModel.uiEvent.first()
                }

            assertSame(MyPageUiEvent.NavigateToUserTypePrimary, event)
        }

    @Test
    fun `logout action clears auth session and emits login navigation event`() =
        runTest {
            val authSessionRepository = FakeAuthSessionRepository()
            val viewModel =
                MyPageViewModel(
                    settingsRepository = FakeSettingsRepository(),
                    authSessionRepository = authSessionRepository,
                )

            viewModel.onAction(MyPageUiAction.LogoutClicked)
            advanceUntilIdle()

            val event =
                withTimeoutOrNull(100) {
                    viewModel.uiEvent.first()
                }

            assertTrue(authSessionRepository.clearAuthSessionCalled)
            assertSame(MyPageUiEvent.NavigateToLogin, event)
        }
}

private class FakeSettingsRepository(
    initSettings: InitSettings = InitSettings(),
    debugSettings: RepositoryDebugSettings =
        RepositoryDebugSettings(
            isRuntimeToggleAvailable = false,
            isRuntimeToggleEnabled = false,
            isForceMockEnabled = false,
        ),
) : SettingsRepository {
    private val initSettingsFlow = MutableStateFlow(initSettings)
    private val debugSettingsFlow = MutableStateFlow(debugSettings)
    var lastForceMockEnabled: Boolean? = null

    override fun observeInitSettings(): Flow<InitSettings> = initSettingsFlow

    override suspend fun getInitSettings(): InitSettings = initSettingsFlow.value

    override suspend fun savePrimaryUserType(selectedPrimaryUserType: String) {
        initSettingsFlow.value = initSettingsFlow.value.copy(selectedPrimaryUserType = selectedPrimaryUserType)
    }

    override suspend fun saveMobilitySubtype(selectedMobilitySubtype: String) {
        initSettingsFlow.value = initSettingsFlow.value.copy(selectedMobilitySubtype = selectedMobilitySubtype)
    }

    override suspend fun saveLowVisionFollowUpCompleted(isCompleted: Boolean) {
        initSettingsFlow.value = initSettingsFlow.value.copy(isLowVisionFollowUpCompleted = isCompleted)
    }

    override suspend fun saveLocationTermsAgreement(
        isLocationTermsAgreed: Boolean,
        isPrivacyPolicyAgreed: Boolean,
    ) {
        initSettingsFlow.value =
            initSettingsFlow.value.copy(
                isLocationTermsAgreed = isLocationTermsAgreed,
                isPrivacyPolicyAgreed = isPrivacyPolicyAgreed,
            )
    }

    override fun observeRepositoryDebugSettings(): Flow<RepositoryDebugSettings> = debugSettingsFlow

    override suspend fun getRepositoryDebugSettings(): RepositoryDebugSettings = debugSettingsFlow.value

    override suspend fun setForceMockEnabled(isEnabled: Boolean) {
        lastForceMockEnabled = isEnabled
        debugSettingsFlow.value = debugSettingsFlow.value.copy(isForceMockEnabled = isEnabled)
    }
}

private class FakeAuthSessionRepository : AuthSessionRepository {
    var clearAuthSessionCalled = false
    private val authGateStateEvents = MutableSharedFlow<AuthGateState>()

    override fun observeAuthGateState(): Flow<AuthGateState> = authGateStateEvents

    override suspend fun getAuthGateState(): AuthGateState = AuthGateState()

    override suspend fun saveAuthSession(
        authSession: AuthSession,
        isProfileCompleted: Boolean,
    ) = Unit

    override suspend fun markProfileCompleted() = Unit

    override suspend fun clearAuthSession() {
        clearAuthSessionCalled = true
    }
}
