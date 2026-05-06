package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.entity.BookmarkEntity
import com.ssafy.e102.eumgil.data.mock.fixture.MockBookmarkFixtures
import com.ssafy.e102.eumgil.data.remote.HttpJsonClient
import com.ssafy.e102.eumgil.data.remote.datasource.BookmarksRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkPageDto
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkPointDto
import com.ssafy.e102.eumgil.data.remote.dto.CreateBookmarkResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkRepositoryTest {
    @Test
    fun `observeBookmarks seeds debug bookmark when local table is empty`() =
        runBlocking {
            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = FakeBookmarkDao(),
                    initialBookmarks = MockBookmarkFixtures.defaultBookmarks,
                )

            val bookmarks = repository.observeBookmarks().first()

            assertEquals(MockBookmarkFixtures.defaultBookmarks, bookmarks)
        }

    @Test
    fun `observeBookmarks keeps existing local bookmarks instead of adding debug bookmark`() =
        runBlocking {
            val localBookmark = testBookmarkEntity(placeId = "existing-bookmark")
            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = FakeBookmarkDao(bookmarks = listOf(localBookmark)),
                    initialBookmarks = MockBookmarkFixtures.defaultBookmarks,
                )

            val bookmarks = repository.observeBookmarks().first()

            assertEquals(listOf(localBookmark.toBookmarkData()), bookmarks)
        }

    @Test
    fun `observeBookmarks fetches from server and replaces cache when token is provided`() =
        runBlocking {
            val serverItem =
                BookmarkListItemDto(
                    bookmarkId = 1L,
                    placeId = 42L,
                    provider = "KAKAO",
                    providerPlaceId = "external-42",
                    name = "부산시민공원",
                    category = "TOURIST_SPOT",
                    address = "부산광역시 부산진구 시민공원로 73",
                    point = BookmarkPointDto(lat = 35.1686, lng = 129.0576),
                )
            val cachedBookmark = testBookmarkEntity(placeId = "stale-cache")
            val fakeDao = FakeBookmarkDao(bookmarks = listOf(cachedBookmark))
            val fakeDataSource = FakeBookmarksRemoteDataSource(serverContent = listOf(serverItem))

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            val bookmarks = repository.observeBookmarks().first()

            assertEquals(1, bookmarks.size)
            assertEquals("42", bookmarks[0].placeId)
            assertEquals("부산시민공원", bookmarks[0].placeName)
        }

    @Test
    fun `observeBookmarks falls back to cache when server fetch fails`() =
        runBlocking {
            val cachedBookmark = testBookmarkEntity(placeId = "cached-bookmark")
            val fakeDao = FakeBookmarkDao(bookmarks = listOf(cachedBookmark))
            val fakeDataSource = FakeBookmarksRemoteDataSource(throwOnGet = true)

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            val bookmarks = repository.observeBookmarks().first()

            assertEquals(listOf(cachedBookmark.toBookmarkData()), bookmarks)
        }

    @Test
    fun `observeBookmarks skips server fetch when access token is null`() =
        runBlocking {
            val cachedBookmark = testBookmarkEntity(placeId = "cached-bookmark")
            val fakeDao = FakeBookmarkDao(bookmarks = listOf(cachedBookmark))
            val fakeDataSource = FakeBookmarksRemoteDataSource()

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { null },
                )

            val bookmarks = repository.observeBookmarks().first()

            assertEquals(listOf(cachedBookmark.toBookmarkData()), bookmarks)
            assertEquals(0, fakeDataSource.getBookmarksCallCount)
        }

    @Test
    fun `saveBookmark posts to server and updates cache when token and numeric placeId provided`() =
        runBlocking {
            val fakeDao = FakeBookmarkDao()
            val fakeDataSource = FakeBookmarksRemoteDataSource()

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            repository.saveBookmark(
                BookmarkData(
                    placeId = "42",
                    placeName = "부산시민공원",
                    address = null,
                    latitude = 35.1686,
                    longitude = 129.0576,
                    category = "TOURIST_SPOT",
                ),
            )

            assertEquals(listOf(42L), fakeDataSource.createdPlaceIds)
            assertEquals(1, fakeDao.getBookmarkCount())
        }

    @Test
    fun `saveBookmark prefers serverPlaceId over string placeId for server call`() =
        runBlocking {
            val fakeDao = FakeBookmarkDao()
            val fakeDataSource = FakeBookmarksRemoteDataSource()

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            repository.saveBookmark(
                BookmarkData(
                    placeId = "non-numeric-uuid",
                    placeName = "부산시민공원",
                    address = null,
                    latitude = 35.1686,
                    longitude = 129.0576,
                    category = "TOURIST_SPOT",
                    serverPlaceId = 99L,
                ),
            )

            assertEquals(listOf(99L), fakeDataSource.createdPlaceIds)
        }

    @Test
    fun `saveBookmark only caches locally when access token is null`() =
        runBlocking {
            val fakeDao = FakeBookmarkDao()
            val fakeDataSource = FakeBookmarksRemoteDataSource()

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { null },
                )

            repository.saveBookmark(
                BookmarkData(
                    placeId = "42",
                    placeName = "부산시민공원",
                    address = null,
                    latitude = 35.1686,
                    longitude = 129.0576,
                    category = null,
                ),
            )

            assertTrue(fakeDataSource.createdPlaceIds.isEmpty())
            assertEquals(1, fakeDao.getBookmarkCount())
        }

    @Test
    fun `saveBookmark only caches locally when placeId is non-numeric and serverPlaceId is null`() =
        runBlocking {
            val fakeDao = FakeBookmarkDao()
            val fakeDataSource = FakeBookmarksRemoteDataSource()

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            repository.saveBookmark(
                BookmarkData(
                    placeId = "kakao-only-id",
                    placeName = "외부 장소",
                    address = null,
                    latitude = 0.0,
                    longitude = 0.0,
                    category = null,
                ),
            )

            assertTrue(fakeDataSource.createdPlaceIds.isEmpty())
            assertEquals(1, fakeDao.getBookmarkCount())
        }

    @Test
    fun `deleteBookmark calls server and removes cache when placeId is numeric`() =
        runBlocking {
            val cachedBookmark = testBookmarkEntity(placeId = "42")
            val fakeDao = FakeBookmarkDao(bookmarks = listOf(cachedBookmark))
            val fakeDataSource = FakeBookmarksRemoteDataSource()

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            repository.deleteBookmark("42")

            assertEquals(listOf(42L), fakeDataSource.deletedPlaceIds)
            assertEquals(0, fakeDao.getBookmarkCount())
        }

    @Test
    fun `deleteBookmark removes cache even when server call fails`() =
        runBlocking {
            val cachedBookmark = testBookmarkEntity(placeId = "42")
            val fakeDao = FakeBookmarkDao(bookmarks = listOf(cachedBookmark))
            val fakeDataSource = FakeBookmarksRemoteDataSource(throwOnDelete = true)

            val repository =
                DefaultBookmarkRepository(
                    bookmarkDao = fakeDao,
                    bookmarksRemoteDataSource = fakeDataSource,
                    accessTokenProvider = { "test-token" },
                )

            repository.deleteBookmark("42")

            assertEquals(0, fakeDao.getBookmarkCount())
        }
}

private class FakeBookmarkDao(
    bookmarks: List<BookmarkEntity> = emptyList(),
) : BookmarkDao {
    private val mutableBookmarks = MutableStateFlow(bookmarks)

    override fun observeBookmarks(): Flow<List<BookmarkEntity>> = mutableBookmarks

    override fun observeBookmark(placeId: String): Flow<BookmarkEntity?> =
        MutableStateFlow(mutableBookmarks.value.firstOrNull { bookmark -> bookmark.placeId == placeId })

    override suspend fun getBookmark(placeId: String): BookmarkEntity? =
        mutableBookmarks.value.firstOrNull { bookmark -> bookmark.placeId == placeId }

    override suspend fun getBookmarkCount(): Int = mutableBookmarks.value.size

    override suspend fun upsertBookmark(bookmark: BookmarkEntity) {
        mutableBookmarks.value = mutableBookmarks.value.upsert(bookmark)
    }

    override suspend fun upsertBookmarks(bookmarks: List<BookmarkEntity>) {
        bookmarks.forEach { bookmark -> upsertBookmark(bookmark) }
    }

    override suspend fun deleteBookmark(placeId: String) {
        mutableBookmarks.value = mutableBookmarks.value.filterNot { bookmark -> bookmark.placeId == placeId }
    }

    override suspend fun clearBookmarks() {
        mutableBookmarks.value = emptyList()
    }
}

private class FakeBookmarksRemoteDataSource(
    private val serverContent: List<BookmarkListItemDto> = emptyList(),
    private val throwOnGet: Boolean = false,
    private val throwOnDelete: Boolean = false,
) : BookmarksRemoteDataSource(httpJsonClient = HttpJsonClient(baseUrl = "http://test.invalid")) {
    val createdPlaceIds = mutableListOf<Long>()
    val deletedPlaceIds = mutableListOf<Long>()
    var getBookmarksCallCount: Int = 0
        private set

    override suspend fun getBookmarks(
        accessToken: String,
        page: Int?,
        size: Int?,
    ): BookmarkPageDto {
        getBookmarksCallCount++
        if (throwOnGet) throw RuntimeException("server get failure")
        return BookmarkPageDto(
            content = serverContent,
            page = page ?: 0,
            size = size ?: serverContent.size,
            totalElements = serverContent.size.toLong(),
            totalPages = 1,
            hasNext = false,
        )
    }

    override suspend fun createBookmark(
        accessToken: String,
        placeId: Long,
    ): CreateBookmarkResponseDto {
        createdPlaceIds.add(placeId)
        return CreateBookmarkResponseDto(bookmarkId = 1L, placeId = placeId)
    }

    override suspend fun deleteBookmark(
        accessToken: String,
        placeId: Long,
    ) {
        if (throwOnDelete) throw RuntimeException("server delete failure")
        deletedPlaceIds.add(placeId)
    }
}

private fun List<BookmarkEntity>.upsert(bookmark: BookmarkEntity): List<BookmarkEntity> =
    filterNot { existing -> existing.placeId == bookmark.placeId } + bookmark

private fun testBookmarkEntity(placeId: String): BookmarkEntity =
    BookmarkEntity(
        placeId = placeId,
        placeName = "기존 북마크",
        address = "부산광역시 동구 중앙대로 206",
        latitude = 35.1151,
        longitude = 129.0415,
        category = "ELEVATOR",
    )

private fun BookmarkEntity.toBookmarkData(): BookmarkData =
    BookmarkData(
        placeId = placeId,
        placeName = placeName,
        address = address,
        latitude = latitude,
        longitude = longitude,
        category = category,
    )
