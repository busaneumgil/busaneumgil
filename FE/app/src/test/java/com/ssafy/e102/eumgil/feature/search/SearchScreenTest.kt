package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.app.navigation.SearchRoute
import androidx.compose.ui.graphics.Color
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchScreenTest {
    @Test
    fun `search screen destination exposes entry results and voice input modes`() {
        assertEquals(
            listOf(
                SearchScreenDestination.Entry,
                SearchScreenDestination.Results,
                SearchScreenDestination.VoiceInput,
            ),
            SearchScreenDestination.entries.toList(),
        )
    }

    @Test
    fun `blank query shows voice action and typed query shows clear action`() {
        assertEquals(SearchTrailingAction.VoiceInput, resolveSearchTrailingAction(query = ""))
        assertEquals(SearchTrailingAction.ClearQuery, resolveSearchTrailingAction(query = "query"))
        assertEquals(SearchTrailingAction.ClearQuery, resolveSearchTrailingAction(query = " "))
    }

    @Test
    fun `voice route is exposed as dedicated search sub route`() {
        assertEquals("search/voice", SearchRoute.VoiceInput.createRoute())
    }

    @Test
    fun `voice input sheet keeps results background when prior results exist`() {
        assertEquals(
            SearchScreenDestination.Results,
            resolveVoiceInputBackgroundDestination(
                SearchResultUiState.Success(
                    query = "Busan Station",
                    results = emptyList(),
                ),
            ),
        )
        assertEquals(
            SearchScreenDestination.Results,
            resolveVoiceInputBackgroundDestination(
                SearchResultUiState.Error(
                    query = "Busan Station",
                    message = "failed",
                ),
            ),
        )
    }

    @Test
    fun `voice input sheet falls back to entry background without loaded results`() {
        assertEquals(
            SearchScreenDestination.Entry,
            resolveVoiceInputBackgroundDestination(SearchResultUiState.Initial),
        )
        assertEquals(
            SearchScreenDestination.Entry,
            resolveVoiceInputBackgroundDestination(SearchResultUiState.EmptyQuery),
        )
    }

    @Test
    fun `voice input sheet uses fe bottom sheet radius and white background`() {
        assertEquals(EumRadius.scaleL, searchVoiceInputSheetTopCornerRadius())
        assertEquals(Color.White, searchVoiceInputSheetContainerColor())
    }
}
