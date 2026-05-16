package com.ssafy.e102.eumgil.feature.search

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenPolicyTest {
    @Test
    fun `apply to route search exposes route endpoint quick actions`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt")
                .readText()

        assertTrue(
            "Apply-to-route search should show quick actions only in route assignment mode.",
            source.contains("shouldShowRouteEndpointQuickActions(uiState.selectionMode)") &&
                source.contains("RouteEndpointQuickActionSection("),
        )
        assertTrue(
            "Route endpoint quick actions should dispatch dedicated current-location and map-picker actions.",
            source.contains("SearchUiAction.CurrentLocationClicked") &&
                source.contains("SearchUiAction.MapPickerClicked"),
        )
        val quickActionSection =
            source
                .substringAfter("private fun RouteEndpointQuickActionSection(")
                .substringBefore("@Composable\nprivate fun RouteEndpointCurrentLocationButton")
        assertTrue(
            "Route endpoint quick actions should place current-location and map-picker buttons on one row.",
            quickActionSection.contains("Row(") &&
                quickActionSection.contains("horizontalArrangement = Arrangement.spacedBy(EumSpacing.small)") &&
                quickActionSection.contains("modifier = Modifier.weight(1f)"),
        )
        assertTrue(
            "Route endpoint quick actions should use target-specific visible labels and accessibility copy.",
            source.contains("R.string.search_screen_current_location_origin_action") &&
                source.contains("R.string.search_screen_current_location_destination_action") &&
                source.contains("R.string.search_screen_map_picker_origin_action") &&
                source.contains("R.string.search_screen_map_picker_destination_action") &&
                source.contains("R.string.search_screen_map_picker_origin_a11y") &&
                source.contains("R.string.search_screen_map_picker_destination_a11y"),
        )
        assertTrue(
            "Current-location failures should remain visible on the screen instead of being only transient feedback.",
            source.contains("currentLocationQuickActionState") &&
                source.contains("resolveSearchCurrentLocationStatusContent("),
        )
    }

    @Test
    fun `search screen suppresses ripple on row taps that navigate away from the current view`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt")
                .readText()
        val recentVisitItemSection =
            source
                .substringAfter("private fun RecentVisitItem(")
                .substringBefore("@Composable\nprivate fun DestinationPromoBanner")
        val searchResultItemSection =
            source
                .substringAfter("private fun SearchResultItem(")
                .substringBefore("private fun SearchResultAccessibilityTagRow")

        assertTrue(
            "Recent search rows should suppress ripple because they push the user into the results route.",
            recentVisitItemSection.contains("indication = null"),
        )
        assertTrue(
            "Recent search rows should keep a dedicated interaction source when ripple is suppressed.",
            recentVisitItemSection.contains("MutableInteractionSource()"),
        )
        assertTrue(
            "Search result rows should suppress ripple because they navigate to map preview from the search flow.",
            searchResultItemSection.contains("indication = null"),
        )
        assertTrue(
            "Search result rows should keep a dedicated interaction source when ripple is suppressed.",
            searchResultItemSection.contains("MutableInteractionSource()"),
        )
        assertTrue(
            "Search result rows should use a list item divider instead of card chrome.",
            searchResultItemSection.contains("HorizontalDivider("),
        )
        assertFalse(
            "Search result rows should not render each result inside a card-like Surface.",
            searchResultItemSection.contains("Surface(") ||
                searchResultItemSection.contains("shadowElevation") ||
                searchResultItemSection.contains("shape = RoundedCornerShape(EumRadius.large)"),
        )
    }

    @Test
    fun `results screen loading state uses spinner without illustration or placeholder card`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt")
                .readText()
        val searchResultsContentSection =
            source
                .substringAfter("private fun SearchResultsContent(")
                .substringBefore("@Composable\nprivate fun SearchInputField")
        val loadingStateSection =
            searchResultsContentSection
                .substringAfter("is SearchResultUiState.Loading ->")
                .substringBefore("is SearchResultUiState.Success ->")

        assertTrue(
            "Search results loading state should use the centered branded state message.",
            loadingStateSection.contains("SearchCenteredStateMessage("),
        )
        assertTrue(
            "Search results loading state should keep a spinner affordance inside the centered state.",
            loadingStateSection.contains("showLoadingIndicator = true"),
        )
        assertTrue(
            "Search results loading state should not render the centered illustration while the request is in progress.",
            loadingStateSection.contains("showIllustration = false"),
        )
        assertFalse(
            "Search results loading state should not fall back to the boxed SearchStateCard placeholder.",
            loadingStateSection.contains("SearchStateCard("),
        )
    }

    @Test
    fun `empty result state renders as centered branded message without card chrome`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt")
                .readText()
        val searchResultsContentSection =
            source
                .substringAfter("private fun SearchResultsContent(")
                .substringBefore("@Composable\nprivate fun SearchInputField")
        val emptyStateSection =
            searchResultsContentSection
                .substringAfter("is SearchResultUiState.Empty ->")
                .substringBefore("is SearchResultUiState.Error ->")
        val emptyMessageSection =
            source
                .substringAfter("private fun SearchCenteredStateMessage(")
                .substringBefore("@Composable\nprivate fun SearchStateCard")

        assertTrue(
            "Empty search results should use a centered branded message block.",
            emptyStateSection.contains("SearchCenteredStateMessage("),
        )
        assertTrue(
            "The centered state should include the Busan Eumgil character asset.",
            emptyMessageSection.contains("R.drawable.manual_galmaegi"),
        )
        assertFalse(
            "Empty search results should not render inside SearchStateCard card chrome.",
            emptyStateSection.contains("SearchStateCard("),
        )
        assertFalse(
            "The plain empty-result message should not add card border or shadow.",
            emptyMessageSection.contains("Surface(") ||
                emptyMessageSection.contains("BorderStroke(") ||
                emptyMessageSection.contains("shadowElevation"),
        )
    }

    @Test
    fun `search result error state avoids card chrome`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt")
                .readText()
        val searchResultsContentSection =
            source
                .substringAfter("private fun SearchResultsContent(")
                .substringBefore("@Composable\nprivate fun SearchInputField")
        val errorStateSection =
            searchResultsContentSection
                .substringAfter("is SearchResultUiState.Error ->")
                .substringBefore("        }\n    }")

        assertTrue(
            "Search result errors should use the same centered branded state as empty/loading states.",
            errorStateSection.contains("SearchCenteredStateMessage("),
        )
        assertFalse(
            "Search result errors should not render as tinted cards over the results screen.",
            errorStateSection.contains("SearchStateCard(") ||
                errorStateSection.contains("errorContainer") ||
                errorStateSection.contains("BorderStroke("),
        )
        assertFalse(
            "Search result errors should not surface low-level supporting messages such as location preconditions.",
            errorStateSection.contains("supportingText = resultState.message"),
        )
    }

    @Test
    fun `search empty and error states use large split titles`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt")
                .readText()
        val stringsSource = File("src/main/res/values/strings.xml").readText()

        assertTrue(
            "Search empty and error titles should use a larger centered title style.",
            source.contains("MaterialTheme.typography.headlineSmall.copy(lineHeight = SearchStateTitleLineHeight)"),
        )
        assertTrue(
            "Search empty and error copy should use explicit line breaks requested for the empty/error states.",
            stringsSource.contains("<string name=\"search_screen_empty_result_title\">검색 결과가\\n존재하지 않습니다.</string>") &&
                stringsSource.contains("<string name=\"search_screen_error_title\">검색 결과를\\n불러오지 못했습니다</string>"),
        )
    }

    @Test
    fun `empty result state uses requested title and description typography`() {
        val source =
            File("src/main/java/com/ssafy/e102/eumgil/feature/search/SearchScreen.kt")
                .readText()
        val emptyStateSection =
            source
                .substringAfter("is SearchResultUiState.Empty ->")
                .substringBefore("is SearchResultUiState.Error ->")
        val centeredStateSection =
            source
                .substringAfter("private fun SearchCenteredStateMessage(")
                .substringBefore("@Composable\nprivate fun SearchStateCard")

        assertTrue(
            "Empty result state should opt into the dedicated 32px title typography.",
            emptyStateSection.contains("useEmptyResultTypography = true"),
        )
        assertTrue(
            "Empty result title should be 32px bold.",
            centeredStateSection.contains("fontSize = 32.sp") &&
                centeredStateSection.contains("fontWeight = FontWeight.Bold"),
        )
        assertTrue(
            "Empty result description should be 16px regular with 16dp spacing from the title.",
            centeredStateSection.contains("fontSize = 16.sp") &&
                centeredStateSection.contains("fontWeight = FontWeight.Normal") &&
                centeredStateSection.contains("val descriptionTopPadding = if (useEmptyResultTypography) 16.dp"),
        )
    }
}
