package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran_progress")
data class QuranProgressEntity(
    @PrimaryKey
    val id: Int = 1,
    val surahNumber: Int = 1,
    val surahName: String = "الفاتحة",
    val ayahNumber: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)
