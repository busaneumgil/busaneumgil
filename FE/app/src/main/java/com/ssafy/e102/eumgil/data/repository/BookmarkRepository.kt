package com.ssafy.e102.eumgil.data.repository

import com.ssafy.e102.eumgil.core.model.FacilityDetailSeed
import com.ssafy.e102.eumgil.data.local.dao.BookmarkDao
import com.ssafy.e102.eumgil.data.local.entity.BookmarkEntity
import com.ssafy.e102.eumgil.data.remote.datasource.BookmarksRemoteDataSource
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkListItemDto
import com.ssafy.e102.eumgil.data.remote.dto.BookmarkPointDto
import com.ssafy.e102.eumgil.data.remote.dto.CreateBookmarkRequestDto
import com.ssafy.e102.eumgil.data.remote.dto.CreateBookmarkResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.Locale

interface BookmarkRepository {
    fun observeBookmarks(): Flow<List<BookmarkData>>

    suspend fun isBookmarked(placeId: String): Boolean

    suspend fun saveBookmark(bookmark: BookmarkData): BookmarkData

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
    val bookmarkTargetId: String? = null,
    val targetType: String? = null,
    val serverPlaceId: Long? = null,
    val provider: String? = null,
    val providerPlaceId: String? = null,
    val providerCategory: String? = null,
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

    override suspend fun saveBookmark(bookmark: BookmarkData): BookmarkData {
        val serverResponse = trySaveOnServer(bookmark)
        val resolvedBookmark = bookmark.withServerResponse(serverResponse)
        cacheBookmark(resolvedBookmark)
        return resolvedBookmark
    }

    override suspend fun deleteBookmark(placeId: String) {
        val cachedBookmark = bookmarkDao.getBookmark(placeId) ?: bookmarkDao.getBookmarkByTargetId(placeId)
        tryDeleteOnServer(placeId = placeId, cachedBookmark = cachedBookmark)
        if (cachedBookmark?.bookmarkTargetId == placeId) {
            bookmarkDao.deleteBookmarkByTargetId(placeId)
        } else {
            bookmarkDao.deleteBookmark(placeId)
        }
    }

    private suspend fun trySaveOnServer(bookmark: BookmarkData): CreateBookmarkResponseDto? {
        val datasource = bookmarksRemoteDataSource ?: return null
        val token = accessTokenProvider() ?: return null
        val request = bookmark.toCreateBookmarkRequestDto() ?: return null

        return datasource.createBookmark(accessToken = token, request = request)
    }

    private suspend fun tryDeleteOnServer(
        placeId: String,
        cachedBookmark: BookmarkEntity?,
    ) {
        val datasource = bookmarksRemoteDataSource ?: return
        val token = accessTokenProvider() ?: return
        val bookmarkTargetId = cachedBookmark?.bookmarkTargetId?.takeIf { it.isNotBlank() }
        if (bookmarkTargetId != null) {
            datasource.deleteBookmarkByTargetId(accessToken = token, bookmarkTargetId = bookmarkTargetId)
            return
        }

        val numericPlaceId = cachedBookmark?.serverPlaceId ?: placeId.toLongOrNull() ?: return

        datasource.deleteBookmark(accessToken = token, placeId = numericPlaceId)
    }

    private suspend fun cacheBookmark(bookmark: BookmarkData) {
        val now = clock()
        val existingBookmark = bookmarkDao.getBookmark(bookmark.placeId)

        bookmarkDao.upsertBookmark(
            BookmarkEntity(
                bookmarkId = existingBookmark?.bookmarkId ?: 0L,
                placeId = bookmark.placeId,
                serverBookmarkId = bookmark.bookmarkId,
                bookmarkTargetId = bookmark.bookmarkTargetId,
                targetType = bookmark.targetType,
                serverPlaceId = bookmark.serverPlaceId,
                provider = bookmark.provider,
                providerPlaceId = bookmark.providerPlaceId,
                providerCategory = bookmark.providerCategory,
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

            val serverBookmarks = fetchAllBookmarksFromServer(datasource = datasource, token = token)

            val now = clock()
            bookmarkDao.clearBookmarks()
            bookmarkDao.upsertBookmarks(
                serverBookmarks.map { item -> item.toBookmarkEntity(createdAt = now, updatedAt = now) },
            )
        }
    }

    private suspend fun fetchAllBookmarksFromServer(
        datasource: BookmarksRemoteDataSource,
        token: String,
    ): List<BookmarkListItemDto> {
        val bookmarks = mutableListOf<BookmarkListItemDto>()
        var cursor: Long? = null

        do {
            val page =
                datasource.getBookmarks(
                    accessToken = token,
                    cursor = cursor,
                    size = DEFAULT_PAGE_SIZE,
                )
            bookmarks += page.content
            cursor = page.nextCursor
        } while (page.hasNext && cursor != null)

        return bookmarks
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
        bookmarkId = serverBookmarkId,
        bookmarkTargetId = bookmarkTargetId,
        targetType = targetType,
        serverPlaceId = serverPlaceId,
        provider = provider,
        providerPlaceId = providerPlaceId,
        providerCategory = providerCategory,
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
        serverBookmarkId = bookmarkId,
        bookmarkTargetId = bookmarkTargetId,
        targetType = targetType,
        serverPlaceId = serverPlaceId,
        provider = provider,
        providerPlaceId = providerPlaceId,
        providerCategory = providerCategory,
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
        placeId = localCachePlaceId(),
        serverBookmarkId = bookmarkId,
        bookmarkTargetId = bookmarkTargetId,
        targetType = targetType,
        serverPlaceId = placeId,
        provider = provider,
        providerPlaceId = providerPlaceId,
        providerCategory = providerCategory,
        placeName = name,
        address = address,
        latitude = point.lat,
        longitude = point.lng,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun BookmarkData.toCreateBookmarkRequestDto(): CreateBookmarkRequestDto? {
    val numericPlaceId = serverPlaceId ?: placeId.toLongOrNull()
    if (numericPlaceId != null) {
        return CreateBookmarkRequestDto(placeId = numericPlaceId)
    }

    val snapshotProvider = provider?.takeIf { it.isNotBlank() } ?: return null
    return CreateBookmarkRequestDto(
        provider = snapshotProvider,
        providerPlaceId = providerPlaceId,
        name = placeName,
        providerCategory = providerCategory ?: category,
        address = address,
        point = BookmarkPointDto(lat = latitude, lng = longitude),
    )
}

private fun BookmarkData.withServerResponse(response: CreateBookmarkResponseDto?): BookmarkData {
    if (response == null) return this

    val resolvedServerPlaceId = response.placeId ?: serverPlaceId

    return copy(
        bookmarkId = response.bookmarkId,
        bookmarkTargetId = response.bookmarkTargetId,
        targetType = response.targetType,
        serverPlaceId = resolvedServerPlaceId,
    )
}

private fun BookmarkListItemDto.localCachePlaceId(): String =
    placeId?.toString()
        ?: providerPlaceId
            ?.takeIf { it.isNotBlank() }
            ?.let { externalPlaceId -> "provider:${provider.orEmpty().trim().lowercase(Locale.US)}:$externalPlaceId" }
        ?: bookmarkTargetId
