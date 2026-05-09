package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumBorderInfo
import com.ssafy.e102.eumgil.core.designsystem.theme.EumBorderSubtle
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary200
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary500
import com.ssafy.e102.eumgil.core.designsystem.theme.EumPrimary600
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSurfaceInfo
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSurfaceSubtle
import com.ssafy.e102.eumgil.core.designsystem.theme.EumTextTertiary
import com.ssafy.e102.eumgil.core.designsystem.theme.EumWhite

@Composable
fun TutorialScreen(
    uiState: TutorialUiState,
    onPrimaryActionClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryActionLabel =
        stringResource(
            id =
                resolveTutorialPrimaryActionLabel(
                    entryPoint = uiState.entryPoint,
                    isLastStep = uiState.step.isLast,
                ),
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(EumSurfaceSubtle)
                .statusBarsPadding()
                .padding(horizontal = EumSpacing.medium)
                .padding(bottom = EumSpacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onSkipClick) {
                Text(
                    text = stringResource(id = R.string.tutorial_action_skip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            TutorialHeader(uiState = uiState)
            TutorialVisualPanel(step = uiState.step)
            TutorialPagerIndicator(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
            )
        }

        Button(
            onClick = onPrimaryActionClick,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = TutorialLayoutDefaults.primaryButtonMinHeight),
            shape = RoundedCornerShape(EumRadius.small),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = EumPrimary600,
                    contentColor = EumWhite,
                ),
        ) {
            Text(
                text = primaryActionLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TutorialHeader(uiState: TutorialUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = uiState.step.titleRes),
            color = EumPrimary600,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = uiState.step.headlineRes),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.headlineSmall.lineHeight,
        )
        Text(
            text = stringResource(id = uiState.step.descriptionRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TutorialVisualPanel(step: TutorialStep) {
    val content = step.visualContent()

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = TutorialLayoutDefaults.visualPanelMaxWidth)
                .heightIn(min = TutorialLayoutDefaults.visualPanelMinHeight),
        shape = RoundedCornerShape(EumRadius.large),
        color = EumWhite,
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderSubtle),
        shadowElevation = TutorialLayoutDefaults.panelElevation,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            TutorialHeroIcon(iconRes = content.heroIconRes)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            ) {
                content.items.forEach { item ->
                    TutorialSupportingItemRow(item = item)
                }
            }
            TutorialEmphasisChip(labelRes = content.emphasisRes)
        }
    }
}

@Composable
private fun TutorialHeroIcon(@DrawableRes iconRes: Int) {
    Box(
        modifier =
            Modifier
                .size(TutorialLayoutDefaults.heroIconContainerSize)
                .clip(RoundedCornerShape(EumRadius.large))
                .background(EumPrimary200),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(TutorialLayoutDefaults.heroIconSize),
            tint = EumPrimary600,
        )
    }
}

@Composable
private fun TutorialSupportingItemRow(item: TutorialSupportingItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Surface(
            modifier = Modifier.size(TutorialLayoutDefaults.supportingIconContainerSize),
            shape = RoundedCornerShape(EumRadius.small),
            color = EumSurfaceInfo,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(TutorialLayoutDefaults.supportingIconSize),
                    tint = EumPrimary600,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.microGap),
        ) {
            Text(
                text = stringResource(id = item.titleRes),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(id = item.descriptionRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}

@Composable
private fun TutorialEmphasisChip(@StringRes labelRes: Int) {
    Surface(
        shape = RoundedCornerShape(EumRadius.full),
        color = EumSurfaceInfo,
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderInfo),
    ) {
        Text(
            text = stringResource(id = labelRes),
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.medium,
                    vertical = TutorialLayoutDefaults.emphasisVerticalPadding,
                ),
            color = EumPrimary500,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

private data class TutorialVisualContent(
    @DrawableRes val heroIconRes: Int,
    val items: List<TutorialSupportingItem>,
    @StringRes val emphasisRes: Int,
)

private data class TutorialSupportingItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)

private fun TutorialStep.visualContent(): TutorialVisualContent =
    when (this) {
        TutorialStep.DESTINATION ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_nav_search,
                items =
                    listOf(
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_nav_search,
                            titleRes = R.string.tutorial_destination_item_search_title,
                            descriptionRes = R.string.tutorial_destination_item_search_description,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_lowvision_category_elevator,
                            titleRes = R.string.tutorial_destination_item_filter_title,
                            descriptionRes = R.string.tutorial_destination_item_filter_description,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_map_current_location,
                            titleRes = R.string.tutorial_destination_item_location_title,
                            descriptionRes = R.string.tutorial_destination_item_location_description,
                        ),
                    ),
                emphasisRes = R.string.tutorial_destination_emphasis,
            )

        TutorialStep.ROUTE_COMPARISON ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_nav_route,
                items =
                    listOf(
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_route_time,
                            titleRes = R.string.tutorial_route_item_compare_title,
                            descriptionRes = R.string.tutorial_route_item_compare_description,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_route_elevator,
                            titleRes = R.string.tutorial_route_item_accessibility_title,
                            descriptionRes = R.string.tutorial_route_item_accessibility_description,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_route_start_navigation,
                            titleRes = R.string.tutorial_route_item_navigation_title,
                            descriptionRes = R.string.tutorial_route_item_navigation_description,
                        ),
                    ),
                emphasisRes = R.string.tutorial_route_emphasis,
            )

        TutorialStep.REPORT ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_nav_report,
                items =
                    listOf(
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_permission_location,
                            titleRes = R.string.tutorial_report_item_location_title,
                            descriptionRes = R.string.tutorial_report_item_location_description,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_report_tactile_damage,
                            titleRes = R.string.tutorial_report_item_type_title,
                            descriptionRes = R.string.tutorial_report_item_type_description,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_status_warning,
                            titleRes = R.string.tutorial_report_item_share_title,
                            descriptionRes = R.string.tutorial_report_item_share_description,
                        ),
                    ),
                emphasisRes = R.string.tutorial_report_emphasis,
            )
    }

@Composable
private fun TutorialPagerIndicator(
    currentStep: Int,
    totalSteps: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.tightGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            val selected = index + 1 == currentStep
            Box(
                modifier =
                    Modifier
                        .width(
                            if (selected) {
                                TutorialLayoutDefaults.indicatorSelectedWidth
                            } else {
                                TutorialLayoutDefaults.indicatorDefaultWidth
                            },
                        )
                        .height(TutorialLayoutDefaults.indicatorHeight)
                        .clip(RoundedCornerShape(TutorialLayoutDefaults.pillCorner))
                        .background(if (selected) EumPrimary600 else TutorialLayoutDefaults.indicatorTrackColor),
            )
        }
        Spacer(modifier = Modifier.width(TutorialLayoutDefaults.microGap))
        Text(
            text = stringResource(id = R.string.tutorial_progress_count, currentStep, totalSteps),
            color = EumTextTertiary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal object TutorialLayoutDefaults {
    const val totalStepCount: Int = TutorialStep.TOTAL_STEPS
    const val supportingItemCount: Int = 3

    val primaryButtonMinHeight = 56.dp
    val visualPanelMaxWidth = 360.dp
    val visualPanelMinHeight = 320.dp
    val panelElevation = 2.dp

    val tightGap = 8.dp
    val microGap = 4.dp
    val hairlineWidth = 1.dp

    val heroIconContainerSize = 76.dp
    val heroIconSize = 40.dp
    val supportingIconContainerSize = 44.dp
    val supportingIconSize = 24.dp
    val emphasisVerticalPadding = 9.dp

    val indicatorSelectedWidth = 70.dp
    val indicatorDefaultWidth = 54.dp
    val indicatorHeight = 6.dp
    val pillCorner = 99.dp

    val indicatorTrackColor = Color(0xFFD9DDE7)
}
