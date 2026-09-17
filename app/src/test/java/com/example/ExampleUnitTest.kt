package com.example

import com.example.data.calendar.HijriCalendarHelper
import com.example.data.prayer.CalculationMethod
import com.example.data.prayer.CityPresets
import com.example.data.prayer.JuristicMethod
import com.example.data.prayer.PrayerTimesCalculator
import com.example.data.prayer.PrayerType
import com.example.data.qibla.QiblaCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun prayerTimesCalculation_calculatesAllSixPrayers() {
        val cal = Calendar.getInstance()
        val schedule = PrayerTimesCalculator.calculatePrayerTimes(
            latitude = CityPresets.DEFAULT.latitude,
            longitude = CityPresets.DEFAULT.longitude,
            calendar = cal,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            juristic = JuristicMethod.SHAFI
        )

        assertNotNull(schedule)
        assertEquals(6, schedule.prayers.size)
        assertTrue(schedule.prayers.any { it.type == PrayerType.FAJR })
        assertTrue(schedule.prayers.any { it.type == PrayerType.SUNRISE })
        assertTrue(schedule.prayers.any { it.type == PrayerType.DHUHR })
        assertTrue(schedule.prayers.any { it.type == PrayerType.ASR })
        assertTrue(schedule.prayers.any { it.type == PrayerType.MAGHRIB })
        assertTrue(schedule.prayers.any { it.type == PrayerType.ISHA })
        assertNotNull(schedule.midnightTimeFormatted)
        assertNotNull(schedule.imsakTimeFormatted)
    }

    @Test
    fun hijriCalendar_returnsValidDate() {
        val cal = Calendar.getInstance()
        val hijriDate = HijriCalendarHelper.getTodayHijriDate(cal)

        assertTrue(hijriDate.year >= 1445)
        assertTrue(hijriDate.month in 1..12)
        assertTrue(hijriDate.day in 1..30)
        assertTrue(hijriDate.monthNameAr.isNotEmpty())
        assertTrue(hijriDate.dayNameAr.isNotEmpty())
    }

    @Test
    fun qiblaCalculation_meccaDistance() {
        // Distance from Kaaba coordinates to Kaaba should be near 0 km
        val distance = QiblaCalculator.calculateDistanceToKaabaKm(21.4225, 39.8262)
        assertTrue(distance < 5.0) // Within 5km of Kaaba
    }
}
