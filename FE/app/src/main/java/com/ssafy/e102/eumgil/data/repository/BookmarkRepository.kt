package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.entity.BookmarkEntity
import com.ssafy.e102.eumgil.data.remote.datasource.BookmarksRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkListItemDto
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
    val bookmarkId: Long? = null,
    val serverPlaceId: Long? = null,
    val provider: String? = null,
    val providerPlaceId: String? = null,
)

class DefaultBookmarkRepository(
    private val bookmarkDao: BookmarkDao,
    private val bookmarksRemoteDataSource: BookmarksRemoteDataSource? = null,
    private val accessTokenProvider: suspend () -> String? = { null },
    private val initialBookmarks: List<BookmarkData> = emptyList(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) : BookmarkRepository {
    private var hasSeededInitialBookmarks = false

    override fun observeBookmarks(): Flow<List<BookmarkData>> =
        bookmarkDao
            .observeBookmarks()
            .onStart {
                seedInitialBookmarksIfNeeded()
                refreshFromServerIfPossible()
            }
            .map { bookmarks ->
                bookmarks.map(BookmarkEntity::toBookmarkData)
            }

    override suspend fun isBookmarked(placeId: String): Boolean = bookmarkDao.getBookmark(placeId) != null

    override suspend fun saveBookmark(bookmark: BookmarkData) {
        runCatching { trySaveOnServer(bookmark) }
        cacheBookmark(bookmark)
    }

    override suspend fun deleteBookmark(placeId: String) {
        runCatching { tryDeleteOnServer(placeId) }
        bookmarkDao.deleteBookmark(placeId)
    }

    private suspend fun trySaveOnServer(bookmark: BookmarkData) {
        val datasource = bookmarksRemoteDataSource ?: return
        val token = accessTokenProvider() ?: return
        val numericPlaceId = bookmark.serverPlaceId ?: bookmark.placeId.toLongOrNull() ?: return

        datasource.createBookmark(accessToken = token, placeId = numericPlaceId)
    }

    private suspend fun tryDeleteOnServer(placeId: String) {
        val datasource = bookmarksRemoteDataSource ?: return
        val token = accessTokenProvider() ?: return
        val numericPlaceId = placeId.toLongOrNull() ?: return

        datasource.deleteBookmark(accessToken = token, placeId = numericPlaceId)
    }

    private suspend fun cacheBookmark(bookmark: BookmarkData) {
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

    private suspend fun refreshFromServerIfPossible() {
        runCatching {
            val datasource = bookmarksRemoteDataSource ?: return@runCatching
            val token = accessTokenProvider() ?: return@runCatching

            val page =
                datasource.getBookmarks(
                    accessToken = token,
                    page = 0,
                    size = DEFAULT_PAGE_SIZE,
                )

            val now = clock()
            bookmarkDao.clearBookmarks()
            bookmarkDao.upsertBookmarks(
                page.content.map { item -> item.toBookmarkEntity(createdAt = now, updatedAt = now) },
            )
        }
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

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 50
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

private fun BookmarkListItemDto.toBookmarkEntity(
    createdAt: Long,
    updatedAt: Long,
): BookmarkEntity =
    BookmarkEntity(
        placeId = placeId.toString(),
        placeName = name,
        address = address,
        latitude = point.lat,
        longitude = point.lng,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
