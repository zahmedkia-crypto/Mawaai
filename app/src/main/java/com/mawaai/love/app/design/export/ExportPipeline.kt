package com.mawaai.love.app.design.export

import android.graphics.Bitmap
import android.net.Uri
import com.mawaai.love.app.design.render.ImageExporter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @deprecated Use [com.mawaai.love.app.design.render.ImageExporter] directly.
 *
 * Late-May 2026 integration sprint committed this thin facade before
 * realising [ImageExporter] already shipped at design/render/ImageExporter.kt
 * with the same export-to-gallery contract -- and more (PNG + JPEG, custom
 * subdirectory, configurable quality).
 *
 * This wrapper preserves the `exportToGallery(bitmap, name): Uri` signature
 * so any call sites built against the duplicate continue to compile while
 * a follow-up commit migrates them off and deletes this file. New code MUST
 * inject [ImageExporter] directly.
 *
 * See PROJECT_LOG.md entry '2026-05-28 -- Creative-Studio -> Mawaai
 * integration sweep' under 'Architecture / scope notes' for context.
 */
@Singleton
@Deprecated(
    message = "Use ImageExporter from design.render -- supports JPEG + custom subdir.",
    replaceWith = ReplaceWith(
        expression = "ImageExporter",
        imports = ["com.mawaai.love.app.design.render.ImageExporter"],
    ),
)
class ExportPipeline @Inject constructor(
    private val delegate: ImageExporter,
) {
    suspend fun exportToGallery(bitmap: Bitmap, displayName: String): Uri =
        delegate.saveToGallery(
            bitmap = bitmap,
            displayName = displayName,
            subdirectory = MAWAAI_ALBUM,
            format = ImageExporter.Format.PNG,
            quality = PNG_QUALITY,
        )

    private companion object {
        const val MAWAAI_ALBUM = "Mawaai"
        const val PNG_QUALITY = 100
    }
}
