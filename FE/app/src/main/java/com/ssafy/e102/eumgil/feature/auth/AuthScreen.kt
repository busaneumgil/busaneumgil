package com.ssafy.e102.eumgil.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    AuthGateScreen(
        screenId = "AUTH-001",
        title = "로그인이 필요합니다",
        description = "부산이음길을 계속 이용하려면 로그인해 주세요.",
        modifier = modifier,
    )
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
