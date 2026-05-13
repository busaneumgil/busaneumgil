package com.ssafy.e102.eumgil.feature.report

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // ViewModel이 `ShowDraftDiscardDialog`를 emit하면 pendingType이 채워지고 AlertDialog가 노출된다.
    // 사용자가 어느 한 선택지를 누르거나 다이얼로그 바깥을 탭하면 다시 null로 초기화한다.
    var draftConflictPendingType: ReportType? by remember { mutableStateOf(null) }

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
                is ReportUiEvent.ShowDraftDiscardDialog -> {
                    draftConflictPendingType = event.pendingType
                }
                ReportUiEvent.OpenLocationPicker,
                ReportUiEvent.OpenPhotoPicker,
                ReportUiEvent.RequestLocationPermission,
                is ReportUiEvent.NavigateToReportComplete -> Unit
                // OpenLocationPicker / OpenPhotoPicker / RequestLocationPermission: Story 2·3 범위
                // NavigateToReportComplete: 현재 화면 내 step 전환과 중복이라 무시 (후속 정리 대상)
            }
        }
    }

    val pendingType = draftConflictPendingType
    if (pendingType != null) {
        ReportDraftConflictDialog(
            onDiscardAndStartNew = {
                viewModel.onAction(ReportUiAction.DiscardDraftAndStartNew(pendingType))
                draftConflictPendingType = null
            },
            onResume = {
                viewModel.onAction(ReportUiAction.ResumeDraftFromDialog)
                draftConflictPendingType = null
            },
            onDismiss = {
                // 다이얼로그 바깥 탭 / 시스템 back: 아무 변경 없이 닫는다.
                // 사용자는 BottomSheet나 type 카드 재선택으로 다시 의사결정할 수 있다.
                draftConflictPendingType = null
            },
        )
    }

    ReportScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        scrollState = scrollState,
        modifier = modifier,
    )
}

@Composable
private fun ReportDraftConflictDialog(
    onDiscardAndStartNew: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "기존 작성 내용") },
        text = {
            Text(
                text = "기존에 작성하던 내용이 있습니다. 삭제하고 새로 작성하시겠습니까?",
            )
        },
        confirmButton = {
            TextButton(onClick = onDiscardAndStartNew) {
                Text(text = "삭제하고 새로 작성")
            }
        },
        dismissButton = {
            TextButton(onClick = onResume) {
                Text(text = "이어서 작성")
            }
        },
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
