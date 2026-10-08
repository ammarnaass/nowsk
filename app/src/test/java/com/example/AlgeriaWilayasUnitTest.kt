package com.example

import com.example.data.prayer.CalculationMethod
import com.example.data.prayer.CityPresets
import com.example.data.prayer.JuristicMethod
import com.example.data.prayer.PrayerTimesCalculator
import com.example.data.prayer.PrayerType
import com.example.data.qibla.QiblaCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AlgeriaWilayasUnitTest {

    @Test
    fun algeriaWilayas_exactly58WilayasArePresentInCorrectOrder() {
        val algeriaCities = CityPresets.CITIES.filter { it.countryAr == "الجزائر" && it.wilayaCode in 1..58 }
        assertEquals("Algeria must have exactly 58 wilayas", 58, algeriaCities.size)

        for (i in 1..58) {
            val wilaya = algeriaCities[i - 1]
            assertEquals("Wilaya code must match index", i, wilaya.wilayaCode)
            assertTrue("Wilaya nameAr must start with code number", wilaya.nameAr.startsWith(String.format("%02d", i)))
            assertTrue("Wilaya nameEn must be provided", wilaya.nameEn.isNotBlank())
            assertEquals("Timezone offset for Algeria must be +1.0", 1.0, wilaya.timeZoneOffsetHours, 0.001)

            // Latitude inside Algeria bounds (18°N to 38°N)
            assertTrue(
                "Latitude of ${wilaya.nameAr} (${wilaya.latitude}) must be between 18.0 and 38.0",
                wilaya.latitude in 18.0..38.0
            )

            // Longitude inside Algeria bounds (-9.0°W to 12.0°E)
            assertTrue(
                "Longitude of ${wilaya.nameAr} (${wilaya.longitude}) must be between -9.0 and 12.0",
                wilaya.longitude in -9.0..12.0
            )
        }
    }

    @Test
    fun prayerTimes_calculatedAccuratelyForAll58Wilayas() {
        val cal = Calendar.getInstance()
        val algeriaCities = CityPresets.CITIES.filter { it.countryAr == "الجزائر" && it.wilayaCode in 1..58 }

        for (wilaya in algeriaCities) {
            val schedule = PrayerTimesCalculator.calculatePrayerTimes(
                latitude = wilaya.latitude,
                longitude = wilaya.longitude,
                calendar = cal,
                method = CalculationMethod.ALGERIA,
                juristic = JuristicMethod.SHAFI
            )

            assertNotNull("Schedule should not be null for ${wilaya.nameAr}", schedule)
            assertEquals("6 prayer items expected for ${wilaya.nameAr}", 6, schedule.prayers.size)

            val fajr = schedule.prayers.first { it.type == PrayerType.FAJR }
            val sunrise = schedule.prayers.first { it.type == PrayerType.SUNRISE }
            val dhuhr = schedule.prayers.first { it.type == PrayerType.DHUHR }
            val asr = schedule.prayers.first { it.type == PrayerType.ASR }
            val maghrib = schedule.prayers.first { it.type == PrayerType.MAGHRIB }
            val isha = schedule.prayers.first { it.type == PrayerType.ISHA }

            // Verify chronological order of prayer times
            assertTrue("Fajr before Sunrise for ${wilaya.nameAr}", fajr.timestampMillis < sunrise.timestampMillis)
            assertTrue("Sunrise before Dhuhr for ${wilaya.nameAr}", sunrise.timestampMillis < dhuhr.timestampMillis)
            assertTrue("Dhuhr before Asr for ${wilaya.nameAr}", dhuhr.timestampMillis < asr.timestampMillis)
            assertTrue("Asr before Maghrib for ${wilaya.nameAr}", asr.timestampMillis < maghrib.timestampMillis)
            assertTrue("Maghrib before Isha for ${wilaya.nameAr}", maghrib.timestampMillis < isha.timestampMillis)

            // Verify Qibla direction for each wilaya is reasonable (toward East-South-East from Algeria ~80° - 120°)
            val qiblaAngle = QiblaCalculator.calculateQiblaAngle(wilaya.latitude, wilaya.longitude)
            assertTrue("Qibla angle for ${wilaya.nameAr} ($qiblaAngle) should point towards Kaaba (roughly 75-125 deg)", qiblaAngle in 75.0..125.0)
        }
    }

    @Test
    fun findNearestCity_detectsCorrectWilaya() {
        // Algiers coordinates
        val nearestAlgiers = CityPresets.findNearestCity(36.7538, 3.0588)
        assertNotNull(nearestAlgiers)
        assertEquals(16, nearestAlgiers?.wilayaCode)

        // Oran coordinates
        val nearestOran = CityPresets.findNearestCity(35.6976, -0.6337)
        assertNotNull(nearestOran)
        assertEquals(31, nearestOran?.wilayaCode)

        // Constantine coordinates
        val nearestConstantine = CityPresets.findNearestCity(36.3650, 6.6147)
        assertNotNull(nearestConstantine)
        assertEquals(25, nearestConstantine?.wilayaCode)

        // M'Sila coordinates
        val nearestMsila = CityPresets.findNearestCity(35.7058, 4.5419)
        assertNotNull(nearestMsila)
        assertEquals(28, nearestMsila?.wilayaCode)

        // In Guezzam (far south)
        val nearestInGuezzam = CityPresets.findNearestCity(19.5714, 5.7694)
        assertNotNull(nearestInGuezzam)
        assertEquals(54, nearestInGuezzam?.wilayaCode)
    }
}
