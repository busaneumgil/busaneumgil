package com.ssafy.e102.eumgil.feature.savedroute

import com.ssafy.e102.eumgil.core.model.GeoCoordinate
import com.ssafy.e102.eumgil.core.model.PlaceCategory
import com.ssafy.e102.eumgil.core.model.RouteBookmark
import com.ssafy.e102.eumgil.core.model.RouteBookmarkDraft
import com.ssafy.e102.eumgil.core.model.RouteBookmarkSaveRequest
import com.ssafy.e102.eumgil.core.model.RouteOption
import com.ssafy.e102.eumgil.data.repository.BookmarkData
import com.ssafy.e102.eumgil.data.repository.BookmarkRepository
import com.ssafy.e102.eumgil.data.repository.InMemoryDestinationSelectionRepository
import com.ssafy.e102.eumgil.data.repository.RouteBookmarkRepository
import com.ssafy.e102.eumgil.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedRouteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `place bookmark list renders content state`() =
        runTest {
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(bookmarks = listOf(testPlaceBookmark())),
                    routeBookmarkRepository = FakeRouteBookmarkRepository(),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            assertEquals(SavedBookmarkContentState.CONTENT, viewModel.uiState.value.placeContent.screenState)
            assertEquals(1, viewModel.uiState.value.placeContent.places.size)
            assertEquals("부산역 KTX", viewModel.uiState.value.placeContent.places.first().name)
        }

    @Test
    fun `route bookmark list renders content state`() =
        runTest {
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(),
                    routeBookmarkRepository =
                        FakeRouteBookmarkRepository(
                            routeBookmarks = listOf(testRouteBookmark()),
                        ),
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            assertEquals(SavedBookmarkContentState.CONTENT, viewModel.uiState.value.routeContent.screenState)
            assertEquals(1, viewModel.uiState.value.routeContent.routes.size)
            assertEquals("부산시청-해운대해수욕장", viewModel.uiState.value.routeContent.routes.first().routeName)
        }

    @Test
    fun `place click stores destination and navigates to map`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(bookmarks = listOf(testPlaceBookmark())),
                    routeBookmarkRepository = FakeRouteBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SavedRouteUiAction.PlaceClicked(placeId = "bookmark-place-1"))
            advanceUntilIdle()

            val destination = destinationSelectionRepository.selectedDestination.value

            assertEquals(SavedRouteUiEvent.NavigateToMap, uiEvent.await())
            assertEquals("bookmark-place-1", destination?.placeId)
            assertEquals(PlaceCategory.ELEVATOR, destination?.category)
        }

    @Test
    fun `place route guide click stores destination and navigates to route setting`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(bookmarks = listOf(testPlaceBookmark())),
                    routeBookmarkRepository = FakeRouteBookmarkRepository(),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SavedRouteUiAction.PlaceRouteGuideClicked(placeId = "bookmark-place-1"))
            advanceUntilIdle()

            val destination = destinationSelectionRepository.selectedDestination.value

            assertEquals(SavedRouteUiEvent.NavigateToRouteSetting(), uiEvent.await())
            assertEquals("bookmark-place-1", destination?.placeId)
            assertEquals(PlaceCategory.ELEVATOR, destination?.category)
        }

    @Test
    fun `route guide click stores saved end point and navigates with saved route option`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(),
                    routeBookmarkRepository =
                        FakeRouteBookmarkRepository(
                            routeBookmarks = listOf(testRouteBookmark()),
                        ),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvent.first() }

            viewModel.onAction(SavedRouteUiAction.RouteGuideClicked(bookmarkId = "route-bookmark-1"))
            advanceUntilIdle()

            val destination = destinationSelectionRepository.selectedDestination.value

            assertEquals(
                SavedRouteUiEvent.NavigateToRouteSetting(initialRouteOption = RouteOption.SHORTEST),
                uiEvent.await(),
            )
            assertEquals("route-bookmark:route-bookmark-1", destination?.placeId)
            assertEquals("광안리해변", destination?.name)
            assertEquals(35.1532, destination?.latitude)
            assertEquals(129.1186, destination?.longitude)
        }

    @Test
    fun `route guide click with invalid coordinate keeps user on saved route screen`() =
        runTest {
            val destinationSelectionRepository = InMemoryDestinationSelectionRepository()
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(),
                    routeBookmarkRepository =
                        FakeRouteBookmarkRepository(
                            routeBookmarks = listOf(testRouteBookmark(endPoint = GeoCoordinate(Double.NaN, 129.1186))),
                        ),
                    destinationSelectionRepository = destinationSelectionRepository,
                )

            advanceUntilIdle()
            val uiEvent = async { viewModel.uiEvent.first() }

            viewModel.onAction(SavedRouteUiAction.RouteGuideClicked(bookmarkId = "route-bookmark-1"))
            advanceUntilIdle()

            assertEquals(
                SavedRouteUiEvent.ShowSnackbar("저장한 경로의 도착지 좌표가 올바르지 않습니다."),
                uiEvent.await(),
            )
            assertNull(destinationSelectionRepository.selectedDestination.value)
            assertEquals(
                "저장한 경로의 도착지 좌표가 올바르지 않습니다.",
                viewModel.uiState.value.routeContent.errorMessage,
            )
        }

    @Test
    fun `remove route bookmark updates route list state`() =
        runTest {
            val routeBookmarkRepository =
                FakeRouteBookmarkRepository(
                    routeBookmarks = listOf(testRouteBookmark()),
                )
            val viewModel =
                SavedRouteViewModel(
                    bookmarkRepository = FakeBookmarkRepository(),
                    routeBookmarkRepository = routeBookmarkRepository,
                    destinationSelectionRepository = InMemoryDestinationSelectionRepository(),
                )

            advanceUntilIdle()

            viewModel.onAction(SavedRouteUiAction.RouteRemoveClicked(bookmarkId = "route-bookmark-1"))
            advanceUntilIdle()

            assertEquals(SavedBookmarkContentState.EMPTY, viewModel.uiState.value.routeContent.screenState)
            assertEquals(emptyList<SavedRouteBookmarkUiModel>(), viewModel.uiState.value.routeContent.routes)
        }
}

private class FakeBookmarkRepository(
    bookmarks: List<BookmarkData> = emptyList(),
    private val failObserve: Boolean = false,
    private val failDelete: Boolean = false,
) : BookmarkRepository {
    private val mutableBookmarks = MutableStateFlow(bookmarks)

    override fun observeBookmarks(): Flow<List<BookmarkData>> =
        if (failObserve) {
            flow { error("bookmark load failed") }
        } else {
            mutableBookmarks
        }

    override suspend fun isBookmarked(placeId: String): Boolean =
        mutableBookmarks.value.any { bookmark -> bookmark.placeId == placeId }

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        mutableBookmarks.value = mutableBookmarks.value.filterNot { it.placeId == bookmark.placeId } + bookmark
    }

    override suspend fun deleteBookmark(placeId: String) {
        if (failDelete) error("bookmark delete failed")
        mutableBookmarks.value = mutableBookmarks.value.filterNot { bookmark -> bookmark.placeId == placeId }
    }
}

private class FakeRouteBookmarkRepository(
    routeBookmarks: List<RouteBookmark> = emptyList(),
) : RouteBookmarkRepository {
    private val mutableRouteBookmarks = MutableStateFlow(routeBookmarks)

    override fun observeRouteBookmarks(): Flow<List<RouteBookmark>> = mutableRouteBookmarks

    override suspend fun isBookmarked(draft: RouteBookmarkDraft): Boolean =
        mutableRouteBookmarks.value.any { bookmark ->
            bookmark.startPoint == draft.startPoint &&
                bookmark.endPoint == draft.endPoint &&
                bookmark.routeOption == draft.routeOption
        }

    override suspend fun saveRouteBookmark(request: RouteBookmarkSaveRequest): RouteBookmark {
        val savedBookmark =
            RouteBookmark(
                bookmarkId = "route-bookmark-1",
                routeName = request.routeName,
                startLabel = request.startLabel,
                endLabel = request.endLabel,
                startPoint = request.startPoint,
                endPoint = request.endPoint,
                routeOption = request.routeOption,
                distanceMeters = request.distanceMeters,
                durationMinutes = request.durationMinutes,
                createdAt = 1L,
                updatedAt = 1L,
            )
        mutableRouteBookmarks.value = listOf(savedBookmark)
        return savedBookmark
    }

    override suspend fun deleteRouteBookmark(bookmarkId: String) {
        mutableRouteBookmarks.value =
            mutableRouteBookmarks.value.filterNot { bookmark ->
                bookmark.bookmarkId == bookmarkId
            }
    }
}

private fun testPlaceBookmark(
    latitude: Double = 35.1151,
    longitude: Double = 129.0415,
): BookmarkData =
    BookmarkData(
        placeId = "bookmark-place-1",
        placeName = "부산역 KTX",
        address = "부산 동구 중앙대로 206",
        latitude = latitude,
        longitude = longitude,
        category = "ELEVATOR",
    )

private fun testRouteBookmark(
    endPoint: GeoCoordinate = GeoCoordinate(latitude = 35.1532, longitude = 129.1186),
): RouteBookmark =
    RouteBookmark(
        bookmarkId = "route-bookmark-1",
        routeName = "부산시청-해운대해수욕장",
        startLabel = "부산시청",
        endLabel = "광안리해변",
        startPoint = GeoCoordinate(latitude = 35.1798, longitude = 129.0750),
        endPoint = endPoint,
        routeOption = RouteOption.SHORTEST,
        distanceMeters = 7_600,
        durationMinutes = 21,
        createdAt = 1L,
        updatedAt = 1L,
    )
