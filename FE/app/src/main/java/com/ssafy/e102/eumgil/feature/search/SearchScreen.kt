package com.ssafy.e102.eumgil.feature.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.component.navigation.EumCenteredTopBar
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.designsystem.theme.EumSpacing
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchResult

enum class SearchScreenDestination {
    Entry,
    Results,
}

internal data class DestinationPromoBannerModel(
    @DrawableRes val imageRes: Int,
    @StringRes val contentDescriptionRes: Int,
)

internal fun searchDestinationPromoBannerModel(): DestinationPromoBannerModel =
    DestinationPromoBannerModel(
        imageRes = R.drawable.dest01_accessibility_banner,
        contentDescriptionRes = R.string.search_screen_promo_banner_content_description,
    )

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    modifier: Modifier = Modifier,
    destination: SearchScreenDestination = SearchScreenDestination.Entry,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopBar(
                titleRes =
                    when (destination) {
                        SearchScreenDestination.Entry -> R.string.search_screen_title
                        SearchScreenDestination.Results -> R.string.search_results_screen_title
                    },
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
            when (destination) {
                SearchScreenDestination.Entry ->
                    SearchEntryContent(
                        uiState = uiState,
                        onAction = onAction,
                    )

                SearchScreenDestination.Results ->
                    SearchResultsContent(
                        uiState = uiState,
                        onAction = onAction,
                    )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    @StringRes titleRes: Int,
    onBackClick: () -> Unit,
) {
    EumCenteredTopBar(
        title = stringResource(id = titleRes),
        onBackClick = onBackClick,
        backContentDescription = stringResource(id = R.string.search_screen_back),
    )
}

@Composable
private fun SearchEntryContent(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
) {
    Text(
        text = stringResource(id = R.string.search_screen_entry_headline),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
    SearchInputField(
        query = uiState.query,
        showEmptyQueryError = uiState.resultState is SearchResultUiState.EmptyQuery,
        onQueryChanged = { onAction(SearchUiAction.QueryChanged(query = it)) },
        onSearch = { onAction(SearchUiAction.SearchSubmitted) },
    )
    RecentVisitSection(
        recentSearches = uiState.recentSearches,
        onAction = onAction,
    )
    DestinationPromoBanner()
}

@Composable
private fun SearchResultsContent(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
) {
    SearchInputField(
        query = uiState.query,
        showEmptyQueryError = uiState.resultState is SearchResultUiState.EmptyQuery,
        onQueryChanged = { onAction(SearchUiAction.QueryChanged(query = it)) },
        onSearch = { onAction(SearchUiAction.SearchSubmitted) },
    )
    SearchResultSection(
        resultState = uiState.resultState,
        onAction = onAction,
    )
}

@Composable
private fun SearchInputField(
    query: String,
    showEmptyQueryError: Boolean,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = stringResource(id = R.string.search_screen_query_placeholder)) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_nav_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_permission_mic),
                contentDescription = stringResource(id = R.string.search_screen_voice_input),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        singleLine = true,
        isError = showEmptyQueryError,
        shape = RoundedCornerShape(EumRadius.small),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(
                onSearch = { onSearch() },
            ),
        supportingText =
            if (showEmptyQueryError) {
                {
                    Text(text = stringResource(id = R.string.search_screen_empty_query_description))
                }
            } else {
                null
            },
    )
}

@Composable
private fun SearchResultSection(
    resultState: SearchResultUiState,
    onAction: (SearchUiAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        when (resultState) {
            SearchResultUiState.Initial ->
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_initial_title),
                    description = stringResource(id = R.string.search_screen_initial_description),
                )

            SearchResultUiState.EmptyQuery ->
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_empty_query_title),
                    description = stringResource(id = R.string.search_screen_empty_query_description),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.26f),
                )

            is SearchResultUiState.Typing ->
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_typing_title, resultState.query),
                    description = stringResource(id = R.string.search_screen_typing_description),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                )

            is SearchResultUiState.Loading ->
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_loading_title, resultState.query),
                    description = stringResource(id = R.string.search_screen_loading_description),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                )

            is SearchResultUiState.Success -> {
                Text(
                    text = stringResource(id = R.string.search_screen_result_summary, resultState.results.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                resultState.results.forEach { result ->
                    SearchResultItem(
                        result = result,
                        onClick = {
                            onAction(SearchUiAction.SearchResultBriefingClicked(result = result))
                        },
                    )
                }
            }

            is SearchResultUiState.Empty ->
                SearchStateCard(
                    title =
                        stringResource(
                            id = R.string.search_screen_empty_result_title,
                            resultState.query,
                        ),
                    description = stringResource(id = R.string.search_screen_empty_result_description),
                )

            is SearchResultUiState.Error ->
                SearchStateCard(
                    title = stringResource(id = R.string.search_screen_error_title),
                    description = stringResource(id = R.string.search_screen_error_description),
                    supportingText = resultState.message,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.26f),
                )
        }
    }
}

@Composable
private fun RecentVisitSection(
    recentSearches: List<RecentSearch>,
    onAction: (SearchUiAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(EumSpacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.search_screen_recent_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (recentSearches.isEmpty()) {
            SearchStateCard(
                title = stringResource(id = R.string.search_screen_recent_section_title),
                description = stringResource(id = R.string.search_screen_recent_empty),
            )
        } else {
            recentSearches.forEach { recentSearch ->
                RecentVisitItem(
                    keyword = recentSearch.keyword,
                    onClick = {
                        onAction(
                            SearchUiAction.RecentSearchClicked(
                                keyword = recentSearch.keyword,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RecentVisitItem(
    keyword: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ),
        shape = RoundedCornerShape(EumRadius.small),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Text(
            text = keyword,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(EumSpacing.medium),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DestinationPromoBanner(
    modifier: Modifier = Modifier,
) {
    val model = searchDestinationPromoBannerModel()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EumRadius.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
        shadowElevation = 2.dp,
    ) {
        Image(
            painter = painterResource(id = model.imageRes),
            contentDescription = stringResource(id = model.contentDescriptionRes),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
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
