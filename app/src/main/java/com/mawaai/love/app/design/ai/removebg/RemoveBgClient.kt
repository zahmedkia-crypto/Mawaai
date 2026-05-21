package com.mawaai.love.app.design.ai.removebg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.mawaai.love.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
 *  - The 50/month quota is the trade-off. The AIEngine should only call
 *    this when the user explicitly requests a "premium cut" or for the
 *    final saved/exported asset; mid-flow previews keep using the HF /
 *    on-device paths to preserve quota.
 *
 * Contract mirrors [HuggingFaceClient]:
 *  - Returns null on any failure (no key, network, 402 quota, decode).
 *  - Caches PNG bytes under `cacheDir/removebg_cache/<sha>.png` keyed
 *    by `sha256(uploaded JPEG)` so repeat calls with the same input are
 *    free and instant.
 *  - All I/O on `Dispatchers.IO`.
 */
@Singleton
class RemoveBgClient @Inject constructor(
    private val api: RemoveBgApi,
    @ApplicationContext private val appContext: Context
) {

    val isConfigured: Boolean get() = BuildConfig.REMOVE_BG_API_KEY.isNotBlank()

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
        highQuality: Boolean = false
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null

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

        val JPEG_MEDIA = "image/jpeg".toMediaTypeOrNull()!!
        val TEXT_MEDIA = "text/plain".toMediaTypeOrNull()!!
    }
}
