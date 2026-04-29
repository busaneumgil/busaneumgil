package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

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

                MyPageReportHistoryScreenState.CONTENT ->
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

@Composable
private fun MyPageReportHistoryTopBar(onBackClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.xSmall, vertical = EumSpacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_back),
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "제보 내역",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ReportHistoryCard(
    report: MyPageReportHistoryUiModel,
    onClick: () -> Unit,
) {
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
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.small),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
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
    Surface(
        modifier = Modifier.size(width = 88.dp, height = 76.dp),
        shape = RoundedCornerShape(EumRadius.medium),
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
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
        shadowElevation = 1.dp,
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
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = primaryActionLabel)
                    }
                    if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = secondaryActionLabel)
                        }
                    }
                }
            }
        }
    }
}
