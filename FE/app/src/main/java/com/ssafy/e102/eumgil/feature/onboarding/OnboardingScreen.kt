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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingSelectionCard
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingSelectionCardStyle
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingStepAction
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingStepScaffold

@Composable
fun PrimaryUserTypeScreen(
    uiState: PrimaryUserTypeUiState,
    onTypeSelected: (PrimaryUserType) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 1,
        totalSteps = 5,
        title = stringResource(id = R.string.onboarding_primary_user_type_screen_title),
        description = "",
        primaryActionLabel = stringResource(id = R.string.action_next_step),
        primaryActionEnabled = uiState.selectedType != null,
        onPrimaryActionClick = onNextClick,
        modifier = modifier,
    ) {
        PrimaryUserType.entries.forEach { primaryUserType ->
            OnboardingSelectionCard(
                title = stringResource(id = primaryUserType.titleRes),
                description = stringResource(id = primaryUserType.descriptionRes),
                selected = uiState.selectedType == primaryUserType,
                onClick = { onTypeSelected(primaryUserType) },
                leadingIconRes = primaryUserType.iconRes,
                style =
                    if (primaryUserType.usesHighContrastCard) {
                        OnboardingSelectionCardStyle.HighContrast
                    } else {
                        OnboardingSelectionCardStyle.Default
                    },
            )
        }

        Text(
            text = stringResource(id = R.string.onboarding_primary_user_type_supporting),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EumSpacing.xSmall),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LowVisionFollowUpScreen(
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 2,
        totalSteps = 5,
        title = stringResource(id = R.string.onboarding_low_vision_follow_up_title),
        description = stringResource(id = R.string.onboarding_low_vision_follow_up_description),
        primaryActionLabel = stringResource(id = R.string.action_next_step),
        primaryActionEnabled = true,
        onPrimaryActionClick = onNextClick,
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(EumRadius.large),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.secondary),
        ) {
            Column(
                modifier = Modifier.padding(EumSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                Text(
                    text = stringResource(id = R.string.onboarding_low_vision_follow_up_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(id = R.string.onboarding_low_vision_follow_up_card_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
fun MobilitySubtypeScreen(
    uiState: MobilitySubtypeUiState,
    onSubtypeSelected: (MobilitySubtype) -> Unit,
    onNavigateBack: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 3,
        totalSteps = 5,
        title = stringResource(id = R.string.onboarding_mobility_subtype_screen_title),
        description = stringResource(id = R.string.onboarding_primary_user_type_supporting),
        primaryActionLabel = stringResource(id = R.string.action_next_step),
        primaryActionEnabled = uiState.selectedMobilitySubtype != null,
        onPrimaryActionClick = onNextClick,
        navigationAction =
            OnboardingStepAction(
                label = stringResource(id = R.string.action_go_back_previous_step),
                onClick = onNavigateBack,
            ),
        modifier = modifier,
    ) {
        MobilitySubtype.entries.forEach { mobilitySubtype ->
            OnboardingSelectionCard(
                title = stringResource(id = mobilitySubtype.titleRes),
                description = stringResource(id = mobilitySubtype.descriptionRes),
                selected = uiState.selectedMobilitySubtype == mobilitySubtype,
                onClick = { onSubtypeSelected(mobilitySubtype) },
                leadingIconRes = mobilitySubtype.iconRes,
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
        currentStep = 4,
        totalSteps = 5,
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
            .fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(EumRadius.large),
        border = BorderStroke(width = if (checked) 2.dp else 1.dp, color = borderColor),
        onClick = { onCheckedChange(!checked) },
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
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
