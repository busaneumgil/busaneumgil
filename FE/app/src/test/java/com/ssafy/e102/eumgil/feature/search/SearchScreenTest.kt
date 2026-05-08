package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.app.navigation.SearchRoute
import androidx.compose.ui.graphics.Color
import com.ssafy.e102.eumgil.core.model.SearchResult
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

    @Test
    fun `verified search result remains selectable`() {
        val result =
            SearchResult(
                placeId = "10",
                serverPlaceId = "10",
                providerPlaceId = "123456789",
                title = "Busan Tower",
                subtitle = "1 Yongdusan-gil, Busan",
                latitude = 35.1000,
                longitude = 129.0320,
                matched = true,
            )

        val interactionState = resolveSearchResultInteractionState(result)

        assertEquals(true, interactionState.isActionEnabled)
        assertEquals(SearchResultVerificationState.Verified, interactionState.verificationState)
    }

    @Test
    fun `provider only search result is exposed as blocked interaction`() {
        val result =
            SearchResult(
                placeId = "provider:kakao:987654321",
                serverPlaceId = null,
                providerPlaceId = "987654321",
                title = "Provider Only Cafe",
                subtitle = "2 Gwangbok-ro, Busan",
                latitude = 35.1010,
                longitude = 129.0330,
                matched = false,
            )

        val interactionState = resolveSearchResultInteractionState(result)

        assertEquals(false, interactionState.isActionEnabled)
        assertEquals(SearchResultVerificationState.Unverified, interactionState.verificationState)
    }
}
