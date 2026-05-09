package com.ssafy.e102.eumgil.feature.tutorial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
            TutorialLineIllustration(step = step, iconRes = content.heroIconRes)
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.supportingItemGap),
                ) {
                    content.items.forEach { item ->
                        TutorialSupportingItemRow(item = item)
                    }
                }
            }
            TutorialPagerIndicator(currentStep = currentStep, totalSteps = totalSteps)
        }
    }
}

@Composable
private fun TutorialLineIllustration(
    step: TutorialStep,
    @DrawableRes iconRes: Int,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(TutorialLayoutDefaults.illustrationHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val primary = EumPrimary600
            val glowColor = primary.copy(alpha = 0.10f)
            val strokeWidth = TutorialLayoutDefaults.illustrationStrokeWidth.toPx()
            val thinStrokeWidth = TutorialLayoutDefaults.illustrationThinStrokeWidth.toPx()
            val baselineY = size.height * 0.72f
            val centerX = size.width / 2f

            drawCircle(
                color = glowColor,
                radius = size.minDimension * 0.38f,
                center = Offset(centerX, size.height * 0.50f),
            )
            drawLine(
                color = primary.copy(alpha = 0.18f),
                start = Offset(size.width * 0.12f, baselineY),
                end = Offset(size.width * 0.88f, baselineY),
                strokeWidth = thinStrokeWidth,
            )

            when (step) {
                TutorialStep.DESTINATION -> drawDestinationIllustration(primary, strokeWidth, thinStrokeWidth)
                TutorialStep.ROUTE_COMPARISON -> drawRouteIllustration(primary, strokeWidth, thinStrokeWidth)
                TutorialStep.REPORT -> drawReportIllustration(primary, strokeWidth, thinStrokeWidth)
            }
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(TutorialLayoutDefaults.heroIconSize),
            tint = EumPrimary600,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDestinationIllustration(
    primary: Color,
    strokeWidth: Float,
    thinStrokeWidth: Float,
) {
    drawCircle(
        color = primary.copy(alpha = 0.80f),
        radius = size.minDimension * 0.12f,
        center = Offset(size.width * 0.36f, size.height * 0.44f),
        style = Stroke(width = strokeWidth),
    )
    drawLine(
        color = primary.copy(alpha = 0.80f),
        start = Offset(size.width * 0.44f, size.height * 0.54f),
        end = Offset(size.width * 0.52f, size.height * 0.64f),
        strokeWidth = strokeWidth,
    )
    drawRoundChipLine(
        xStart = size.width * 0.56f,
        y = size.height * 0.40f,
        primary = primary,
        strokeWidth = thinStrokeWidth,
    )
    drawRoundChipLine(
        xStart = size.width * 0.58f,
        y = size.height * 0.54f,
        primary = primary,
        strokeWidth = thinStrokeWidth,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRouteIllustration(
    primary: Color,
    strokeWidth: Float,
    thinStrokeWidth: Float,
) {
    val path =
        Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.64f)
            cubicTo(
                size.width * 0.34f,
                size.height * 0.24f,
                size.width * 0.54f,
                size.height * 0.82f,
                size.width * 0.76f,
                size.height * 0.42f,
            )
        }
    drawPath(path = path, color = primary.copy(alpha = 0.80f), style = Stroke(width = strokeWidth))
    listOf(0.22f to 0.64f, 0.52f to 0.62f, 0.76f to 0.42f).forEach { (x, y) ->
        drawCircle(
            color = EumWhite,
            radius = size.minDimension * 0.045f,
            center = Offset(size.width * x, size.height * y),
        )
        drawCircle(
            color = primary,
            radius = size.minDimension * 0.045f,
            center = Offset(size.width * x, size.height * y),
            style = Stroke(width = thinStrokeWidth),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReportIllustration(
    primary: Color,
    strokeWidth: Float,
    thinStrokeWidth: Float,
) {
    val pinCenter = Offset(size.width * 0.36f, size.height * 0.42f)
    drawCircle(
        color = primary.copy(alpha = 0.80f),
        radius = size.minDimension * 0.10f,
        center = pinCenter,
        style = Stroke(width = strokeWidth),
    )
    drawLine(
        color = primary.copy(alpha = 0.80f),
        start = Offset(pinCenter.x, pinCenter.y + size.minDimension * 0.10f),
        end = Offset(size.width * 0.36f, size.height * 0.68f),
        strokeWidth = strokeWidth,
    )

    val warning =
        Path().apply {
            moveTo(size.width * 0.66f, size.height * 0.28f)
            lineTo(size.width * 0.82f, size.height * 0.62f)
            lineTo(size.width * 0.50f, size.height * 0.62f)
            close()
        }
    drawPath(path = warning, color = primary.copy(alpha = 0.80f), style = Stroke(width = strokeWidth))
    drawLine(
        color = primary,
        start = Offset(size.width * 0.66f, size.height * 0.40f),
        end = Offset(size.width * 0.66f, size.height * 0.52f),
        strokeWidth = thinStrokeWidth,
    )
    drawCircle(
        color = primary,
        radius = size.minDimension * 0.014f,
        center = Offset(size.width * 0.66f, size.height * 0.57f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundChipLine(
    xStart: Float,
    y: Float,
    primary: Color,
    strokeWidth: Float,
) {
    drawLine(
        color = primary.copy(alpha = 0.70f),
        start = Offset(xStart, y),
        end = Offset(xStart + size.width * 0.22f, y),
        strokeWidth = strokeWidth,
    )
}

@Composable
private fun TutorialSupportingItemRow(item: TutorialSupportingItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.tightGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            TutorialIconTile(iconRes = item.iconRes)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TutorialLayoutDefaults.microGap),
            ) {
                Text(
                    text = stringResource(id = item.titleRes),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = TutorialLayoutDefaults.supportingTitleLineHeight,
                )
                item.descriptionRes?.let { descriptionRes ->
                    Text(
                        text = stringResource(id = descriptionRes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = TutorialLayoutDefaults.supportingDescriptionLineHeight,
                    )
                }
            }
        }
        if (item.filterChips.isNotEmpty()) {
            TutorialFilterChipRow(chips = item.filterChips)
        }
    }
}

@Composable
private fun TutorialIconTile(@DrawableRes iconRes: Int) {
    Surface(
        modifier = Modifier.size(TutorialLayoutDefaults.supportingIconContainerSize),
        shape = RoundedCornerShape(EumRadius.small),
        color = EumSurfaceInfo,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(TutorialLayoutDefaults.supportingIconSize),
                tint = EumPrimary600,
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
    val items: List<TutorialSupportingItem>,
)

private data class TutorialSupportingItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int? = null,
    val filterChips: List<TutorialFilterChip> = emptyList(),
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
                items =
                    listOf(
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_nav_search,
                            titleRes = R.string.tutorial_destination_item_search_title,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_lowvision_category_elevator,
                            titleRes = R.string.tutorial_destination_item_filter_title,
                            filterChips =
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
                        ),
                    ),
            )

        TutorialStep.ROUTE_COMPARISON ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_route_start_navigation,
                items =
                    listOf(
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_route_time,
                            titleRes = R.string.tutorial_route_item_compare_title,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_route_elevator,
                            titleRes = R.string.tutorial_route_item_accessibility_title,
                            filterChips =
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
                        ),
                    ),
            )

        TutorialStep.REPORT ->
            TutorialVisualContent(
                heroIconRes = R.drawable.ic_nav_report,
                items =
                    listOf(
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_permission_location,
                            titleRes = R.string.tutorial_report_item_location_title,
                        ),
                        TutorialSupportingItem(
                            iconRes = R.drawable.ic_report_tactile_damage,
                            titleRes = R.string.tutorial_report_item_type_title,
                            filterChips =
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
    const val supportingItemCount: Int = 2
    const val destinationFilterChipCount: Int = 3
    const val routeAccessibilityChipCount: Int = 3
    const val reportCategoryChipCount: Int = 3
    const val showsEmphasisChip: Boolean = false
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

    val illustrationHeight = 132.dp
    val illustrationStrokeWidth = 3.dp
    val illustrationThinStrokeWidth = 2.dp
    val heroIconSize = 56.dp
    val supportingIconContainerSize = 44.dp
    val supportingIconSize = 24.dp
    val supportingTitleLineHeight = 22.sp
    val supportingDescriptionLineHeight = 21.sp
    val supportingItemGap = 18.dp
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
