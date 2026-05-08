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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
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
    VoiceInput,
}

enum class SearchTrailingAction {
    VoiceInput,
    ClearQuery,
}

internal fun resolveSearchTrailingAction(query: String): SearchTrailingAction =
    if (query.isEmpty()) {
        SearchTrailingAction.VoiceInput
    } else {
        SearchTrailingAction.ClearQuery
    }

internal fun resolveVoiceInputBackgroundDestination(resultState: SearchResultUiState): SearchScreenDestination =
    when (resultState) {
        SearchResultUiState.Initial,
        SearchResultUiState.EmptyQuery,
        is SearchResultUiState.Typing,
        -> SearchScreenDestination.Entry

        is SearchResultUiState.Loading,
        is SearchResultUiState.Success,
        is SearchResultUiState.Empty,
        is SearchResultUiState.Error,
        -> SearchScreenDestination.Results
    }

internal fun searchVoiceInputSheetTopCornerRadius(): Dp = EumRadius.scaleL

internal fun searchVoiceInputSheetContainerColor(): Color = Color.White

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
    when (destination) {
        SearchScreenDestination.VoiceInput ->
            SearchVoiceInputScreen(
                uiState = uiState,
                onAction = onAction,
                modifier = modifier,
            )

        SearchScreenDestination.Entry,
        SearchScreenDestination.Results,
        -> SearchPrimaryScreen(
            uiState = uiState,
            onAction = onAction,
            destination = destination,
            modifier = modifier,
        )
    }
}

@Composable
private fun SearchPrimaryScreen(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    destination: SearchScreenDestination,
    modifier: Modifier = Modifier,
) {
    val titleRes =
        when (destination) {
            SearchScreenDestination.Entry -> R.string.search_screen_title
            SearchScreenDestination.Results -> R.string.search_results_screen_title
            SearchScreenDestination.VoiceInput -> R.string.search_voice_input_title
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopBar(
                titleRes = titleRes,
                onBackClick = { onAction(SearchUiAction.BackClicked) },
            )
        },
    ) { innerPadding ->
        SearchContentBody(
            uiState = uiState,
            onAction = onAction,
            destination = destination,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
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
private fun SearchContentBody(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    destination: SearchScreenDestination,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
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

            SearchScreenDestination.VoiceInput -> Unit
        }
    }
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
        onVoiceInputClick = { onAction(SearchUiAction.VoiceInputClicked) },
        onClearQueryClick = { onAction(SearchUiAction.ClearQueryClicked) },
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
        onVoiceInputClick = { onAction(SearchUiAction.VoiceInputClicked) },
        onClearQueryClick = { onAction(SearchUiAction.ClearQueryClicked) },
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
    onVoiceInputClick: () -> Unit,
    onClearQueryClick: () -> Unit,
    onSearch: () -> Unit,
) {
    val trailingAction = resolveSearchTrailingAction(query = query)

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
            when (trailingAction) {
                SearchTrailingAction.VoiceInput ->
                    IconButton(onClick = onVoiceInputClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search_voice_mic),
                            contentDescription = stringResource(id = R.string.search_screen_voice_input),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }

                SearchTrailingAction.ClearQuery ->
                    IconButton(onClick = onClearQueryClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_action_close),
                            contentDescription = stringResource(id = R.string.search_screen_clear_query),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchVoiceInputScreen(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundDestination = resolveVoiceInputBackgroundDestination(uiState.resultState)
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = modifier.fillMaxSize()) {
        SearchPrimaryScreen(
            uiState = uiState,
            onAction = onAction,
            destination = backgroundDestination,
            modifier = Modifier.fillMaxSize(),
        )
        ModalBottomSheet(
            onDismissRequest = { onAction(SearchUiAction.VoiceInputDismissed) },
            sheetState = bottomSheetState,
            dragHandle = null,
            shape =
                RoundedCornerShape(
                    topStart = searchVoiceInputSheetTopCornerRadius(),
                    topEnd = searchVoiceInputSheetTopCornerRadius(),
                    bottomEnd = 0.dp,
                    bottomStart = 0.dp,
                ),
            containerColor = searchVoiceInputSheetContainerColor(),
            scrimColor = Color.Black.copy(alpha = 0.38f),
        ) {
            SearchVoiceInputContent(
                uiState = uiState,
                onAction = onAction,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SearchVoiceInputContent(
    uiState: SearchUiState,
    onAction: (SearchUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusTitleRes =
        when (uiState.voiceInputState.status) {
            SearchVoiceInputStatus.Idle -> R.string.search_voice_input_status_idle_title
            SearchVoiceInputStatus.Listening -> R.string.search_voice_input_status_listening_title
        }
    val statusDescriptionRes =
        when (uiState.voiceInputState.status) {
            SearchVoiceInputStatus.Idle -> R.string.search_voice_input_status_idle_description
            SearchVoiceInputStatus.Listening -> R.string.search_voice_input_status_listening_description
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.search_voice_input_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = { onAction(SearchUiAction.VoiceInputDismissed) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_close),
                    contentDescription = stringResource(id = R.string.search_voice_input_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = stringResource(id = R.string.search_voice_input_headline),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Surface(
            modifier = Modifier.padding(top = EumSpacing.medium),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        ) {
            Text(
                text = stringResource(id = R.string.search_voice_input_example_phrase),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier =
                Modifier
                    .padding(top = 28.dp)
                    .size(132.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {}
            Surface(
                onClick = { onAction(SearchUiAction.VoiceCaptureButtonClicked) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
            ) {
                Box(
                    modifier = Modifier.size(108.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search_voice_mic),
                        contentDescription = stringResource(id = R.string.search_screen_voice_input),
                        modifier = Modifier.size(40.dp),
                        tint = Color.White,
                    )
                }
            }
        }

        Text(
            text = stringResource(id = statusTitleRes),
            modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(id = statusDescriptionRes),
            modifier = Modifier.padding(top = EumSpacing.xSmall),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.voiceInputState.transcript.isNotBlank()) {
            SearchStateCard(
                title = stringResource(id = R.string.search_voice_input_transcript_title),
                description = uiState.voiceInputState.transcript,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = EumSpacing.large, bottom = EumSpacing.medium),
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
            )
        }
    }
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
                            onAction(SearchUiAction.SearchResultClicked(result = result))
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
