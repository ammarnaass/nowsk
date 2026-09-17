package com.example.data.repository

import com.example.data.local.NusakkirDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class NusakkirRepository(
    private val database: NusakkirDatabase,
    private val nusakkirDao: NusakkirDao = database.nusakkirDao(),
    private val settingsDao: SettingsDao = database.settingsDao(),
    private val prayerTimesDao: PrayerTimesDao = database.prayerTimesDao(),
    private val adhkarDao: AdhkarDao = database.adhkarDao(),
    private val favoritesDao: FavoritesDao = database.favoritesDao(),
    private val tasbeehDao: TasbeehDao = database.tasbeehDao()
) {

    // ==========================================
    // 1. Settings Table Operations
    // ==========================================
    val allSettings: Flow<List<SettingEntity>> = settingsDao.getAllSettings()

    fun getSetting(key: String): Flow<String?> = settingsDao.getSetting(key)

    suspend fun setSetting(key: String, value: String) {
        settingsDao.setSetting(SettingEntity(key, value))
    }

    suspend fun deleteSetting(key: String) {
        settingsDao.deleteSetting(key)
    }

    // ==========================================
    // 2. PrayerTimes Table Operations
    // ==========================================
    fun getPrayerTimesForDate(date: String): Flow<PrayerTimeEntity?> =
        prayerTimesDao.getPrayerTimesForDate(date)

    val allPrayerTimes: Flow<List<PrayerTimeEntity>> = prayerTimesDao.getAllPrayerTimes()

    suspend fun savePrayerTimes(prayerTime: PrayerTimeEntity) {
        prayerTimesDao.insertPrayerTimes(prayerTime)
    }

    suspend fun saveAllPrayerTimes(prayerTimes: List<PrayerTimeEntity>) {
        prayerTimesDao.insertAllPrayerTimes(prayerTimes)
    }

    // ==========================================
    // 3. Adhkar Table Operations
    // ==========================================
    val allAdhkar: Flow<List<AdhkarEntity>> = adhkarDao.getAllAdhkar()

    fun getAdhkarByCategory(categoryId: String): Flow<List<AdhkarEntity>> =
        adhkarDao.getAdhkarByCategory(categoryId)

    fun getFavoriteAdhkar(): Flow<List<AdhkarEntity>> = adhkarDao.getFavoriteAdhkar()

    suspend fun updateDhikrCount(id: String, count: Int) {
        adhkarDao.updateDhikrCount(id, count)
    }

    suspend fun markDhikrCompleted(id: String, completed: Boolean, date: String) {
        adhkarDao.markDhikrCompleted(id, completed, date)
    }

    suspend fun resetCategoryAdhkar(categoryId: String) {
        adhkarDao.resetCategoryProgress(categoryId)
    }

    suspend fun setDhikrFavorite(id: String, isFavorite: Boolean) {
        adhkarDao.setDhikrFavorite(id, isFavorite)
    }

    // ==========================================
    // 4. Favorites Table Operations
    // ==========================================
    val allFavorites: Flow<List<FavoriteEntity>> = favoritesDao.getAllFavorites()

    fun getFavoritesByType(type: String): Flow<List<FavoriteEntity>> =
        favoritesDao.getFavoritesByType(type)

    suspend fun addFavorite(favorite: FavoriteEntity): Long =
        favoritesDao.insertFavorite(favorite)

    suspend fun removeFavorite(id: Long) =
        favoritesDao.deleteFavoriteById(id)

    suspend fun removeFavoriteByTitleAndType(title: String, type: String) =
        favoritesDao.deleteFavoriteByTitleAndType(title, type)

    fun isFavorite(title: String, type: String): Flow<Boolean> =
        favoritesDao.isFavorite(title, type)

    // ==========================================
    // 5. Tasbeeh Table Operations
    // ==========================================
    val allTasbeeh: Flow<List<TasbeehEntity>> = tasbeehDao.getAllTasbeeh()

    suspend fun addTasbeeh(title: String, target: Int): Long {
        return tasbeehDao.insertTasbeeh(
            TasbeehEntity(
                title = title,
                target = target,
                count = 0,
                isCustom = true
            )
        )
    }

    suspend fun incrementTasbeeh(id: Long, currentCount: Int) {
        tasbeehDao.updateCount(id, currentCount + 1, System.currentTimeMillis())
    }

    suspend fun resetTasbeeh(id: Long) {
        tasbeehDao.resetCount(id, System.currentTimeMillis())
    }

    suspend fun deleteTasbeeh(id: Long) {
        tasbeehDao.deleteTasbeehById(id)
    }

    // ==========================================
    // 6. Quran Progress Operations
    // ==========================================
    val quranProgress: Flow<QuranProgressEntity?> = nusakkirDao.getQuranProgress()

    suspend fun updateQuranProgress(surahNumber: Int, surahName: String, ayahNumber: Int) {
        nusakkirDao.saveQuranProgress(
            QuranProgressEntity(
                id = 1,
                surahNumber = surahNumber,
                surahName = surahName,
                ayahNumber = ayahNumber,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
