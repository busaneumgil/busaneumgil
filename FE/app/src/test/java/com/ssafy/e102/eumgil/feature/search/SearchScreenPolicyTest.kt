package com.ssafy.e102.eumgil.feature.search

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScreenPolicyTest {
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
                .substringBefore("@Composable\nprivate fun SearchResultAccessibilityTagRow")

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
    }

    @Test
    fun `results screen loading state uses spinner instead of placeholder card`() {
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
    }
}
