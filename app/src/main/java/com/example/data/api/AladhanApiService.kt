package com.example.data.api

import com.example.data.api.model.AladhanBaseResponse
import com.example.data.api.model.AladhanDateContainer
import com.example.data.api.model.AladhanMethodRaw
import com.example.data.api.model.AladhanQiblaData
import com.example.data.api.model.AladhanTimingsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AladhanApiService {

    /**
     * Get prayer timings for a specific date and coordinates.
     * Date format: DD-MM-YYYY or unix timestamp.
     */
    @GET("v1/timings/{date}")
    suspend fun getTimings(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int? = null,
        @Query("school") school: Int? = null,
        @Query("tune") tune: String? = null
    ): AladhanBaseResponse<AladhanTimingsResponse>

    /**
     * Get prayer timings for a specific date, city, and country.
     * Date format: DD-MM-YYYY.
     */
    @GET("v1/timingsByCity/{date}")
    suspend fun getTimingsByCity(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int? = null,
        @Query("school") school: Int? = null,
        @Query("tune") tune: String? = null
    ): AladhanBaseResponse<AladhanTimingsResponse>

    /**
     * Get prayer timings by address string (e.g. "M'Sila, Algeria").
     */
    @GET("v1/timingsByAddress/{date}")
    suspend fun getTimingsByAddress(
        @Path("date") date: String,
        @Query("address") address: String,
        @Query("method") method: Int? = null,
        @Query("school") school: Int? = null,
        @Query("tune") tune: String? = null
    ): AladhanBaseResponse<AladhanTimingsResponse>

    /**
     * Get all supported calculation methods from Aladhan API.
     */
    @GET("v1/methods")
    suspend fun getMethods(): AladhanBaseResponse<Map<String, AladhanMethodRaw>>

    /**
     * Get monthly prayer calendar for coordinates.
     */
    @GET("v1/calendar/{year}/{month}")
    suspend fun getCalendar(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int? = null,
        @Query("school") school: Int? = null,
        @Query("tune") tune: String? = null
    ): AladhanBaseResponse<List<AladhanTimingsResponse>>

    /**
     * Get monthly prayer calendar for a city.
     */
    @GET("v1/calendarByCity/{year}/{month}")
    suspend fun getCalendarByCity(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int? = null,
        @Query("school") school: Int? = null,
        @Query("tune") tune: String? = null
    ): AladhanBaseResponse<List<AladhanTimingsResponse>>

    /**
     * Get Qibla angle from coordinates.
     */
    @GET("v1/qibla/{latitude}/{longitude}")
    suspend fun getQibla(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double
    ): AladhanBaseResponse<AladhanQiblaData>

    /**
     * Gregorian date to Hijri conversion.
     * Date format: DD-MM-YYYY.
     */
    @GET("v1/gToH/{date}")
    suspend fun getGregorianToHijri(
        @Path("date") date: String
    ): AladhanBaseResponse<AladhanDateContainer>
}

