package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.TasbeehEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbeehDao {
    @Query("SELECT * FROM tasbeeh ORDER BY id ASC")
    fun getAllTasbeeh(): Flow<List<TasbeehEntity>>

    @Query("SELECT * FROM tasbeeh WHERE id = :id LIMIT 1")
    fun getTasbeehById(id: Long): Flow<TasbeehEntity?>

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

    @Query("DELETE FROM tasbeeh")
    suspend fun clearAllTasbeeh()
}
