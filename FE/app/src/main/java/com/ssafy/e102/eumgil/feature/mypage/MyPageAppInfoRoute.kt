package com.ssafy.e102.eumgil.feature.mypage

import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.app.BusanEumgilApp
import com.ssafy.e102.eumgil.core.config.AppEnvironment
import com.ssafy.e102.eumgil.data.repository.provideAccountWithdrawalRepository
import kotlinx.coroutines.launch

private const val SERVICE_TERMS_URL =
    "https://www.notion.so/ryuwon-project/350a58d49be680ab9931f226486dac58"
private const val PRIVACY_POLICY_URL =
    "https://www.notion.so/ryuwon-project/350a58d49be68063bbd1f633be85badb"

@Composable
fun MyPageAppInfoRoute(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer =
        remember(context.applicationContext) {
            (context.applicationContext as BusanEumgilApp).appContainer
        }
    val activity = remember(context) { context.findComponentActivity() }
    val accountWithdrawalRepository =
        remember(appContainer) {
            provideAccountWithdrawalRepository(
                baseUrl = AppEnvironment.baseUrl,
                authSessionRepository = appContainer.authSessionRepository,
                initSettingsRepository = appContainer.settingsRepository,
                bookmarkDao = appContainer.localDatabase.bookmarkDao(),
                isMockMode = AppEnvironment.isMockMode,
            )
        }
    val viewModelFactory =
        remember(accountWithdrawalRepository) {
            MyPageAppInfoViewModel.provideFactory(accountWithdrawalRepository = accountWithdrawalRepository)
        }
    val viewModel =
        remember(activity, viewModelFactory) {
            val owner = checkNotNull(activity) { "MyPageAppInfoRoute requires a ComponentActivity host." }
            ViewModelProvider(owner, viewModelFactory)[MyPageAppInfoViewModel::class.java]
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val preparingMessage = stringResource(id = R.string.my_page_preparing_message)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel, snackbarHostState, onNavigateToLogin) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                MyPageAppInfoUiEvent.NavigateToLogin -> onNavigateToLogin()
                is MyPageAppInfoUiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    MyPageAppInfoScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onNavigateBack,
        onGuideClick = onNavigateToGuide,
        onInquiryClick = {
            coroutineScope.launch { snackbarHostState.showSnackbar(preparingMessage) }
        },
        onPrivacyPolicyClick = {
            context.startActivity(createPrivacyPolicyIntent())
        },
        onServiceTermsClick = {
            context.startActivity(createServiceTermsIntent())
        },
        onWithdrawClick = {
            viewModel.onAction(MyPageAppInfoUiAction.WithdrawClicked)
        },
        modifier = modifier,
    )
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

internal fun createServiceTermsIntent(): Intent =
    createExternalLinkIntent(SERVICE_TERMS_URL)

internal fun createPrivacyPolicyIntent(): Intent =
    createExternalLinkIntent(PRIVACY_POLICY_URL)

private fun createExternalLinkIntent(url: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
