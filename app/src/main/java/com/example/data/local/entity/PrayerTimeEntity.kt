package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PrayerTimes Entity storing calculated daily prayer schedules offline.
 * Maps to the 'prayer_times' table in Room database.
 */
@Entity(tableName = "prayer_times")
data class PrayerTimeEntity(
    @PrimaryKey
    val date: String, // format YYYY-MM-DD e.g. "2026-03-15"
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val imsak: String = "",
    val midnight: String = "",
    val calculationMethod: String,
    val juristicMethod: String,
    val dateHijri: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
