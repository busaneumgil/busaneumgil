package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.app.navigation.SearchRoute
import com.ssafy.e102.eumgil.R
import com.ssafy.e102.eumgil.core.designsystem.theme.BusanEumgilLightColorScheme
import com.ssafy.e102.eumgil.core.designsystem.theme.EumRadius
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchScreenTest {
    @Test
    fun `origin editing target resolves origin specific search copy`() {
        assertEquals(
            SearchCopyUiState(
                entryTitleRes = R.string.search_origin_screen_title,
                resultsTitleRes = R.string.search_origin_results_screen_title,
                entryHeadlineRes = R.string.search_origin_screen_entry_headline,
                queryPlaceholderRes = R.string.search_origin_screen_query_placeholder,
                voiceInputTitleRes = R.string.search_origin_voice_input_title,
                voiceInputHeadlineRes = R.string.search_origin_voice_input_headline,
                voiceInputDescriptionRes = R.string.search_origin_voice_input_description,
                voiceInputExamplePhraseRes = R.string.search_origin_voice_input_example_phrase,
                initialTitleRes = R.string.search_origin_screen_initial_title,
                initialDescriptionRes = R.string.search_origin_screen_initial_description,
                resultSummaryRes = R.string.search_origin_screen_result_summary,
                emptyResultDescriptionRes = R.string.search_origin_screen_empty_result_description,
                resultActionLabelRes = R.string.search_origin_screen_result_action_label,
                resultSelectableDescriptionRes = R.string.search_origin_screen_result_selectable,
            ),
            resolveSearchCopyUiState(RouteEditingTarget.ORIGIN),
        )
    }

    @Test
    fun `destination editing target resolves destination specific search copy`() {
        assertEquals(
            SearchCopyUiState(
                entryTitleRes = R.string.search_screen_title,
                resultsTitleRes = R.string.search_results_screen_title,
                entryHeadlineRes = R.string.search_screen_entry_headline,
                queryPlaceholderRes = R.string.search_screen_query_placeholder,
                voiceInputTitleRes = R.string.search_voice_input_title,
                voiceInputHeadlineRes = R.string.search_voice_input_headline,
                voiceInputDescriptionRes = null,
                voiceInputExamplePhraseRes = R.string.search_voice_input_example_phrase,
                initialTitleRes = R.string.search_screen_initial_title,
                initialDescriptionRes = R.string.search_screen_initial_description,
                resultSummaryRes = R.string.search_screen_result_summary,
                emptyResultDescriptionRes = R.string.search_screen_empty_result_description,
                resultActionLabelRes = R.string.search_screen_result_action_label,
                resultSelectableDescriptionRes = R.string.search_screen_result_selectable,
            ),
            resolveSearchCopyUiState(RouteEditingTarget.DESTINATION),
        )
    }

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
    fun `results screen suppresses non-result placeholder cards while editing`() {
        assertEquals(
            false,
            shouldShowSearchResultSection(
                resultState = SearchResultUiState.EmptyQuery,
            ),
        )
        assertEquals(
            false,
            shouldShowSearchResultSection(
                resultState = SearchResultUiState.Typing(query = "Busan Station"),
            ),
        )
        assertEquals(
            true,
            shouldShowSearchResultSection(
                resultState = SearchResultUiState.Empty(query = "Busan Station"),
            ),
        )
    }

    @Test
    fun `voice input sheet uses fe bottom sheet radius and app surface background`() {
        assertEquals(EumRadius.scaleL, searchVoiceInputSheetTopCornerRadius())
        assertEquals(BusanEumgilLightColorScheme.surface, searchVoiceInputSheetContainerColor())
    }

    @Test
    fun `voice input sheet hides the initial start prompt until the mic button is tapped`() {
        assertEquals(
            null,
            resolveSearchVoiceInputStatusContent(
                SearchVoiceInputUiState(
                    isActive = true,
                    status = SearchVoiceInputStatus.Idle,
                    guidance = SearchVoiceInputGuidance.None,
                ),
            ),
        )
    }

    @Test
    fun `voice input sheet shows retry guidance after an empty capture`() {
        assertEquals(
            SearchVoiceInputStatusContent(
                titleRes = R.string.search_voice_input_status_retry_message,
                descriptionRes = null,
            ),
            resolveSearchVoiceInputStatusContent(
                SearchVoiceInputUiState(
                    isActive = true,
                    status = SearchVoiceInputStatus.Idle,
                    guidance = SearchVoiceInputGuidance.RetryRequired,
                ),
            ),
        )
    }

    @Test
    fun `voice input sheet shows recognized status before moving to results`() {
        assertEquals(
            SearchVoiceInputStatusContent(
                titleRes = R.string.search_voice_input_status_recognized_title,
                descriptionRes = R.string.search_voice_input_status_recognized_description,
            ),
            resolveSearchVoiceInputStatusContent(
                SearchVoiceInputUiState(
                    isActive = true,
                    transcript = "recognized speech",
                    status = SearchVoiceInputStatus.Recognized,
                    guidance = SearchVoiceInputGuidance.None,
                ),
            ),
        )
    }

    @Test
    fun `voice input sheet keeps listening status title without extra helper copy`() {
        assertEquals(
            SearchVoiceInputStatusContent(
                titleRes = R.string.search_voice_input_status_listening_title,
                descriptionRes = null,
            ),
            resolveSearchVoiceInputStatusContent(
                SearchVoiceInputUiState(
                    isActive = true,
                    status = SearchVoiceInputStatus.Listening,
                    guidance = SearchVoiceInputGuidance.None,
                ),
            ),
        )
    }

    @Test
    fun `voice input sheet replaces the example phrase with transcript preview once speech is recognized`() {
        assertEquals(
            false,
            shouldShowSearchVoiceInputTranscriptPreview(
                SearchVoiceInputUiState(
                    isActive = true,
                    transcript = "",
                ),
            ),
        )
        assertEquals(
            true,
            shouldShowSearchVoiceInputTranscriptPreview(
                SearchVoiceInputUiState(
                    isActive = true,
                    transcript = "recognized speech",
                    status = SearchVoiceInputStatus.Recognized,
                ),
            ),
        )
    }

    @Test
    fun `verified search result exposes selectable state description`() {
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

        assertEquals(R.string.search_screen_result_selectable, resolveSearchResultStateDescriptionRes(result))
    }

    @Test
    fun `provider only search result with valid coordinates exposes selectable state description`() {
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

        assertEquals(R.string.search_screen_result_selectable, resolveSearchResultStateDescriptionRes(result))
    }

    @Test
    fun `search result accessibility labels keep positive labels only sorted and collapsed`() {
        val uiState =
            resolveSearchResultAccessibilityTagUiState(
                listOf(
                    "accessible-parking",
                    "wide-entry",
                    "elevator",
                    "accessible-toilet",
                    "table-spacing",
                ),
            )

        assertEquals(
            listOf(
                R.string.place_accessibility_label_entry_available,
                R.string.place_accessibility_label_elevator,
                R.string.place_accessibility_label_accessible_parking,
            ),
            uiState.labelResIds,
        )
        assertEquals(1, uiState.overflowCount)
    }

    @Test
    fun `search result accessibility labels ignore unsupported keys`() {
        val uiState =
            resolveSearchResultAccessibilityTagUiState(
                listOf(
                    "charging-station",
                    "open-24-hours",
                ),
            )

        assertEquals(emptyList<Int>(), uiState.labelResIds)
        assertEquals(0, uiState.overflowCount)
    }
}
