package com.mawaai.love.app.design.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.util.Log
import android.util.LruCache
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.mawaai.love.app.design.domain.model.Template
import com.mawaai.love.app.design.domain.model.TemplateMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateAssetManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private val cache = mutableMapOf<String, List<Template>>()
    private val lock = Mutex()
    private val gson = Gson()

    /**
     * Decoded-bitmap cache. Keyed by asset path so the Result screen and the
     * gallery thumbnail share a single decode per template. Sized to roughly
     * 1/8 of available app memory — enough for ~10 high-res templates without
     * starving the rest of the design pipeline.
     */
    private val bitmapCache = object : LruCache<String, Bitmap>(maxBitmapCacheBytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) oldValue.recycle()
        }
    }

    suspend fun forCategory(categoryId: String): List<Template> = lock.withLock {
        cache.getOrPut(categoryId) {
            withContext(Dispatchers.IO) { scanCategory(categoryId) }
        }
    }

    suspend fun loadBitmap(template: Template): Bitmap = withContext(Dispatchers.IO) {
        bitmapCache.get(template.assetPath)?.takeIf { !it.isRecycled }?.let { return@withContext it }
        val decoded = appContext.assets.open(template.assetPath).use { stream ->
            BitmapFactory.decodeStream(stream)
                ?: error("Failed to decode template ${template.assetPath}")
        }
        bitmapCache.put(template.assetPath, decoded)
        decoded
    }

    /**
     * Loads the optional fabric mask shipped alongside the template as
     * `<templateId>.mask.png`. Returns null when no mask asset exists; the
     * caller (e.g. [com.mawaai.love.app.design.render.GarmentColorEngine])
     * then derives a heuristic mask from the base bitmap. Decoded masks
     * share the [bitmapCache] so a subsequent slider-driven recolor on the
     * same template never touches the assets directory again.
     */
    suspend fun loadMaskBitmap(template: Template): Bitmap? = withContext(Dispatchers.IO) {
        val maskPath = maskPathFor(template)
        bitmapCache.get(maskPath)?.takeIf { !it.isRecycled }?.let { return@withContext it }
        val exists = runCatching {
            appContext.assets.list("templates/${template.categoryId}")?.any { it == maskFileName(template) } == true
        }.getOrDefault(false)
        if (!exists) return@withContext null
        val decoded = runCatching {
            appContext.assets.open(maskPath).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull() ?: return@withContext null
        bitmapCache.put(maskPath, decoded)
        decoded
    }

    fun clearBitmapCache() {
        bitmapCache.evictAll()
    }

    private fun maskFileName(template: Template): String = "${template.id}.mask.png"
    private fun maskPathFor(template: Template): String = "templates/${template.categoryId}/${maskFileName(template)}"

    private fun scanCategory(categoryId: String): List<Template> {
        val dir = "templates/$categoryId"
        val entries: List<String> = runCatching { appContext.assets.list(dir)?.toList().orEmpty() }
            .getOrDefault(emptyList())
        val metadataByTemplateId = loadMetadata(dir, entries)
        return entries
            .filter { it.matches(IMAGE_PATTERN) && !it.endsWith(MASK_SUFFIX, ignoreCase = true) }
            .sorted()
            .map { fileName ->
                val id = fileName.substringBeforeLast('.')
                Template(
                    id = id,
                    categoryId = categoryId,
                    assetPath = "$dir/$fileName",
                    metadata = metadataByTemplateId[id]
                )
            }
    }

    private fun loadMetadata(dir: String, entries: List<String>): Map<String, TemplateMetadata> {
        if (METADATA_FILENAME !in entries) return emptyMap()
        return runCatching {
            appContext.assets.open("$dir/$METADATA_FILENAME").use { stream ->
                val json = stream.bufferedReader().readText()
                val doc = gson.fromJson(json, TemplatesJsonDoc::class.java) ?: return@runCatching emptyMap()
                buildMap {
                    doc.templates?.forEach { entry ->
                        if (entry.id.isNullOrBlank()) return@forEach
                        val quad = entry.quad?.takeIf { it.size == 4 }?.map { pair ->
                            PointF(pair.getOrNull(0) ?: 0f, pair.getOrNull(1) ?: 0f)
                        }
                        put(
                            entry.id,
                            TemplateMetadata(
                                targetQuad = quad,
                                blendMode = entry.blend,
                                overlayAlpha = entry.alpha
                            )
                        )
                    }
                }
            }
        }.getOrElse {
            if (it is JsonSyntaxException) {
                Log.w(TAG, "Malformed $dir/$METADATA_FILENAME — ignoring", it)
            }
            emptyMap()
        }
    }

    private fun maxBitmapCacheBytes(): Int {
        val runtime = Runtime.getRuntime()
        val maxKb = (runtime.maxMemory() / 1024L).coerceAtLeast(64 * 1024)
        return (maxKb / 8).toInt() * 1024
    }

    private data class TemplatesJsonDoc(
        @SerializedName("templates") val templates: List<TemplateEntryJson>? = null
    )

    private data class TemplateEntryJson(
        @SerializedName("id") val id: String? = null,
        @SerializedName("quad") val quad: List<List<Float>>? = null,
        @SerializedName("blend") val blend: String? = null,
        @SerializedName("alpha") val alpha: Double? = null
    )

    private companion object {
        const val TAG = "TemplateAssetManager"
        const val METADATA_FILENAME = "templates.json"
        const val MASK_SUFFIX = ".mask.png"
        val IMAGE_PATTERN = Regex(".+\\.(jpg|jpeg|png)$", RegexOption.IGNORE_CASE)
    }
}
