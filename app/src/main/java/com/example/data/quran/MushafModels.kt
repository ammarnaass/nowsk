package com.example.data.quran

/**
 * Data model representing a line in the physical 15-line Madani Mushaf page.
 */
data class MushafPageLine(
    val type: String, // "text", "surah_header", "bismillah", "ornament", "empty"
    val text: String = "",
    val surahName: String = "",
    val surahNumber: Int = 0,
    val versesCount: Int = 0,
    val typeAr: String = ""
)

/**
 * Data model representing a single authentic page of the Holy Quran (1 to 604).
 */
data class MushafPage(
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbNumber: Int,
    val surahName: String,
    val surahNumber: Int,
    val lines: List<MushafPageLine>
)

/**
 * Result model representing a search hit within the Quranic Mushaf pages.
 */
data class MushafSearchResult(
    val pageNumber: Int,
    val surahNumber: Int,
    val surahName: String,
    val text: String,
    val ayahNumber: Int = 1
)

