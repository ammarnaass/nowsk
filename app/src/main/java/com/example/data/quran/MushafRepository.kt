package com.example.data.quran

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Repository responsible for managing the 604 pages of the Holy Quran
 * in full Uthmani script and authentic 15-line layout.
 */
object MushafRepository {
    private const val TAG = "MushafRepository"
    private const val ASSET_PATH = "quran/mushaf_structured_pages.json"

    private var cachedPages: List<MushafPage> = emptyList()
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /**
     * Preloads and parses the 604 Quran pages on a background IO thread.
     */
    suspend fun initialize(context: Context) {
        if (_isLoaded.value && cachedPages.isNotEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val inputStream = context.assets.open(ASSET_PATH)
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val jsonString = reader.use { it.readText() }

                val jsonArray = JSONArray(jsonString)
                val list = ArrayList<MushafPage>(jsonArray.length())

                for (i in 0 until jsonArray.length()) {
                    val pObj = jsonArray.getJSONObject(i)
                    val pNum = pObj.getInt("pageNumber")
                    val jNum = pObj.getInt("juzNumber")
                    val hNum = pObj.getInt("hizbNumber")
                    val sName = pObj.getString("surahName")
                    val sNum = pObj.getInt("surahNumber")

                    val linesArray = pObj.getJSONArray("lines")
                    val lines = ArrayList<MushafPageLine>(linesArray.length())

                    for (j in 0 until linesArray.length()) {
                        val lObj = linesArray.getJSONObject(j)
                        val type = lObj.getString("type")
                        val text = if (lObj.has("text")) lObj.getString("text") else ""
                        val surahName = if (lObj.has("surahName")) lObj.getString("surahName") else ""
                        val surahNumber = if (lObj.has("surahNumber")) lObj.getInt("surahNumber") else 0
                        val versesCount = if (lObj.has("versesCount")) lObj.getInt("versesCount") else 0
                        val typeAr = if (lObj.has("typeAr")) lObj.getString("typeAr") else ""

                        lines.add(
                            MushafPageLine(
                                type = type,
                                text = text,
                                surahName = surahName,
                                surahNumber = surahNumber,
                                versesCount = versesCount,
                                typeAr = typeAr
                            )
                        )
                    }

                    list.add(
                        MushafPage(
                            pageNumber = pNum,
                            juzNumber = jNum,
                            hizbNumber = hNum,
                            surahName = sName,
                            surahNumber = sNum,
                            lines = lines
                        )
                    )
                }

                cachedPages = list
                _isLoaded.value = true
                Log.d(TAG, "Successfully loaded ${list.size} Mushaf pages in ${System.currentTimeMillis() - startTime}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading mushaf structured pages", e)
            }
        }
    }

    /**
     * Returns the requested page (1 to 604) or null if out of range or not loaded.
     */
    fun getPage(pageNumber: Int): MushafPage? {
        if (pageNumber in 1..cachedPages.size) {
            return cachedPages[pageNumber - 1]
        }
        return null
    }

    /**
     * Returns all pages.
     */
    fun getAllPages(): List<MushafPage> = cachedPages

    /**
     * Returns the starting page for a given surah (1 to 114).
     */
    fun getPageForSurah(surahNumber: Int): Int {
        val surah = QuranRepository.SURAHS.find { it.number == surahNumber }
        return surah?.page ?: 1
    }

    /**
     * Returns the starting page for a given juz (1 to 30).
     */
    fun getPageForJuz(juzNumber: Int): Int {
        val juzStarts = listOf(
            1, 22, 42, 62, 82, 102, 122, 142, 162, 182,
            202, 222, 242, 262, 282, 302, 322, 342, 362, 382,
            402, 422, 442, 462, 482, 502, 522, 542, 562, 582
        )
        if (juzNumber in 1..30) {
            return juzStarts[juzNumber - 1]
        }
        return 1
    }

    /**
     * Normalizes Arabic text for flexible, diacritics-insensitive Quran search.
     */
    fun normalizeArabic(text: String): String {
        // 1. Remove Tashkeel / Harakat, Tatweel, and special Quranic stop marks
        var result = text.replace(Regex("[\u064B-\u065F\u0670\u06D6-\u06ED\u0640]"), "")
        // 2. Normalize Alef variants (إ, أ, آ, ٱ -> ا)
        result = result.replace(Regex("[إأآٱ]"), "ا")
        // 3. Normalize Ta Marbuta and Alif Maqsura (ة -> ه, ى -> ي)
        result = result.replace('ة', 'ه').replace('ى', 'ي')
        // 4. Normalize Uthmani spelling of core Quranic words where Waw represents Alif
        result = result.replace(Regex("صلو([ته])"), "صلا$1")
        result = result.replace(Regex("زكو([ته])"), "زكا$1")
        result = result.replace(Regex("حيو([ته])"), "حيا$1")
        result = result.replace(Regex("ربو([ته])"), "ربا$1")
        return result
    }

    /**
     * Searches across all 604 pages and 9060+ lines of the Holy Quran.
     */
    fun searchPages(query: String, limit: Int = 60): List<MushafSearchResult> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val normalizedQuery = normalizeArabic(q)
        if (normalizedQuery.isBlank()) return emptyList()

        val results = mutableListOf<MushafSearchResult>()
        for (page in cachedPages) {
            for (line in page.lines) {
                if (line.type == "text" && line.text.isNotBlank()) {
                    val normalizedLine = normalizeArabic(line.text)
                    if (normalizedLine.contains(normalizedQuery)) {
                        val ayahMatch = Regex("﴿([٠-٩0-9]+)﴾").find(line.text)
                        val aNum = ayahMatch?.groupValues?.get(1)?.let { parseArabicDigits(it) } ?: 1
                        results.add(
                            MushafSearchResult(
                                pageNumber = page.pageNumber,
                                surahNumber = page.surahNumber,
                                surahName = page.surahName,
                                text = line.text,
                                ayahNumber = aNum
                            )
                        )
                        if (results.size >= limit) return results
                    }
                }
            }
        }
        return results
    }

    private fun parseArabicDigits(str: String): Int {
        val western = str.map { ch ->
            when (ch) {
                '٠' -> '0'
                '١' -> '1'
                '٢' -> '2'
                '٣' -> '3'
                '٤' -> '4'
                '٥' -> '5'
                '٦' -> '6'
                '٧' -> '7'
                '٨' -> '8'
                '٩' -> '9'
                else -> ch
            }
        }.joinToString("")
        return western.toIntOrNull() ?: 1
    }
}
