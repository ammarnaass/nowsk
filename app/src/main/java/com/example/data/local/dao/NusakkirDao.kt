package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.QuranProgressEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TasbeehEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NusakkirDao {
    // Favorites
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

    // Tasbeeh
    @Query("SELECT * FROM tasbeeh ORDER BY id ASC")
    fun getAllTasbeeh(): Flow<List<TasbeehEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbeeh(tasbeeh: TasbeehEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllTasbeeh(items: List<TasbeehEntity>)

    @Update
    suspend fun updateTasbeeh(tasbeeh: TasbeehEntity)

    @Query("UPDATE tasbeeh SET count = :count, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateCount(id: Long, count: Int, timestamp: Long)

    @Query("UPDATE tasbeeh SET count = 0, lastUpdated = :timestamp WHERE id = :id")
    suspend fun resetCount(id: Long, timestamp: Long)

    @Query("DELETE FROM tasbeeh WHERE id = :id")
    suspend fun deleteTasbeehById(id: Long)

    // Quran Progress
    @Query("SELECT * FROM quran_progress WHERE id = 1 LIMIT 1")
    fun getQuranProgress(): Flow<QuranProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuranProgress(progress: QuranProgressEntity)

    // Settings
    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<String?>

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)
}
