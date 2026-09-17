package com.example.data.calendar

import java.util.Calendar
import java.util.Locale
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int,
    val monthNameAr: String,
    val year: Int,
    val dayNameAr: String
)

data class IslamicOccasion(
    val nameAr: String,
    val hijriDay: Int,
    val hijriMonth: Int,
    val descriptionAr: String
)

object HijriCalendarHelper {

    val HIJRI_MONTHS_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    val ISLAMIC_OCCASIONS = listOf(
        IslamicOccasion("رأس السنة الهجرية", 1, 1, "بداية العام الهجري الجديد وهجرة النبي ﷺ"),
        IslamicOccasion("يوم عاشوراء", 10, 1, "يوم نجّى الله فيه موسى وقومه"),
        IslamicOccasion("المولد النبوي الشريف", 12, 3, "ذكرى ميلاد خاتم الأنبياء محمد ﷺ"),
        IslamicOccasion("ليلة الإسراء والمعراج", 27, 7, "معجزة إسراء ومعراج النبي ﷺ"),
        IslamicOccasion("ليلة النصف من شعبان", 15, 8, "ليلة مباركة يستحب قيامها وصيام نهارها"),
        IslamicOccasion("بداية شهر رمضان المبارك", 1, 9, "شهر الصيام والقرآن والرحمة والمغفرة"),
        IslamicOccasion("ليلة القدر (المتحرّاة)", 27, 9, "خير من ألف شهر، تنزل الملائكة والروح فيها"),
        IslamicOccasion("عيد الفطر المبارك", 1, 10, "يوم الفرح والسرور بتمام صيام شهر رمضان"),
        IslamicOccasion("يوم عرفة", 9, 12, "أفضل أيام العام وركن الحج الأعظم"),
        IslamicOccasion("عيد الأضحى المبارك", 10, 12, "يوم النحر وأعظم الأيام عند الله")
    )

    private val ARABIC_DAYS = listOf(
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    fun getTodayHijriDate(calendar: Calendar = Calendar.getInstance()): HijriDate {
        return gregorianToHijri(calendar)
    }

    /**
     * Accurate arithmetic Umm Al-Qura / standard Kuweit algorithmic conversion
     */
    fun gregorianToHijri(cal: Calendar): HijriDate {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0: Sun, 6: Sat

        val dayName = ARABIC_DAYS.getOrElse(dayOfWeek) { "اليوم" }

        var jd = if ((y > 1582) || (y == 1582 && m > 10) || (y == 1582 && m == 10 && d > 14)) {
            floor((1461.0 * (y + 4800.0 + floor((m - 14.0) / 12.0))) / 4.0) +
                    floor((367.0 * (m - 2.0 - 12.0 * floor((m - 14.0) / 12.0))) / 12.0) -
                    floor((3.0 * floor((y + 4900.0 + floor((m - 14.0) / 12.0)) / 100.0)) / 4.0) +
                    d - 32075.0
        } else {
            367.0 * y - floor((7.0 * (y + 5001.0 + floor((m - 9.0) / 7.0))) / 4.0) +
                    floor((275.0 * m) / 9.0) + d + 1729777.0
        }

        val l = jd - 1948440.0 + 10632.0
        val n = floor((l - 1.0) / 10631.0)
        val l2 = l - 10631.0 * n + 354.0
        val j = (floor((10985.0 - l2) / 5316.0)) * (floor((50.0 * l2) / 17719.0)) +
                (floor(l2 / 5670.0)) * (floor((43.0 * l2) / 15238.0))
        val l3 = l2 - (floor((30.0 - j) / 15.0)) * (floor((17719.0 * j) / 50.0)) -
                (floor(j / 16.0)) * (floor((15238.0 * j) / 43.0)) + 29.0
        val month = floor((24.0 * l3) / 709.0).toInt()
        val day = (l3 - floor((709.0 * month) / 24.0)).toInt()
        val year = (30.0 * n + j - 30.0).toInt()

        val safeMonth = month.coerceIn(1, 12)
        val monthName = HIJRI_MONTHS_AR[safeMonth - 1]

        return HijriDate(
            day = day.coerceIn(1, 30),
            month = safeMonth,
            monthNameAr = monthName,
            year = year,
            dayNameAr = dayName
        )
    }

    fun formatHijriFull(hijri: HijriDate): String {
        return "${hijri.dayNameAr} ${hijri.day} ${hijri.monthNameAr} ${hijri.year} هـ"
    }

    fun formatGregorianArabic(cal: Calendar): String {
        val months = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = months[cal.get(Calendar.MONTH)]
        val y = cal.get(Calendar.YEAR)
        return "$d $m $y م"
    }
}
