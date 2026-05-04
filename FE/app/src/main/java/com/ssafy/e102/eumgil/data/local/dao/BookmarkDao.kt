package com.ssafy.e102.eumgil.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssafy.e102.eumgil.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark ORDER BY updatedAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark WHERE placeId = :placeId LIMIT 1")
    fun observeBookmark(placeId: String): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmark WHERE placeId = :placeId LIMIT 1")
    suspend fun getBookmark(placeId: String): BookmarkEntity?

    @Query("SELECT COUNT(*) FROM bookmark")
    suspend fun getBookmarkCount(): Int

    @Upsert
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Upsert
    suspend fun upsertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmark WHERE placeId = :placeId")
    suspend fun deleteBookmark(placeId: String)

    @Query("DELETE FROM bookmark")
    suspend fun clearBookmarks()
}
