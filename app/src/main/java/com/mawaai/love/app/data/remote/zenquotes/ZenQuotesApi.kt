package com.mawaai.love.app.data.remote.zenquotes

import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit interface for the ZenQuotes public API.
 *
 * No API key required. Rate-limited to ~5 requests / 30 seconds per IP.
 *
 * Both endpoints return a JSON array of quote objects with fields:
 *  - `q` — the quote text
 *  - `a` — the author (string `"unknown"` when missing)
 *  - `h` — pre-rendered HTML (ignored by this client)
 *
 * Attribution requirement: any UI surface that shows a ZenQuotes quote
 * must include the text "Inspirational quotes provided by ZenQuotes API"
 * with a link to https://zenquotes.io/. The string is in
 * `R.string.zenquotes_attribution`.
 */
interface ZenQuotesApi {

    /**
     * Returns the same single quote for the entire calendar day. Useful
     * for the daily-romantic-quote feature on the home screen and for
     * the morning notification — same quote per day means consistent
     * messaging across surfaces.
     */
    @GET("api/today")
    suspend fun today(): Response<List<ZenQuoteDto>>

    /**
     * Returns a single random quote. Used by the "next quote" affordance
     * on the daily-quote card so the user can browse beyond the
     * day-locked one.
     */
    @GET("api/random")
    suspend fun random(): Response<List<ZenQuoteDto>>
}
