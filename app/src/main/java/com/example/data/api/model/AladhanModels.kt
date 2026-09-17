package com.example.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Generic response wrapper for Aladhan API responses.
 */
@JsonClass(generateAdapter = true)
data class AladhanBaseResponse<T>(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: T
)

/**
 * Data container for daily prayer timings.
 */
@JsonClass(generateAdapter = true)
data class AladhanTimingsResponse(
    @Json(name = "timings") val timings: AladhanTimings,
    @Json(name = "date") val date: AladhanDateContainer,
    @Json(name = "meta") val meta: AladhanMeta
)

/**
 * Prayer timings in format "HH:mm" (e.g. "05:03").
 */
@JsonClass(generateAdapter = true)
data class AladhanTimings(
    @Json(name = "Fajr") val fajr: String,
    @Json(name = "Sunrise") val sunrise: String,
    @Json(name = "Dhuhr") val dhuhr: String,
    @Json(name = "Asr") val asr: String,
    @Json(name = "Sunset") val sunset: String? = null,
    @Json(name = "Maghrib") val maghrib: String,
    @Json(name = "Isha") val isha: String,
    @Json(name = "Imsak") val imsak: String? = null,
    @Json(name = "Midnight") val midnight: String? = null,
    @Json(name = "Firstthird") val firstThird: String? = null,
    @Json(name = "Lastthird") val lastThird: String? = null
) {
    fun cleanTime(raw: String): String = raw.substringBefore(" ").trim()
}

@JsonClass(generateAdapter = true)
data class AladhanDateContainer(
    @Json(name = "readable") val readable: String? = null,
    @Json(name = "timestamp") val timestamp: String? = null,
    @Json(name = "hijri") val hijri: AladhanHijriDate? = null,
    @Json(name = "gregorian") val gregorian: AladhanGregorianDate? = null
)

@JsonClass(generateAdapter = true)
data class AladhanHijriDate(
    @Json(name = "date") val date: String,
    @Json(name = "day") val day: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "month") val month: AladhanMonth? = null,
    @Json(name = "weekday") val weekday: AladhanWeekday? = null
)

@JsonClass(generateAdapter = true)
data class AladhanGregorianDate(
    @Json(name = "date") val date: String,
    @Json(name = "day") val day: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "month") val month: AladhanMonth? = null,
    @Json(name = "weekday") val weekday: AladhanWeekday? = null
)

@JsonClass(generateAdapter = true)
data class AladhanMonth(
    @Json(name = "number") val number: Int,
    @Json(name = "en") val en: String? = null,
    @Json(name = "ar") val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanWeekday(
    @Json(name = "en") val en: String? = null,
    @Json(name = "ar") val ar: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanMeta(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "method") val method: AladhanMethodDetail? = null,
    @Json(name = "school") val school: String? = null
)

@JsonClass(generateAdapter = true)
data class AladhanMethodDetail(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null
)

/**
 * Raw method model returned in the Map of /v1/methods
 */
@JsonClass(generateAdapter = true)
data class AladhanMethodRaw(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String
)

/**
 * Normalized method model for UI and selection
 */
data class AladhanCalculationMethod(
    val id: Int,
    val codeName: String,
    val nameEn: String,
    val nameAr: String
)

/**
 * Qibla direction data returned from /v1/qibla
 */
@JsonClass(generateAdapter = true)
data class AladhanQiblaData(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "direction") val direction: Double
)
