package com.ssafy.e102.eumgil.feature.lowvision

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
import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceDestination
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.core.model.RouteSearchQuery
import com.ssafy.e102.eumgil.core.model.RouteWaypoint
import com.ssafy.e102.eumgil.core.model.toRouteWaypointOrNull
import com.ssafy.e102.eumgil.data.repository.DestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteRepository
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiAction
import com.ssafy.e102.eumgil.feature.navigation.NavigationUiEvent
import com.ssafy.e102.eumgil.feature.navigation.NavigationViewModel
import com.ssafy.e102.eumgil.feature.route.RouteNavigationRequest
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
        remember(appContainer.currentLocationManager, appContainer.bookmarkRepository) {
            NavigationViewModel.provideFactory(
                currentLocationManager = appContainer.currentLocationManager,
                bookmarkRepository = appContainer.bookmarkRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "LowVisionNavigationRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[NavigationViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(appContainer.destinationSelectionRepository.selectedDestination.value) {
        appContainer.routeRepository
            .buildLowVisionNavigationRequest(appContainer.destinationSelectionRepository)
            ?.let(viewModel::bindNavigationRequest)
    }

    LaunchedEffect(viewModel, onNavigateToComplete, onNavigateToBookmark) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                NavigationUiEvent.NavigateToLowVisionHome -> onNavigateToComplete()
                NavigationUiEvent.NavigateToSavedRoute -> onNavigateToBookmark()
                NavigationUiEvent.NavigateBack,
                NavigationUiEvent.NavigateToMap,
                is NavigationUiEvent.SpeakBriefing,
                NavigationUiEvent.StopBriefing,
                is NavigationUiEvent.SetVoiceGuidanceEnabled -> Unit
            }
        }
    }

    LowVisionNavigationScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onTabSelected = onTabSelected,
        modifier = modifier,
    )
}

private suspend fun RouteRepository.buildLowVisionNavigationRequest(
    destinationSelectionRepository: DestinationSelectionRepository,
): RouteNavigationRequest? {
    val destination = destinationSelectionRepository.selectedDestination.value.toLowVisionRouteWaypoint()
    val searchData =
        getRouteSearchData(
            RouteSearchQuery(
                origin = LOW_VISION_DEFAULT_ORIGIN,
                destination = destination,
                requestedOptions = listOf(RouteOption.SAFE),
            ),
        )
    val selectedRoute = searchData.primaryRoute ?: searchData.routes.firstOrNull() ?: return null
    return RouteNavigationRequest(
        origin = searchData.result.origin,
        destination = searchData.result.destination,
        selectedRoute = selectedRoute,
        source = searchData.source,
    )
}

private fun PlaceDestination?.toLowVisionRouteWaypoint(): RouteWaypoint =
    this?.toRouteWaypointOrNull() ?: LOW_VISION_DEFAULT_DESTINATION

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

private val LOW_VISION_DEFAULT_ORIGIN =
    RouteWaypoint(
        name = "현재 위치",
        address = "데모 출발지",
        coordinate = GeoCoordinate(latitude = 35.1796, longitude = 129.0756),
    )

private val LOW_VISION_DEFAULT_DESTINATION =
    RouteWaypoint(
        name = "부산역",
        address = "부산 동구 중앙대로 206",
        coordinate = GeoCoordinate(latitude = 35.1151, longitude = 129.0414),
    )
