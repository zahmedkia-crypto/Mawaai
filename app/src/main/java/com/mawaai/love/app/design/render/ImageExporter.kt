package com.mawaai.love.app.design.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageExporter @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    /**
     * Format of the saved file. PNG is lossless (~5–10 MB for a 1080×1440 card);
     * JPEG is much smaller (~200–500 KB at the default quality 92) and is the
     * right pick for photographs / composited designs where the alpha channel
     * is not load-bearing.
     */
    enum class Format(internal val compress: Bitmap.CompressFormat, internal val mime: String, internal val extension: String) {
        PNG(Bitmap.CompressFormat.PNG, "image/png", "png"),
        JPEG(Bitmap.CompressFormat.JPEG, "image/jpeg", "jpg")
    }

    suspend fun saveToGallery(
        bitmap: Bitmap,
        displayName: String,
        subdirectory: String = "Mawaai",
        format: Format = Format.PNG,
        quality: Int = 100
    ): Uri = withContext(Dispatchers.IO) {
        val q = quality.coerceIn(1, 100)
        val filename = "${displayName}-${System.currentTimeMillis()}.${format.extension}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertScopedStorage(bitmap, filename, subdirectory, format, q)
        } else {
            insertLegacy(bitmap, filename, subdirectory, format, q)
        }
    }

    private fun insertScopedStorage(
        bitmap: Bitmap,
        filename: String,
        subdirectory: String,
        format: Format,
        quality: Int
    ): Uri {
        val resolver = appContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subdirectory")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert returned null")
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(format.compress, quality, stream)) {
                    error("Bitmap.compress failed")
                }
            } ?: error("openOutputStream returned null")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun insertLegacy(
        bitmap: Bitmap,
        filename: String,
        subdirectory: String,
        format: Format,
        quality: Int
    ): Uri {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), subdirectory)
        if (!dir.exists() && !dir.mkdirs()) error("Failed to create $dir")
        val file = File(dir, filename)
        FileOutputStream(file).use { stream ->
            if (!bitmap.compress(format.compress, quality, stream)) {
                error("Bitmap.compress failed")
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        return appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: Uri.fromFile(file)
    }
}
