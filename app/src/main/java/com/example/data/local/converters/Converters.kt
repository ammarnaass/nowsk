package com.example.data.local.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room Database using KSP annotation processing.
 * Handles conversion for timestamps, collections, and custom types.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(separator = "|--|") ?: ""
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split("|--|").filter { it.isNotEmpty() }
    }
}
