package com.ssafy.e102.eumgil.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ssafy.e102.eumgil.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(
    onLoginCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedProviderRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedProvider = SocialLoginProvider.fromRouteValue(selectedProviderRoute)
    val defaultErrorMessage = stringResource(id = R.string.auth_login_status_error)

    LoginScreen(
        uiState =
            LoginUiState(
                selectedProvider = selectedProvider,
                errorMessage = errorMessage,
            ),
        onProviderClick = onProviderClick@ { provider ->
            if (selectedProviderRoute != null) return@onProviderClick

            selectedProviderRoute = provider.routeValue
            errorMessage = null
            coroutineScope.launch {
                runCatching {
                    delay(LoginMockHandoffDelayMillis)
                    // TODO(S14P31E102-313): Replace this mock handoff with BE token exchange
                    // after provider/token policy is confirmed.
                    onLoginCompleted()
                }.onFailure {
                    errorMessage = defaultErrorMessage
                    selectedProviderRoute = null
                }.onSuccess {
                    selectedProviderRoute = null
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ProfileSetupRoute(modifier: Modifier = Modifier) {
    ProfileSetupScreen(modifier = modifier)
}

private const val LoginMockHandoffDelayMillis = 450L
