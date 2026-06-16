package com.mawaai.love.app.design.ai.removebg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.mawaai.love.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud background-removal client built on remove.bg's HTTP API.
 *
 * Why it exists alongside [com.mawaai.love.app.design.ai.huggingface.HuggingFaceClient.removeBackground]:
 *  - remove.bg ships a commercial-grade cut quality that beats RMBG-1.4
 *    on hair edges and translucent fabrics — the typical Mawaai content
 *    (women in toubs, henna on hands) is exactly where the upgrade pays
 *    off.
 *  - The 50/month preview quota + PAYG credit balance is the trade-off.
 *    The AIEngine should only call this when the user explicitly requests
 *    a "premium cut" or for the final saved/exported asset; mid-flow
 *    previews keep using the HF / on-device paths to preserve quota.
 *
 * Contract mirrors [com.mawaai.love.app.design.ai.huggingface.HuggingFaceClient]:
 *  - Returns null on any failure (no key, network, 402 quota, decode).
 *  - Caches PNG bytes under `cacheDir/removebg_cache/<sha>.png` keyed
 *    by `sha256(uploaded JPEG + size param)` so repeat calls with the
 *    same input are free and instant.
 *  - All I/O on `Dispatchers.IO`.
 *
 * MT-011 (2026-05-28): added a pre-flight `/account` quota check. Before
 * uploading the multipart body, we ask remove.bg how much quota is left
 * and short-circuit with `null` if we already know the call will 402.
 * The check is cached in-memory for [QUOTA_CACHE_TTL_MS] so a session of
 * BG-removal taps does not burn one account-info call per tap, and
 * structurally fails open: if the account endpoint itself fails (network
 * blip, 5xx), the upload still proceeds and the existing null-on-failure
 * path catches the real 402 if quota is in fact exhausted.
 */
@Singleton
class RemoveBgClient @Inject constructor(
    private val api: RemoveBgApi,
    @ApplicationContext private val appContext: Context,
) {

    val isConfigured: Boolean get() = BuildConfig.REMOVE_BG_API_KEY.isNotBlank()

    // ─── State for MT-011 quota cache ────────────────────────────────────────

    @Volatile
    private var cachedQuota: RemoveBgQuotaSnapshot? = null

    @Volatile
    private var cachedQuotaAt: Long = 0L

    private val quotaFetchMutex = Mutex()

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Cuts the subject out of [input] via remove.bg and returns an ARGB
     * bitmap with the alpha channel encoded by the model. Returns null
     * when the cloud path is unavailable so callers can fall back to
     * the existing HF / ML Kit pipeline.
     *
     * [highQuality]: when true uses `size=auto` (full resolution — debits
     * credit balance). Default false uses `size=preview` to stay within
     * the free monthly quota. The AIEngine flips this only for the
     * final exported asset.
     */
    suspend fun removeBackground(
        input: Bitmap,
        highQuality: Boolean = false,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null

        // MT-011 pre-flight: skip uploads we know will 402. Pre-flight is
        // additive — on any kind of account-endpoint failure we fall through
        // to the existing upload path so transient outages don't block work.
        val snapshot = precheckQuota(highQuality = highQuality)
        if (!snapshot.hasAvailableQuota) {
            Log.i(
                TAG,
                "Pre-flight skip: remove.bg quota exhausted " +
                    "(previewCalls=${snapshot.remainingPreviewCalls}, " +
                    "credits=${snapshot.remainingCredits}, source=${snapshot.source}). " +
                    "Falling back to on-device cutout."
            )
            return@withContext null
        }

        val resized = resizeForUpload(input)
        val jpegBytes = compressToJpeg(resized)
        if (resized !== input) resized.recycle()

        val sizeParam = if (highQuality) "auto" else "preview"
        val cacheKey = sha256(jpegBytes + sizeParam.toByteArray())
        cacheLookup(cacheKey)?.let { return@withContext it }

        val responseBytes = try {
            val imagePart = MultipartBody.Part.createFormData(
                name = "image_file",
                filename = "input.jpg",
                body = jpegBytes.toRequestBody(JPEG_MEDIA)
            )
            val sizeBody = sizeParam.toRequestBody(TEXT_MEDIA)
            val response = api.removeBg(
                apiKey = BuildConfig.REMOVE_BG_API_KEY,
                imagePart = imagePart,
                size = sizeBody
            )
            if (!response.isSuccessful) {
                // 402 = quota exhausted, 403 = invalid key, 429 = rate-limited.
                // All of these mean "fall back" — never surface as an error.
                // Invalidate the quota cache so the next call re-probes and
                // catches any out-of-band quota changes (e.g. the user
                // topped up via the dashboard).
                if (response.code() == 402 || response.code() == 429) {
                    invalidateQuotaCache()
                }
                Log.w(TAG, "remove.bg failed: ${response.code()} ${response.errorBody()?.string()}")
                return@withContext null
            }
            response.body()?.bytes() ?: return@withContext null
        } catch (e: Throwable) {
            Log.w(TAG, "remove.bg threw", e)
            return@withContext null
        }

        val bitmap = BitmapFactory.decodeByteArray(responseBytes, 0, responseBytes.size)
            ?: return@withContext null
        cacheStore(cacheKey, responseBytes)
        bitmap
    }

    /**
     * Returns the latest [RemoveBgQuotaSnapshot]. Cached for
     * [QUOTA_CACHE_TTL_MS]. Public so UI code can render a "low quota"
     * banner without forcing a separate API call.
     *
     * Never throws. On any error the returned snapshot has
     * `source = Optimistic` and `hasAvailableQuota = true` so callers do not
     * block on a transient network blip.
     */
    suspend fun precheckQuota(
        highQuality: Boolean = false,
        forceRefresh: Boolean = false,
    ): RemoveBgQuotaSnapshot {
        if (!isConfigured) {
            return RemoveBgQuotaSnapshot(
                remainingPreviewCalls = 0,
                remainingCredits = 0,
                hasAvailableQuota = false,
                source = RemoveBgQuotaSnapshot.Source.Unconfigured,
            )
        }

        val now = System.currentTimeMillis()
        val cached = cachedQuota
        if (!forceRefresh &&
            cached != null &&
            (now - cachedQuotaAt) < QUOTA_CACHE_TTL_MS
        ) {
            return cached.copy(
                hasAvailableQuota = cached.hasAvailableQuotaFor(highQuality),
                source = RemoveBgQuotaSnapshot.Source.Cached,
            )
        }

        // Single-flight: only one /account call in flight at a time per process.
        return quotaFetchMutex.withLock {
            // Re-read inside the lock — another caller may have populated it.
            val freshCheck = cachedQuota
            if (!forceRefresh &&
                freshCheck != null &&
                (System.currentTimeMillis() - cachedQuotaAt) < QUOTA_CACHE_TTL_MS
            ) {
                return@withLock freshCheck.copy(
                    hasAvailableQuota = freshCheck.hasAvailableQuotaFor(highQuality),
                    source = RemoveBgQuotaSnapshot.Source.Cached,
                )
            }
            fetchAndCacheQuota(highQuality)
        }
    }

    private suspend fun fetchAndCacheQuota(
        highQuality: Boolean,
    ): RemoveBgQuotaSnapshot = withContext(Dispatchers.IO) {
        val snapshot = try {
            val response = api.getAccount(apiKey = BuildConfig.REMOVE_BG_API_KEY)
            if (!response.isSuccessful) {
                Log.w(TAG, "remove.bg /account returned ${response.code()} — proceeding optimistically")
                optimisticSnapshot()
            } else {
                val attrs = response.body()?.data?.attributes
                val credits = attrs?.credits
                val totalCredits = listOfNotNull(
                    credits?.subscription, credits?.payg, credits?.enterprise
                ).sum().let { sum ->
                    // The `total` field is the same sum but be defensive.
                    credits?.total ?: sum
                }
                val previewCalls = attrs?.api?.freeCalls ?: 0
                RemoveBgQuotaSnapshot(
                    remainingPreviewCalls = previewCalls,
                    remainingCredits = totalCredits,
                    hasAvailableQuota = (previewCalls > 0 || totalCredits > 0).let {
                        if (highQuality) totalCredits > 0 else it
                    },
                    source = RemoveBgQuotaSnapshot.Source.Live,
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "remove.bg /account threw — proceeding optimistically", t)
            optimisticSnapshot()
        }
        cachedQuota = snapshot
        cachedQuotaAt = System.currentTimeMillis()
        snapshot
    }

    private fun invalidateQuotaCache() {
        cachedQuota = null
        cachedQuotaAt = 0L
    }

    private fun optimisticSnapshot(): RemoveBgQuotaSnapshot = RemoveBgQuotaSnapshot(
        remainingPreviewCalls = Int.MAX_VALUE,
        remainingCredits = Int.MAX_VALUE,
        hasAvailableQuota = true,
        source = RemoveBgQuotaSnapshot.Source.Optimistic,
    )

    /**
     * "Do I have quota for a [highQuality] call right now?" — derived from the
     * snapshot we already have so the cached value remains valid for both
     * preview and full-size requests without re-fetching.
     */
    private fun RemoveBgQuotaSnapshot.hasAvailableQuotaFor(highQuality: Boolean): Boolean =
        if (highQuality) remainingCredits > 0
        else (remainingPreviewCalls > 0 || remainingCredits > 0)

    // ─── Bitmap cache helpers (unchanged from pre-MT-011) ────────────────────

    private fun cacheLookup(key: String): Bitmap? {
        val file = cacheFile(key)
        if (!file.exists() || file.length() == 0L) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun cacheStore(key: String, bytes: ByteArray) {
        runCatching {
            FileOutputStream(cacheFile(key)).use { it.write(bytes) }
        }.onFailure { Log.w(TAG, "Failed to write remove.bg cache entry", it) }
    }

    private fun cacheFile(key: String): File {
        val dir = File(appContext.cacheDir, "removebg_cache").apply { mkdirs() }
        return File(dir, "$key.png")
    }

    private fun resizeForUpload(bitmap: Bitmap): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= MAX_UPLOAD_DIMENSION) return bitmap
        val scale = MAX_UPLOAD_DIMENSION.toFloat() / max
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.toByteArray()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    internal companion object {
        const val TAG = "RemoveBgClient"

        // remove.bg accepts up to 25 MP but the free tier renders preview
        // at ~0.25 MP. Downsizing client-side to 1024 px on the long edge
        // keeps the upload small without hurting cut quality.
        const val MAX_UPLOAD_DIMENSION = 1024
        const val JPEG_QUALITY = 90

        // MT-011: how long the /account response stays fresh in memory.
        // 5 minutes balances "don't burn 1 quota probe per BG-removal tap"
        // against "users can top up via the dashboard mid-session and
        // pre-flight notices within a reasonable time".
        const val QUOTA_CACHE_TTL_MS = 5L * 60L * 1000L

        val JPEG_MEDIA = "image/jpeg".toMediaTypeOrNull()!!
        val TEXT_MEDIA = "text/plain".toMediaTypeOrNull()!!
    }
}
