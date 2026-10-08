package com.example.data.prayer

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

enum class PrayerType(val arabicName: String, val englishName: String) {
    FAJR("الفجر", "Fajr"),
    SUNRISE("الشروق", "Sunrise"),
    DHUHR("الظهر", "Dhuhr"),
    ASR("العصر", "Asr"),
    MAGHRIB("المغرب", "Maghrib"),
    ISHA("العشاء", "Isha")
}

data class PrayerTimeItem(
    val type: PrayerType,
    val timeFormatted: String,
    val hour: Int,
    val minute: Int,
    val timestampMillis: Long,
    val isNext: Boolean = false
)

data class PrayerSchedule(
    val dateString: String,
    val prayers: List<PrayerTimeItem>,
    val nextPrayer: PrayerTimeItem?,
    val remainingMillisToNext: Long,
    val midnightTimeFormatted: String? = null,
    val imsakTimeFormatted: String? = null
)

enum class CalculationMethod(
    val displayNameAr: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val isIshaInterval: Boolean = false,
    val ishaIntervalMinutes: Int = 90
) {
    ALGERIA("وزارة الشؤون الدينية والأوقاف (الجزائر)", 18.0, 17.0),
    MUSLIM_WORLD_LEAGUE("رابطة العالم الإسلامي", 18.0, 17.0),
    EGYPTIAN("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    UMM_AL_QURA("جامعة أم القرى (مكة المكرمة)", 18.5, 0.0, true, 90),
    KARACHI("جامعة العلوم الإسلامية (كراتشي)", 18.0, 18.0),
    TEHRAN("معهد الجيوفيزياء (جامعة طهران)", 17.7, 14.0),
    SHIA_ITHNA_ASHARI("الشيعة الإثنا عشرية (قم)", 16.0, 14.0)
}

enum class JuristicMethod(val displayNameAr: String, val shadowFactor: Double) {
    SHAFI("الشافعي والمالكي والحنبلي", 1.0),
    HANAFI("الحنفي", 2.0)
}

data class CityLocation(
    val nameAr: String,
    val countryAr: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneOffsetHours: Double,
    val nameEn: String = "",
    val wilayaCode: Int = 0
)

object CityPresets {
    val CITIES = listOf(
        // الولايات الجزائرية الـ 58 مرتبة حسب الترقيم الإداري الرسمي مع إحداثيات مركز كل ولاية
        CityLocation("01 - أدرار", "الجزائر", 27.8742, -0.2939, 1.0, "Adrar", 1),
        CityLocation("02 - الشلف", "الجزائر", 36.1653, 1.3345, 1.0, "Chlef", 2),
        CityLocation("03 - الأغواط", "الجزائر", 33.8000, 2.8651, 1.0, "Laghouat", 3),
        CityLocation("04 - أم البواقي", "الجزائر", 35.8755, 7.1135, 1.0, "Oum El Bouaghi", 4),
        CityLocation("05 - باتنة", "الجزائر", 35.5559, 6.1741, 1.0, "Batna", 5),
        CityLocation("06 - بجاية", "الجزائر", 36.7511, 5.0567, 1.0, "Béjaïa", 6),
        CityLocation("07 - بسكرة", "الجزائر", 34.8504, 5.7281, 1.0, "Biskra", 7),
        CityLocation("08 - بشار", "الجزائر", 31.6167, -2.2167, 1.0, "Béchar", 8),
        CityLocation("09 - البليدة", "الجزائر", 36.4700, 2.8300, 1.0, "Blida", 9),
        CityLocation("10 - البويرة", "الجزائر", 36.3749, 3.9020, 1.0, "Bouira", 10),
        CityLocation("11 - تمنراست", "الجزائر", 22.7850, 5.5228, 1.0, "Tamanrasset", 11),
        CityLocation("12 - تبسة", "الجزائر", 35.4042, 8.1242, 1.0, "Tébessa", 12),
        CityLocation("13 - تلمسان", "الجزائر", 34.8783, -1.3150, 1.0, "Tlemcen", 13),
        CityLocation("14 - تيارت", "الجزائر", 35.3710, 1.3170, 1.0, "Tiaret", 14),
        CityLocation("15 - تيزي وزو", "الجزائر", 36.7118, 4.0459, 1.0, "Tizi Ouzou", 15),
        CityLocation("16 - الجزائر العاصمة", "الجزائر", 36.7538, 3.0588, 1.0, "Alger", 16),
        CityLocation("17 - الجلفة", "الجزائر", 34.6728, 3.2630, 1.0, "Djelfa", 17),
        CityLocation("18 - جيجل", "الجزائر", 36.8206, 5.7667, 1.0, "Jijel", 18),
        CityLocation("19 - سطيف", "الجزائر", 36.1898, 5.4108, 1.0, "Sétif", 19),
        CityLocation("20 - سعيدة", "الجزائر", 34.8303, 0.1517, 1.0, "Saïda", 20),
        CityLocation("21 - سكيكدة", "الجزائر", 36.8663, 6.9047, 1.0, "Skikda", 21),
        CityLocation("22 - سيدي بلعباس", "الجزائر", 35.1899, -0.6308, 1.0, "Sidi Bel Abbès", 22),
        CityLocation("23 - عنابة", "الجزائر", 36.9000, 7.7667, 1.0, "Annaba", 23),
        CityLocation("24 - قالمة", "الجزائر", 36.4622, 7.4261, 1.0, "Guelma", 24),
        CityLocation("25 - قسنطينة", "الجزائر", 36.3650, 6.6147, 1.0, "Constantine", 25),
        CityLocation("26 - المدية", "الجزائر", 36.2642, 2.7539, 1.0, "Médéa", 26),
        CityLocation("27 - مستغانم", "الجزائر", 35.9312, 0.0892, 1.0, "Mostaganem", 27),
        CityLocation("28 - المسيلة", "الجزائر", 35.7058, 4.5419, 1.0, "M'Sila", 28),
        CityLocation("29 - معسكر", "الجزائر", 35.3967, 0.1403, 1.0, "Mascara", 29),
        CityLocation("30 - ورقلة", "الجزائر", 31.9493, 5.3250, 1.0, "Ouargla", 30),
        CityLocation("31 - وهران", "الجزائر", 35.6976, -0.6337, 1.0, "Oran", 31),
        CityLocation("32 - البيض", "الجزائر", 33.6832, 1.0193, 1.0, "El Bayadh", 32),
        CityLocation("33 - إليزي", "الجزائر", 26.4833, 8.4667, 1.0, "Illizi", 33),
        CityLocation("34 - برج بوعريريج", "الجزائر", 36.0732, 4.7611, 1.0, "Bordj Bou Arréridj", 34),
        CityLocation("35 - بومرداس", "الجزائر", 36.7664, 3.4772, 1.0, "Boumerdès", 35),
        CityLocation("36 - الطارف", "الجزائر", 36.7672, 8.3138, 1.0, "El Tarf", 36),
        CityLocation("37 - تندوف", "الجزائر", 27.6711, -8.1474, 1.0, "Tindouf", 37),
        CityLocation("38 - تسمسيلت", "الجزائر", 35.6072, 1.8108, 1.0, "Tissemsilt", 38),
        CityLocation("39 - الوادي", "الجزائر", 33.3683, 6.8674, 1.0, "El Oued", 39),
        CityLocation("40 - خنشلة", "الجزائر", 35.4358, 7.1433, 1.0, "Khenchela", 40),
        CityLocation("41 - سوق أهراس", "الجزائر", 36.2864, 7.9511, 1.0, "Souk Ahras", 41),
        CityLocation("42 - تيبازة", "الجزائر", 36.5897, 2.4475, 1.0, "Tipaza", 42),
        CityLocation("43 - ميلة", "الجزائر", 36.4503, 6.2644, 1.0, "Mila", 43),
        CityLocation("44 - عين الدفلى", "الجزائر", 36.2642, 1.9679, 1.0, "Aïn Defla", 44),
        CityLocation("45 - النعامة", "الجزائر", 33.2667, -0.3167, 1.0, "Naâma", 45),
        CityLocation("46 - عين تموشنت", "الجزائر", 35.2975, -1.1404, 1.0, "Aïn Témouchent", 46),
        CityLocation("47 - غرداية", "الجزائر", 32.4909, 3.6736, 1.0, "Ghardaïa", 47),
        CityLocation("48 - غليزان", "الجزائر", 35.7373, 0.5558, 1.0, "Relizane", 48),
        CityLocation("49 - تيميمون", "الجزائر", 29.2639, 0.2310, 1.0, "Timimoun", 49),
        CityLocation("50 - برج باجي مختار", "الجزائر", 21.3283, 0.9544, 1.0, "Bordj Badji Mokhtar", 50),
        CityLocation("51 - أولاد جلال", "الجزائر", 34.4333, 5.0667, 1.0, "Ouled Djellal", 51),
        CityLocation("52 - بني عباس", "الجزائر", 30.1333, -2.1667, 1.0, "Béni Abbès", 52),
        CityLocation("53 - عين صالح", "الجزائر", 27.1936, 2.4608, 1.0, "In Salah", 53),
        CityLocation("54 - عين قزام", "الجزائر", 19.5714, 5.7694, 1.0, "In Guezzam", 54),
        CityLocation("55 - تقرت", "الجزائر", 33.1053, 6.0581, 1.0, "Touggourt", 55),
        CityLocation("56 - جانت", "الجزائر", 24.5531, 9.4853, 1.0, "Djanet", 56),
        CityLocation("57 - المغير", "الجزائر", 33.9500, 5.9167, 1.0, "El M'Ghair", 57),
        CityLocation("58 - المنيعة", "الجزائر", 30.5847, 2.8778, 1.0, "El Meniaa", 58),

        // الحرمان الشريفان والعواصم الإسلامية الكبرى
        CityLocation("مكة المكرمة", "السعودية", 21.4225, 39.8262, 3.0, "Makkah"),
        CityLocation("المدينة المنورة", "السعودية", 24.4672, 39.6111, 3.0, "Madinah"),
        CityLocation("القدس الشريف", "فلسطين", 31.7683, 35.2137, 2.0, "Jerusalem"),
        CityLocation("الرياض", "السعودية", 24.7136, 46.6753, 3.0, "Riyadh"),
        CityLocation("جدة", "السعودية", 21.5433, 39.1728, 3.0, "Jeddah"),
        CityLocation("القاهرة", "مصر", 30.0444, 31.2357, 2.0, "Cairo"),
        CityLocation("الإسكندرية", "مصر", 31.2001, 29.9187, 2.0, "Alexandria"),
        CityLocation("عمان", "الأردن", 31.9454, 35.9284, 3.0, "Amman"),
        CityLocation("الدار البيضاء", "المغرب", 33.5731, -7.5898, 1.0, "Casablanca"),
        CityLocation("الرباط", "المغرب", 34.0209, -6.8416, 1.0, "Rabat"),
        CityLocation("فاس", "المغرب", 34.0181, -5.0078, 1.0, "Fes"),
        CityLocation("مراكش", "المغرب", 31.6295, -7.9811, 1.0, "Marrakech"),
        CityLocation("تونس العاصمة", "تونس", 36.8065, 10.1815, 1.0, "Tunis"),
        CityLocation("صفاقس", "تونس", 34.7406, 10.7603, 1.0, "Sfax"),
        CityLocation("سوسة", "تونس", 35.8256, 10.6084, 1.0, "Sousse"),
        CityLocation("طرابلس", "ليبيا", 32.8872, 13.1913, 2.0, "Tripoli"),
        CityLocation("بنغازي", "ليبيا", 32.1167, 20.0667, 2.0, "Benghazi"),
        CityLocation("الخرطوم", "السودان", 15.5007, 32.5599, 2.0, "Khartoum"),
        CityLocation("دبي", "الإمارات", 25.2048, 55.2708, 4.0, "Dubai"),
        CityLocation("أبوظبي", "الإمارات", 24.4539, 54.3773, 4.0, "Abu Dhabi"),
        CityLocation("الدوحة", "قطر", 25.2854, 51.5310, 3.0, "Doha"),
        CityLocation("مدينة الكويت", "الكويت", 29.3759, 47.9774, 3.0, "Kuwait City"),
        CityLocation("المنامة", "البحرين", 26.2285, 50.5860, 3.0, "Manama"),
        CityLocation("مسقط", "عُمان", 23.5880, 58.3829, 4.0, "Muscat"),
        CityLocation("بغداد", "العراق", 33.3152, 44.3661, 3.0, "Baghdad"),
        CityLocation("البصرة", "العراق", 30.5081, 47.7835, 3.0, "Basra"),
        CityLocation("دمشق", "سوريا", 33.5138, 36.2765, 3.0, "Damascus"),
        CityLocation("حلب", "سوريا", 36.2021, 37.1343, 3.0, "Aleppo"),
        CityLocation("بيروت", "لبنان", 33.8938, 35.5018, 2.0, "Beirut"),
        CityLocation("صنعاء", "اليمن", 15.3694, 44.1910, 3.0, "Sanaa"),
        CityLocation("إسطنبول", "تركيا", 41.0082, 28.9784, 3.0, "Istanbul"),
        CityLocation("أنقرة", "تركيا", 39.9334, 32.8597, 3.0, "Ankara"),
        CityLocation("جاكرتا", "إندونيسيا", -6.2088, 106.8456, 7.0, "Jakarta"),
        CityLocation("كوالالمبور", "ماليزيا", 3.1390, 101.6869, 8.0, "Kuala Lumpur")
    )

    val DEFAULT = CITIES.find { it.wilayaCode == 16 } ?: CITIES[0]

    /**
     * Find nearest city from GPS coordinates using haversine formula
     */
    fun findNearestCity(latitude: Double, longitude: Double): CityLocation? {
        if (CITIES.isEmpty()) return null
        return CITIES.minByOrNull { city ->
            val dLat = Math.toRadians(city.latitude - latitude)
            val dLon = Math.toRadians(city.longitude - longitude)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(latitude)) * cos(Math.toRadians(city.latitude)) * sin(dLon / 2).pow(2)
            2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}

object PrayerTimesCalculator {

    fun calculatePrayerTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance(),
        method: CalculationMethod = CalculationMethod.ALGERIA,
        juristic: JuristicMethod = JuristicMethod.SHAFI,
        manualOffsets: Map<PrayerType, Int> = emptyMap()
    ): PrayerSchedule {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val timezoneOffsetHours = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0

        // Julian Date
        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)

        // Solar Coordinates
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(toRadians(g)) + 0.020 * sin(toRadians(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val declination = toDegrees(asin(sin(toRadians(e)) * sin(toRadians(l))))
        val ra = toDegrees(atan2(cos(toRadians(e)) * sin(toRadians(l)), cos(toRadians(l)))) / 15.0
        val rightAscension = fixHour(ra)

        val eqt = q / 15.0 - rightAscension

        // Noon
        val noon = fixHour(12.0 + timezoneOffsetHours - longitude / 15.0 - eqt)

        // Sunrise & Sunset Angle: 0.833 degrees for atmospheric refraction
        val sunAngle = 0.833
        val sunriseHour = noon - sunAngleHour(sunAngle, latitude, declination)
        val sunsetHour = noon + sunAngleHour(sunAngle, latitude, declination)

        // Fajr
        val fajrHour = noon - sunAngleHour(method.fajrAngle, latitude, declination)

        // Asr
        val asrAngle = -toDegrees(atan(1.0 / (juristic.shadowFactor + tan(toRadians(abs(latitude - declination))))))
        val asrHour = noon + sunAngleHour(-asrAngle, latitude, declination)

        // Dhuhr (solar noon with safety margin for Algeria)
        val dhuhrHour = if (method == CalculationMethod.ALGERIA) {
            noon + (2.0 / 60.0) // 2 minutes security margin after solar noon
        } else {
            noon
        }

        // Maghrib
        val maghribHour = if (method == CalculationMethod.TEHRAN || method == CalculationMethod.SHIA_ITHNA_ASHARI) {
            noon + sunAngleHour(4.5, latitude, declination)
        } else if (method == CalculationMethod.ALGERIA) {
            sunsetHour + (2.0 / 60.0) // 2 minutes security margin after sunset
        } else {
            sunsetHour
        }

        // Isha
        val ishaHour = if (method.isIshaInterval) {
            maghribHour + (method.ishaIntervalMinutes / 60.0)
        } else {
            noon + sunAngleHour(method.ishaAngle, latitude, declination)
        }

        // Apply manual minute adjustments
        fun toMillis(hours: Double, prayer: PrayerType): Long {
            val offsetMin = manualOffsets[prayer] ?: 0
            val totalMinutes = (hours * 60.0).roundToInt() + offsetMin
            val h = (totalMinutes / 60) % 24
            val m = totalMinutes % 60
            val c = calendar.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, (h + 24) % 24)
            c.set(Calendar.MINUTE, (m + 60) % 60)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        fun formatTime(millis: Long): String {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            val h = c.get(Calendar.HOUR_OF_DAY)
            val m = c.get(Calendar.MINUTE)
            return String.format("%02d:%02d", h, m)
        }

        val fajrMillis = toMillis(fajrHour, PrayerType.FAJR)
        val sunriseMillis = toMillis(sunriseHour, PrayerType.SUNRISE)
        val dhuhrMillis = toMillis(dhuhrHour, PrayerType.DHUHR)
        val asrMillis = toMillis(asrHour, PrayerType.ASR)
        val maghribMillis = toMillis(maghribHour, PrayerType.MAGHRIB)
        val ishaMillis = toMillis(ishaHour, PrayerType.ISHA)

        val rawItems = listOf(
            PrayerTimeItem(PrayerType.FAJR, formatTime(fajrMillis), getHour(fajrMillis), getMin(fajrMillis), fajrMillis),
            PrayerTimeItem(PrayerType.SUNRISE, formatTime(sunriseMillis), getHour(sunriseMillis), getMin(sunriseMillis), sunriseMillis),
            PrayerTimeItem(PrayerType.DHUHR, formatTime(dhuhrMillis), getHour(dhuhrMillis), getMin(dhuhrMillis), dhuhrMillis),
            PrayerTimeItem(PrayerType.ASR, formatTime(asrMillis), getHour(asrMillis), getMin(asrMillis), asrMillis),
            PrayerTimeItem(PrayerType.MAGHRIB, formatTime(maghribMillis), getHour(maghribMillis), getMin(maghribMillis), maghribMillis),
            PrayerTimeItem(PrayerType.ISHA, formatTime(ishaMillis), getHour(ishaMillis), getMin(ishaMillis), ishaMillis)
        )

        val now = System.currentTimeMillis()
        var nextPrayer: PrayerTimeItem? = null
        for (item in rawItems) {
            if (item.timestampMillis > now) {
                nextPrayer = item
                break
            }
        }

        // If all prayers today have passed, next is tomorrow's Fajr
        val remainingMillis: Long
        if (nextPrayer != null) {
            remainingMillis = nextPrayer.timestampMillis - now
        } else {
            // Tomorrow Fajr is ~ 24h + (fajr - now)
            val tomorrowFajr = fajrMillis + 24 * 3600 * 1000
            nextPrayer = rawItems[0].copy(timestampMillis = tomorrowFajr)
            remainingMillis = tomorrowFajr - now
        }

        val itemsWithNextFlag = rawItems.map {
            it.copy(isNext = it.type == nextPrayer.type)
        }

        // Calculate Islamic Midnight (midpoint between Maghrib and tomorrow's Fajr)
        val tomorrowFajrTime = fajrMillis + 24 * 3600 * 1000
        val midnightMillis = maghribMillis + (tomorrowFajrTime - maghribMillis) / 2
        val midnightFormatted = String.format("%02d:%02d", getHour(midnightMillis), getMin(midnightMillis))

        // Calculate Imsak (~10 mins before Fajr)
        val imsakMillis = fajrMillis - 10 * 60 * 1000
        val imsakFormatted = String.format("%02d:%02d", getHour(imsakMillis), getMin(imsakMillis))

        val dateStr = String.format("%04d-%02d-%02d", year, month, day)

        return PrayerSchedule(
            dateString = dateStr,
            prayers = itemsWithNextFlag,
            nextPrayer = nextPrayer,
            remainingMillisToNext = max(0L, remainingMillis),
            midnightTimeFormatted = midnightFormatted,
            imsakTimeFormatted = imsakFormatted
        )
    }

    /**
     * Calculates prayer schedules for an entire month locally and offline in milliseconds
     */
    fun calculateMonthlyPrayerTimes(
        latitude: Double,
        longitude: Double,
        year: Int,
        month: Int,
        method: CalculationMethod = CalculationMethod.ALGERIA,
        juristic: JuristicMethod = JuristicMethod.SHAFI,
        manualOffsets: Map<PrayerType, Int> = emptyMap()
    ): List<PrayerSchedule> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..maxDays).map { day ->
            val dayCal = Calendar.getInstance().apply {
                set(year, month - 1, day, 12, 0, 0)
            }
            calculatePrayerTimes(
                latitude = latitude,
                longitude = longitude,
                calendar = dayCal,
                method = method,
                juristic = juristic,
                manualOffsets = manualOffsets
            )
        }
    }

    private fun getHour(millis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.HOUR_OF_DAY)
    }

    private fun getMin(millis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.MINUTE)
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunAngleHour(angle: Double, latitude: Double, declination: Double): Double {
        val cosH = (sin(toRadians(-angle)) - sin(toRadians(latitude)) * sin(toRadians(declination))) /
                (cos(toRadians(latitude)) * cos(toRadians(declination)))
        if (cosH > 1.0) return 0.0
        if (cosH < -1.0) return 12.0
        return toDegrees(acos(cosH)) / 15.0
    }

    private fun fixAngle(a: Double): Double {
        var angle = a - 360.0 * floor(a / 360.0)
        if (angle < 0) angle += 360.0
        return angle
    }

    private fun fixHour(h: Double): Double {
        var hour = h - 24.0 * floor(h / 24.0)
        if (hour < 0) hour += 24.0
        return hour
    }

    private fun toRadians(deg: Double): Double = deg * Math.PI / 180.0
    private fun toDegrees(rad: Double): Double = rad * 180.0 / Math.PI
}
