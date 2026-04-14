package com.ssafy.e102.eumgil.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
fun LocationTermsScreen(
    uiState: LocationTermsUiState,
    onAllTermsCheckedChange: (Boolean) -> Unit,
    onLocationTermsCheckedChange: (Boolean) -> Unit,
    onPrivacyPolicyCheckedChange: (Boolean) -> Unit,
    onPrimaryActionClick: () -> Unit,
    onSecondaryActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 3,
        totalSteps = 3,
        title = stringResource(id = R.string.onboarding_terms_screen_title),
        description = stringResource(id = R.string.onboarding_terms_screen_description),
        primaryActionLabel = stringResource(id = R.string.action_agree_and_start),
        primaryActionEnabled = uiState.canProceed,
        onPrimaryActionClick = onPrimaryActionClick,
        secondaryAction =
            if (uiState.canProceed) {
                null
            } else {
                OnboardingStepAction(
                    label = stringResource(id = R.string.action_defer_terms),
                    onClick = onSecondaryActionClick,
                )
            },
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

        LocationTermsStatusCard(consentStatus = uiState.consentStatus)

        TermsInfoCard(
            title = stringResource(id = R.string.onboarding_terms_overview_title),
            description = stringResource(id = R.string.onboarding_terms_overview_description),
        )

        LocationTermsDocumentCard()

        Text(
            text = stringResource(id = R.string.onboarding_terms_consent_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        ConsentOptionCard(
            title = stringResource(id = R.string.onboarding_terms_all_agreement_title),
            description = stringResource(id = R.string.onboarding_terms_all_agreement_description),
            checked = uiState.isAllTermsChecked,
            onCheckedChange = onAllTermsCheckedChange,
            highlight = true,
        )

        ConsentOptionCard(
            title = stringResource(id = R.string.onboarding_terms_required_title),
            description = stringResource(id = R.string.onboarding_terms_required_description),
            checked = uiState.isLocationTermsChecked,
            onCheckedChange = onLocationTermsCheckedChange,
        )

        ConsentOptionCard(
            title = stringResource(id = R.string.onboarding_terms_optional_title),
            description = stringResource(id = R.string.onboarding_terms_optional_description),
            checked = uiState.isPrivacyPolicyChecked,
            onCheckedChange = onPrivacyPolicyCheckedChange,
        )

        if (!uiState.canProceed) {
            Text(
                text = stringResource(id = R.string.onboarding_terms_required_notice),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (uiState.hasRestrictionNotice) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
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

@Composable
private fun LocationTermsStatusCard(
    consentStatus: LocationTermsConsentStatus,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when (consentStatus) {
            LocationTermsConsentStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
            LocationTermsConsentStatus.RESTRICTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
            LocationTermsConsentStatus.READY -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        }
    val borderColor =
        when (consentStatus) {
            LocationTermsConsentStatus.PENDING -> MaterialTheme.colorScheme.outline
            LocationTermsConsentStatus.RESTRICTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            LocationTermsConsentStatus.READY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        }
    val titleColor =
        when (consentStatus) {
            LocationTermsConsentStatus.PENDING -> MaterialTheme.colorScheme.onSurface
            LocationTermsConsentStatus.RESTRICTED -> MaterialTheme.colorScheme.error
            LocationTermsConsentStatus.READY -> MaterialTheme.colorScheme.primary
        }
    val titleRes =
        when (consentStatus) {
            LocationTermsConsentStatus.PENDING -> R.string.onboarding_terms_status_pending_label
            LocationTermsConsentStatus.RESTRICTED -> R.string.onboarding_terms_status_restricted_label
            LocationTermsConsentStatus.READY -> R.string.onboarding_terms_status_ready_label
        }
    val descriptionRes =
        when (consentStatus) {
            LocationTermsConsentStatus.PENDING -> R.string.onboarding_terms_status_pending_description
            LocationTermsConsentStatus.RESTRICTED -> R.string.onboarding_terms_status_restricted_description
            LocationTermsConsentStatus.READY -> R.string.onboarding_terms_status_ready_description
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(width = 1.dp, color = borderColor),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
            Text(
                text = stringResource(id = descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TermsInfoCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
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
private fun LocationTermsDocumentCard(modifier: Modifier = Modifier) {
    val documentSections =
        listOf(
            R.string.onboarding_terms_section_collection_title to
                R.string.onboarding_terms_section_collection_body,
            R.string.onboarding_terms_section_usage_title to
                R.string.onboarding_terms_section_usage_body,
            R.string.onboarding_terms_section_retention_title to
                R.string.onboarding_terms_section_retention_body,
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = stringResource(id = R.string.onboarding_terms_document_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(id = R.string.onboarding_terms_document_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            documentSections.forEach { (titleRes, descriptionRes) ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                ) {
                    Text(
                        text = stringResource(id = titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(id = descriptionRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsentOptionCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val borderColor =
        if (checked || highlight) {
            MaterialTheme.colorScheme.primary.copy(alpha = if (checked) 1f else 0.35f)
        } else {
            MaterialTheme.colorScheme.outline
        }
    val containerColor =
        if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        } else if (highlight) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
            ),
        color = containerColor,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(width = if (checked) 2.dp else 1.dp, color = borderColor),
    ) {
        Row(
            modifier = Modifier.padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
