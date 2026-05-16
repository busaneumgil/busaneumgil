package com.ssafy.e102.eumgil.feature.route

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.app.navigation.rememberNavigationGuidanceViewModel
import com.ssafy.e102.eumgil.core.external.createDuribalDialIntent
import com.ssafy.e102.eumgil.core.external.requestLowFloorBusReservation
import com.ssafy.e102.eumgil.core.model.LowFloorBusReservation
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun RouteSettingEntryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onNavigateToSearch: (RouteEditingTarget) -> Unit = {},
    onNavigateToRouteDetail: (RouteOption) -> Unit = {},
    onStartNavigation: (RouteNavigationRequest) -> Unit = {},
    autoStartNavigation: Boolean = false,
    initialRouteOption: RouteOption? = null,
    requestLocationPermissionIfNeeded: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModel = rememberRouteSettingViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var initialRouteOptionApplied by rememberSaveable(initialRouteOption) { mutableStateOf(false) }
    var isDuribalConfirmDialogVisible by rememberSaveable { mutableStateOf(false) }
    var pendingLowFloorReservation by remember { mutableStateOf<LowFloorBusReservation?>(null) }
    var isLowFloorReservationRequesting by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToMap, onNavigateToSearch, onNavigateToRouteDetail, onStartNavigation) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RouteSettingUiEvent.NavigateBack -> onNavigateBack()
                RouteSettingUiEvent.NavigateToMap -> onNavigateToMap()
                RouteSettingUiEvent.RequestLocationPermission ->
                    activity?.let(appContainer.locationPermissionManager::requestLocationPermission)
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

    DisposableEffect(viewModel, requestLocationPermissionIfNeeded) {
        viewModel.startLocationUpdates(requestLocationPermissionIfNeeded = requestLocationPermissionIfNeeded)
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
        isDuribalConfirmDialogVisible = isDuribalConfirmDialogVisible,
        onDuribalCallClick = { isDuribalConfirmDialogVisible = true },
        onDuribalConfirmDismiss = { isDuribalConfirmDialogVisible = false },
        onDuribalConfirm = {
            isDuribalConfirmDialogVisible = false
            context.startActivity(createDuribalDialIntent())
        },
        pendingLowFloorReservation = pendingLowFloorReservation,
        isLowFloorReservationRequesting = isLowFloorReservationRequesting,
        onLowFloorReservationClick = { reservation -> pendingLowFloorReservation = reservation },
        onLowFloorReservationDismiss = {
            if (!isLowFloorReservationRequesting) {
                pendingLowFloorReservation = null
            }
        },
        onLowFloorReservationConfirm = {
            pendingLowFloorReservation?.let { reservation ->
                isLowFloorReservationRequesting = true
                coroutineScope.launch {
                    val success =
                        runCatching { requestLowFloorBusReservation(reservation) }
                            .getOrDefault(false)
                    isLowFloorReservationRequesting = false
                    pendingLowFloorReservation = null
                    Toast.makeText(
                        context,
                        if (success) {
                            context.getString(R.string.route_setting_low_floor_reservation_success)
                        } else {
                            context.getString(R.string.route_setting_low_floor_reservation_failure)
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun RouteDetailEntryRoute(
    routeOption: RouteOption,
    hydrateFromNavigation: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onStartNavigation: (RouteNavigationRequest) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberRouteSettingViewModel()
    val navigationViewModel = if (hydrateFromNavigation) rememberNavigationGuidanceViewModel() else null
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingLowFloorReservation by remember { mutableStateOf<LowFloorBusReservation?>(null) }
    var isLowFloorReservationRequesting by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel, navigationViewModel, routeOption, hydrateFromNavigation) {
        val detailRequest =
            if (hydrateFromNavigation) {
                navigationViewModel
                    ?.currentRouteDetailRequest()
                    ?.takeIf { request -> request.selectedRoute.routeOption == routeOption }
            } else {
                null
            }
        if (detailRequest != null) {
            viewModel.bindRouteDetailRequest(detailRequest)
        } else {
            viewModel.onAction(RouteSettingUiAction.RouteOptionSelected(routeOption))
        }
    }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToMap, onStartNavigation) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RouteSettingUiEvent.NavigateBack -> onNavigateBack()
                RouteSettingUiEvent.NavigateToMap -> onNavigateToMap()
                RouteSettingUiEvent.RequestLocationPermission -> Unit
                is RouteSettingUiEvent.NavigateToSearch -> Unit
                is RouteSettingUiEvent.NavigateToRouteDetail -> Unit
                is RouteSettingUiEvent.StartNavigationRequested -> onStartNavigation(event.request)
            }
        }
    }

    RouteDetailScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onCloseClick = onNavigateToMap,
        onStartClick = {
            viewModel.onAction(RouteSettingUiAction.StartNavigationClicked)
        },
        pendingLowFloorReservation = pendingLowFloorReservation,
        isLowFloorReservationRequesting = isLowFloorReservationRequesting,
        onLowFloorReservationClick = { reservation -> pendingLowFloorReservation = reservation },
        onLowFloorReservationDismiss = {
            if (!isLowFloorReservationRequesting) {
                pendingLowFloorReservation = null
            }
        },
        onLowFloorReservationConfirm = {
            pendingLowFloorReservation?.let { reservation ->
                isLowFloorReservationRequesting = true
                coroutineScope.launch {
                    val success =
                        runCatching { requestLowFloorBusReservation(reservation) }
                            .getOrDefault(false)
                    isLowFloorReservationRequesting = false
                    pendingLowFloorReservation = null
                    Toast.makeText(
                        context,
                        if (success) {
                            context.getString(R.string.route_setting_low_floor_reservation_success)
                        } else {
                            context.getString(R.string.route_setting_low_floor_reservation_failure)
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
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
                locationPermissionManager = appContainer.locationPermissionManager,
                placesRepository = appContainer.placesRepository,
                searchRepository = appContainer.searchRepository,
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
