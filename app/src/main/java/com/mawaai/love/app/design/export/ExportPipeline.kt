package com.mawaai.love.app.design.export

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

/**
 * MT-035: writes a composited mockup [Bitmap] to the system gallery
 * (MediaStore on Q+, legacy Environment.DIRECTORY_PICTURES on pre-Q) so the
 * user can share it from Photos / Files / WhatsApp without granting any
 * extra runtime permission on Q+.
 *
 * On Android 10+ (API 29) we write through MediaStore.Images using scoped
 * storage -- no WRITE_EXTERNAL_STORAGE permission needed. On pre-Q devices
 * (API 26-28, the minSdk of this project is 26) the legacy path requires
 * WRITE_EXTERNAL_STORAGE in the manifest; the AIEngine's existing image
 * export already has that permission declared so we reuse it here.
 */
@Singleton
class ExportPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Export [bitmap] under [displayName] (without extension; '.png' is
     * appended automatically). Returns the [Uri] of the saved image so the
     * caller can immediately fire an ACTION_SEND share intent.
     *
     * Throws if the bitmap can't be written -- callers should wrap in
     * `runCatching { ... }` and surface the failure as a snackbar.
     */
    suspend fun exportToGallery(bitmap: Bitmap, displayName: String): Uri =
        withContext(Dispatchers.IO) {
            val safeName = sanitize(displayName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                exportViaMediaStore(bitmap, safeName)
            } else {
                exportLegacy(bitmap, safeName)
            }
        }

    private fun exportViaMediaStore(bitmap: Bitmap, name: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore.insert returned null for $name")
        resolver.openOutputStream(uri).use { out ->
            requireNotNull(out) { "openOutputStream null for $uri" }
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun exportLegacy(bitmap: Bitmap, name: String): Uri {
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(pictures, ALBUM).apply { mkdirs() }
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
        }
        return Uri.fromFile(file)
    }

    /**
     * MediaStore display names cannot contain '/' and conventionally avoid
     * other path-confusing characters; collapse them to underscores so
     * user-provided project titles export cleanly.
     */
    private fun sanitize(raw: String): String =
        raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "design_${System.currentTimeMillis()}" }
            .take(MAX_NAME_LENGTH)

    private companion object {
        const val ALBUM = "Mawaai"
        const val MIME_PNG = "image/png"
        const val PNG_QUALITY = 100
        const val MAX_NAME_LENGTH = 80
    }
}
