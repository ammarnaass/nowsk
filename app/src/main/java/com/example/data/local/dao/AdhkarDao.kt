package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AdhkarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdhkarDao {
    @Query("SELECT * FROM adhkar ORDER BY categoryId ASC")
    fun getAllAdhkar(): Flow<List<AdhkarEntity>>

    @Query("SELECT * FROM adhkar WHERE categoryId = :categoryId")
    fun getAdhkarByCategory(categoryId: String): Flow<List<AdhkarEntity>>

    @Query("SELECT * FROM adhkar WHERE id = :id LIMIT 1")
    fun getDhikrById(id: String): Flow<AdhkarEntity?>

    @Query("SELECT * FROM adhkar WHERE isFavorite = 1")
    fun getFavoriteAdhkar(): Flow<List<AdhkarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdhkar(dhikr: AdhkarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAdhkar(items: List<AdhkarEntity>)

    @Query("UPDATE adhkar SET currentCount = :count WHERE id = :id")
    suspend fun updateDhikrCount(id: String, count: Int)

    @Query("UPDATE adhkar SET isCompletedToday = :completed, lastCompletedDate = :date WHERE id = :id")
    suspend fun markDhikrCompleted(id: String, completed: Boolean, date: String)

    @Query("UPDATE adhkar SET isFavorite = :isFav WHERE id = :id")
    suspend fun setDhikrFavorite(id: String, isFav: Boolean)

    @Query("UPDATE adhkar SET currentCount = 0, isCompletedToday = 0 WHERE categoryId = :categoryId")
    suspend fun resetCategoryProgress(categoryId: String)

    @Query("DELETE FROM adhkar WHERE id = :id")
    suspend fun deleteDhikrById(id: String)
}
