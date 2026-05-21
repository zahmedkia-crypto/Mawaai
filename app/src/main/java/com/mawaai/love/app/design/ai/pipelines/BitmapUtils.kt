package com.mawaai.love.app.design.ai.pipelines

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

internal fun downsizeIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
    val max = maxOf(source.width, source.height)
    if (max <= maxDimension) return source
    val scale = maxDimension.toFloat() / max
    val w = (source.width * scale).toInt().coerceAtLeast(1)
    val h = (source.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, w, h, true)
}

internal fun createSolidBitmap(width: Int, height: Int, argb: Int): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint().apply { color = argb }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return bmp
}

/**
 * Recycle every bitmap in [candidates] exactly once, using referential equality,
 * except those present in [keep]. Safe against aliasing — when a stage falls
 * through and returns its input (e.g. `applyTone` short-circuiting so
 * `tinted === stylized`), the underlying bitmap is still recycled a single time.
 */
internal fun recycleIntermediates(candidates: List<Bitmap>, keep: List<Bitmap>) {
    val seen = ArrayList<Bitmap>(candidates.size)
    for (bmp in candidates) {
        if (keep.any { it === bmp }) continue
        if (seen.any { it === bmp }) continue
        seen += bmp
        bmp.safeRecycle()
    }
}

internal fun Bitmap.safeRecycle() {
    if (!isRecycled) recycle()
}
