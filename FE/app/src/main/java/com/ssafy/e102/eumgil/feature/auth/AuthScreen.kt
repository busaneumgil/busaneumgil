package com.ssafy.e102.eumgil.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
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

        Image(
            painter = painterResource(id = R.drawable.auth_login_skyline),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(260.dp)
                    .alpha(0.92f),
            contentScale = ContentScale.FillWidth,
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
            SocialLoginPanel(
                uiState = uiState,
                onAction = onAction,
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
        verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier.size(132.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = stringResource(id = R.string.auth_login_service_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.auth_login_service_english_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(EumSpacing.small))
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Text(
                text = stringResource(id = R.string.auth_login_social_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            LoginStatusText(uiState = uiState)
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
}

@Composable
private fun LoginStatusText(
    uiState: AuthUiState,
    modifier: Modifier = Modifier,
) {
    val selectedProviderName =
        uiState.providers
            .firstOrNull { provider -> provider.key == uiState.loadingProviderKey }
            ?.let { provider ->
                stringResource(id = provider.providerNameRes)
            }
    val errorMessage = uiState.errorMessage
    val statusText =
        when {
            errorMessage != null -> errorMessage
            selectedProviderName != null ->
                stringResource(
                    id = R.string.auth_login_status_loading,
                    selectedProviderName,
                )
            else -> stringResource(id = R.string.auth_login_status_idle)
        }

    val statusModifier =
        if (uiState.errorMessage != null) {
            modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                error(statusText)
            }
        } else {
            modifier
        }

    Text(
        text = statusText,
        modifier = statusModifier,
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (uiState.errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
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
    val disabledContainerColor =
        if (isSelectedLoading) {
            containerColor
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val disabledContentColor =
        if (isSelectedLoading) {
            contentColor
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        }

    Button(
        onClick = onClick,
        enabled = !isAnyLoading,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SocialLoginButtonHeight)
                .semantics {
                    stateDescription = buttonState
                },
        shape = RoundedCornerShape(EumRadius.medium),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            ),
        border = provider.buttonBorder(isSelectedLoading = isSelectedLoading),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            SocialProviderMark(
                provider = provider,
                isDisabled = isAnyLoading && !isSelectedLoading,
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (isSelectedLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.24f),
                )
            } else {
                Spacer(modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SocialProviderMark(
    provider: AuthLoginProviderUiModel,
    isDisabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isDisabled) {
            MaterialTheme.colorScheme.surface
        } else {
            provider.markContainerColor()
        }
    val contentColor =
        if (isDisabled) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        } else {
            provider.markContentColor()
        }

    Surface(
        modifier =
            modifier
                .size(36.dp)
                .clearAndSetSemantics { },
        shape = CircleShape,
        color = containerColor,
        border = provider.markBorder(isDisabled = isDisabled),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = provider.mark,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
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

private fun AuthLoginProviderUiModel.markContainerColor(): Color =
    when (key) {
        AuthLoginProviderUiKeys.KAKAO -> Color(0xFF2D1600)
        else -> Color.White
    }

private fun AuthLoginProviderUiModel.markContentColor(): Color =
    when (key) {
        AuthLoginProviderUiKeys.GOOGLE -> GoogleBlue
        AuthLoginProviderUiKeys.NAVER -> NaverGreen
        AuthLoginProviderUiKeys.KAKAO -> KakaoYellow
        else -> Color(0xFF111827)
    }

private fun AuthLoginProviderUiModel.buttonBorder(isSelectedLoading: Boolean): BorderStroke? =
    when (key) {
        AuthLoginProviderUiKeys.GOOGLE ->
            BorderStroke(
                width = if (isSelectedLoading) 2.dp else 1.dp,
                color = if (isSelectedLoading) GoogleBlue else Color(0xFFD1D5DB),
            )
        else -> null
    }

private fun AuthLoginProviderUiModel.markBorder(isDisabled: Boolean): BorderStroke? =
    when {
        isDisabled -> BorderStroke(width = 1.dp, color = Color(0xFFD1D5DB))
        key == AuthLoginProviderUiKeys.GOOGLE -> BorderStroke(width = 1.dp, color = Color(0xFFD1D5DB))
        else -> null
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
private val SocialLoginButtonHeight = 56.dp
