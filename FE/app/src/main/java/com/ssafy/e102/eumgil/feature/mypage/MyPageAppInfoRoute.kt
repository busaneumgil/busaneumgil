package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ssafy.e102.eumgil.R
import kotlinx.coroutines.launch

@Composable
fun MyPageAppInfoRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val preparingMessage = stringResource(id = R.string.my_page_preparing_message)
    val coroutineScope = rememberCoroutineScope()

    MyPageAppInfoScreen(
        snackbarHostState = snackbarHostState,
        onBackClick = onNavigateBack,
        onGuideClick = {
            coroutineScope.launch { snackbarHostState.showSnackbar(preparingMessage) }
        },
        onInquiryClick = {
            coroutineScope.launch { snackbarHostState.showSnackbar(preparingMessage) }
        },
        onPrivacyPolicyClick = {
            coroutineScope.launch { snackbarHostState.showSnackbar(preparingMessage) }
        },
        onServiceTermsClick = {
            coroutineScope.launch { snackbarHostState.showSnackbar(preparingMessage) }
        },
        onWithdrawClick = {
            coroutineScope.launch { snackbarHostState.showSnackbar(preparingMessage) }
        },
        modifier = modifier,
    )
}
