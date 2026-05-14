package com.ssafy.e102.eumgil.feature.map

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import kotlinx.coroutines.flow.collect

@Composable
fun MapRoute(
    onNavigateToSavedRoutes: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    onNavigateToRouteSetting: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    shouldResetForHomeEntry: Boolean = false,
    onHomeReentryResetConsumed: () -> Unit = {},
    onFacilityDetailVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory =
        remember(appContainer) {
            MapViewModel.provideFactory(
                locationPermissionManager = appContainer.locationPermissionManager,
                currentLocationManager = appContainer.currentLocationManager,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
                destinationPreviewRepository = appContainer.destinationPreviewRepository,
                facilitySeedRepository = appContainer.facilitySeedRepository,
                bookmarkRepository = appContainer.bookmarkRepository,
                searchRepository = appContainer.searchRepository,
                placesRepository = appContainer.placesRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "MapRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[MapViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val consumeHomeReentryReset by rememberUpdatedState(onHomeReentryResetConsumed)

    LaunchedEffect(viewModel, shouldResetForHomeEntry) {
        if (!shouldResetForHomeEntry) return@LaunchedEffect

        viewModel.onHomeReentered()
        consumeHomeReentryReset()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.onRouteStarted()
                    Lifecycle.Event.ON_STOP -> viewModel.onRouteStopped()
                    else -> Unit
                }
            }

        lifecycle.addObserver(observer)
        if (shouldStartMapRouteImmediately(lifecycle.currentState)) {
            viewModel.onRouteStarted()
        }

        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.onRouteStopped()
        }
    }

    LaunchedEffect(
        viewModel,
        activity,
        appContainer,
        onNavigateToRouteSetting,
        onNavigateToSearch,
        snackbarHostState,
    ) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                MapUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
                MapUiEvent.NavigateToSearch -> onNavigateToSearch()
                MapUiEvent.RequestLocationPermission ->
                    activity?.let(appContainer.locationPermissionManager::requestLocationPermission)
                is MapUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    BackHandler(enabled = uiState.facilityDetailSheetState.isVisible) {
        viewModel.onAction(MapUiAction.FacilityDetailDismissed)
    }

    BackHandler(enabled = uiState.facilityDetailSheetState.isVisible.not()) {
        if (activity?.moveTaskToBack(true) == false) {
            activity.finish()
        }
    }

    LaunchedEffect(uiState.facilityDetailSheetState.isVisible, onFacilityDetailVisibilityChanged) {
        onFacilityDetailVisibilityChanged(uiState.facilityDetailSheetState.isVisible)
    }

    DisposableEffect(onFacilityDetailVisibilityChanged) {
        onDispose {
            onFacilityDetailVisibilityChanged(false)
        }
    }

    MapScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onNavigateToSavedRoutes = onNavigateToSavedRoutes,
        onNavigateToMyPage = onNavigateToMyPage,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

internal fun shouldStartMapRouteImmediately(currentState: Lifecycle.State): Boolean =
    currentState.isAtLeast(Lifecycle.State.STARTED)
