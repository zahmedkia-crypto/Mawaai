package com.mawaai.love.app.data.remote.aladhan

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Aladhan public API.
 *
 * No API key required. Generously rate-limited (no published cap, but
 * we keep client-side caching to be a good citizen).
 *
 * Used by Mawaai's countdown feature to attach Hijri equivalents to
 * Gregorian event dates ("3 weeks until the wedding — that's 25 Shawwal
 * in the Islamic calendar"). The romantic side can also surface
 * upcoming Islamic holidays for special-occasion countdowns.
 *
 * Date format for path params: `DD-MM-YYYY`. The API enforces it
 * strictly — pad single-digit days/months with zero before sending.
 */
interface AladhanApi {

    /**
     * Converts a Gregorian date string to its Hijri equivalent.
     * Returns the full envelope (gregorian + hijri) so callers can pick
     * either side without a second round-trip.
     */
    @GET("v1/gToH/{date}")
    suspend fun gregorianToHijri(
        @Path("date") gregorianDate: String
    ): Response<AladhanEnvelope<AladhanDateResponse>>

    /**
     * Converts a Hijri date string to its Gregorian equivalent. Used
     * by countdowns that target an Islamic date (e.g. "the next Eid
     * al-Fitr falls on …").
     */
    @GET("v1/hToG/{date}")
    suspend fun hijriToGregorian(
        @Path("date") hijriDate: String
    ): Response<AladhanEnvelope<AladhanDateResponse>>

    /**
     * Returns the full Hijri calendar for a given Hijri year, with one
     * entry per day. The romantic side uses this to surface upcoming
     * Islamic holidays as ready-to-pick countdown targets (Ramadan,
     * Eid, Mawlid, etc.).
     */
    @GET("v1/hijriCalendar")
    suspend fun hijriCalendar(
        @Query("year") hijriYear: Int
    ): Response<AladhanEnvelope<List<AladhanDateResponse>>>
}
