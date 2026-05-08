package com.ssafy.e102.eumgil.feature.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.BuildConfig
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing

private data class MyPageAppInfoActionItem(
    val titleRes: Int,
    val iconRes: Int,
    val trailingText: String? = null,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun MyPageAppInfoScreen(
    uiState: MyPageAppInfoUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onGuideClick: () -> Unit,
    onInquiryClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onServiceTermsClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isWithdrawLoading = uiState.isWithdrawLoading
    val withdrawLabel =
        if (isWithdrawLoading) {
            stringResource(id = R.string.my_page_app_info_withdraw_loading)
        } else {
            stringResource(id = R.string.my_page_app_info_withdraw)
        }
    val withdrawStateDescription =
        if (isWithdrawLoading) {
            stringResource(id = R.string.my_page_app_info_withdraw_state_loading)
        } else {
            stringResource(id = R.string.my_page_app_info_withdraw_state_enabled)
        }
    val supportItems =
        listOf(
            MyPageAppInfoActionItem(
                titleRes = R.string.my_page_app_info_guide,
                iconRes = R.drawable.ic_terms_document,
                onClick = onGuideClick,
            ),
            MyPageAppInfoActionItem(
                titleRes = R.string.my_page_app_info_customer_center,
                iconRes = R.drawable.ic_permission_contacts,
                trailingText = stringResource(id = R.string.my_page_app_info_customer_center_number),
            ),
            MyPageAppInfoActionItem(
                titleRes = R.string.my_page_app_info_inquiry,
                iconRes = R.drawable.ic_status_help_circle,
                onClick = onInquiryClick,
            ),
        )
    val policyItems =
        listOf(
            MyPageAppInfoActionItem(
                titleRes = R.string.my_page_app_info_privacy_policy,
                iconRes = R.drawable.ic_terms_privacy,
                onClick = onPrivacyPolicyClick,
            ),
            MyPageAppInfoActionItem(
                titleRes = R.string.my_page_app_info_service_terms,
                iconRes = R.drawable.ic_terms_document,
                onClick = onServiceTermsClick,
            ),
        )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MyPageAppInfoTopBar(onBackClick = onBackClick)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            item {
                MyPageAppInfoHeroCard()
            }

            item {
                MyPageAppInfoSectionCard(items = supportItems)
            }

            item {
                MyPageAppInfoSectionCard(items = policyItems)
            }

            item {
                Button(
                    onClick = onWithdrawClick,
                    enabled = !isWithdrawLoading,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics {
                                stateDescription = withdrawStateDescription
                            },
                    shape = RoundedCornerShape(EumRadius.small),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                            disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.60f),
                        ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isWithdrawLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            text = withdrawLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPageAppInfoTopBar(onBackClick: () -> Unit) {
    EumCenteredTopBar(
        title = stringResource(id = R.string.my_page_app_info_title),
        onBackClick = onBackClick,
        backContentDescription = stringResource(id = R.string.my_page_back),
        titleFontWeight = FontWeight.Bold,
    )
}

@Composable
private fun MyPageAppInfoHeroCard() {
    val style = myPageAppInfoHeroCardStyle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        colors = CardDefaults.cardColors(containerColor = style.containerColor),
        border = BorderStroke(width = 1.dp, color = style.borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(EumRadius.medium),
                color = style.logoSurfaceColor,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(EumSpacing.xxSmall),
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = style.titleColor,
                )
                Text(
                    text = stringResource(id = R.string.my_page_app_info_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.titleSmall,
                    color = style.versionColor,
                )
                Text(
                    text = stringResource(id = R.string.my_page_app_info_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = style.descriptionColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MyPageAppInfoSectionCard(items: List<MyPageAppInfoActionItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
    ) {
        Column {
            items.forEachIndexed { index, item ->
                MyPageAppInfoRow(item = item)
                if (index != items.lastIndex) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = EumSpacing.medium),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPageAppInfoRow(item: MyPageAppInfoActionItem) {
    val title = stringResource(id = item.titleRes)
    val clickableModifier =
        if (item.onClick != null) {
            Modifier.clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = item.onClick,
            )
        } else {
            Modifier
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.small),
        horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        item.trailingText?.let { trailingText ->
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.onClick != null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_dropdown),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(20.dp)
                        .rotate(-90f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
