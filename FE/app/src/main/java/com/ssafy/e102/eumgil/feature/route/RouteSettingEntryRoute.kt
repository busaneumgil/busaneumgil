package com.ssafy.e102.eumgil.feature.route

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import kotlinx.coroutines.flow.collect

@Composable
fun RouteSettingEntryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: (RouteEditingTarget) -> Unit = {},
    onNavigateToRouteDetail: (RouteOption) -> Unit = {},
    onStartNavigation: (RouteNavigationRequest) -> Unit = {},
    autoStartNavigation: Boolean = false,
    initialRouteOption: RouteOption? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberRouteSettingViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var initialRouteOptionApplied by rememberSaveable(initialRouteOption) { mutableStateOf(false) }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToSearch, onNavigateToRouteDetail, onStartNavigation) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RouteSettingUiEvent.NavigateBack -> onNavigateBack()
                is RouteSettingUiEvent.NavigateToSearch -> onNavigateToSearch(event.editingTarget)
                is RouteSettingUiEvent.NavigateToRouteDetail -> onNavigateToRouteDetail(event.routeOption)
                is RouteSettingUiEvent.StartNavigationRequested -> onStartNavigation(event.request)
            }
        }
    }

    LaunchedEffect(viewModel, autoStartNavigation, uiState.isStartEnabled, uiState.ctaAcknowledged, uiState.pendingTravelMode) {
        if (autoStartNavigation && uiState.isStartEnabled && !uiState.ctaAcknowledged && uiState.pendingTravelMode == null) {
            viewModel.onAction(RouteSettingUiAction.StartNavigationClicked)
        }
    }

    DisposableEffect(viewModel) {
        viewModel.startLocationUpdates()
        onDispose {
            viewModel.stopLocationUpdates()
        }
    }

    LaunchedEffect(viewModel, initialRouteOption, uiState.isLoading, uiState.optionCards, initialRouteOptionApplied) {
        if (
            !initialRouteOptionApplied &&
            initialRouteOption != null &&
            !uiState.isLoading &&
            uiState.optionCards.any { optionCard -> optionCard.routeOption == initialRouteOption }
        ) {
            if (uiState.selectedOption != initialRouteOption) {
                viewModel.onAction(RouteSettingUiAction.RouteOptionSelected(initialRouteOption))
            }
            initialRouteOptionApplied = true
        }
    }

    RouteSettingScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun RouteDetailEntryRoute(
    routeOption: RouteOption,
    onNavigateBack: () -> Unit,
    onStartNavigation: (RouteNavigationRequest) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberRouteSettingViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, routeOption) {
        viewModel.onAction(RouteSettingUiAction.RouteOptionSelected(routeOption))
    }

    LaunchedEffect(viewModel, onNavigateBack, onStartNavigation) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RouteSettingUiEvent.NavigateBack -> onNavigateBack()
                is RouteSettingUiEvent.NavigateToSearch -> Unit
                is RouteSettingUiEvent.NavigateToRouteDetail -> Unit
                is RouteSettingUiEvent.StartNavigationRequested -> onStartNavigation(event.request)
            }
        }
    }

    RouteDetailScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onStartClick = {
            viewModel.onAction(RouteSettingUiAction.StartNavigationClicked)
        },
        modifier = modifier,
    )
}

@Composable
private fun rememberRouteSettingViewModel(): RouteSettingViewModel {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory =
        remember(appContainer) {
            RouteSettingViewModel.provideFactory(
                routeRepository = appContainer.routeRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
                currentLocationManager = appContainer.currentLocationManager,
            )
        }

    return remember(activity, viewModelFactory) {
        val owner = checkNotNull(activity) { "RouteSettingEntryRoute requires a ComponentActivity host." }
        ViewModelProvider(owner, viewModelFactory)[RouteSettingViewModel::class.java]
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
