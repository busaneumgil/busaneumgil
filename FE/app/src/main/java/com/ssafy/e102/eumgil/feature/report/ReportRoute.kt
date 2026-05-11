package com.ssafy.e102.eumgil.feature.report

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
import kotlinx.coroutines.flow.collect

@Composable
fun ReportRoute(
    onNavigateBack: () -> Unit,
    onNavigateToReportHistory: () -> Unit,
    onNavigateToMap: () -> Unit,
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
            ReportViewModel.provideFactory(
                reportRepository = appContainer.reportRepository,
            )
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "ReportRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[ReportViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        // 탭 재진입 시 완료 화면이면 자동으로 새 제보 시작 상태로 초기화 (T10).
        // 작성 중·실패 상태는 보존되어야 하므로 ViewModel에서 분기 처리한다.
        viewModel.onAction(ReportUiAction.TabReentered)
    }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToReportHistory, onNavigateToMap) {
        viewModel.uiEvent.collect { event ->
            handleReportUiEvent(
                event = event,
                onNavigateBack = onNavigateBack,
                onNavigateToReportHistory = onNavigateToReportHistory,
                onNavigateToMap = onNavigateToMap,
            )
        }
    }

    ReportScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

private fun handleReportUiEvent(
    event: ReportUiEvent,
    onNavigateBack: () -> Unit,
    onNavigateToReportHistory: () -> Unit,
    onNavigateToMap: () -> Unit,
) {
    when (event) {
        ReportUiEvent.NavigateBack -> onNavigateBack()
        ReportUiEvent.NavigateToReportHistory -> onNavigateToReportHistory()
        ReportUiEvent.NavigateToMap -> onNavigateToMap()
        ReportUiEvent.OpenLocationPicker,
        ReportUiEvent.OpenPhotoPicker,
        ReportUiEvent.RequestLocationPermission,
        ReportUiEvent.ScrollToFirstError,
        ReportUiEvent.ShowDraftDiscardDialog,
        is ReportUiEvent.AnnounceForAccessibility,
        is ReportUiEvent.NavigateToReportComplete,
        is ReportUiEvent.ShowSnackbar -> Unit
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
