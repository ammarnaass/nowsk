package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.adhkar.AdhkarRepository
import com.example.data.local.converters.Converters
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        SettingEntity::class,
        PrayerTimeEntity::class,
        AdhkarEntity::class,
        FavoriteEntity::class,
        TasbeehEntity::class,
        QuranProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NusakkirDatabase : RoomDatabase() {

    abstract fun settingsDao(): SettingsDao
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun adhkarDao(): AdhkarDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun tasbeehDao(): TasbeehDao
    abstract fun nusakkirDao(): NusakkirDao

    companion object {
        @Volatile
        private var INSTANCE: NusakkirDatabase? = null

        fun getInstance(context: Context): NusakkirDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext, CoroutineScope(SupervisorJob() + Dispatchers.IO)).also {
                    INSTANCE = it
                }
            }
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ): NusakkirDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext, scope).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(appContext: Context, scope: CoroutineScope): NusakkirDatabase {
            return Room.databaseBuilder(
                appContext,
                NusakkirDatabase::class.java,
                "nusakkir_database"
            )
                .fallbackToDestructiveMigration()
                .addCallback(NusakkirDatabaseCallback(scope))
                .build()
        }

        private class NusakkirDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        ensureInitialData(database)
                    }
                }
            }

            private suspend fun ensureInitialData(database: NusakkirDatabase) {
                try {
                    val hasSettings = database.settingsDao().getSettingValue("onboarding_completed") != null
                    if (!hasSettings) {
                        populateInitialData(database)
                    }
                } catch (e: Exception) {
                    // Fallback to populate if table check fails
                    try {
                        populateInitialData(database)
                    } catch (_: Exception) { }
                }
            }

            suspend fun populateInitialData(database: NusakkirDatabase) {
                // 1. Pre-populate Tasbeeh items
                val defaultTasbeeh = listOf(
                    TasbeehEntity(title = "سبحان الله", target = 33, count = 0),
                    TasbeehEntity(title = "الحمد لله", target = 33, count = 0),
                    TasbeehEntity(title = "الله أكبر", target = 34, count = 0),
                    TasbeehEntity(title = "لا إله إلا الله", target = 100, count = 0),
                    TasbeehEntity(title = "أستغفر الله وأتوب إليه", target = 100, count = 0),
                    TasbeehEntity(title = "اللهم صلِّ وسلم على نبينا محمد", target = 100, count = 0),
                    TasbeehEntity(title = "لا حول ولا قوة إلا بالله", target = 100, count = 0),
                    TasbeehEntity(title = "سبحان الله وبحمده، سبحان الله العظيم", target = 100, count = 0)
                )
                database.tasbeehDao().insertAllTasbeeh(defaultTasbeeh)

                // 2. Pre-populate Adhkar catalog
                val defaultAdhkar = AdhkarRepository.DHIKR_ITEMS.map { dhikr ->
                    AdhkarEntity(
                        id = dhikr.id,
                        categoryId = dhikr.categoryId,
                        categoryNameAr = dhikr.categoryNameAr,
                        title = dhikr.title,
                        text = dhikr.text,
                        targetCount = dhikr.count,
                        currentCount = 0,
                        virtue = dhikr.virtue,
                        source = dhikr.source,
                        isFavorite = false,
                        isCompletedToday = false
                    )
                }
                database.adhkarDao().insertAllAdhkar(defaultAdhkar)

                // 3. Pre-populate Quran Progress
                database.nusakkirDao().saveQuranProgress(
                    QuranProgressEntity(
                        id = 1,
                        surahNumber = 1,
                        surahName = "الفاتحة",
                        ayahNumber = 1
                    )
                )

                // 4. Pre-populate default Settings
                val defaultSettings = listOf(
                    SettingEntity("onboarding_completed", "false"),
                    SettingEntity("selected_theme", "SYSTEM"),
                    SettingEntity("selected_city", "المسيلة"),
                    SettingEntity("calc_method", "MUSLIM_WORLD_LEAGUE"),
                    SettingEntity("juristic_method", "SHAFI"),
                    SettingEntity("notify_prayer", "true"),
                    SettingEntity("notify_before_prayer", "true"),
                    SettingEntity("notify_morning_adhkar", "true"),
                    SettingEntity("notify_evening_adhkar", "true")
                )
                defaultSettings.forEach { setting ->
                    database.settingsDao().setSetting(setting)
                }
            }
        }
    }
}
