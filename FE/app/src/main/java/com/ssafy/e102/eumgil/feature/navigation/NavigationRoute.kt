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
import com.ssafy.e102.eumgil.core.tts.AndroidTextToSpeechController
import com.ssafy.e102.eumgil.core.tts.TextToSpeechAvailability
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun NavigationRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToSavedRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val activity = remember(context) { context.findComponentActivity() }
    val textToSpeechController =
        remember(appContext) {
            AndroidTextToSpeechController(context = appContext)
        }
    val currentLocationManager = remember(appContext) {
        (appContext as BusanEumgilApp).appContainer.currentLocationManager
    }
    val viewModelFactory = remember(currentLocationManager) {
        NavigationViewModel.provideFactory(currentLocationManager = currentLocationManager)
    }
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

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToMap, onNavigateToSavedRoute) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    NavigationUiEvent.NavigateBack -> onNavigateBack()
                    NavigationUiEvent.NavigateToMap -> onNavigateToMap()
                    NavigationUiEvent.NavigateToSavedRoute -> onNavigateToSavedRoute()
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
            textToSpeechController.shutdown()
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
        TextToSpeechAvailability.Initia