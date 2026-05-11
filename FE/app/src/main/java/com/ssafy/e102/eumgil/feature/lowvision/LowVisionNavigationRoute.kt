package com.ssafy.e102.eumgil.feature.lowvision

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
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
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiEvent
import com.ssafy.e102.eumgil.feature.navigation.NavigationViewModel
import kotlinx.coroutines.flow.collect

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
    val latestLocation by appContainer.currentLocationManager.latestLocation.collectAsStateWithLifecycle()
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    val currentLocationAddressResolver =
        remember(appContext) { AndroidCurrentLocationAddressResolver(context = appContext) }
    val currentLocationAddress =
        rememberLowVisionCurrentLocationAddress(
            coordinate = uiState.mapOverlay.currentLocation?.coordinate,
            addressResolver = currentLocationAddressResolver,
        )

    LaunchedEffect(appContainer.currentLocationManager) {
        appContainer.currentLocationManager.refreshLatestLocation()
    }

    LaunchedEffect(selectedDestination, latestLocation) {
        loadErrorMessage = null
        val request =
            appContainer.routeRepository
                .buildLowVisionNavigationRequest(
                    destinationSelectionRepository = appContainer.destinationSelectionRepository,
                    origin = latestLocation.toLowVisionRouteOriginWaypoint(),
                )
        if (request == null) {
            loadErrorMessage = LOW_VISION_NAVIGATION_LOAD_ERROR_MESSAGE
        } else {
            viewModel.bindNavigationRequest(request)
        }
    }

    LaunchedEffect(viewModel, onNavigateToComplete, onNavigateToBookmark) {
        viewModel.uiEvent.collect { event ->
            when {
                shouldNavigateLowVisionHome(event) -> onNavigateToComplete()
                event == NavigationUiEvent.NavigateToSavedRoute -> onNavigateToBookmark()
                else -> Unit
            }
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

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
