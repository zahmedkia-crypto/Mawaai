package com.mawaai.love.app.design.ai.render

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase-5 render persistence: writes a rendered [Bitmap] to the app's internal
 * files dir and returns its absolute path.
 *
 * Lives next to [ImageEditRenderer] so the IO concern is decoupled from the
 * Room-layer update (see [com.mawaai.love.app.data.repository.ProjectRepository.saveRender]).
 *
 * Storage layout:
 *   `{context.filesDir}/renders/{projectId}-{timestamp}.png`
 *
 * The timestamp suffix keeps every render of the same project as its own file
 * so the user can scroll history without one render overwriting the next.
 * Cleanup of older renders is out of scope here -- handled by a future
 * housekeeping task.
 *
 * PNG is chosen over JPEG because the downstream HuggingFace ControlNet
 * pipeline emits images with alpha-friendly edges (henna and embroidery
 * details near transparent / soft boundaries) and JPEG compression artifacts
 * are visible at the sketch-line transitions.
 */
@Singleton
class RenderFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Persist [bitmap] for [projectId] and return the absolute file path.
     *
     * Caller is responsible for passing the returned path to
     * [com.mawaai.love.app.data.repository.ProjectRepository.saveRender]
     * so the Room row picks up the location.
     */
    suspend fun saveRender(projectId: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, RENDERS_DIR).apply { mkdirs() }
            val file = File(dir, "${projectId}-${System.currentTimeMillis()}.${EXT}")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
            }
            file.absolutePath
        }

    private companion object {
        const val RENDERS_DIR = "renders"
        const val EXT = "png"
        const val PNG_QUALITY = 100
    }
}
