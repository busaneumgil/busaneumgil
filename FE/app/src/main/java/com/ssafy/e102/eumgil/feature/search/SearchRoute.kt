package com.ssafy.e102.eumgil.feature.search

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.tts.AndroidTextToSpeechController
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import kotlinx.coroutines.flow.collect

@Composable
fun SearchEntryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    onNavigateToRouteBriefing: () -> Unit,
    initialEditingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
    preserveEntryStateOnReentry: Boolean = false,
    modifier: Modifier = Modifier,
) {
    SearchRouteContent(
        destination = SearchScreenDestination.Entry,
        initialQuery = null,
        initialEditingTarget = initialEditingTarget,
        preserveEntryStateOnReentry = preserveEntryStateOnReentry,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToVoiceInput = onNavigateToVoiceInput,
        onNavigateToRouteSetting = onNavigateToRouteSetting,
        onNavigateToRouteBriefing = onNavigateToRouteBriefing,
        modifier = modifier,
    )
}

@Composable
fun SearchResultsRoute(
    initialQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    onNavigateToRouteBriefing: () -> Unit,
    initialEditingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
    modifier: Modifier = Modifier,
) {
    SearchRouteContent(
        destination = SearchScreenDestination.Results,
        initialQuery = initialQuery,
        initialEditingTarget = initialEditingTarget,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToVoiceInput = onNavigateToVoiceInput,
        onNavigateToRouteSetting = onNavigateToRouteSetting,
        onNavigateToRouteBriefing = onNavigateToRouteBriefing,
        modifier = modifier,
    )
}

@Composable
fun SearchVoiceInputRoute(
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    initialEditingTarget: RouteEditingTarget = RouteEditingTarget.DESTINATION,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val searchViewModel = rememberSearchViewModel()
    val sttViewModel: SearchVoiceInputViewModel = viewModel()
    val ttsController = remember(context.applicationContext) {
        AndroidTextToSpeechController(context = context.applicationContext)
    }
    val ttsState by ttsController.state.collectAsStateWithLifecycle()
    val voiceInputPrompt = stringResource(R.string.voice_input_prompt)

    LaunchedEffect(searchViewModel, sttViewModel) {
        // Route entry auto-start must not depend on SearchViewModel UI-event collection order.
        searchViewModel.onAction(SearchUiAction.VoiceRouteEntered)
        sttViewModel.startListening()
    }

    // -1: 아직 speak()를 한 번도 호출하지 않은 상태.
    // completedUtteranceCount >= 0 조건을 함께 쓰면 앱 진입 시 spurious 트리거 방지.
    val lastCompletedCount = remember { mutableIntStateOf(-1) }

    // TTS 완료 감지 → beginRecording().
    // lastCompletedCount >= 0 이어야 실제로 speak()를 호출한 이후임을 보장한다.
    LaunchedEffect(ttsState.completedUtteranceCount) {
        if (lastCompletedCount.intValue >= 0 &&
            ttsState.completedUtteranceCount > lastCompletedCount.intValue
        ) {
            sttViewModel.beginRecording()
        }
        lastCompletedCount.intValue = ttsState.completedUtteranceCount
    }

    LaunchedEffect(sttViewModel) {
        sttViewModel.uiEvent.collect { event ->
            when (event) {
                is SearchVoiceInputEvent.TranscriptReady ->
                    searchViewModel.onAction(
                        SearchUiAction.VoiceTranscriptReceived(
                            transcript = event.recognizedText,
                            searchQuery = event.searchQuery,
                        ),
                    )
                SearchVoiceInputEvent.TranscriptEmpty -> {
                    searchViewModel.onAction(SearchUiAction.VoiceCaptureEmpty)
                }
                is SearchVoiceInputEvent.SpeakError -> ttsController.speak(event.text)
                SearchVoiceInputEvent.ReadyToRecord -> {
                    // AndroidTextToSpeechController가 내부적으로 pendingText를 처리하므로
                    // 엔진 초기화 전에 호출해도 초기화 완료 후 자동 재생됨
                    lastCompletedCount.intValue = ttsState.completedUtteranceCount
                    ttsController.speak(voiceInputPrompt)
                }
            }
        }
    }

    DisposableEffect(ttsController) {
        onDispose {
            ttsController.stop()
            ttsController.shutdown()
        }
    }

    SearchRouteContent(
        destination = SearchScreenDestination.VoiceInput,
        initialQuery = null,
        initialEditingTarget = initialEditingTarget,
        onNavigateBack = onNavigateBack,
        onNavigateToResults = onNavigateToResults,
        onNavigateToVoiceInput = {},
        onNavigateToRouteSetting = {},
        onNavigateToRouteBriefing = {},
        onStartVoiceCapture = { sttViewModel.startListening() },
        onStopVoiceCapture = { sttViewModel.stopListening() },
        modifier = modifier,
    )
}

@Composable
private fun SearchRouteContent(
    destination: SearchScreenDestination,
    initialQuery: String?,
    initialEditingTarget: RouteEditingTarget,
    preserveEntryStateOnReentry: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (String, RouteEditingTarget) -> Unit,
    onNavigateToVoiceInput: () -> Unit,
    onNavigateToRouteSetting: () -> Unit,
    onNavigateToRouteBriefing: () -> Unit = {},
    onStartVoiceCapture: () -> Unit = {},
    onStopVoiceCapture: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel = rememberSearchViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, initialEditingTarget) {
        viewModel.onAction(SearchUiAction.EditingTargetConfigured(editingTarget = initialEditingTarget))
    }

    LaunchedEffect(viewModel, destination, preserveEntryStateOnReentry) {
        if (destination == SearchScreenDestination.Entry) {
            viewModel.onAction(
                SearchUiAction.EntryRouteEntered(
                    preserveState = preserveEntryStateOnReentry,
                ),
            )
        }
    }

    LaunchedEffect(viewModel, initialQuery) {
        if (initialQuery != null) {
            viewModel.onAction(SearchUiAction.ResultsRouteEntered(query = initialQuery))
        }
    }

    LaunchedEffect(
        viewModel,
        onNavigateBack,
        onNavigateToResults,
        onNavigateToVoiceInput,
        onNavigateToRouteSetting,
        onNavigateToRouteBriefing,
        onStartVoiceCapture,
        onStopVoiceCapture,
    ) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SearchUiEvent.NavigateBack -> onNavigateBack()
                SearchUiEvent.NavigateToVoiceInput -> onNavigateToVoiceInput()
                is SearchUiEvent.NavigateToResults -> onNavigateToResults(event.query, event.editingTarget)
                SearchUiEvent.StartVoiceCapture -> onStartVoiceCapture()
                SearchUiEvent.StopVoiceCapture -> onStopVoiceCapture()
                SearchUiEvent.NavigateToRouteSetting -> onNavigateToRouteSetting()
                SearchUiEvent.NavigateToRouteBriefing -> onNavigateToRouteBriefing()
                SearchUiEvent.NavigateToLowVisionBookmark -> Unit
            }
        }
    }

    SearchScreen(
        destination = destination,
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
private fun rememberSearchViewModel(): SearchViewModel {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val viewModelFactory =
        remember(appContainer) {
            SearchViewModel.provideFactory(
                searchRepository = appContainer.searchRepository,
                bookmarkRepository = appContainer.bookmarkRepository,
                destinationSelectionRepository = appContainer.destinationSelectionRepository,
                placesRepository = appContainer.placesRepository,
            )
        }

    return remember(activity, viewModelFactory) {
        val owner = checkNotNull(activity) { "SearchRoute requires a ComponentActivity host." }
        ViewModelProvider(owner, viewModelFactory)[SearchViewModel::class.java]
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
