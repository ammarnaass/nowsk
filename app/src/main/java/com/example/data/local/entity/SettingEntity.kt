package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Settings Entity storing user preferences and app configuration.
 * Maps to the 'settings' table in Room database.
 */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
