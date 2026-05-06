package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

internal data class ReportHistoryLayoutSpec(
    val cardCornerRadiusDp: Int,
    val thumbnailCornerRadiusDp: Int,
    val buttonCornerRadiusDp: Int,
    val buttonMinHeightDp: Int,
    val cardShadowElevationDp: Int,
)

internal fun reportHistoryLayoutSpec(): ReportHistoryLayoutSpec =
    ReportHistoryLayoutSpec(
        cardCornerRadiusDp = 12,
        thumbnailCornerRadiusDp = 12,
        buttonCornerRadiusDp = 12,
        buttonMinHeightDp = 48,
        cardShadowElevationDp = 0,
    )

@Composable
fun MyPageReportHistoryScreen(
    uiState: MyPageReportHistoryUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (MyPageReportHistoryUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MyPageReportHistoryTopBar(
                onBackClick = { onAction(MyPageReportHistoryUiAction.BackClicked) },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            when (uiState.screenState) {
                MyPageReportHistoryScreenState.LOADING ->
                    item {
                        ReportHistoryStateCard(
                            title = "제보 내역을 불러오는 중입니다",
                            description = "저장된 제보 목록을 확인하고 있어요.",
                            isLoading = true,
                        )
                    }

                MyPageReportHistoryScreenState.EMPTY ->
                    item {
                        ReportHistoryStateCard(
                            title = "아직 제보 내역이 없어요",
                            description = "이동 중 발견한 보행 불편 사항을 제보해 주세요.",
                            primaryActionLabel = "제보하기",
                            onPrimaryActionClick = {
                                onAction(MyPageReportHistoryUiAction.ReportCtaClicked)
                            },
                        )
                    }

                MyPageReportHistoryScreenState.ERROR ->
                    item {
                        ReportHistoryStateCard(
                            title = "제보 내역을 불러오지 못했습니다",
                            description = uiState.errorMessage ?: "잠시 후 다시 시도해 주세요.",
                            primaryActionLabel = "다시 시도",
                            onPrimaryActionClick = {
                                onAction(MyPageReportHistoryUiAction.RetryClicked)
                            },
                            secondaryActionLabel = "제보하기",
                            onSecondaryActionClick = {
                                onAction(MyPageReportHistoryUiAction.ReportCtaClicked)
                            },
                            isError = true,
                        )
                    }

                MyPageReportHistoryScreenState.CONTENT -> {
                    if (shouldShowReportHistoryCreateCta(uiState.screenState)) {
                        item {
                            ReportHistoryStateCard(
                                title = "새 제보 등록",
                                description = "이동 중 발견한 보행 불편 사항을 추가로 제보할 수 있어요.",
                                primaryActionLabel = "제보하기",
                                onPrimaryActionClick = {
                                    onAction(MyPageReportHistoryUiAction.ReportCtaClicked)
                                },
                            )
                        }
                    }

                    items(
                        items = uiState.reports,
                        key = { report -> report.outboxId },
                    ) { report ->
                        ReportHistoryCard(
                            report = report,
                            onClick = {
                                onAction(MyPageReportHistoryUiAction.ReportClicked(report.outboxId))
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun shouldShowReportHistoryCreateCta(screenState: MyPageReportHistoryScreenState): Boolean =
    screenState == MyPageReportHistoryScreenState.CONTENT

@Composable
private fun MyPageReportHistoryTopBar(onBackClick: () -> Unit) {
    EumCenteredTopBar(
        title = "제보 내역",
        onBackClick = onBackClick,
        backContentDescription = stringResource(id = R.string.my_page_back),
        titleFontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ReportHistoryCard(
    report: MyPageReportHistoryUiModel,
    onClick: () -> Unit,
) {
    val spec = reportHistoryLayoutSpec()
    val accessibilityDescription =
        "제보 내역, ${report.title}, ${report.address}, ${report.submittedAtText}"

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = accessibilityDescription
                },
        shape = RoundedCornerShape(spec.cardCornerRadiusDp.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        shadowElevation = spec.cardShadowElevationDp.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = report.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = report.submittedAtText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ReportHistoryThumbnail(hasPhoto = report.photoUri != null)
        }
    }
}

@Composable
private fun ReportHistoryThumbnail(hasPhoto: Boolean) {
    val spec = reportHistoryLayoutSpec()

    Surface(
        modifier = Modifier.size(width = 88.dp, height = 76.dp),
        shape = RoundedCornerShape(spec.thumbnailCornerRadiusDp.dp),
        color =
            if (hasPhoto) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nav_report),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint =
                    if (hasPhoto) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun ReportHistoryStateCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    primaryActionLabel: String? = null,
    onPrimaryActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    isError: Boolean = false,
) {
    val spec = reportHistoryLayoutSpec()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spec.cardCornerRadiusDp.dp),
        color = MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.36f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
            ),
        shadowElevation = spec.cardShadowElevationDp.dp,
    ) {
        Column(
            modifier = Modifier.padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
            horizontalAlignment = Alignment.Start,
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            if (primaryActionLabel != null && onPrimaryActionClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
                ) {
                    Button(
                        onClick = onPrimaryActionClick,
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(min = spec.buttonMinHeightDp.dp),
                        shape = RoundedCornerShape(spec.buttonCornerRadiusDp.dp),
                        elevation =
                            ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                disabledElevation = 0.dp,
                            ),
                    ) {
                        Text(
                            text = primaryActionLabel,
                            modifier = Modifier.padding(vertical = EumSpacing.xSmall),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = spec.buttonMinHeightDp.dp),
                            shape = RoundedCornerShape(spec.buttonCornerRadiusDp.dp),
                        ) {
                            Text(
                                text = secondaryActionLabel,
                                modifier = Modifier.padding(vertical = EumSpacing.xSmall),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
