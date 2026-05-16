package com.ssafy.e102.eumgil.feature.voiceassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data object ConfirmClicked : UiAction

data object CloseOverlay : UiEvent

data class DispatchAction(
    val action: VoiceAssistantAction,
) : UiEvent

class VoiceAssistantViewModel(
    private val interpreter: VoiceAssistantInterpreter = RuleBasedVoiceAssistantInterpreter(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    private val mutableUiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = mutableUiEvent.asSharedFlow()

    fun onAction(action: UiAction) {
        when (action) {
            UiAction.AssistantClicked -> startListening()
            UiAction.Dismissed -> resetOverlayState()
            is UiAction.ContextChanged -> updateContext(action.context)
            is UiAction.TranscriptChanged -> resolveTranscript(action.transcript)
            is UiAction.ActionResolved -> resolveAction(action.action)
            UiAction.ConfirmationAccepted -> acceptPendingConfirmation()
            UiAction.ConfirmationRejected -> rejectPendingConfirmation()
            ConfirmClicked -> acceptPendingConfirmation()
        }
    }

    private fun startListening() {
        mutableUiState.update { state ->
            state.copy(
                status = VoiceAssistantStatus.Listening,
                transcript = "",
                pendingConfirmationAction = null,
                errorMessage = null,
            )
        }
    }

    private fun resetOverlayState() {
        mutableUiState.update { state ->
            state.copy(
                status = VoiceAssistantStatus.Idle,
                pendingConfirmationAction = null,
                errorMessage = null,
            )
        }
        emitUiEvent(CloseOverlay)
    }

    private fun updateContext(context: VoiceAssistantContext) {
        mutableUiState.update { state ->
            state.copy(context = context)
        }
    }

    private fun resolveTranscript(transcript: String) {
        val context = uiState.value.context
        mutableUiState.update { state ->
            state.copy(
                status = VoiceAssistantStatus.Processing,
                transcript = transcript,
                errorMessage = null,
            )
        }
        resolveAction(interpreter.interpret(transcript = transcript, context = context))
    }

    private fun resolveAction(action: VoiceAssistantAction) {
        if (action.requiresConfirmation) {
            mutableUiState.update { state ->
                state.copy(
                    status = VoiceAssistantStatus.AwaitingConfirmation,
                    pendingConfirmationAction = action,
                    lastResolvedAction = action,
                    errorMessage = null,
                )
            }
            return
        }

        mutableUiState.update { state ->
            state.copy(
                status = VoiceAssistantStatus.Idle,
                pendingConfirmationAction = null,
                lastResolvedAction = action,
                errorMessage = null,
            )
        }
        emitUiEvent(DispatchAction(action))
    }

    private fun acceptPendingConfirmation() {
        val pendingAction = uiState.value.pendingConfirmationAction ?: return
        mutableUiState.update { state ->
            state.copy(
                status = VoiceAssistantStatus.Idle,
                pendingConfirmationAction = null,
                lastResolvedAction = pendingAction,
                errorMessage = null,
            )
        }
        emitUiEvent(DispatchAction(pendingAction))
    }

    private fun rejectPendingConfirmation() {
        mutableUiState.update { state ->
            state.copy(
                status = VoiceAssistantStatus.Idle,
                pendingConfirmationAction = null,
                errorMessage = null,
            )
        }
    }

    private fun emitUiEvent(event: UiEvent) {
        viewModelScope.launch {
            mutableUiEvent.emit(event)
        }
    }
}
