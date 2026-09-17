package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.PrayerTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {
    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    fun getPrayerTimesForDate(date: String): Flow<PrayerTimeEntity?>

    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    suspend fun getPrayerTimesForDateSync(date: String): PrayerTimeEntity?

    @Query("SELECT * FROM prayer_times ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPrayerTimeSync(): PrayerTimeEntity?

    @Query("SELECT * FROM prayer_times ORDER BY date ASC")
    fun getAllPrayerTimes(): Flow<List<PrayerTimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTimes(prayerTime: PrayerTimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPrayerTimes(prayerTimes: List<PrayerTimeEntity>)

    @Query("DELETE FROM prayer_times WHERE date = :date")
    suspend fun deletePrayerTimesForDate(date: String)

    @Query("DELETE FROM prayer_times")
    suspend fun clearAllPrayerTimes()
}
