package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tasbeeh Entity storing digital counter items, goals, and lifetime statistics.
 * Maps to the 'tasbeeh' table in Room database.
 */
@Entity(tableName = "tasbeeh")
data class TasbeehEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val count: Int = 0,
    val target: Int = 33,
    val totalCount: Int = 0,
    val isCustom: Boolean = false,
    val dailyGoal: Int = 100,
    val lastUpdated: Long = System.currentTimeMillis()
)
