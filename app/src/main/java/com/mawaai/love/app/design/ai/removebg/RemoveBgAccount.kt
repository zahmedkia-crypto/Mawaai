package com.mawaai.love.app.design.ai.removebg

import com.google.gson.annotations.SerializedName

/**
 * DTO for `GET /v1.0/account`. Maps the nested JSON-API-style envelope
 * returned by remove.bg into a flat structure that's convenient for the
 * pre-flight quota check in [RemoveBgClient].
 *
 * Example response body:
 * ```
 * {
 *   "data": {
 *     "attributes": {
 *       "credits": { "total": 116, "subscription": 0, "payg": 116, "enterprise": 0 },
 *       "api":     { "free_calls": 50, "sizes": "all" }
 *     }
 *   }
 * }
 * ```
 */
data class RemoveBgAccountResponse(
    val data: Data? = null
) {
    data class Data(val attributes: Attributes? = null)

    data class Attributes(
        val credits: Credits? = null,
        val api: ApiQuota? = null
    )

    data class Credits(
        val total: Int? = null,
        val subscription: Int? = null,
        val payg: Int? = null,
        val enterprise: Int? = null
    )

    data class ApiQuota(
        @SerializedName("free_calls") val freeCalls: Int? = null,
        val sizes: String? = null
    )
}

/**
 * Snapshot of the remaining remove.bg quota, evaluated against the size the
 * caller is about to request. Returned by
 * [RemoveBgClient.precheckQuota] so the caller can:
 *  - Skip the upload when [hasAvailableQuota] is false.
 *  - Show a low-quota warning to the user when [remainingPreviewCalls] or
 *    [remainingCredits] is below a UI-defined threshold.
 *
 * On any failure to reach the account endpoint, [RemoveBgClient.precheckQuota]
 * returns a snapshot with [hasAvailableQuota] = true and
 * [source] = [Source.Optimistic] so the caller proceeds anyway — pre-flight
 * must never block work that might still succeed.
 */
data class RemoveBgQuotaSnapshot(
    /** Number of preview-resolution calls left this calendar month. */
    val remainingPreviewCalls: Int,
    /** PAYG + subscription + enterprise credits remaining (for full-size calls). */
    val remainingCredits: Int,
    /**
     * True when the upload should be attempted. False only when we
     * affirmatively know the call will 402.
     */
    val hasAvailableQuota: Boolean,
    /** Where the snapshot came from — useful for logging. */
    val source: Source,
) {
    enum class Source {
        /** Live `/account` response within the cache window. */
        Live,

        /** Cached `/account` response — still within freshness window. */
        Cached,

        /**
         * Account endpoint failed (network blip, 5xx, etc.). The snapshot is
         * synthesised to allow the upload to proceed; the real call will
         * return 402 if quota is in fact exhausted, and the existing
         * null-on-failure path will catch it.
         */
        Optimistic,

        /** API key not configured. Quota is structurally zero. */
        Unconfigured,
    }
}
