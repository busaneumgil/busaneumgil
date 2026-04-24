package com.ssafy.e102.eumgil.feature.navigation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.tts.TextToSpeechAvailability
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun NavigationRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val textToSpeechController = appContainer.textToSpeechController
    val viewModelFactory = remember { NavigationViewModel.provideFactory() }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "NavigationRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[NavigationViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textToSpeechState by textToSpeechController.state.collectAsStateWithLifecycle()

    LaunchedEffect(textToSpeechState) {
        viewModel.updateTextToSpeechState(
            isEnabled = textToSpeechState.enabled,
            canSpeak = textToSpeechState.canSpeak,
            status = textToSpeechState.availability.toNavigationTtsStatus(),
        )
    }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToMap) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    NavigationUiEvent.NavigateBack -> onNavigateBack()
                    NavigationUiEvent.NavigateToMap -> onNavigateToMap()
                    is NavigationUiEvent.SpeakBriefing -> textToSpeechController.speak(event.text)
                    NavigationUiEvent.StopBriefing -> textToSpeechController.stop()
                    is NavigationUiEvent.SetVoiceGuidanceEnabled ->
                        textToSpeechController.setEnabled(event.enabled)
                }
            }
        }
        viewModel.onAction(NavigationUiAction.NavigationEntered)
    }

    DisposableEffect(textToSpeechController) {
        onDispose {
            textToSpeechController.stop()
        }
    }

    NavigationScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

private fun TextToSpeechAvailability.toNavigationTtsStatus(): NavigationTtsStatus =
    when (this) {
        TextToSpeechAvailability.Initializing -> NavigationTtsStatus.Initializing
        TextToSpeechAvailability.Ready -> NavigationTtsStatus.Ready
        TextToSpeechAvailability.Unavailable -> NavigationTtsStatus.Unavailable
    }

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
