package com.mawaai.love.app.design.canvas.engine

import android.content.Context
import android.graphics.Bitmap
import com.mawaai.love.app.data.model.Artwork
import com.mawaai.love.app.data.repository.ArtworkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ExportEngine(
    private val context: Context,
    private val artworkRepository: ArtworkRepository
) {
    suspend fun saveAsArtwork(
        bitmap: Bitmap,
        title: String,
        categoryId: String,
        subTypeId: String?,
        tags: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val artworksDir = File(context.filesDir, "artworks").apply { mkdirs() }
        val thumbsDir = File(artworksDir, "thumbs").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        val safeTitle = title.replace("[^\\p{L}\\p{N}_-]+".toRegex(), "_")
        val fullFile = File(artworksDir, "${safeTitle}_$timestamp.png")
        val thumbFile = File(thumbsDir, "${safeTitle}_$timestamp.jpg")

        FileOutputStream(fullFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val thumbSize = 256
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val (tw, th) = if (ratio >= 1f) thumbSize to (thumbSize / ratio).toInt()
        else (thumbSize * ratio).toInt() to thumbSize
        val thumb = Bitmap.createScaledBitmap(bitmap, tw.coerceAtLeast(1), th.coerceAtLeast(1), true)
        FileOutputStream(thumbFile).use { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (thumb !== bitmap) thumb.recycle()

        val artwork = Artwork(
            title = title.ifBlank { "Untitled" },
            categoryId = categoryId,
            subTypeId = subTypeId,
            fullImagePath = fullFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            width = bitmap.width,
            height = bitmap.height,
            tags = tags
        )
        artworkRepository.save(artwork)
    }

    suspend fun exportPngToCache(bitmap: Bitmap, name: String = "export"): File =
        withContext(Dispatchers.IO) {
            val file = File(context.cacheDir, "$name-${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file
        }
}
