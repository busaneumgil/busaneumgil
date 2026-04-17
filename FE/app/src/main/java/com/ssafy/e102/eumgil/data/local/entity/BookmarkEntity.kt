package com.ssafy.e102.eumgil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmark",
    indices = [Index(value = ["placeId"], unique = true), Index(value = ["updatedAt"])],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val bookmarkId: Long = 0L,
    val placeId: String,
    val placeName: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
