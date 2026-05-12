package com.ssafy.e102.eumgil.feature.lowvision

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.location.AndroidCurrentLocationAddressResolver
import com.ssafy.e102.eumgil.core.tts.AndroidTextToSpeechController
import com.ssafy.e102.eumgil.core.tts.TextToSpeechAvailability
import com.ssafy.e102.eumgil.feature.navigation.NavigationTtsStatus
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiEvent
import com.ssafy.e102.eumgil.feature.navigation.NavigationViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun LowVisionNavigationRoute(
    onNavigateToComplete: () -> Unit,
    onNavigateToBookmark: () -> Unit,
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
        remember(appContainer.currentLocationManager, appContainer.bookmarkRepository, appContainer.routeRepository) {
            NavigationViewModel.provideFactory(
                currentLocationManager = appContainer.currentLocationManager,
                bookmarkRepository = appContainer.bookmarkRepository,
                routeRepository = appContainer.routeRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "LowVisionNavigationRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[NavigationViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDestination by
        appContainer.destinationSelectionRepository.selectedDestination.collectAsStateWithLifecycle()
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    val textToSpeechController =
        remember(appContext) {
            AndroidTextToSpeechController(context = appContext)
        }
    val textToSpeechState by textToSpeechController.state.collectAsStateWithLifecycle()
    val currentLocationAddressResolver =
        remember(appContext) { AndroidCurrentLocationAddressResolver(context = appContext) }
    val currentLocationAddress =
        rememberLowVisionCurrentLocationAddress(
            coordinate = uiState.mapOverlay.currentLocation?.coordinate,
            addressResolver = currentLocationAddressResolver,
        )

    LaunchedEffect(textToSpeechState) {
        viewModel.updateTextToSpeechState(
            isEnabled = textToSpeechState.enabled,
            canSpeak = textToSpeechState.canSpeak,
            status = textToSpeechState.availability.toLowVisionNavigationTtsStatus(),
        )
    }

    LaunchedEffect(viewModel, onNavigateToComplete, onNavigateToBookmark) {
        launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    NavigationUiEvent.NavigateBack -> Unit
                    is NavigationUiEvent.NavigateToRouteDetail -> Unit
                    NavigationUiEvent.NavigateToMap,
                    NavigationUiEvent.NavigateToArrival,
                        -> onNavigateToComplete()

                    NavigationUiEvent.NavigateToSavedRoute -> onNavigateToBookmark()
                    is NavigationUiEvent.SpeakBriefing -> textToSpeechController.speak(event.text)
                    NavigationUiEvent.StopBriefing -> textToSpeechController.stop()
                    is NavigationUiEvent.SetVoiceGuidanceEnabled ->
                        textToSpeechController.setEnabled(event.enabled)
                }
            }
        }
    }

    LaunchedEffect(selectedDestination) {
        loadErrorMessage = null
        appContainer.currentLocationManager.refreshLatestLocation()
        val origin = appContainer.currentLocationManager.latestLocation.value.toLowVisionRouteOriginWaypoint()
        val request =
            appContainer.routeRepository
                .buildLowVisionNavigationRequest(
                    destinationSelectionRepository = appContainer.destinationSelectionRepository,
                    origin = origin,
                )
        if (request == null) {
            loadErrorMessage = LOW_VISION_NAVIGATION_LOAD_ERROR_MESSAGE
        } else {
            viewModel.bindNavigationRequest(request)
            viewModel.onAction(NavigationUiAction.NavigationEntered)
        }
    }

    DisposableEffect(textToSpeechController) {
        onDispose {
            textToSpeechController.stop()
            textToSpeechController.shutdown()
        }
    }

    LowVisionFontTheme {
        LowVisionNavigationScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onTabSelected = onTabSelected,
            modifier = modifier,
            loadErrorMessage = loadErrorMessage,
            currentLocationAddress = currentLocationAddress,
        )
    }
}

internal fun shouldNavigateLowVisionHome(event: NavigationUiEvent): Boolean =
    event == NavigationUiEvent.NavigateToMap || event == NavigationUiEvent.NavigateToArrival

private fun TextToSpeechAvailability.toLowVisionNavigationTtsStatus(): NavigationTtsStatus =
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
