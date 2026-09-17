package com.example.data.api

import com.example.data.api.model.AladhanCalculationMethod
import com.example.data.api.model.AladhanTimingsResponse
import com.example.data.local.NusakkirDatabase
import com.example.data.local.entity.PrayerTimeEntity
import com.example.data.prayer.CityLocation
import com.example.data.prayer.PrayerSchedule
import com.example.data.prayer.PrayerTimeItem
import com.example.data.prayer.PrayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repository to interact with Aladhan Prayer Times API (https://aladhan.com/prayer-times-api),
 * fetch prayer times, methods, and places, and persist to local Room Database.
 */
class AladhanRepository(
    private val apiService: AladhanApiService = AladhanApiClient.service,
    private val database: NusakkirDatabase? = null
) {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val standardDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Arabic labels mapping for Aladhan method IDs
     */
    private val arabicMethodNames = mapOf(
        0 to "الشيعة الإثنا عشرية (قم)",
        1 to "جامعة العلوم الإسلامية (كراتشي)",
        2 to "الجمعية الإسلامية لأمريكا الشمالية (ISNA)",
        3 to "رابطة العالم الإسلامي (MWL)",
        4 to "جامعة أم القرى (مكة المكرمة)",
        5 to "الهيئة المصرية العامة للمساحة",
        7 to "معهد الجيوفيزياء (جامعة طهران)",
        8 to "منطقة الخليج العربي",
        9 to "وزارة الأوقاف والشؤون الإسلامية (الكويت)",
        10 to "قطر",
        11 to "مجلس سنغافورة الإسلامي",
        12 to "اتحاد المنظمات الإسلامية (فرنسا)",
        13 to "رئاسة الشؤون الدينية (تركيا)",
        14 to "الإدارة الدينية لمسلمي روسيا",
        15 to "لجنة رؤية الهلال العالمية",
        16 to "دبي (دائرة الشؤون الإسلامية)",
        17 to "إدارة التنمية الإسلامية (ماليزيا)",
        18 to "تونس (معهد الرصد الجوي)",
        19 to "الجزائر (وزارة الشؤون الدينية والأوقاف)",
        20 to "إندونيسيا (وزارة الشؤون الدينية)",
        21 to "المغرب (وزارة الأوقاف والشؤون الإسلامية)",
        22 to "البرتغال (الجامعة الإسلامية بلشبونة)",
        23 to "الأردن (وزارة الأوقاف والشؤون والمقدسات الإسلامية)"
    )

    /**
     * Extended catalog of Places (Algeria, Arab world, Islamic world)
     */
    val availablePlaces: List<CityLocation> = listOf(
        // الجزائر (جميع الولايات والمدن الرئيسية)
        CityLocation("المسيلة", "الجزائر", 35.7058, 4.5419, 1.0),
        CityLocation("الجزائر العاصمة", "الجزائر", 36.7538, 3.0588, 1.0),
        CityLocation("وهران", "الجزائر", 35.6976, -0.6337, 1.0),
        CityLocation("قسنطينة", "الجزائر", 36.3650, 6.6147, 1.0),
        CityLocation("سطيف", "الجزائر", 36.1898, 5.4108, 1.0),
        CityLocation("عنابة", "الجزائر", 36.9000, 7.7667, 1.0),
        CityLocation("باتنة", "الجزائر", 35.5559, 6.1741, 1.0),
        CityLocation("تلمسان", "الجزائر", 34.8783, -1.3150, 1.0),
        CityLocation("بسكرة", "الجزائر", 34.8504, 5.7281, 1.0),
        CityLocation("غرداية", "الجزائر", 32.4909, 3.6735, 1.0),
        CityLocation("ورقلة", "الجزائر", 31.9493, 5.3250, 1.0),
        CityLocation("بشار", "الجزائر", 31.6167, -2.2167, 1.0),
        CityLocation("تمنراست", "الجزائر", 22.7850, 5.5228, 1.0),
        CityLocation("البليدة", "الجزائر", 36.4700, 2.8277, 1.0),
        CityLocation("الشلف", "الجزائر", 36.1653, 1.3344, 1.0),
        CityLocation("جيجل", "الجزائر", 36.8206, 5.7667, 1.0),
        CityLocation("سكيكدة", "الجزائر", 36.8792, 6.9069, 1.0),
        CityLocation("مستغانم", "الجزائر", 35.9311, 0.0892, 1.0),
        CityLocation("برج بوعريريج", "الجزائر", 36.0732, 4.7611, 1.0),
        CityLocation("البويرة", "الجزائر", 36.3749, 3.9020, 1.0),
        CityLocation("تيزي وزو", "الجزائر", 36.7118, 4.0459, 1.0),
        CityLocation("بجاية", "الجزائر", 36.7509, 5.0567, 1.0),
        CityLocation("المدية", "الجزائر", 36.2642, 2.7539, 1.0),
        CityLocation("الجلفة", "الجزائر", 34.6728, 3.2630, 1.0),
        CityLocation("تبسة", "الجزائر", 35.4042, 8.1242, 1.0),

        // الحرمان الشريفان والمدن الإسلامية
        CityLocation("مكة المكرمة", "السعودية", 21.4225, 39.8262, 3.0),
        CityLocation("المدينة المنورة", "السعودية", 24.4672, 39.6111, 3.0),
        CityLocation("القدس الشريف", "فلسطين", 31.7683, 35.2137, 2.0),
        CityLocation("الرياض", "السعودية", 24.7136, 46.6753, 3.0),
        CityLocation("جدة", "السعودية", 21.5433, 39.1728, 3.0),
        CityLocation("القاهرة", "مصر", 30.0444, 31.2357, 2.0),
        CityLocation("الإسكندرية", "مصر", 31.2001, 29.9187, 2.0),
        CityLocation("عمان", "الأردن", 31.9454, 35.9284, 3.0),
        CityLocation("الدار البيضاء", "المغرب", 33.5731, -7.5898, 1.0),
        CityLocation("الرباط", "المغرب", 34.0209, -6.8416, 1.0),
        CityLocation("فاس", "المغرب", 34.0181, -5.0078, 1.0),
        CityLocation("مراكش", "المغرب", 31.6295, -7.9811, 1.0),
        CityLocation("تونس العاصمة", "تونس", 36.8065, 10.1815, 1.0),
        CityLocation("صفاقس", "تونس", 34.7406, 10.7603, 1.0),
        CityLocation("سوسة", "تونس", 35.8256, 10.6084, 1.0),
        CityLocation("طرابلس", "ليبيا", 32.8872, 13.1913, 2.0),
        CityLocation("بنغازي", "ليبيا", 32.1167, 20.0667, 2.0),
        CityLocation("الخرطوم", "السودان", 15.5007, 32.5599, 2.0),
        CityLocation("دبي", "الإمارات", 25.2048, 55.2708, 4.0),
        CityLocation("أبوظبي", "الإمارات", 24.4539, 54.3773, 4.0),
        CityLocation("الدوحة", "قطر", 25.2854, 51.5310, 3.0),
        CityLocation("مدينة الكويت", "الكويت", 29.3759, 47.9774, 3.0),
        CityLocation("المنامة", "البحرين", 26.2285, 50.5860, 3.0),
        CityLocation("مسقط", "عُمان", 23.5880, 58.3829, 4.0),
        CityLocation("بغداد", "العراق", 33.3152, 44.3661, 3.0),
        CityLocation("البصرة", "العراق", 30.5081, 47.7835, 3.0),
        CityLocation("أربيل", "العراق", 36.1901, 44.0091, 3.0),
        CityLocation("دمشق", "سوريا", 33.5138, 36.2765, 3.0),
        CityLocation("حلب", "سوريا", 36.2021, 37.1343, 3.0),
        CityLocation("بيروت", "لبنان", 33.8938, 35.5018, 2.0),
        CityLocation("صنعاء", "اليمن", 15.3694, 44.1910, 3.0),
        CityLocation("إسطنبول", "تركيا", 41.0082, 28.9784, 3.0),
        CityLocation("أنقرة", "تركيا", 39.9334, 32.8597, 3.0),
        CityLocation("جاكرتا", "إندونيسيا", -6.2088, 106.8456, 7.0),
        CityLocation("كوالالمبور", "ماليزيا", 3.1390, 101.6869, 8.0)
    )

    /**
     * Search places by Arabic or English query
     */
    fun searchPlaces(query: String): List<CityLocation> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return availablePlaces
        return availablePlaces.filter {
            it.nameAr.contains(q, ignoreCase = true) ||
            it.countryAr.contains(q, ignoreCase = true)
        }
    }

    /**
     * Fetch all calculation methods (الدوال) from Aladhan API
     */
    suspend fun getCalculationMethods(): Result<List<AladhanCalculationMethod>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMethods()
            if (response.code == 200) {
                val methodsList = response.data.map { (key, method) ->
                    val nameAr = arabicMethodNames[method.id] ?: method.name
                    AladhanCalculationMethod(
                        id = method.id,
                        codeName = key,
                        nameEn = method.name,
                        nameAr = nameAr
                    )
                }.sortedBy { it.id }
                Result.success(methodsList)
            } else {
                Result.success(getDefaultMethodsFallback())
            }
        } catch (e: Exception) {
            // Return rich offline fallback if network is unreachable
            Result.success(getDefaultMethodsFallback())
        }
    }

    /**
     * Fetch prayer times from Aladhan API by coordinates and cache into Room Database
     */
    suspend fun getPrayerTimesByCoordinates(
        latitude: Double,
        longitude: Double,
        cityName: String = "",
        date: Date = Date(),
        methodId: Int = 19, // Default to 19 (Algeria)
        schoolId: Int = 0 // 0 = Shafi/Maliki/Hanbali, 1 = Hanafi
    ): Result<PrayerSchedule> = withContext(Dispatchers.IO) {
        try {
            val dateStr = dateFormat.format(date)
            val response = apiService.getTimings(
                date = dateStr,
                latitude = latitude,
                longitude = longitude,
                method = methodId,
                school = schoolId
            )

            if (response.code == 200) {
                val data = response.data
                val schedule = mapToPrayerSchedule(data, date)

                // Cache in Room Database
                val dbDate = standardDateFormat.format(date)
                saveToRoom(dbDate, cityName, latitude, longitude, data, methodId.toString())

                Result.success(schedule)
            } else {
                Result.failure(Exception("Aladhan API error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch prayer times by City and Country name from Aladhan API
     */
    suspend fun getPrayerTimesByCity(
        city: String,
        country: String = "Algeria",
        date: Date = Date(),
        methodId: Int = 19,
        schoolId: Int = 0
    ): Result<PrayerSchedule> = withContext(Dispatchers.IO) {
        try {
            val dateStr = dateFormat.format(date)
            val response = apiService.getTimingsByCity(
                date = dateStr,
                city = city,
                country = country,
                method = methodId,
                school = schoolId
            )

            if (response.code == 200) {
                val data = response.data
                val schedule = mapToPrayerSchedule(data, date)

                val lat = data.meta.latitude ?: 0.0
                val lng = data.meta.longitude ?: 0.0
                val dbDate = standardDateFormat.format(date)
                saveToRoom(dbDate, city, lat, lng, data, methodId.toString())

                Result.success(schedule)
            } else {
                Result.failure(Exception("Aladhan API error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveToRoom(
        date: String,
        cityName: String,
        latitude: Double,
        longitude: Double,
        data: AladhanTimingsResponse,
        methodName: String
    ) {
        try {
            val entity = PrayerTimeEntity(
                date = date,
                cityName = cityName,
                latitude = latitude,
                longitude = longitude,
                fajr = data.timings.cleanTime(data.timings.fajr),
                sunrise = data.timings.cleanTime(data.timings.sunrise),
                dhuhr = data.timings.cleanTime(data.timings.dhuhr),
                asr = data.timings.cleanTime(data.timings.asr),
                maghrib = data.timings.cleanTime(data.timings.maghrib),
                isha = data.timings.cleanTime(data.timings.isha),
                imsak = data.timings.imsak?.let { data.timings.cleanTime(it) } ?: "",
                midnight = data.timings.midnight?.let { data.timings.cleanTime(it) } ?: "",
                calculationMethod = methodName,
                juristicMethod = data.meta.school ?: "STANDARD",
                dateHijri = data.date.hijri?.date ?: ""
            )
            database?.prayerTimesDao()?.insertPrayerTimes(entity)
        } catch (_: Exception) {
            // Room cache silent fallback
        }
    }

    private fun mapToPrayerSchedule(data: AladhanTimingsResponse, date: Date): PrayerSchedule {
        val timings = data.timings
        val calendar = Calendar.getInstance().apply { time = date }
        val now = System.currentTimeMillis()

        fun parseTimeToMillis(timeStr: String): Pair<Int, Int> {
            val cleaned = timings.cleanTime(timeStr)
            val parts = cleaned.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return Pair(h, m)
        }

        fun createPrayerItem(type: PrayerType, rawTime: String): PrayerTimeItem {
            val cleaned = timings.cleanTime(rawTime)
            val (h, m) = parseTimeToMillis(rawTime)
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return PrayerTimeItem(
                type = type,
                timeFormatted = cleaned,
                hour = h,
                minute = m,
                timestampMillis = cal.timeInMillis,
                isNext = false
            )
        }

        val rawPrayers = listOf(
            createPrayerItem(PrayerType.FAJR, timings.fajr),
            createPrayerItem(PrayerType.SUNRISE, timings.sunrise),
            createPrayerItem(PrayerType.DHUHR, timings.dhuhr),
            createPrayerItem(PrayerType.ASR, timings.asr),
            createPrayerItem(PrayerType.MAGHRIB, timings.maghrib),
            createPrayerItem(PrayerType.ISHA, timings.isha)
        )

        val nextIndex = rawPrayers.indexOfFirst { it.timestampMillis > now }
        val nextPrayer = if (nextIndex != -1) rawPrayers[nextIndex] else rawPrayers.first()
        val remainingMillis = if (nextIndex != -1) {
            nextPrayer.timestampMillis - now
        } else {
            (nextPrayer.timestampMillis + 24 * 3600 * 1000) - now
        }

        val prayersWithNext = rawPrayers.map { item ->
            item.copy(isNext = item.type == nextPrayer.type)
        }

        val dateString = standardDateFormat.format(date)
        return PrayerSchedule(
            dateString = dateString,
            prayers = prayersWithNext,
            nextPrayer = nextPrayer,
            remainingMillisToNext = remainingMillis.coerceAtLeast(0),
            midnightTimeFormatted = timings.midnight?.let { timings.cleanTime(it) },
            imsakTimeFormatted = timings.imsak?.let { timings.cleanTime(it) }
        )
    }

    /**
     * Fetch monthly prayer calendar for coordinates from Aladhan API
     */
    suspend fun getMonthlyPrayerCalendar(
        year: Int,
        month: Int,
        latitude: Double,
        longitude: Double,
        methodId: Int = 19,
        schoolId: Int = 0,
        tune: String? = null
    ): Result<List<PrayerSchedule>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCalendar(
                year = year,
                month = month,
                latitude = latitude,
                longitude = longitude,
                method = methodId,
                school = schoolId,
                tune = tune
            )
            if (response.code == 200) {
                val list = response.data.map { dayData ->
                    // Parse day date e.g. "DD-MM-YYYY"
                    val parsedDate = try {
                        dateFormat.parse(dayData.date.gregorian?.date ?: "") ?: Date()
                    } catch (_: Exception) {
                        Date()
                    }
                    mapToPrayerSchedule(dayData, parsedDate)
                }
                Result.success(list)
            } else {
                Result.failure(Exception("Aladhan API error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch Qibla direction from Aladhan API for coordinates
     */
    suspend fun getQiblaDirection(
        latitude: Double,
        longitude: Double
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getQibla(latitude, longitude)
            if (response.code == 200) {
                Result.success(response.data.direction)
            } else {
                Result.failure(Exception("Aladhan API error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDefaultMethodsFallback(): List<AladhanCalculationMethod> {
        return listOf(
            AladhanCalculationMethod(19, "ALGERIA", "Algeria", "الجزائر (وزارة الشؤون الدينية والأوقاف)"),
            AladhanCalculationMethod(3, "MWL", "Muslim World League", "رابطة العالم الإسلامي"),
            AladhanCalculationMethod(4, "MAKKAH", "Umm Al-Qura University, Makkah", "جامعة أم القرى (مكة المكرمة)"),
            AladhanCalculationMethod(5, "EGYPT", "Egyptian General Authority of Survey", "الهيئة المصرية العامة للمساحة"),
            AladhanCalculationMethod(18, "TUNISIA", "Tunisia", "تونس (معهد الرصد الجوي)"),
            AladhanCalculationMethod(21, "MOROCCO", "Morocco", "المغرب (وزارة الأوقاف والشؤون الإسلامية)"),
            AladhanCalculationMethod(2, "ISNA", "Islamic Society of North America", "الجمعية الإسلامية لأمريكا الشمالية"),
            AladhanCalculationMethod(1, "KARACHI", "University of Islamic Sciences, Karachi", "جامعة العلوم الإسلامية (كراتشي)"),
            AladhanCalculationMethod(16, "DUBAI", "Dubai", "دبي (دائرة الشؤون الإسلامية)"),
            AladhanCalculationMethod(9, "KUWAIT", "Kuwait", "الكويت (وزارة الأوقاف)"),
            AladhanCalculationMethod(10, "QATAR", "Qatar", "قطر"),
            AladhanCalculationMethod(13, "TURKEY", "Diyanet İşleri Başkanlığı", "رئاسة الشؤون الدينية (تركيا)"),
            AladhanCalculationMethod(23, "JORDAN", "Jordan", "الأردن (وزارة الأوقاف)")
        )
    }
}
