package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.entity.BookmarkEntity
import com.ssafy.e102.eumgil.data.mock.fixture.MockBookmarkFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
