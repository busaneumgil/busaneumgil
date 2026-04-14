package com.ssafy.e102.eumgil.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingSelectionCard
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingStepAction
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingStepScaffold

@Composable
fun DisabilityTypeScreen(
    uiState: DisabilityTypeUiState,
    onTypeSelected: (DisabilityType) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 1,
        totalSteps = 3,
        title = stringResource(id = R.string.onboarding_type_screen_title),
        description = stringResource(id = R.string.onboarding_type_screen_description),
        primaryActionLabel = stringResource(id = R.string.action_next_step),
        primaryActionEnabled = uiState.selectedType != null,
        onPrimaryActionClick = onNextClick,
        modifier = modifier,
    ) {
        DisabilityType.entries.forEach { disabilityType ->
            OnboardingSelectionCard(
                title = stringResource(id = disabilityType.titleRes),
                description = stringResource(id = disabilityType.descriptionRes),
                selected = uiState.selectedType == disabilityType,
                onClick = { onTypeSelected(disabilityType) },
            )
        }

        uiState.selectedType?.let { disabilityType ->
            OnboardingModeCard(
                title = stringResource(id = disabilityType.modeTitleRes),
                description = stringResource(id = disabilityType.modeDescriptionRes),
                highlightVisualMode = disabilityType.supportsVoiceGuide,
            )
        }
    }
}

@Composable
fun DisabilityLevelScreen(
    uiState: DisabilityLevelUiState,
    onLevelSelected: (DisabilityLevel) -> Unit,
    onToggleVoiceGuide: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 2,
        totalSteps = 3,
        title = stringResource(id = R.string.onboarding_level_screen_title),
        description = stringResource(id = R.string.onboarding_level_screen_description),
        primaryActionLabel = stringResource(id = R.string.action_next_step),
        primaryActionEnabled = uiState.selectedLevel != null,
        onPrimaryActionClick = onNextClick,
        topAction =
            if (uiState.disabilityType.supportsVoiceGuide) {
                OnboardingStepAction(
                    label =
                        if (uiState.isVoiceGuideExpanded) {
                            stringResource(id = R.string.onboarding_voice_guide_hide)
                        } else {
                            stringResource(id = R.string.onboarding_voice_guide_show)
                        },
                    highlighted = uiState.isVoiceGuideExpanded,
                    onClick = onToggleVoiceGuide,
                )
            } else {
                null
            },
        modifier = modifier,
    ) {
        OnboardingSummaryCard(
            label = stringResource(id = R.string.onboarding_selected_type_label),
            value = stringResource(id = uiState.disabilityType.titleRes),
        )

        if (uiState.disabilityType.supportsVoiceGuide && uiState.isVoiceGuideExpanded) {
            OnboardingModeCard(
                title = stringResource(id = R.string.onboarding_voice_guide_title),
                description = stringResource(id = R.string.onboarding_voice_guide_description),
                highlightVisualMode = true,
            )
        }

        DisabilityLevel.entries.forEach { disabilityLevel ->
            OnboardingSelectionCard(
                title = stringResource(id = disabilityLevel.titleRes),
                description = stringResource(id = disabilityLevel.descriptionRes(uiState.disabilityType)),
                selected = uiState.selectedLevel == disabilityLevel,
                onClick = { onLevelSelected(disabilityLevel) },
            )
        }
    }
}

@Composable
fun LocationTermsPlaceholderScreen(
    uiState: LocationTermsPlaceholderUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 3,
        totalSteps = 3,
        title = stringResource(id = R.string.onboarding_terms_placeholder_title),
        description = stringResource(id = R.string.onboarding_terms_placeholder_description),
        primaryActionLabel = stringResource(id = R.string.action_go_back_previous_step),
        primaryActionEnabled = true,
        onPrimaryActionClick = onBackClick,
        modifier = modifier,
    ) {
        OnboardingSummaryCard(
            label = stringResource(id = R.string.onboarding_selected_type_label),
            value = stringResource(id = uiState.disabilityType.titleRes),
        )
        OnboardingSummaryCard(
            label = stringResource(id = R.string.onboarding_selected_level_label),
            value = stringResource(id = uiState.disabilityLevel.titleRes),
        )
    }
}

@Composable
private fun OnboardingModeCard(
    title: String,
    description: String,
    highlightVisualMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val accentColor =
        if (highlightVisualMode) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.primary
        }
    val containerColor =
        if (highlightVisualMode) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(width = 1.dp, color = accentColor.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OnboardingSummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
