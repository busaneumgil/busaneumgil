package com.ssafy.e102.eumgil.feature.lowvision

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.tts.AndroidTextToSpeechController

@Composable
fun LowVisionRouteBriefingRoute(
    onTabSelected: (LowVisionBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appContainer =
        remember(appContext) {
            (appContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory =
        remember(appContainer) {
            LowVisionRouteBriefingViewModel.provideFactory(
                routeRepository = appContainer.routeRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "LowVisionRouteBriefingRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[LowVisionRouteBriefingViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ttsController =
        remember(appContext) {
            AndroidTextToSpeechController(context = appContext)
        }
    val ttsState by ttsController.state.collectAsStateWithLifecycle()
    var playbackActive by rememberSaveable { mutableStateOf(false) }
    var playbackToken by rememberSaveable { mutableIntStateOf(ttsState.completedUtteranceCount) }

    LaunchedEffect(ttsState.completedUtteranceCount) {
        if (playbackActive && ttsState.completedUtteranceCount > playbackToken) {
            playbackActive = false
        }
    }

    DisposableEffect(ttsController) {
        onDispose {
            ttsController.stop()
            ttsController.shutdown()
        }
    }

    LowVisionRouteBriefingScreen(
        uiState = uiState,
        isPlaying = playbackActive,
        onPlaybackClick = {
            if (playbackActive) {
                playbackActive = false
                ttsController.stop()
            } else {
                playbackToken = ttsState.completedUtteranceCount
                playbackActive = true
                ttsController.speak(uiState.briefingText)
            }
        },
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
