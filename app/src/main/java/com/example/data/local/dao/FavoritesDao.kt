package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY createdAt DESC")
    fun getFavoritesByType(type: String): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    @Query("DELETE FROM favorites WHERE title = :title AND type = :type")
    suspend fun deleteFavoriteByTitleAndType(title: String, type: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE title = :title AND type = :type)")
    fun isFavorite(title: String, type: String): Flow<Boolean>

    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()
}
