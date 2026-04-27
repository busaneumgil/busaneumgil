package com.ssafy.e102.eumgil.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun LoginScreen(
    uiState: AuthUiState = AuthUiState(),
    onAction: (AuthUiAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(SkyBlue, Color.White),
                        ),
                ),
    ) {
        Image(
            painter = painterResource(id = R.drawable.auth_login_clouds),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(300.dp)
                    .alpha(0.88f),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = EumSpacing.large, vertical = EumSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(EumSpacing.xLarge))
            LoginHero()
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.auth_login_skyline),
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .alpha(0.92f),
                alignment = Alignment.BottomCenter,
                contentScale = ContentScale.FillWidth,
            )
            Spacer(modifier = Modifier.height(10.dp))
            SocialLoginPanel(
                uiState = uiState,
                onAction = onAction,
                modifier =
                    Modifier
                        .fillMaxWidth()
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
fun ProfileSetupScreen(modifier: Modifier = Modifier) {
    AuthGateScreen(
        screenId = "AUTH-002",
        title = "프로필 설정이 필요합니다",
        description = "지도 홈으로 이동하기 전에 필수 프로필을 완료해 주세요.",
        modifier = modifier,
    )
}

@Composable
private fun LoginHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = null,
            modifier =
                Modifier
                    .width(164.dp)
                    .height(92.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = stringResource(id = R.string.auth_login_service_name),
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
                ),
            color = EumPrimary600,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.auth_login_service_english_name),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                ),
            color = EumPrimary600,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.auth_login_service_tagline),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SocialLoginPanel(
    uiState: AuthUiState,
    onAction: (AuthUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        uiState.providers.forEach { provider ->
            SocialLoginButton(
                provider = provider,
                isAnyLoading = uiState.isLoading,
                isSelectedLoading = uiState.loadingProviderKey == provider.key,
                onClick = {
                    onAction(AuthUiAction.SocialLoginClicked(providerKey = provider.key))
                },
            )
        }
    }
}

@Composable
private fun SocialLoginButton(
    provider: AuthLoginProviderUiModel,
    isAnyLoading: Boolean,
    isSelectedLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val providerName = stringResource(id = provider.providerNameRes)
    val label =
        if (isSelectedLoading) {
            stringResource(id = R.string.auth_login_provider_loading, providerName)
        } else {
            stringResource(id = provider.actionLabelRes)
        }
    val buttonState =
        when {
            isSelectedLoading -> stringResource(id = R.string.auth_login_state_loading)
            isAnyLoading -> stringResource(id = R.string.auth_login_state_disabled)
            else -> stringResource(id = R.string.auth_login_state_enabled)
        }
    val containerColor = provider.containerColor()
    val contentColor = provider.contentColor()

    Button(
        onClick = onClick,
        enabled = !isAnyLoading,
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics {
                    stateDescription = buttonState
                },
        shape = MaterialTheme.shapes.large,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.6f),
                disabledContentColor = contentColor.copy(alpha = 0.6f),
            ),
        border = provider.buttonBorder(isSelectedLoading = isSelectedLoading),
        contentPadding = PaddingValues(0.dp),
    ) {
        if (isSelectedLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = provider.iconRes()),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .padding(start = 20.dp)
                            .size(20.dp)
                            .align(Alignment.CenterStart),
                    contentScale = ContentScale.Fit,
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun AuthLoginProviderUiModel.containerColor(): Color =
    when (key) {
        AuthLoginProviderUiKeys.GOOGLE -> Color.White
        AuthLoginProviderUiKeys.NAVER -> NaverGreen
        AuthLoginProviderUiKeys.KAKAO -> KakaoYellow
        else -> Color.White
    }

private fun AuthLoginProviderUiModel.contentColor(): Color =
    when (key) {
        AuthLoginProviderUiKeys.NAVER -> Color.White
        else -> Color(0xFF111827)
    }

private fun AuthLoginProviderUiModel.buttonBorder(isSelectedLoading: Boolean): BorderStroke? =
    when (key) {
        AuthLoginProviderUiKeys.GOOGLE ->
            BorderStroke(
                width = 1.dp,
                color = if (isSelectedLoading) GoogleBlue else Color(0xFFD1D5DB),
            )
        else -> null
    }

private fun AuthLoginProviderUiModel.iconRes(): Int =
    when (key) {
        AuthLoginProviderUiKeys.GOOGLE -> R.drawable.ic_logo_google
        AuthLoginProviderUiKeys.NAVER -> R.drawable.ic_logo_naver
        AuthLoginProviderUiKeys.KAKAO -> R.drawable.ic_logo_kakao
        else -> R.drawable.ic_logo_google
    }

@Composable
private fun AuthGateScreen(
    screenId: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(EumSpacing.large),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = screenId,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private val SkyBlue = Color(0xFFEAF6FF)
private val GoogleBlue = Color(0xFF4285F4)
private val NaverGreen = Color(0xFF03C75A)
private val KakaoYellow = Color(0xFFFFE500)
