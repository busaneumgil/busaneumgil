package com.ssafy.e102.eumgil.feature.voiceassistant

import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceAssistantViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `transcript changed emits dispatch action when command does not require confirmation`() =
        runTest {
            val viewModel = VoiceAssistantViewModel()
            val eventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(1).toList() }

            viewModel.onAction(UiAction.TranscriptChanged("제보해줘"))

            assertEquals(listOf(DispatchAction(VoiceAssistantAction.OpenReport())), eventsDeferred.await())
            assertEquals(VoiceAssistantAction.OpenReport(), viewModel.uiState.value.lastResolvedAction)
        }

    @Test
    fun `confirmation required action waits until accepted before dispatch event`() =
        runTest {
            val viewModel = VoiceAssistantViewModel()
            val firstEventDeferred =
                async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeoutOrNull(100) { viewModel.uiEvent.first() }
                }

            viewModel.onAction(UiAction.ActionResolved(VoiceAssistantAction.StopNavigation()))

            assertNull(firstEventDeferred.await())
            assertEquals(VoiceAssistantAction.StopNavigation(), viewModel.uiState.value.pendingConfirmationAction)

            val acceptedEventsDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEvent.take(1).toList() }

            viewModel.onAction(ConfirmClicked)

            assertEquals(listOf(DispatchAction(VoiceAssistantAction.StopNavigation())), acceptedEventsDeferred.await())
            assertNull(viewModel.uiState.value.pendingConfirmationAction)
        }
}
