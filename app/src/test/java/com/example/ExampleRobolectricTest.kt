package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.calendar.HijriCalendarHelper
import com.example.data.prayer.CalculationMethod
import com.example.data.prayer.JuristicMethod
import com.example.data.prayer.PrayerTimesCalculator
import com.example.data.qibla.QiblaCalculator
import androidx.room.Room
import com.example.data.local.NusakkirDatabase
import com.example.data.local.entity.*
import com.example.data.repository.NusakkirRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("نسكّر", appName)
  }

  @Test
  fun `calculate prayer times for Makkah`() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.MARCH, 15, 12, 0, 0)
    }
    val schedule = PrayerTimesCalculator.calculatePrayerTimes(
      latitude = 21.4225,
      longitude = 39.8262,
      calendar = cal,
      method = CalculationMethod.UMM_AL_QURA,
      juristic = JuristicMethod.SHAFI
    )
    assertEquals(6, schedule.prayers.size)
    assertNotNull(schedule.nextPrayer)
  }

  @Test
  fun `calculate qibla angle and hijri date`() {
    val qibla = QiblaCalculator.calculateQiblaAngle(30.0444, 31.2357) // Cairo
    assertTrue(qibla > 130.0 && qibla < 145.0)

    val hijri = HijriCalendarHelper.getTodayHijriDate()
    assertTrue(hijri.year >= 1447)
    assertTrue(hijri.day in 1..30)
  }

  @Test
  fun `verify Room Database tables and repository operations`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, NusakkirDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val repo = NusakkirRepository(db)

    // 1. Settings Table
    repo.setSetting("selected_theme", "DARK")
    val theme = repo.getSetting("selected_theme").first()
    assertEquals("DARK", theme)

    // 2. PrayerTimes Table
    repo.savePrayerTimes(
      PrayerTimeEntity(
        date = "2026-03-15",
        cityName = "مكة المكرمة",
        latitude = 21.4225,
        longitude = 39.8262,
        fajr = "05:15",
        sunrise = "06:30",
        dhuhr = "12:30",
        asr = "15:50",
        maghrib = "18:30",
        isha = "20:00",
        calculationMethod = "UMM_AL_QURA",
        juristicMethod = "SHAFI"
      )
    )
    val prayerTimes = repo.getPrayerTimesForDate("2026-03-15").first()
    assertNotNull(prayerTimes)
    assertEquals("05:15", prayerTimes?.fajr)

    // 3. Adhkar Table
    db.adhkarDao().insertAdhkar(
      AdhkarEntity(
        id = "test_dhikr_1",
        categoryId = "morning",
        categoryNameAr = "أذكار الصباح",
        title = "سبحان الله",
        text = "سبحان الله وبحمده",
        targetCount = 33,
        currentCount = 10
      )
    )
    val adhkarList = repo.getAdhkarByCategory("morning").first()
    assertEquals(1, adhkarList.size)
    assertEquals("سبحان الله", adhkarList[0].title)

    // 4. Favorites Table
    repo.addFavorite(
      FavoriteEntity(
        type = "dhikr",
        title = "آية الكرسي",
        subtitle = "سورة البقرة",
        content = "الله لا إله إلا هو الحي القيوم"
      )
    )
    val favorites = repo.allFavorites.first()
    assertEquals(1, favorites.size)
    assertEquals("آية الكرسي", favorites[0].title)

    // 5. Tasbeeh Table
    val id = repo.addTasbeeh("الحمد لله", 33)
    repo.incrementTasbeeh(id, 0)
    val tasbeehList = repo.allTasbeeh.first()
    assertEquals(1, tasbeehList.size)
    assertEquals(1, tasbeehList[0].count)

    db.close()
  }

  @Test
  fun `verify NusakkirDatabase Singleton pattern returns same instance`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val instance1 = NusakkirDatabase.getInstance(context)
    val instance2 = NusakkirDatabase.getInstance(context)
    val instance3 = NusakkirDatabase.getDatabase(context)

    assertNotNull(instance1)
    assertEquals(instance1, instance2)
    assertEquals(instance1, instance3)
    assertNotNull(instance1.settingsDao())
    assertNotNull(instance1.prayerTimesDao())
    assertNotNull(instance1.adhkarDao())
    assertNotNull(instance1.favoritesDao())
    assertNotNull(instance1.tasbeehDao())
  }

  @Test
  fun `verify Room TypeConverters and PRD entity fields`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, NusakkirDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    // Test AdhkarEntity with tags (List<String> TypeConverter)
    val testAdhkar = AdhkarEntity(
      id = "test_prd_dhikr",
      categoryId = "evening",
      categoryNameAr = "أذكار المساء",
      title = "أمسينا وأمسى الملك لله",
      text = "أمسينا وأمسى الملك لله والحمد لله",
      targetCount = 1,
      currentCount = 0,
      virtue = "حفظ من الشرور",
      source = "صحيح مسلم",
      audioUrl = "https://example.com/audio.mp3",
      isFavorite = true,
      tags = listOf("مساء", "حفظ", "سنة")
    )
    db.adhkarDao().insertAdhkar(testAdhkar)
    val fetchedAdhkar = db.adhkarDao().getDhikrById("test_prd_dhikr").first()
    assertNotNull(fetchedAdhkar)
    assertEquals(listOf("مساء", "حفظ", "سنة"), fetchedAdhkar?.tags)
    assertEquals("https://example.com/audio.mp3", fetchedAdhkar?.audioUrl)

    // Test FavoriteEntity with tags and referenceId
    val testFav = FavoriteEntity(
      type = "hadith",
      referenceId = "bukhari_1",
      title = "إنما الأعمال بالنيات",
      subtitle = "صحيح البخاري",
      content = "إنما الأعمال بالنيات وإنما لكل امرئ ما نوى",
      tags = listOf("نية", "إخلاص")
    )
    val favId = db.favoritesDao().insertFavorite(testFav)
    val allFavs = db.favoritesDao().getAllFavorites().first()
    val savedFav = allFavs.find { it.id == favId }
    assertNotNull(savedFav)
    assertEquals("bukhari_1", savedFav?.referenceId)
    assertEquals(listOf("نية", "إخلاص"), savedFav?.tags)

    // Test PrayerTimeEntity with extra PRD fields
    val testPrayer = PrayerTimeEntity(
      date = "2026-09-16",
      cityName = "الجزائر",
      latitude = 36.7538,
      longitude = 3.0588,
      fajr = "04:55",
      sunrise = "06:25",
      dhuhr = "12:45",
      asr = "16:15",
      maghrib = "18:55",
      isha = "20:15",
      imsak = "04:45",
      midnight = "23:50",
      calculationMethod = "ALGERIA",
      juristicMethod = "MALIKI",
      dateHijri = "05-04-1448"
    )
    db.prayerTimesDao().insertPrayerTimes(testPrayer)
    val savedPrayer = db.prayerTimesDao().getPrayerTimesForDate("2026-09-16").first()
    assertNotNull(savedPrayer)
    assertEquals("04:45", savedPrayer?.imsak)
    assertEquals("05-04-1448", savedPrayer?.dateHijri)

    // Test TasbeehEntity totalCount and dailyGoal
    val testTasbeeh = TasbeehEntity(
      title = "سبحان الله وبحمده",
      count = 33,
      target = 33,
      totalCount = 330,
      isCustom = true,
      dailyGoal = 100
    )
    val tasbeehId = db.tasbeehDao().insertTasbeeh(testTasbeeh)
    val savedTasbeeh = db.tasbeehDao().getTasbeehById(tasbeehId).first()
    assertNotNull(savedTasbeeh)
    assertEquals(330, savedTasbeeh?.totalCount)
    assertEquals(100, savedTasbeeh?.dailyGoal)

    // Test SettingEntity description
    db.settingsDao().setSetting(SettingEntity("app_language", "ar", "لغة التطبيق الأساسية"))
    val savedSetting = db.settingsDao().getAllSettings().first().find { it.key == "app_language" }
    assertNotNull(savedSetting)
    assertEquals("ar", savedSetting?.value)
    assertEquals("لغة التطبيق الأساسية", savedSetting?.description)

    db.close()
  }

  @Test
  fun `verify MainActivity and MainViewModel launch without crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun `verify aladhan repository places and available methods`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, NusakkirDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val aladhanRepo = com.example.data.api.AladhanRepository(database = db)

    // Verify places catalog contains key Algerian & Arab cities
    val places = aladhanRepo.availablePlaces
    assertTrue(places.size >= 15)
    assertNotNull(places.find { it.nameAr == "الجزائر العاصمة" })
    assertNotNull(places.find { it.nameAr == "وهران" })
    assertNotNull(places.find { it.nameAr == "مكة المكرمة" })

    // Test place search
    val searchResults = aladhanRepo.searchPlaces("قسنطينة")
    assertTrue(searchResults.any { it.nameAr.contains("قسنطينة") })

    db.close()
  }
}
