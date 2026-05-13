package com.ssafy.e102.eumgil.feature.report

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val view = LocalView.current

    LaunchedEffect(viewModel) {
        // 탭 재진입 시 완료 화면이면 자동으로 새 제보 시작 상태로 초기화 (T10).
        // 작성 중·실패 상태는 보존되어야 하므로 ViewModel에서 분기 처리한다.
        viewModel.onAction(ReportUiAction.TabReentered)
    }

    LaunchedEffect(viewModel, onNavigateBack, onNavigateToReportHistory, onNavigateToMap) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ReportUiEvent.NavigateBack -> onNavigateBack()
                ReportUiEvent.NavigateToReportHistory -> onNavigateToReportHistory()
                ReportUiEvent.NavigateToMap -> onNavigateToMap()
                is ReportUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ReportUiEvent.AnnounceForAccessibility -> {
                    // View.announceForAccessibility는 API 33+에서 deprecated이지만
                    // 모든 API 레벨에서 동작하며 Compose에는 1회성 announcement 공식 API가 없어 사용.
                    @Suppress("DEPRECATION")
                    view.announceForAccessibility(event.message)
                }
                ReportUiEvent.ScrollToFirstError -> scrollState.animateScrollTo(0)
                ReportUiEvent.OpenLocationPicker,
                ReportUiEvent.OpenPhotoPicker,
                ReportUiEvent.RequestLocationPermission,
                ReportUiEvent.ShowDraftDiscardDialog,
                is ReportUiEvent.NavigateToReportComplete -> Unit
                // OpenLocationPicker / OpenPhotoPicker / RequestLocationPermission: Story 2·3 범위
                // ShowDraftDiscardDialog: Task 1.2 범위
                // NavigateToReportComplete: 현재 화면 내 step 전환과 중복이라 무시 (후속 정리 대상)
            }
        }
    }

    ReportScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        scrollState = scrollState,
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
