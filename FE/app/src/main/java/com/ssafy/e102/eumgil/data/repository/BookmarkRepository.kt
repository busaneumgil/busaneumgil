package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

interface BookmarkRepository {
    fun observeBookmarks(): Flow<List<BookmarkData>>

    suspend fun isBookmarked(placeId: String): Boolean

    suspend fun saveBookmark(bookmark: BookmarkData)

    suspend fun deleteBookmark(placeId: String)
}

data class BookmarkData(
    val placeId: String,
    val placeName: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val category: String?,
)

class DefaultBookmarkRepository(
    private val bookmarkDao: BookmarkDao,
    private val initialBookmarks: List<BookmarkData> = emptyList(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) : BookmarkRepository {
    private var hasSeededInitialBookmarks = false

    override fun observeBookmarks(): Flow<List<BookmarkData>> =
        bookmarkDao
            .observeBookmarks()
            .onStart { seedInitialBookmarksIfNeeded() }
            .map { bookmarks ->
                bookmarks.map(BookmarkEntity::toBookmarkData)
            }

    override suspend fun isBookmarked(placeId: String): Boolean =
        bookmarkDao.getBookmark(placeId) != null

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        val now = clock()
        val existingBookmark = bookmarkDao.getBookmark(bookmark.placeId)

        bookmarkDao.upsertBookmark(
            BookmarkEntity(
                bookmarkId = existingBookmark?.bookmarkId ?: 0L,
                placeId = bookmark.placeId,
                placeName = bookmark.placeName,
                address = bookmark.address,
                latitude = bookmark.latitude,
                longitude = bookmark.longitude,
                category = bookmark.category,
                createdAt = existingBookmark?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun deleteBookmark(placeId: String) {
        bookmarkDao.deleteBookmark(placeId)
    }

    private suspend fun seedInitialBookmarksIfNeeded() {
        if (hasSeededInitialBookmarks || initialBookmarks.isEmpty()) return

        hasSeededInitialBookmarks = true
        if (bookmarkDao.getBookmarkCount() > 0) return

        val now = clock()
        bookmarkDao.upsertBookmarks(
            initialBookmarks.map { bookmark ->
                bookmark.toBookmarkEntity(createdAt = now, updatedAt = now)
            },
        )
    }
}

private fun BookmarkEntity.toBookmarkData(): BookmarkData =
    BookmarkData(
        placeId = placeId,
        placeName = placeName,
        address = address,
        latitude = latitude,
        longitude = longitude,
        category = category,
    )

fun FacilityDetailSeed.toBookmarkData(): BookmarkData =
    BookmarkData(
        placeId = facilityId,
        placeName = name,
        address = address.takeIf { it.isNotBlank() },
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
        category = category.name,
    )

private fun BookmarkData.toBookmarkEntity(
    createdAt: Long,
    updatedAt: Long,
): BookmarkEntity =
    BookmarkEntity(
        placeId = placeId,
        placeName = placeName,
        address = address,
        latitude = latitude,
        longitude = longitude,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
