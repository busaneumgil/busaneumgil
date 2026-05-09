package com.ssafy.e102.eumgil.feature.search

import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.PlaceDetail
import com.ssafy.e102.eumgil.core.model.RecentDestination
import com.ssafy.e102.eumgil.core.model.RecentSearch
import com.ssafy.e102.eumgil.core.model.SearchQuery
import com.ssafy.e102.eumgil.core.model.SearchResult
import com.ssafy.e102.eumgil.core.model.SearchVoiceAnalysis
import com.ssafy.e102.eumgil.core.model.SearchVoiceIntent
import com.ssafy.e102.eumgil.core.model.SearchVoiceMode
import com.ssafy.e102.eumgil.core.model.toPlaceDestination
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.PlacesRepository
import com.ssafy.e102.eumgil.data.repository.RouteEditingTarget
import com.ssafy.e102.eumgil.data.repository.SearchRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `search submit with query emits results navigation and loads matching results`() =
        runTest {
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(searchResults = listOf(result)),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.QueryChanged(query = "  Busan City Hall  "))
            viewModel.onAction(SearchUiAction.SearchSubmitted)
            advanceUntilIdle()

            assertEquals(
                SearchUiEvent.NavigateToResults(
                    query = "Busan City Hall",
                    editingTarget = RouteEditingTarget.DESTINATION,
                ),
                uiEvent.await(),
            )
            val resultState = viewModel.uiState.value.resultState
            assertTrue(resultState is SearchResultUiState.Success)
            assertEquals("Busan City Hall", (resultState as SearchResultUiState.Success).query)
            assertEquals(listOf(result), resultState.results)
        }

    @Test
    fun `recent search click emits results navigation with selected keyword`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.RecentSearchClicked(keyword = "Busan Station"))
            advanceUntilIdle()

            assertEquals(
                SearchUiEvent.NavigateToResults(
                    query = "Busan Station",
                    editingTarget = RouteEditingTarget.DESTINATION,
                ),
                uiEvent.await(),
            )
            assertEquals("Busan Station", viewModel.uiState.value.query)
        }

    @Test
    fun `recent search delete removes keyword from ui state`() =
        runTest {
            val searchRepository =
                FakeSearchRepository(
                    recentSearches =
                        listOf(
                            RecentSearch(keyword = "Busan City Hall", searchedAtMillis = 2_000L),
                            RecentSearch(keyword = "Busan Station", searchedAtMillis = 1_000L),
                        ),
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = searchRepository,
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.RecentSearchDeleteClicked(keyword = "Busan City Hall"))
            advanceUntilIdle()

            assertEquals(listOf("Busan City Hall"), searchRepository.deletedRecentSearchKeywords)
            assertEquals(
                listOf("Busan Station"),
                viewModel.uiState.value.recentSearches.map(RecentSearch::keyword),
            )
        }

    @Test
    fun `recent search clear all removes every keyword from ui state`() =
        runTest {
            val searchRepository =
                FakeSearchRepository(
                    recentSearches =
                        listOf(
                            RecentSearch(keyword = "Busan City Hall", searchedAtMillis = 2_000L),
                            RecentSearch(keyword = "Busan Station", searchedAtMillis = 1_000L),
                        ),
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = searchRepository,
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.RecentSearchClearAllClicked)
            advanceUntilIdle()

            assertEquals(1, searchRepository.clearRecentSearchesCallCount)
            assertEquals(emptyList<String>(), viewModel.uiState.value.recentSearches.map(RecentSearch::keyword))
        }

    @Test
    fun `voice input click emits voice route navigation`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.VoiceInputClicked)
            advanceUntilIdle()

            assertEquals(SearchUiEvent.NavigateToVoiceInput, uiEvent.await())
        }

    @Test
    fun `fresh search entry re-entry resets empty query warning to default state`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.QueryChanged(query = "   "))
            viewModel.onAction(SearchUiAction.SearchSubmitted)
            advanceUntilIdle()

            assertEquals(SearchResultUiState.EmptyQuery, viewModel.uiState.value.resultState)

            viewModel.onAction(SearchUiAction.EntryRouteEntered(preserveState = false))
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.query)
            assertFalse(viewModel.uiState.value.hasEditedQuery)
            assertEquals(SearchResultUiState.Initial, viewModel.uiState.value.resultState)
        }

    @Test
    fun `search flow re-entry preserves current query and result state`() =
        runTest {
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(searchResults = listOf(result)),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.QueryChanged(query = "Busan City Hall"))
            viewModel.onAction(SearchUiAction.SearchSubmitted)
            advanceUntilIdle()

            val resultStateBeforeReentry = viewModel.uiState.value.resultState

            viewModel.onAction(SearchUiAction.EntryRouteEntered(preserveState = true))
            advanceUntilIdle()

            assertEquals("Busan City Hall", viewModel.uiState.value.query)
            assertTrue(viewModel.uiState.value.hasEditedQuery)
            assertEquals(resultStateBeforeReentry, viewModel.uiState.value.resultState)
        }

    @Test
    fun `voice route entered primes voice input state without emitting capture event`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val unexpectedEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.VoiceRouteEntered)
            advanceUntilIdle()

            assertFalse(unexpectedEvent.isCompleted)
            assertEquals(SearchVoiceInputStatus.Listening, viewModel.uiState.value.voiceInputState.status)
            assertEquals(true, viewModel.uiState.value.voiceInputState.isActive)
            assertEquals(SearchVoiceInputGuidance.None, viewModel.uiState.value.voiceInputState.guidance)
            assertEquals("", viewModel.uiState.value.voiceInputState.transcript)
            unexpectedEvent.cancel()
        }

    @Test
    fun `voice capture button click emits start voice capture event`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.VoiceCaptureButtonClicked)
            advanceUntilIdle()

            assertEquals(SearchUiEvent.StartVoiceCapture, uiEvent.await())
            assertEquals(SearchVoiceInputStatus.Listening, viewModel.uiState.value.voiceInputState.status)
        }

    @Test
    fun `empty voice capture keeps the sheet open and shows retry guidance`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }
            val unexpectedEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.drop(1).first() }

            viewModel.onAction(SearchUiAction.VoiceCaptureButtonClicked)
            advanceUntilIdle()
            viewModel.onAction(SearchUiAction.VoiceCaptureEmpty)
            advanceUntilIdle()

            assertEquals(SearchUiEvent.StartVoiceCapture, uiEvent.await())
            assertFalse(unexpectedEvent.isCompleted)
            assertEquals(true, viewModel.uiState.value.voiceInputState.isActive)
            assertEquals(SearchVoiceInputStatus.Idle, viewModel.uiState.value.voiceInputState.status)
            assertEquals(SearchVoiceInputGuidance.RetryRequired, viewModel.uiState.value.voiceInputState.guidance)
            assertEquals("", viewModel.uiState.value.voiceInputState.transcript)
            unexpectedEvent.cancel()
        }

    @Test
    fun `voice transcript previews the recognized sentence before emitting results navigation`() =
        runTest {
            val result =
                SearchResult(
                    placeId = "place-voice-1",
                    title = "Busan Station",
                    subtitle = "123 Busan-daero, Busan",
                    latitude = 35.1151,
                    longitude = 129.0414,
                    category = PlaceCategory.PUBLIC_OFFICE,
                )
            val searchRepository =
                FakeSearchRepository(
                    searchResults = listOf(result),
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = searchRepository,
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val startEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }
            viewModel.onAction(SearchUiAction.VoiceCaptureButtonClicked)
            advanceUntilIdle()
            assertEquals(SearchUiEvent.StartVoiceCapture, startEvent.await())
            val firstEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }
            val secondEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.drop(1).first() }

            viewModel.onAction(
                SearchUiAction.VoiceTranscriptReceived(
                    transcript = "recognized speech",
                    searchQuery = "Busan Station",
                ),
            )
            runCurrent()

            assertEquals(SearchUiEvent.StopVoiceCapture, firstEvent.await())
            assertFalse(secondEvent.isCompleted)
            assertEquals(true, viewModel.uiState.value.voiceInputState.isActive)
            assertEquals(SearchVoiceInputStatus.Recognized, viewModel.uiState.value.voiceInputState.status)
            assertEquals(SearchVoiceInputGuidance.None, viewModel.uiState.value.voiceInputState.guidance)
            assertEquals("recognized speech", viewModel.uiState.value.voiceInputState.transcript)
            assertTrue(searchRepository.voiceAnalysisRequests.isEmpty())

            advanceTimeBy(VOICE_INPUT_RESULT_PREVIEW_DELAY_MILLIS)
            runCurrent()

            assertEquals(
                SearchUiEvent.NavigateToResults(
                    query = "Busan Station",
                    editingTarget = RouteEditingTarget.DESTINATION,
                ),
                secondEvent.await(),
            )
            val resultState = viewModel.uiState.value.resultState
            assertTrue(resultState is SearchResultUiState.Success)
            assertEquals("Busan Station", (resultState as SearchResultUiState.Success).query)
            assertEquals(listOf(result), resultState.results)
            assertEquals("recognized speech", viewModel.uiState.value.voiceInputState.transcript)
        }

    @Test
    fun `voice transcript without resolved query previews first then analyzes before navigation`() =
        runTest {
            val result =
                SearchResult(
                    placeId = "place-voice-2",
                    title = "Busan Station",
                    subtitle = "123 Busan-daero, Busan",
                    latitude = 35.1151,
                    longitude = 129.0414,
                    category = PlaceCategory.PUBLIC_OFFICE,
                )
            val searchRepository =
                FakeSearchRepository(
                    searchResults = listOf(result),
                    voiceAnalysis =
                        SearchVoiceAnalysis(
                            intent = SearchVoiceIntent.PLACE_SEARCH,
                            placeName = "Busan Station",
                        ),
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = searchRepository,
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val startEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }
            viewModel.onAction(SearchUiAction.VoiceCaptureButtonClicked)
            advanceUntilIdle()
            assertEquals(SearchUiEvent.StartVoiceCapture, startEvent.await())
            val firstEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }
            val secondEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.drop(1).first() }

            viewModel.onAction(
                SearchUiAction.VoiceTranscriptReceived(
                    transcript = "take me to busan station",
                    searchQuery = null,
                ),
            )
            runCurrent()

            assertEquals(SearchUiEvent.StopVoiceCapture, firstEvent.await())
            assertFalse(secondEvent.isCompleted)
            assertEquals("take me to busan station", viewModel.uiState.value.voiceInputState.transcript)
            assertTrue(searchRepository.voiceAnalysisRequests.isEmpty())

            advanceTimeBy(VOICE_INPUT_RESULT_PREVIEW_DELAY_MILLIS)
            runCurrent()

            assertEquals(
                listOf("take me to busan station" to SearchVoiceMode.MOBILITY_IMPAIRED),
                searchRepository.voiceAnalysisRequests,
            )
            assertEquals(
                SearchUiEvent.NavigateToResults(
                    query = "Busan Station",
                    editingTarget = RouteEditingTarget.DESTINATION,
                ),
                secondEvent.await(),
            )
        }

    @Test
    fun `dismissing voice input during transcript preview cancels delayed results navigation`() =
        runTest {
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()
            val startEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }
            viewModel.onAction(SearchUiAction.VoiceRouteEntered)
            advanceUntilIdle()
            assertEquals(SearchUiEvent.StartVoiceCapture, startEvent.await())

            val firstEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }
            val secondEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.drop(1).first() }
            val unexpectedEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.drop(2).first() }

            viewModel.onAction(
                SearchUiAction.VoiceTranscriptReceived(
                    transcript = "recognized speech",
                    searchQuery = "Busan Station",
                ),
            )
            runCurrent()
            viewModel.onAction(SearchUiAction.VoiceInputDismissed)
            runCurrent()

            assertEquals(SearchUiEvent.StopVoiceCapture, firstEvent.await())
            assertEquals(SearchUiEvent.NavigateBack, secondEvent.await())
            assertEquals(SearchVoiceInputStatus.Recognized, viewModel.uiState.value.voiceInputState.status)
            assertEquals(true, viewModel.uiState.value.voiceInputState.isActive)
            assertEquals("recognized speech", viewModel.uiState.value.voiceInputState.transcript)

            advanceTimeBy(VOICE_INPUT_RESULT_PREVIEW_DELAY_MILLIS)
            runCurrent()

            assertFalse(unexpectedEvent.isCompleted)
            unexpectedEvent.cancel()
        }

    @Test
    fun `search result click stores selected destination and emits route setting navigation`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(result.toPlaceDestination(), destinationSelectionRepository.selectedDestination.value)
            assertEquals(SearchUiEvent.NavigateToRouteSetting, uiEvent.await())
        }

    @Test
    fun `search result briefing click stores selected destination and emits route briefing navigation`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.SearchResultBriefingClicked(result = result))
            advanceUntilIdle()

            assertEquals(result.toPlaceDestination(), destinationSelectionRepository.selectedDestination.value)
            assertEquals(SearchUiEvent.NavigateToRouteBriefing, uiEvent.await())
        }

    @Test
    fun `search result click with invalid coordinates keeps user on search and exposes handoff error`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )
            val invalidResult =
                SearchResult(
                    placeId = "place-invalid",
                    title = "Broken Coordinates Place",
                    subtitle = "Unknown address",
                    latitude = Double.NaN,
                    longitude = 129.0750,
                    category = PlaceCategory.OTHER,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = invalidResult))
            advanceUntilIdle()

            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            val resultState = viewModel.uiState.value.resultState
            assertTrue(resultState is SearchResultUiState.Error)
            assertTrue((resultState as SearchResultUiState.Error).message.isNullOrBlank().not())
        }

    @Test
    fun `search result click stores recent destination`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val searchRepository = FakeSearchRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = searchRepository,
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(1, searchRepository.savedRecentDestinations.size)
            assertEquals(
                RecentDestination(
                    placeId = "place-1",
                    name = "Busan City Hall",
                    address = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                    searchedAtMillis = 0L,
                ),
                searchRepository.savedRecentDestinations.single().copy(searchedAtMillis = 0L),
            )
        }

    @Test
    fun `provider only search result click blocks handoff and does not store recent destination`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val searchRepository = FakeSearchRepository()
            val placesRepository = FakePlacesRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = searchRepository,
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    placesRepository = placesRepository,
                )
            val result =
                SearchResult(
                    placeId = "provider:kakao:987654321",
                    serverPlaceId = null,
                    providerPlaceId = "987654321",
                    title = "Provider Only Cafe",
                    subtitle = "2 Gwangbok-ro, Busan",
                    latitude = 35.1010,
                    longitude = 129.0330,
                    category = null,
                    accessibilityTagKeys = listOf("step-free-entrance"),
                    matched = false,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(null, destinationSelectionRepository.selectedDestination.value)
            assertTrue(placesRepository.detailRequests.isEmpty())
            assertTrue(searchRepository.savedRecentDestinations.isEmpty())
            val resultState = viewModel.uiState.value.resultState
            assertTrue(resultState is SearchResultUiState.Error)
        }

    @Test
    fun `matched search result click enriches recent destination from place detail`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val searchRepository = FakeSearchRepository()
            val placesRepository =
                FakePlacesRepository(
                    placeDetailsById =
                        mapOf(
                            "10" to
                                PlaceDetail(
                                    placeId = "10",
                                    name = "Busan Tower",
                                    address = "1 Yongdusan-gil, Busan",
                                    latitude = 35.1000,
                                    longitude = 129.0320,
                                    category = PlaceCategory.TOURIST_SPOT,
                                    accessibilityTags = listOf("elevator", "accessible-toilet"),
                                ),
                        ),
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = searchRepository,
                    bookmarkRepository = FakeBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                    placesRepository = placesRepository,
                )
            val result =
                SearchResult(
                    placeId = "10",
                    serverPlaceId = "10",
                    providerPlaceId = "123456789",
                    title = "Busan Tower",
                    subtitle = "1 Yongdusan-gil, Busan",
                    latitude = 35.1000,
                    longitude = 129.0320,
                    category = PlaceCategory.TOURIST_SPOT,
                    accessibilityTagKeys = listOf("step-free-entrance"),
                    matched = true,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.SearchResultClicked(result = result))
            advanceUntilIdle()

            assertEquals(listOf("10"), placesRepository.detailRequests)
            assertEquals(
                listOf("elevator", "accessible-toilet"),
                searchRepository.savedRecentDestinations.single().accessibilityTagKeys,
            )
        }

    @Test
    fun `provider only search result bookmark toggle is blocked`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = bookmarkRepository,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )
            val result =
                SearchResult(
                    placeId = "provider:kakao:987654321",
                    serverPlaceId = null,
                    providerPlaceId = "987654321",
                    title = "Provider Only Cafe",
                    subtitle = "2 Gwangbok-ro, Busan",
                    latitude = 35.1010,
                    longitude = 129.0330,
                    category = null,
                    accessibilityTagKeys = listOf("step-free-entrance"),
                    matched = false,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.BookmarkToggleClicked(result = result))
            advanceUntilIdle()

            assertTrue(bookmarkRepository.bookmarks.value.isEmpty())
            val resultState = viewModel.uiState.value.resultState
            assertTrue(resultState is SearchResultUiState.Error)
        }

    @Test
    fun `bookmark toggle saves unbookmarked search result`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = bookmarkRepository,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.BookmarkToggleClicked(result = result))
            advanceUntilIdle()

            assertEquals(
                BookmarkData(
                    placeId = "place-1",
                    placeName = "Busan City Hall",
                    address = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = "TOURIST_ATTRACTION",
                ),
                bookmarkRepository.bookmarks.value.single(),
            )
        }

    @Test
    fun `provider only low vision bookmark save is blocked`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = bookmarkRepository,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )
            val result =
                SearchResult(
                    placeId = "provider:kakao:987654321",
                    serverPlaceId = null,
                    providerPlaceId = "987654321",
                    title = "Provider Only Cafe",
                    subtitle = "2 Gwangbok-ro, Busan",
                    latitude = 35.1010,
                    longitude = 129.0330,
                    category = null,
                    accessibilityTagKeys = listOf("step-free-entrance"),
                    matched = false,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.LowVisionBookmarkSaveClicked(result = result))
            advanceUntilIdle()

            assertTrue(bookmarkRepository.bookmarks.value.isEmpty())
            val resultState = viewModel.uiState.value.resultState
            assertTrue(resultState is SearchResultUiState.Error)
        }

    @Test
    fun `low vision bookmark save always saves result and navigates to low vision bookmark`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = bookmarkRepository,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SearchUiAction.LowVisionBookmarkSaveClicked(result = result))
            advanceUntilIdle()

            assertEquals(
                BookmarkData(
                    placeId = "place-1",
                    placeName = "Busan City Hall",
                    address = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = "TOURIST_ATTRACTION",
                ),
                bookmarkRepository.bookmarks.value.single(),
            )
            assertEquals(SearchUiEvent.NavigateToLowVisionBookmark, uiEvent.await())
        }

    @Test
    fun `bookmark toggle deletes already bookmarked search result`() =
        runTest {
            val bookmarkRepository =
                FakeBookmarkRepository(
                    bookmarks =
                        listOf(
                            BookmarkData(
                                placeId = "place-1",
                                placeName = "Busan City Hall",
                                address = "123 Jungang-daero, Busan",
                                latitude = 35.1797,
                                longitude = 129.0750,
                                category = "TOURIST_ATTRACTION",
                            ),
                        ),
                )
            val viewModel =
                SearchViewModel(
                    searchRepository = FakeSearchRepository(),
                    bookmarkRepository = bookmarkRepository,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )
            val result =
                SearchResult(
                    placeId = "place-1",
                    title = "Busan City Hall",
                    subtitle = "123 Jungang-daero, Busan",
                    latitude = 35.1797,
                    longitude = 129.0750,
                    category = PlaceCategory.TOURIST_ATTRACTION,
                )

            advanceUntilIdle()

            viewModel.onAction(SearchUiAction.BookmarkToggleClicked(result = result))
            advanceUntilIdle()

            assertEquals(emptyList<BookmarkData>(), bookmarkRepository.bookmarks.value)
        }
}

private class FakeSearchRepository(
    private val searchResults: List<SearchResult> = emptyList(),
    private val voiceAnalysis: SearchVoiceAnalysis? = null,
    recentSearches: List<RecentSearch> = emptyList(),
) : SearchRepository {
    val savedRecentDestinations = mutableListOf<RecentDestination>()
    val voiceAnalysisRequests = mutableListOf<Pair<String, SearchVoiceMode>>()
    val deletedRecentSearchKeywords = mutableListOf<String>()
    var clearRecentSearchesCallCount = 0
    private val recentSearches = MutableStateFlow(recentSearches)

    override suspend fun search(query: SearchQuery): List<SearchResult> = searchResults

    override suspend fun analyzeVoiceSearch(
        text: String,
        mode: SearchVoiceMode,
    ): SearchVoiceAnalysis {
        voiceAnalysisRequests += text to mode
        return voiceAnalysis
            ?: SearchVoiceAnalysis(
                intent = SearchVoiceIntent.PLACE_SEARCH,
                placeName = text.trim(),
            )
    }

    override suspend fun getRecentSearches(): List<RecentSearch> = recentSearches.value

    override suspend fun saveRecentSearch(keyword: String) = Unit

    override suspend fun deleteRecentSearch(keyword: String) {
        deletedRecentSearchKeywords += keyword
        recentSearches.value =
            recentSearches.value.filterNot { recentSearch ->
                recentSearch.keyword.equals(keyword, ignoreCase = true)
            }
    }

    override suspend fun clearRecentSearches() {
        clearRecentSearchesCallCount += 1
        recentSearches.value = emptyList()
    }

    override suspend fun getRecentDestinations(): List<RecentDestination> = emptyList()

    override suspend fun saveRecentDestination(destination: RecentDestination) {
        savedRecentDestinations += destination
    }
}

private class FakePlacesRepository(
    private val placeDetailsById: Map<String, PlaceDetail> = emptyMap(),
) : PlacesRepository {
    val detailRequests = mutableListOf<String>()

    override suspend fun getPlaces(query: com.ssafy.e102.eumgil.core.model.PlaceQuery) =
        emptyList<com.ssafy.e102.eumgil.core.model.PlaceSummary>()

    override suspend fun getPlaceDetail(placeId: String): PlaceDetail? {
        detailRequests += placeId
        return placeDetailsById[placeId]
    }
}

private class FakeBookmarkRepository(
    bookmarks: List<BookmarkData> = emptyList(),
) : BookmarkRepository {
    val bookmarks = MutableStateFlow(bookmarks)

    override fun observeBookmarks(): Flow<List<BookmarkData>> = bookmarks

    override suspend fun isBookmarked(placeId: String): Boolean =
        bookmarks.value.any { bookmark -> bookmark.placeId == placeId }

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        bookmarks.value = bookmarks.value.filterNot { it.placeId == bookmark.placeId } + bookmark
    }

    override suspend fun deleteBookmark(placeId: String) {
        bookmarks.value = bookmarks.value.filterNot { bookmark -> bookmark.placeId == placeId }
    }
}
