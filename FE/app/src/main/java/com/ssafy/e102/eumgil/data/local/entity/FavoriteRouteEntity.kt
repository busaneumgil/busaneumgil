package com.ssafy.e102.eumgil.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favoriteRoute",
    indices = [Index(value = ["updatedAt"])],
)
data class FavoriteRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val favoriteRouteId: Long = 0L,
    val routeName: String,
    val originName: String,
    val originPlaceId: String? = null,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationName: String,
    val destinationPlaceId: String? = null,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val routeOption: String? = null,
    val summaryDistanceMeters: Int? = null,
    val summaryDurationSeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
