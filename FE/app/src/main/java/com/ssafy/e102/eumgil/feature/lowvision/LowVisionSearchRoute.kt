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
import com.ssafy.e102.eumgil.core.location.CurrentLocationManager
import com.ssafy.e102.eumgil.core.location.LocationSnapshot
import com.ssafy.e102.eumgil.core.location.isFreshCurrentLocation
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import com.ssafy.e102.eumgil.feature.search.SearchUiAction
import com.ssafy.e102.eumgil.feature.search.SearchUiEvent
import com.ssafy.e102.eumgil.feature.search.SearchViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun LowVisionSearchRoute(
    initialQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    onNavigateToRouteBriefing: () -> Unit,
    onNavigateToBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: String? = null,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    var retainedCurrentLocationSnapshot by remember { mutableStateOf<LocationSnapshot?>(null) }
    val lowVisionSearchRepository =
        remember(appContainer.searchRepository, appContainer.placesRepository, appContainer.currentLocationManager) {
            LowVisionSearchRepository(
                delegate = appContainer.searchRepository,
                placesRepository = appContainer.placesRepository,
                currentLocationProvider = {
                    appContainer.currentLocationManager.latestLocation.value ?: retainedCurrentLocationSnapshot
                },
            )
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory =
        remember(appContainer, lowVisionSearchRepository) {
            SearchViewModel.provideFactory(
                searchRepository = lowVisionSearchRepository,
                bookmarkRepository = appContainer.bookmarkRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
                destinationPreviewRepository = appContainer.destinationPreviewRepository,
                placesRepository = appContainer.placesRepository,
                currentLocationManager = appContainer.currentLocationManager,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "LowVisionSearchRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory).get(LOW_VISION_SEARCH_VIEW_MODEL_KEY, SearchViewModel::class.java)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLocationSnapshot by
        appContainer.currentLocationManager.latestLocation.collectAsStateWithLifecycle()
    var hasSubmittedInitialSearch by remember(initialQuery, categoryLabel) { mutableStateOf(false) }
    var isPreparingInitialSearch by remember(initialQuery, categoryLabel) {
        mutableStateOf(!categoryLabel.isNullOrBlank())
    }

    LaunchedEffect(appContainer.currentLocationManager) {
        retainedCurrentLocationSnapshot =
            appContainer.currentLocationManager.latestLocation.value ?: retainedCurrentLocationSnapshot
        appContainer.currentLocationManager.startLocationUpdates()
        appContainer.currentLocationManager.refreshLatestLocation()
    }

    LaunchedEffect(currentLocationSnapshot) {
        if (currentLocationSnapshot != null) {
            retainedCurrentLocationSnapshot = currentLocationSnapshot
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onAction(SearchUiAction.EditingTargetConfigured(editingTarget = RouteEditingTarget.DESTINATION))
    }

    LaunchedEffect(viewModel, initialQuery, categoryLabel, currentLocationSnapshot, hasSubmittedInitialSearch) {
        if (hasSubmittedInitialSearch) return@LaunchedEffect
        if (!categoryLabel.isNullOrBlank()) {
            currentLocationSnapshot?.takeIf { snapshot -> snapshot.isFreshCurrentLocation() }
                ?: awaitLowVisionSearchLocationSnapshot(
                    currentLocationManager = appContainer.currentLocationManager,
                    immediateSnapshot = currentLocationSnapshot ?: retainedCurrentLocationSnapshot,
                )
        }
        viewModel.onAction(SearchUiAction.ResultsRouteEntered(query = initialQuery))
        isPreparingInitialSearch = false
        hasSubmittedInitialSearch = true
    }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToRouteSetting) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SearchUiEvent.NavigateBack -> onNavigateBack()
                is SearchUiEvent.NavigateToResults -> Unit
                SearchUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
                SearchUiEvent.NavigateToRouteBriefing -> onNavigateToRouteBriefing()
                SearchUiEvent.NavigateToMapPreview -> Unit
                SearchUiEvent.NavigateToLowVisionBookmark -> onNavigateToBookmark()
                SearchUiEvent.NavigateToVoiceInput -> Unit
                SearchUiEvent.StartVoiceCapture -> Unit
                SearchUiEvent.StopVoiceCapture -> Unit
            }
        }
    }

    DisposableEffect(appContainer.currentLocationManager) {
        onDispose {
            appContainer.currentLocationManager.stopLocationUpdates()
        }
    }

    LowVisionFontTheme {
        LowVisionSearchScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            modifier = modifier,
            categoryLabel = categoryLabel,
            isPreparingLocation = isPreparingInitialSearch,
        )
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

private suspend fun awaitLowVisionSearchLocationSnapshot(
    currentLocationManager: CurrentLocationManager,
    immediateSnapshot: LocationSnapshot?,
): LocationSnapshot? {
    val freshImmediateSnapshot =
        immediateSnapshot?.takeIf { snapshot ->
            snapshot.isFreshCurrentLocation() &&
                snapshot.recordedAtEpochMillis >= System.currentTimeMillis() - LOW_VISION_SEARCH_ACCEPTABLE_LOCATION_AGE_MILLIS
        }
    if (freshImmediateSnapshot != null) return freshImmediateSnapshot

    val baselineRecordedAt = immediateSnapshot?.recordedAtEpochMillis ?: 0L
    return withTimeoutOrNull(LOW_VISION_SEARCH_LOCATION_TIMEOUT_MILLIS) {
        currentLocationManager.latestLocation
            .filterNotNull()
            .first { snapshot ->
                snapshot.isFreshCurrentLocation() && snapshot.recordedAtEpochMillis > baselineRecordedAt
            }
    }
}

private const val LOW_VISION_SEARCH_VIEW_MODEL_KEY: String = "low-vision-search-view-model"
private const val LOW_VISION_SEARCH_LOCATION_TIMEOUT_MILLIS: Long = 3_000L
private const val LOW_VISION_SEARCH_ACCEPTABLE_LOCATION_AGE_MILLIS: Long = 15_000L
