package com.ssafy.e102.eumgil.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ssafy.e102.eumgil.data.local.entity.FavoriteRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteRouteDao {
    @Query("SELECT * FROM favoriteRoute ORDER BY updatedAt DESC")
    fun observeFavoriteRoutes(): Flow<List<FavoriteRouteEntity>>

    @Query("SELECT * FROM favoriteRoute WHERE favoriteRouteId = :favoriteRouteId LIMIT 1")
    fun observeFavoriteRoute(favoriteRouteId: Long): Flow<FavoriteRouteEntity?>

    @Query("SELECT * FROM favoriteRoute WHERE favoriteRouteId = :favoriteRouteId LIMIT 1")
    suspend fun getFavoriteRoute(favoriteRouteId: Long): FavoriteRouteEntity?

    @Upsert
    suspend fun upsertFavoriteRoute(favoriteRoute: FavoriteRouteEntity)

    @Upsert
    suspend fun upsertFavoriteRoutes(favoriteRoutes: List<FavoriteRouteEntity>)

    @Query("DELETE FROM favoriteRoute WHERE favoriteRouteId = :favoriteRouteId")
    suspend fun deleteFavoriteRoute(favoriteRouteId: Long)

    @Query("DELETE FROM favoriteRoute")
    suspend fun clearFavoriteRoutes()
}
