package com.mawaai.love.app.data.remote.zenquotes

import com.google.gson.annotations.SerializedName

/**
 * Wire-format representation of a single ZenQuotes entry.
 *
 * The API uses single-letter field names (`q`, `a`, `h`) for bandwidth.
 * We map them to readable property names via [SerializedName] so the
 * domain layer never sees the cryptic shorthand.
 *
 * The `h` field carries pre-rendered HTML (`<blockquote>` etc.) which we
 * intentionally drop — Mawaai renders quotes in Compose with its own
 * romantic typography, so the server-side HTML is unused.
 */
data class ZenQuoteDto(
    @SerializedName("q") val text: String,
    @SerializedName("a") val author: String?
)
