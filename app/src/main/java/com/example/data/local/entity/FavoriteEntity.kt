package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favorites Entity storing bookmarked Quran verses, Adhkar, and Duas.
 * Maps to the 'favorites' table in Room database.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "quran", "dhikr", "dua", "hadith"
    val referenceId: String = "",
    val title: String,
    val subtitle: String = "",
    val content: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
