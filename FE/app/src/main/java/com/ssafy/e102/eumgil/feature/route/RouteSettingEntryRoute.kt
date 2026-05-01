package com.ssafy.e102.eumgil.feature.route

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.model.RouteOption
import kotlinx.coroutines.flow.collect

@Composable
fun RouteSettingEntryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToRouteDetail: (RouteOption) -> Unit = {},
    onStartNavigation: (RouteNavigationRequest) -> Unit = {},
    autoStartNavigation: Boolean = false,
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
            RouteSettingViewModel.provideFactory(
                routeRepository = appContainer.routeRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "RouteSettingEntryRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[RouteSettingViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToRouteDetail, onStartNavigation) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RouteSettingUiEvent.NavigateBack -> onNavigateBack()
                is RouteSettingUiEvent.NavigateToRouteDetail -> onNavigateToRouteDetail(event.routeOption)
                is RouteSettingUiEvent.StartNavigationRequested -> onStartNavigation(event.request)
            }
        }
    }

    LaunchedEffect(viewModel, autoStartNavigation, uiState.isStartEnabled, uiState.ctaAcknowledged) {
        if (autoStartNavigation && uiState.isStartEnabled && !uiState.ctaAcknowledged) {
            viewModel.onAction(RouteSettingUiAction.StartNavigationClicked)
        }
    }

    RouteSettingScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
