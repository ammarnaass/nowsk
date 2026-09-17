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
    val timeZoneOffsetHours: Double
)

object CityPresets {
    val CITIES = listOf(
        CityLocation("المسيلة", "الجزائر", 35.7058, 4.5419, 1.0),
        CityLocation("الجزائر العاصمة", "الجزائر", 36.7538, 3.0588, 1.0),
        CityLocation("وهران", "الجزائر", 35.6976, -0.6337, 1.0),
        CityLocation("قسنطينة", "الجزائر", 36.3650, 6.6147, 1.0),
        CityLocation("سطيف", "الجزائر", 36.1898, 5.4108, 1.0),
        CityLocation("مكة المكرمة", "السعودية", 21.4225, 39.8262, 3.0),
        CityLocation("المدينة المنورة", "السعودية", 24.4672, 39.6111, 3.0),
        CityLocation("الرياض", "السعودية", 24.7136, 46.6753, 3.0),
        CityLocation("القاهرة", "مصر", 30.0444, 31.2357, 2.0),
        CityLocation("القدس الشريف", "فلسطين", 31.7683, 35.2137, 2.0),
        CityLocation("عمان", "الأردن", 31.9454, 35.9284, 3.0),
        CityLocation("الدار البيضاء", "المغرب", 33.5731, -7.5898, 1.0),
        CityLocation("تونس", "تونس", 36.8065, 10.1815, 1.0),
        CityLocation("طرابلس", "ليبيا", 32.8872, 13.1913, 2.0),
        CityLocation("الخرطوم", "السودان", 15.5007, 32.5599, 2.0),
        CityLocation("دبي", "الإمارات", 25.2048, 55.2708, 4.0),
        CityLocation("بغداد", "العراق", 33.3152, 44.3661, 3.0),
        CityLocation("دمشق", "سوريا", 33.5138, 36.2765, 3.0),
        CityLocation("إسطنبول", "تركيا", 41.0082, 28.9784, 3.0)
    )

    val DEFAULT = CITIES[0] // المسيلة
}

object PrayerTimesCalculator {

    fun calculatePrayerTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance(),
        method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
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

        // Maghrib
        val maghribHour = if (method == CalculationMethod.TEHRAN || method == CalculationMethod.SHIA_ITHNA_ASHARI) {
            noon + sunAngleHour(4.5, latitude, declination)
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
        val dhuhrMillis = toMillis(noon, PrayerType.DHUHR)
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
