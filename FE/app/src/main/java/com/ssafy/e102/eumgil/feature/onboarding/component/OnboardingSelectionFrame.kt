package com.ssafy.e102.eumgil.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

@Composable
fun OnboardingSelectionFrame(
    currentStep: Int,
    totalSteps: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    headerTitleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    headerTitleFontWeight: FontWeight = FontWeight.SemiBold,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = EumSpacing.medium, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
    ) {
        OnboardingProgressHeader(
            currentStep = currentStep,
            totalSteps = totalSteps,
            title = title,
            description = description,
            titleStyle = headerTitleStyle,
            titleFontWeight = headerTitleFontWeight,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            content = content,
        )
    }
}
