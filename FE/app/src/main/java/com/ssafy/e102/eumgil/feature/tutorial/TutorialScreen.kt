package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumBorderInfo
import com.ssafy.e102.eumgil.core.designsystem.theme.EumBorderSubtle
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
    onPreviousActionClick: () -> Unit,
    onPanelNextStepClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryActionLabelRes =
        resolveTutorialPrimaryActionLabel(
            entryPoint = uiState.entryPoint,
            isLastStep = uiState.step.isLast,
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
                    .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            TutorialHeader(uiState = uiState)
            TutorialVisualPanel(
                step = uiState.step,
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                canMovePrevious = uiState.canMovePrevious,
                canMoveNext = uiState.canMoveNext,
                onPreviousActionClick = onPreviousActionClick,
                onPanelNextStepClick = onPanelNextStepClick,
                modifier = Modifier.weight(TutorialLayoutDefaults.visualPanelWeight),
            )
        }

        Spacer(modifier = Modifier.height(TutorialLayoutDefaults.visualPanelButtonGap))

        TutorialBottomActions(
            primaryActionLabel = primaryActionLabelRes,
            canMovePrevious = uiState.canMovePrevious,
            onPreviousActionClick = onPreviousActionClick,
            onPrimaryActionClick = onPrimaryActionClick,
        )
    }
}

@Composable
private fun TutorialBottomActions(
    @StringRes primaryActionLabel: Int,
    canMovePrevious: Boolean,
    onPreviousActionClick: () -> Unit,
    onPrimaryActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canMovePrevious) {
            OutlinedButton(
                onClick = onPreviousActionClick,
                modifier =
                    Modifier
                        .width(TutorialLayoutDefaults.previousButtonMinWidth)
                        .heightIn(min = TutorialLayoutDefaults.primaryButtonMinHeight),
                shape = RoundedCornerShape(EumRadius.small),
                border = BorderStroke(TutorialLayoutDefaults.previousButtonBorderWidth, EumBorderInfo),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = EumPrimary600,
                    ),
            ) {
                Text(
                    text = stringResource(id = R.string.tutorial_action_previous),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Button(
            onClick = onPrimaryActionClick,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = TutorialLayoutDefaults.primaryButtonMinHeight),
            shape = RoundedCornerShape(EumRadius.small),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = EumPrimary600,
                    contentColor = EumWhite,
                ),
        ) {
            Text(
                text = stringResource(id = primaryActionLabel),
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
        verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.headerSectionGap),
    ) {
        Text(
            text = stringResource(id = uiState.step.titleRes),
            color = EumPrimary600,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = TutorialLayoutDefaults.headerTitleLineHeight,
        )
        Text(
            text = stringResource(id = uiState.step.headlineRes),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            lineHeight = TutorialLayoutDefaults.headerHeadlineLineHeight,
        )
        Text(
            text = stringResource(id = uiState.step.descriptionRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = TutorialLayoutDefaults.headerDescriptionLineHeight,
        )
    }
}

@Composable
private fun TutorialVisualPanel(
    step: TutorialStep,
    currentStep: Int,
    totalSteps: Int,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    onPreviousActionClick: () -> Unit,
    onPanelNextStepClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = step.visualContent()

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = TutorialLayoutDefaults.visualPanelMaxWidth)
                .pointerInput(canMovePrevious, canMoveNext) {
                    detectTapGestures { offset ->
                        val isLeftSide = offset.x < size.width / 2f
                        when {
                            isLeftSide && canMovePrevious -> onPreviousActionClick()
                            !isLeftSide && canMoveNext -> onPanelNextStepClick()
                        }
                    }
                },
        shape = RoundedCornerShape(EumRadius.large),
        color = EumWhite,
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderSubtle),
        shadowElevation = TutorialLayoutDefaults.panelElevation,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(EumSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            TutorialFlatIllustration(
                content = content,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            )
            TutorialPagerIndicator(currentStep = currentStep, totalSteps = totalSteps)
        }
    }
}

@Composable
private fun TutorialFlatIllustration(
    content: TutorialVisualContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TutorialMockupScene(content = content)
        Spacer(modifier = Modifier.height(TutorialLayoutDefaults.illustrationContentGap))
        TutorialCueStrip(cues = content.cues)
        Spacer(modifier = Modifier.height(EumSpacing.medium))
        TutorialFilterChipRow(chips = content.chips)
    }
}

@Composable
private fun TutorialMockupScene(content: TutorialVisualContent) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(TutorialLayoutDefaults.illustrationHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(TutorialLayoutDefaults.illustrationHaloSize)
                    .clip(RoundedCornerShape(TutorialLayoutDefaults.pillCorner))
                    .background(EumSurfaceInfo),
        )
        TutorialMockPhone()
        TutorialFloatingPanel(
            iconRes = content.heroIconRes,
            labelRes = content.cues.first().labelRes,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        TutorialSmallInfoCard(
            iconRes = content.cues.last().iconRes,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = TutorialLayoutDefaults.illustrationSidePadding),
        )
    }
}

@Composable
private fun TutorialMockPhone() {
    Surface(
        modifier =
            Modifier
                .width(TutorialLayoutDefaults.mockPhoneWidth)
                .height(TutorialLayoutDefaults.mockPhoneHeight),
        shape = RoundedCornerShape(TutorialLayoutDefaults.mockPhoneCorner),
        color = EumWhite,
        border = BorderStroke(TutorialLayoutDefaults.mockPhoneBorderWidth, EumPrimary600.copy(alpha = 0.35f)),
        shadowElevation = TutorialLayoutDefaults.mockPhoneElevation,
    ) {
        Column(
            modifier = Modifier.padding(TutorialLayoutDefaults.mockPhonePadding),
            verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.tightGap),
        ) {
            repeat(TutorialLayoutDefaults.mockPhoneLineCount) { index ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(if (index == 0) 0.78f else 0.58f)
                            .height(TutorialLayoutDefaults.mockPhoneLineHeight)
                            .clip(RoundedCornerShape(TutorialLayoutDefaults.pillCorner))
                            .background(EumSurfaceInfo),
                )
            }
            Spacer(modifier = Modifier.height(TutorialLayoutDefaults.microGap))
            Row(horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.microGap)) {
                repeat(TutorialLayoutDefaults.mockPhoneDotCount) {
                    Box(
                        modifier =
                            Modifier
                                .size(TutorialLayoutDefaults.mockPhoneDotSize)
                                .clip(RoundedCornerShape(TutorialLayoutDefaults.pillCorner))
                                .background(EumPrimary600.copy(alpha = 0.28f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialFloatingPanel(
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .width(TutorialLayoutDefaults.floatingPanelWidth)
                .height(TutorialLayoutDefaults.floatingPanelHeight),
        shape = RoundedCornerShape(EumRadius.small),
        color = EumPrimary600,
        shadowElevation = TutorialLayoutDefaults.floatingPanelElevation,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = EumSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(TutorialLayoutDefaults.floatingPanelIconSize),
                tint = EumWhite,
            )
            Text(
                text = stringResource(id = labelRes),
                color = EumWhite,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TutorialSmallInfoCard(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .width(TutorialLayoutDefaults.smallCardWidth)
                .height(TutorialLayoutDefaults.smallCardHeight),
        shape = RoundedCornerShape(EumRadius.small),
        color = EumSurfaceInfo,
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderInfo),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(TutorialLayoutDefaults.smallCardIconSize),
                tint = EumPrimary600,
            )
        }
    }
}

@Composable
private fun TutorialCueStrip(cues: List<TutorialVisualCue>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cues.forEach { cue ->
            TutorialCuePill(cue = cue)
        }
    }
}

@Composable
private fun TutorialCuePill(cue: TutorialVisualCue) {
    Surface(
        shape = RoundedCornerShape(EumRadius.full),
        color = EumSurfaceInfo,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = EumSpacing.small,
                    vertical = TutorialLayoutDefaults.cuePillVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.filterChipGap),
        ) {
            Icon(
                painter = painterResource(id = cue.iconRes),
                contentDescription = null,
                modifier = Modifier.size(TutorialLayoutDefaults.cuePillIconSize),
                tint = EumPrimary600,
            )
            Text(
                text = stringResource(id = cue.labelRes),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TutorialFilterChipRow(chips: List<TutorialFilterChip>) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.filterChipGap),
    ) {
        chips.forEach { chip ->
            TutorialFilterChip(chip = chip)
        }
    }
}

@Composable
private fun TutorialFilterChip(chip: TutorialFilterChip) {
    Surface(
        shape = RoundedCornerShape(EumRadius.small),
        color = EumSurfaceInfo,
        border = BorderStroke(TutorialLayoutDefaults.hairlineWidth, EumBorderInfo),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = TutorialLayoutDefaults.filterChipHorizontalPadding,
                    vertical = TutorialLayoutDefaults.filterChipVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.filterChipGap),
        ) {
            Icon(
                painter = painterResource(id = chip.iconRes),
                contentDescription = null,
                modifier = Modifier.size(TutorialLayoutDefaults.filterChipIconSize),
                tint = EumPrimary600,
            )
            Text(
                text = stringResource(id = chip.labelRes),
                color = EumPrimary600,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class TutorialVisualContent(
    @DrawableRes val heroIconRes: Int,
    val cues: List<TutorialVisualCue>,
    val chips: List<TutorialFilterChip>,
)

private data class TutorialVisualCue(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
)

private data class TutorialFilterChip(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
)

private fun TutorialStep.visualContent(): TutorialVisualContent =
    when (this) {
        TutorialStep.DESTINATION ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_nav_search,
                cues =
                    listOf(
                        TutorialVisualCue(
                            iconRes = R.drawable.ic_nav_search,
                            labelRes = R.string.tutorial_destination_item_search_title,
                        ),
                        TutorialVisualCue(
                            iconRes = R.drawable.ic_lowvision_category_elevator,
                            labelRes = R.string.tutorial_destination_item_filter_title,
                        ),
                    ),
                chips =
                    listOf(
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_place_restroom,
                            labelRes = R.string.tutorial_filter_toilet,
                        ),
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_lowvision_category_elevator,
                            labelRes = R.string.tutorial_filter_elevator,
                        ),
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_place_parking,
                            labelRes = R.string.tutorial_filter_parking,
                        ),
                    ),
            )

        TutorialStep.ROUTE_COMPARISON ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_route_start_navigation,
                cues =
                    listOf(
                        TutorialVisualCue(
                            iconRes = R.drawable.ic_route_time,
                            labelRes = R.string.tutorial_route_item_compare_title,
                        ),
                        TutorialVisualCue(
                            iconRes = R.drawable.ic_route_elevator,
                            labelRes = R.string.tutorial_route_item_accessibility_title,
                        ),
                    ),
                chips =
                    listOf(
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_route_elevator,
                            labelRes = R.string.tutorial_route_chip_elevator,
                        ),
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_route_auto_door,
                            labelRes = R.string.tutorial_route_chip_accessible,
                        ),
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_route_ramp,
                            labelRes = R.string.tutorial_route_chip_low_step,
                        ),
                    ),
            )

        TutorialStep.REPORT ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_nav_report,
                cues =
                    listOf(
                        TutorialVisualCue(
                            iconRes = R.drawable.ic_permission_location,
                            labelRes = R.string.tutorial_report_item_location_title,
                        ),
                        TutorialVisualCue(
                            iconRes = R.drawable.ic_report_tactile_damage,
                            labelRes = R.string.tutorial_report_item_type_title,
                        ),
                    ),
                chips =
                    listOf(
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_route_tactile_blocks,
                            labelRes = R.string.tutorial_report_chip_tactile,
                        ),
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_report_sidewalk,
                            labelRes = R.string.tutorial_report_chip_sidewalk,
                        ),
                        TutorialFilterChip(
                            iconRes = R.drawable.ic_status_warning,
                            labelRes = R.string.tutorial_report_chip_damage,
                        ),
                    ),
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
    const val supportingItemCount: Int = 0
    const val destinationFilterChipCount: Int = 3
    const val routeAccessibilityChipCount: Int = 3
    const val reportCategoryChipCount: Int = 3
    const val showsEmphasisChip: Boolean = false
    const val usesLayeredFlatIllustration: Boolean = true
    const val visualPanelWeight: Float = 1f
    const val hasHeroIconBackground: Boolean = false
    const val firstStepWithPreviousAction: Int = 2
    const val panelTouchNavigationZoneWeight: Float = 1f

    val primaryButtonMinHeight = 56.dp
    val previousButtonMinWidth = 88.dp
    val previousButtonBorderWidth = 1.dp
    val visualPanelButtonGap = 12.dp
    val visualPanelMaxWidth = 360.dp
    val panelElevation = 2.dp

    val headerSectionGap = 10.dp
    val headerTitleLineHeight = 30.sp
    val headerHeadlineLineHeight = 34.sp
    val headerDescriptionLineHeight = 22.sp

    val tightGap = 8.dp
    val microGap = 4.dp
    val hairlineWidth = 1.dp

    val illustrationHeight = 210.dp
    val illustrationHaloSize = 150.dp
    val illustrationContentGap = 22.dp
    val illustrationSidePadding = 42.dp
    val heroIconSize = 56.dp
    val mockPhoneWidth = 116.dp
    val mockPhoneHeight = 158.dp
    val mockPhoneCorner = 24.dp
    val mockPhoneBorderWidth = 2.dp
    val mockPhoneElevation = 2.dp
    val mockPhonePadding = 18.dp
    val mockPhoneLineCount = 3
    val mockPhoneLineHeight = 10.dp
    val mockPhoneDotCount = 3
    val mockPhoneDotSize = 8.dp
    val floatingPanelWidth = 210.dp
    val floatingPanelHeight = 56.dp
    val floatingPanelElevation = 3.dp
    val floatingPanelIconSize = 22.dp
    val smallCardWidth = 72.dp
    val smallCardHeight = 60.dp
    val smallCardIconSize = 28.dp
    val cuePillVerticalPadding = 8.dp
    val cuePillIconSize = 18.dp
    val filterChipHorizontalPadding = 12.dp
    val filterChipVerticalPadding = 8.dp
    val filterChipIconSize = 18.dp
    val filterChipGap = 6.dp

    val indicatorSelectedWidth = 70.dp
    val indicatorDefaultWidth = 54.dp
    val indicatorHeight = 6.dp
    val pillCorner = 99.dp

    val indicatorTrackColor = Color(0xFFD9DDE7)
}
