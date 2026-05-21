package com.mawaai.love.app.data.remote.zenquotes

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain-layer wrapper around [ZenQuotesApi].
 *
 * - Returns null on any failure (network, rate-limit, empty body) so
 *   ViewModels can fall back to a static seed string without surfacing
 *   an error toast.
 * - Single-quote `today()` is cheap enough that we don't add a memory
 *   cache here — the Room layer + ViewModel `SharingStarted.WhileSubscribed`
 *   handles UI-side memoisation.
 * - All work hops to `Dispatchers.IO` because OkHttp's blocking calls
 *   would otherwise pin the calling coroutine to the IO dispatcher of
 *   whatever scope launched them.
 */
@Singleton
class ZenQuotesClient @Inject constructor(
    private val api: ZenQuotesApi
) {

    /**
     * Quote of the day. Same value for the whole 24h cycle so the
     * morning notification + the home-card show identical messaging.
     */
    suspend fun dailyQuote(): Quote? = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.today()
            response.body()?.firstOrNull()?.toDomain()
        }.onFailure { Log.w(TAG, "ZenQuotes today() failed", it) }.getOrNull()
    }

    /**
     * Random fresh quote. Used when the user taps "next" on the daily
     * card — the API has its own diversity logic so consecutive calls
     * almost always return different quotes.
     */
    suspend fun randomQuote(): Quote? = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.random()
            response.body()?.firstOrNull()?.toDomain()
        }.onFailure { Log.w(TAG, "ZenQuotes random() failed", it) }.getOrNull()
    }

    private fun ZenQuoteDto.toDomain(): Quote? {
        if (text.isBlank()) return null
        return Quote(text = text.trim(), author = author?.takeIf { it.isNotBlank() && it != "unknown" })
    }

    /** Minimal domain model — strips the wire-format quirks. */
    data class Quote(val text: String, val author: String?)

    private companion object {
        const val TAG = "ZenQuotesClient"
    }
}
