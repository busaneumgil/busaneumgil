package com.ssafy.e102.eumgil.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchResult

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopBar(
                onBackClick = { onAction(SearchUiAction.BackClicked) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = EumSpacing.medium, vertical = EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.medium),
        ) {
            SearchQuerySection(
                uiState = uiState,
                onAction = onAction,
            )
            SearchResultSection(
                uiState = uiState,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun SearchTopBar(
    onBackClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EumSpacing.small, vertical = EumSpacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            TextButton(onClick = onBackClick) {
                Text(text = stringResource(id = R.string.search_screen_back))
            }
            Text(
                text = stringResource(id = R.string.search_screen_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SearchQuerySection(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
) {
    val supportingText =
        when (uiState.resultState) {
            SearchResultUiState.Initial ->
                stringResource(id = R.string.search_screen_field_support_initial)

            SearchResultUiState.EmptyQuery ->
                stringResource(id = R.string.search_screen_field_support_empty)

            is SearchResultUiState.Typing ->
                stringResource(id = R.string.search_screen_field_support_typing)

            else -> stringResource(id = R.string.search_screen_field_support_result)
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { onAction(SearchUiAction.QueryChanged(query = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.search_screen_query_label)) },
            placeholder = { Text(text = stringResource(id = R.string.search_screen_query_placeholder)) },
            singleLine = true,
            isError = uiState.resultState is SearchResultUiState.EmptyQuery,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
                KeyboardActions(
                    onSearch = { onAction(SearchUiAction.SearchSubmitted) },
                ),
            supportingText = {
                Text(text = supportingText)
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EumSpacing.small),
        ) {
            OutlinedButton(
                onClick = { onAction(SearchUiAction.ClearQueryClicked) },
                enabled = uiState.query.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = R.string.search_screen_clear_query))
            }

            Button(
                onClick = { onAction(SearchUiAction.SearchSubmitted) },
                enabled = uiState.query.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = R.string.search_screen_submit))
            }
        }
    }
}

@Composable
private fun SearchResultSection(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.search_screen_result_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        when (val resultState = uiState.resultState) {
            SearchResultUiState.Initial -> {
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_initial_title),
                    description = stringResource(id = R.string.search_screen_initial_description),
                )
                RecentSearchSection(
                    recentSearches = uiState.recentSearches,
                    onAction = onAction,
                )
            }

            SearchResultUiState.EmptyQuery -> {
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_empty_query_title),
                    description = stringResource(id = R.string.search_screen_empty_query_description),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.26f),
                )
                RecentSearchSection(
                    recentSearches = uiState.recentSearches,
                    onAction = onAction,
                )
            }

            is SearchResultUiState.Typing -> {
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_typing_title, resultState.query),
                    description = stringResource(id = R.string.search_screen_typing_description),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                )
                SearchResultShellList(query = resultState.query)
                RecentSearchSection(
                    recentSearches = uiState.recentSearches,
                    onAction = onAction,
                )
            }

            is SearchResultUiState.Loading ->
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_loading_title, resultState.query),
                    description = stringResource(id = R.string.search_screen_loading_description),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                )

            is SearchResultUiState.Success -> {
                SearchStateCard(
                    title =
                        stringResource(
                            id = R.string.search_screen_success_title,
                            resultState.query,
                            resultState.results.size,
                        ),
                    description = stringResource(id = R.string.search_screen_success_description),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                )
                resultState.results.forEach { result ->
                    SearchResultItem(
                        result = result,
                        onClick = {
                            onAction(SearchUiAction.SearchResultClicked(result = result))
                        },
                    )
                }
            }

            is SearchResultUiState.Empty -> {
                SearchStateCard(
                    title =
                        stringResource(
                            id = R.string.search_screen_empty_result_title,
                            resultState.query,
                        ),
                    description = stringResource(id = R.string.search_screen_empty_result_description),
                )
                RecentSearchSection(
                    recentSearches = uiState.recentSearches,
                    onAction = onAction,
                )
            }

            is SearchResultUiState.Error -> {
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_error_title),
                    description = stringResource(id = R.string.search_screen_error_description),
                    supportingText = resultState.message,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.26f),
                )
                RecentSearchSection(
                    recentSearches = uiState.recentSearches,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun SearchResultShellList(
    query: String,
) {
    repeat(2) { index ->
        SearchStateCard(
            title = stringResource(id = R.string.search_screen_shell_slot_title, index + 1),
            description =
                stringResource(
                    id = R.string.search_screen_shell_item_description,
                    query,
                ),
            supportingText = stringResource(id = R.string.search_screen_shell_item_supporting),
            containerColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun RecentSearchSection(
    recentSearches: List<RecentSearch>,
    onAction: (SearchUiAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Text(
            text = stringResource(id = R.string.search_screen_recent_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (recentSearches.isEmpty()) {
            SearchStateCard(
                title = stringResource(id = R.string.search_screen_recent_section_title),
                description = stringResource(id = R.string.search_screen_recent_empty),
            )
        } else {
            recentSearches.forEach { recentSearch ->
                OutlinedButton(
                    onClick = {
                        onAction(
                            SearchUiAction.RecentSearchClicked(
                                keyword = recentSearch.keyword,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = recentSearch.keyword)
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit,
) {
    val actionLabel = stringResource(id = R.string.search_screen_result_action_label)
    val selectableStateDescription = stringResource(id = R.string.search_screen_result_selectable)
    val accessibilityDescription =
        if (result.subtitle.isBlank()) {
            stringResource(
                id = R.string.search_screen_result_a11y_without_address,
                result.title,
            )
        } else {
            stringResource(
                id = R.string.search_screen_result_a11y_with_address,
                result.title,
                result.subtitle,
            )
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = actionLabel,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    contentDescription = accessibilityDescription
                    stateDescription = selectableStateDescription
                },
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(EumSpacing.xSmall),
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = result.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    stringResource(
                        id = R.string.search_screen_result_coordinates,
                        result.latitude,
                        result.longitude,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.search_screen_result_id, result.placeId),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SearchStateCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    containerColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
) {
    val resolvedContainerColor =
        if (containerColor == Color.Unspecified) {
            MaterialTheme.colorScheme.surface
        } else {
            containerColor
        }
    val resolvedBorderColor =
        if (borderColor == Color.Unspecified) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
        } else {
            borderColor
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = resolvedContainerColor,
        border = BorderStroke(1.dp, resolvedBorderColor),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
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
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
