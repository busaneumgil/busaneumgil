package com.ssafy.e102.eumgil.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSurfaceMuted
import com.ssafy.e102.eumgil.core.designsystem.theme.EumTextPrimary
import com.ssafy.e102.eumgil.core.designsystem.theme.EumTextTertiary
import com.ssafy.e102.eumgil.core.designsystem.theme.EumWhite
import com.ssafy.e102.eumgil.feature.onboarding.component.onboardingBodyLineBreak
import com.ssafy.e102.eumgil.feature.onboarding.component.onboardingHeadingLineBreak
import com.ssafy.e102.eumgil.feature.onboarding.component.stabilizeOnboardingWrap
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingSelectionFrame
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingStepHeaderStyle
import com.ssafy.e102.eumgil.feature.onboarding.component.OnboardingStepScaffold

@Composable
fun PrimaryUserTypeScreen(
    onTypeSelected: (PrimaryUserType) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingSelectionFrame(
        currentStep = 1,
        totalSteps = 2,
        title = stringResource(id = R.string.onboarding_primary_user_type_screen_title),
        description = "",
        modifier = modifier,
    ) {
        PrimaryUserType.entries.forEach { primaryUserType ->
            PrimaryUserTypeButton(
                primaryUserType = primaryUserType,
                onClick = { onTypeSelected(primaryUserType) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PrimaryUserTypeButton(
    primaryUserType: PrimaryUserType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = primaryUserTypeButtonStyleFor(primaryUserType)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
            },
        color = style.containerColor,
        shape = RoundedCornerShape(EumRadius.scaleL),
        border = BorderStroke(width = 2.dp, color = style.borderColor),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = EumSpacing.large, vertical = EumSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = style.iconContainerColor,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = primaryUserType.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(104.dp),
                    tint = style.iconTint,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
            ) {
                Text(
                    text = stringResource(id = primaryUserType.titleRes).stabilizeOnboardingWrap(),
                    style = MaterialTheme.typography.headlineMedium.onboardingHeadingLineBreak(),
                    fontSize = 38.sp,
                    lineHeight = 46.sp,
                    color = style.titleColor,
                )
                Text(
                    text = stringResource(id = primaryUserType.descriptionRes).stabilizeOnboardingWrap(),
                    style = MaterialTheme.typography.bodyLarge.onboardingBodyLineBreak(),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = style.descriptionColor,
                )
            }
        }
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
                    text = stringResource(id = R.string.onboarding_low_vision_follow_up_card_title).stabilizeOnboardingWrap(),
                    style = MaterialTheme.typography.titleMedium.onboardingHeadingLineBreak(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(id = R.string.onboarding_low_vision_follow_up_card_description).stabilizeOnboardingWrap(),
                    style = MaterialTheme.typography.bodyLarge.onboardingBodyLineBreak(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
fun MobilitySubtypeScreen(
    uiState: MobilitySubtypeUiState,
    onSubtypeClick: (MobilitySubtype) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingSelectionFrame(
        currentStep = 2,
        totalSteps = 2,
        title = stringResource(id = R.string.onboarding_mobility_subtype_screen_title),
        description = stringResource(id = R.string.onboarding_mobility_subtype_screen_supporting),
        modifier = modifier,
    ) {
        MobilitySubtype.entries.forEach { mobilitySubtype ->
            MobilitySubtypeButton(
                mobilitySubtype = mobilitySubtype,
                selected = uiState.selectedMobilitySubtype == mobilitySubtype,
                onClick = { onSubtypeClick(mobilitySubtype) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MobilitySubtypeButton(
    mobilitySubtype: MobilitySubtype,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = mobilitySubtypeButtonStyleFor(selected = selected)
    val selectedStateDescription = stringResource(id = R.string.a11y_option_selected)
    val unselectedStateDescription = stringResource(id = R.string.a11y_option_unselected)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.selected = selected
                stateDescription =
                    if (selected) {
                        selectedStateDescription
                    } else {
                        unselectedStateDescription
                    }
                role = Role.RadioButton
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        color =
            if (selected) {
                style.selectedContainerColor
            } else {
                style.containerColor
            },
        shape = RoundedCornerShape(EumRadius.scaleL),
        border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = style.borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(MobilitySubtypeIconSlotSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = mobilitySubtype.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(mobilitySubtype.iconSizeDp.dp),
                    tint = style.iconTint,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = stringResource(id = mobilitySubtype.titleRes).stabilizeOnboardingWrap(),
                    style = MaterialTheme.typography.titleMedium.onboardingHeadingLineBreak(),
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    color = style.titleColor,
                )
                Text(
                    text = stringResource(id = mobilitySubtype.descriptionRes).stabilizeOnboardingWrap(),
                    style = MaterialTheme.typography.bodyMedium.onboardingBodyLineBreak(),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = style.descriptionColor,
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_control_next),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = style.trailingTint,
            )
        }
    }
}

@Composable
fun LocationTermsScreen(
    uiState: LocationTermsUiState,
    onAllTermsCheckedChange: (Boolean) -> Unit,
    onServiceTermsCheckedChange: (Boolean) -> Unit,
    onSensitiveInfoTermsCheckedChange: (Boolean) -> Unit,
    onPersonalLocationInfoTermsCheckedChange: (Boolean) -> Unit,
    onOverFourteenCheckedChange: (Boolean) -> Unit,
    onPrimaryActionClick: () -> Unit,
    onRequestDetails: (LocationTermsItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = 4,
        totalSteps = 5,
        title = stringResource(id = R.string.onboarding_terms_screen_title),
        description = "",
        primaryActionLabel = stringResource(id = R.string.action_next_step),
        primaryActionEnabled = uiState.canProceed,
        onPrimaryActionClick = onPrimaryActionClick,
        headerStyle = OnboardingStepHeaderStyle.CENTERED_COMPACT,
        modifier = modifier,
    ) {
        LocationTermsAllAgreementRow(
            checked = uiState.isAllTermsChecked,
            onCheckedChange = onAllTermsCheckedChange,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            LocationTermsItem.entries.forEach { locationTermsItem ->
                val checked =
                    when (locationTermsItem) {
                        LocationTermsItem.SERVICE_AND_LOCATION_BASED_SERVICE -> uiState.isServiceTermsChecked
                        LocationTermsItem.SENSITIVE_INFO -> uiState.isSensitiveInfoTermsChecked
                        LocationTermsItem.PERSONAL_LOCATION_INFO -> uiState.isPersonalLocationInfoTermsChecked
                        LocationTermsItem.OVER_FOURTEEN -> uiState.isOverFourteenChecked
                    }
                val onCheckedChange =
                    when (locationTermsItem) {
                        LocationTermsItem.SERVICE_AND_LOCATION_BASED_SERVICE -> onServiceTermsCheckedChange
                        LocationTermsItem.SENSITIVE_INFO -> onSensitiveInfoTermsCheckedChange
                        LocationTermsItem.PERSONAL_LOCATION_INFO -> onPersonalLocationInfoTermsCheckedChange
                        LocationTermsItem.OVER_FOURTEEN -> onOverFourteenCheckedChange
                    }

                LocationTermsAgreementRow(
                    item = locationTermsItem,
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    onRequestDetails = { onRequestDetails(locationTermsItem) },
                )
            }
        }

        if (!uiState.canProceed) {
            Text(
                text = stringResource(id = R.string.onboarding_terms_required_notice),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTermsBottomSheet(
    uiState: LocationTermsUiState,
    onDismissRequest: () -> Unit,
    onAllTermsCheckedChange: (Boolean) -> Unit,
    onServiceTermsCheckedChange: (Boolean) -> Unit,
    onSensitiveInfoTermsCheckedChange: (Boolean) -> Unit,
    onPersonalLocationInfoTermsCheckedChange: (Boolean) -> Unit,
    onOverFourteenCheckedChange: (Boolean) -> Unit,
    onPrimaryActionClick: () -> Unit,
    onRequestDetails: (LocationTermsItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        shape =
            RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
            ),
        containerColor = EumWhite,
        scrimColor = Color.Black.copy(alpha = 0.54f),
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        LocationTermsBottomSheetContent(
            uiState = uiState,
            onAllTermsCheckedChange = onAllTermsCheckedChange,
            onServiceTermsCheckedChange = onServiceTermsCheckedChange,
            onSensitiveInfoTermsCheckedChange = onSensitiveInfoTermsCheckedChange,
            onPersonalLocationInfoTermsCheckedChange = onPersonalLocationInfoTermsCheckedChange,
            onOverFourteenCheckedChange = onOverFourteenCheckedChange,
            onPrimaryActionClick = onPrimaryActionClick,
            onRequestDetails = onRequestDetails,
            modifier =
                modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
        )
    }
}

@Composable
private fun LocationTermsBottomSheetContent(
    uiState: LocationTermsUiState,
    onAllTermsCheckedChange: (Boolean) -> Unit,
    onServiceTermsCheckedChange: (Boolean) -> Unit,
    onSensitiveInfoTermsCheckedChange: (Boolean) -> Unit,
    onPersonalLocationInfoTermsCheckedChange: (Boolean) -> Unit,
    onOverFourteenCheckedChange: (Boolean) -> Unit,
    onPrimaryActionClick: () -> Unit,
    onRequestDetails: (LocationTermsItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.onboarding_terms_sheet_title).stabilizeOnboardingWrap(),
                style = MaterialTheme.typography.headlineSmall.onboardingHeadingLineBreak(),
                fontWeight = FontWeight.SemiBold,
                color = EumTextPrimary,
            )
            Text(
                text = stringResource(id = R.string.onboarding_terms_sheet_description).stabilizeOnboardingWrap(),
                style = MaterialTheme.typography.bodyLarge.onboardingBodyLineBreak(),
                color = EumTextTertiary,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LocationTermsSheetAllAgreementRow(
                checked = uiState.isAllTermsChecked,
                onCheckedChange = onAllTermsCheckedChange,
            )

            LocationTermsItem.entries.forEach { item ->
                val checked =
                    when (item) {
                        LocationTermsItem.SERVICE_AND_LOCATION_BASED_SERVICE -> uiState.isServiceTermsChecked
                        LocationTermsItem.SENSITIVE_INFO -> uiState.isSensitiveInfoTermsChecked
                        LocationTermsItem.PERSONAL_LOCATION_INFO -> uiState.isPersonalLocationInfoTermsChecked
                        LocationTermsItem.OVER_FOURTEEN -> uiState.isOverFourteenChecked
                    }
                val onCheckedChange =
                    when (item) {
                        LocationTermsItem.SERVICE_AND_LOCATION_BASED_SERVICE -> onServiceTermsCheckedChange
                        LocationTermsItem.SENSITIVE_INFO -> onSensitiveInfoTermsCheckedChange
                        LocationTermsItem.PERSONAL_LOCATION_INFO -> onPersonalLocationInfoTermsCheckedChange
                        LocationTermsItem.OVER_FOURTEEN -> onOverFourteenCheckedChange
                    }

                LocationTermsSheetAgreementRow(
                    item = item,
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    onRequestDetails = { onRequestDetails(item) },
                )
            }
        }

        Button(
            onClick = onPrimaryActionClick,
            enabled = uiState.canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(EumRadius.scaleM),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = EumPrimary600,
                    contentColor = EumWhite,
                    disabledContainerColor = Color(0xFFBDBDBD),
                    disabledContentColor = EumWhite,
                ),
        ) {
            Text(
                text = stringResource(id = R.string.action_start),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LocationTermsSheetAllAgreementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStateDescription = stringResource(id = R.string.a11y_option_selected)
    val unselectedStateDescription = stringResource(id = R.string.a11y_option_unselected)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (checked) selectedStateDescription else unselectedStateDescription
            }
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
            ),
        color = EumSurfaceMuted,
        shape = RoundedCornerShape(EumRadius.scaleM),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.onboarding_terms_all_agreement_title).stabilizeOnboardingWrap(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.onboardingBodyLineBreak(),
                fontWeight = FontWeight.SemiBold,
                color = EumTextPrimary,
            )
            LocationTermsSheetCheckIcon(checked = checked)
        }
    }
}

@Composable
private fun LocationTermsSheetAgreementRow(
    item: LocationTermsItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRequestDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStateDescription = stringResource(id = R.string.a11y_option_selected)
    val unselectedStateDescription = stringResource(id = R.string.a11y_option_unselected)
    val canOpenDetails = item != LocationTermsItem.OVER_FOURTEEN

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = item.titleRes).stabilizeOnboardingWrap(),
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = canOpenDetails, onClick = onRequestDetails),
            style = MaterialTheme.typography.bodyLarge.onboardingBodyLineBreak(),
            fontWeight = FontWeight.SemiBold,
            color = EumTextTertiary,
            textDecoration = if (canOpenDetails) TextDecoration.Underline else TextDecoration.None,
        )

        IconButton(
            onClick = { onCheckedChange(!checked) },
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    role = Role.Checkbox
                    stateDescription = if (checked) selectedStateDescription else unselectedStateDescription
                },
        ) {
            LocationTermsSheetCheckIcon(checked = checked)
        }
    }
}

@Composable
private fun LocationTermsSheetCheckIcon(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(32.dp),
        color = if (checked) EumPrimary600 else Color(0xFFE5E7EB),
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_mark),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = EumWhite,
                )
            }
        }
    }
}

@Composable
private fun LocationTermsAllAgreementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStateDescription = stringResource(id = R.string.a11y_option_selected)
    val unselectedStateDescription = stringResource(id = R.string.a11y_option_unselected)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription =
                    if (checked) {
                        selectedStateDescription
                    } else {
                        unselectedStateDescription
                    }
            }
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox,
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(EumRadius.scaleM),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (checked) {
                        EumPrimary600.copy(alpha = 0.28f)
                    } else {
                        Color.Transparent
                    },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = locationTermsCheckboxColors(),
            )
            Text(
                text = stringResource(id = R.string.onboarding_terms_all_agreement_title).stabilizeOnboardingWrap(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.onboardingBodyLineBreak(),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LocationTermsAgreementRow(
    item: LocationTermsItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRequestDetails: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val selectedStateDescription = stringResource(id = R.string.a11y_option_selected)
    val unselectedStateDescription = stringResource(id = R.string.a11y_option_unselected)
    val showDetailDisclosure = item != LocationTermsItem.OVER_FOURTEEN
    val detailContentDescription = stringResource(id = R.string.a11y_terms_detail_open)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(EumRadius.scaleM),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (checked) {
                        EumPrimary600.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        role = Role.Checkbox
                        stateDescription =
                            if (checked) {
                                selectedStateDescription
                            } else {
                                unselectedStateDescription
                            }
                    }
                    .toggleable(
                        value = checked,
                        onValueChange = onCheckedChange,
                        role = Role.Checkbox,
                    ),
                horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    colors = locationTermsCheckboxColors(),
                )

                Text(
                    text = buildLocationTermsLabel(stringResource(id = item.titleRes).stabilizeOnboardingWrap()),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.onboardingBodyLineBreak(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (showDetailDisclosure) {
                IconButton(
                    onClick = onRequestDetails,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_control_next),
                        contentDescription = detailContentDescription,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

private fun buildLocationTermsLabel(label: String) =
    buildAnnotatedString {
        val badgeEndIndex = label.indexOf(']')

        if (badgeEndIndex in 1 until label.lastIndex) {
            withStyle(
                style =
                    SpanStyle(
                        color = EumPrimary600,
                        fontWeight = FontWeight.SemiBold,
                    ),
            ) {
                append(label.substring(startIndex = 0, endIndex = badgeEndIndex + 1))
            }

            val title = label.substring(startIndex = badgeEndIndex + 1).trimStart()
            if (title.isNotEmpty()) {
                append(" ")
                append(title)
            }
        } else {
            append(label)
        }
    }

@Composable
private fun locationTermsCheckboxColors() =
    CheckboxDefaults.colors(
        checkedColor = EumPrimary600,
        uncheckedColor = MaterialTheme.colorScheme.outline,
        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
        disabledCheckedColor = EumPrimary600,
        disabledUncheckedColor = MaterialTheme.colorScheme.outline,
    )

private val MobilitySubtypeIconSlotSize = 96.dp
