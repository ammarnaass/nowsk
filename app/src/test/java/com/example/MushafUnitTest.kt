package com.example

import com.example.data.quran.QuranRepository
import com.example.data.quran.MushafRepository
import org.junit.Assert.*
import org.junit.Test

class MushafUnitTest {

    @Test
    fun quranSurahs_hasExactly114Surahs() {
        assertEquals(114, QuranRepository.SURAHS.size)
        assertEquals("الفاتحة", QuranRepository.SURAHS.first().nameAr)
        assertEquals("الناس", QuranRepository.SURAHS.last().nameAr)
    }

    @Test
    fun surahPageMapping_isAccurateToMadaniMushaf() {
        // Al-Fatiha starts on page 1
        assertEquals(1, QuranRepository.SURAHS[0].page)
        // Al-Baqarah starts on page 2
        assertEquals(2, QuranRepository.SURAHS[1].page)
        // Al-Kahf starts on page 293
        assertEquals(293, QuranRepository.SURAHS[17].page)
        // Ya-Sin starts on page 440
        assertEquals(440, QuranRepository.SURAHS[35].page)
        // Al-Mulk starts on page 562
        assertEquals(562, QuranRepository.SURAHS[66].page)
        // An-Nas starts on page 604
        assertEquals(604, QuranRepository.SURAHS[113].page)
    }

    @Test
    fun juzPageMapping_hasValidStartingPages() {
        assertEquals(1, MushafRepository.getPageForJuz(1))
        assertEquals(22, MushafRepository.getPageForJuz(2))
        assertEquals(582, MushafRepository.getPageForJuz(30))
    }

    @Test
    fun normalizeArabic_normalizesUthmaniScriptAccurately() {
        val uthmaniSalah = "وَيُقِيمُونَ ٱلصَّلَوٰةَ"
        val normalized = MushafRepository.normalizeArabic(uthmaniSalah)
        assertTrue(normalized.contains("الصلاه"))

        val uthmaniRahman = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
        val normalizedRahman = MushafRepository.normalizeArabic(uthmaniRahman)
        assertTrue(normalizedRahman.contains("الرحمن"))
    }

    @Test
    fun tafseerRepository_providesAuthenticContent() {
        val fatihaIntro = com.example.data.quran.TafseerRepository.getSurahIntro(1)
        assertEquals("الفاتحة", fatihaIntro.nameAr)
        assertEquals(7, fatihaIntro.versesCount)
        assertTrue(fatihaIntro.theme.isNotEmpty())

        val ayatKursiTafseer = com.example.data.quran.TafseerRepository.getAyahTafseer(2, 255, "", "البقرة")
        assertTrue(ayatKursiTafseer.contains("آية الكرسي"))
    }
}
