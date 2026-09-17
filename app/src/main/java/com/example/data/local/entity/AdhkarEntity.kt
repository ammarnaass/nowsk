package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Adhkar Entity storing daily invocations, dhikr progress, and repetition targets.
 * Maps to the 'adhkar' table in Room database.
 */
@Entity(tableName = "adhkar")
data class AdhkarEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val categoryNameAr: String,
    val title: String,
    val text: String,
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val virtue: String = "",
    val source: String = "",
    val audioUrl: String? = null,
    val isFavorite: Boolean = false,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: String = "",
    val tags: List<String> = emptyList()
)
