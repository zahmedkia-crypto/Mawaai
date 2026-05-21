package com.mawaai.love.app.design.ai.processors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerspectiveWarpProcessor @Inject constructor() {

    suspend fun warp(
        source: Bitmap,
        destinationSize: Size,
        destinationQuad: List<PointF>
    ): Bitmap = withContext(Dispatchers.Default) {
        require(destinationQuad.size == 4) { "destinationQuad must have exactly 4 points" }

        // Fall back to Android's setPolyToPoly when OpenCV isn't loaded. The
        // result is visually identical at this resolution; the safety net
        // prevents UnsatisfiedLinkError from killing the template flow.
        if (!OpenCVBootstrap.ensureLoaded()) {
            return@withContext warpWithAndroidMatrix(source, destinationSize, destinationQuad)
        }

        matScope {
            val src = take(Mat())
            Utils.bitmapToMat(source, src)

            val srcCorners = take(MatOfPoint2f(
                Point(0.0, 0.0),
                Point(source.width.toDouble(), 0.0),
                Point(source.width.toDouble(), source.height.toDouble()),
                Point(0.0, source.height.toDouble())
            ))
            val dstCorners = take(MatOfPoint2f(
                *destinationQuad.map { Point(it.x.toDouble(), it.y.toDouble()) }.toTypedArray()
            ))

            val transform = take(Imgproc.getPerspectiveTransform(srcCorners, dstCorners))
            val out = take(Mat(destinationSize, CvType.CV_8UC4))
            Imgproc.warpPerspective(src, out, transform, destinationSize)

            val result = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(out, result)
            result
        }
    }

    /**
     * Pure-Android fallback used when [OpenCVBootstrap] reports the native
     * library is unavailable. [android.graphics.Matrix.setPolyToPoly] supports
     * 4-point quad mappings directly, producing the same perspective warp
     * the OpenCV path does.
     *
     * Audit fix #9 (2026-05-13): `setPolyToPoly` returns `false` for
     * degenerate quads (collinear points, zero area). The previous version
     * silently used the resulting identity matrix and drew the source
     * unwarped, which the user would mistake for an Apply that did
     * nothing. We now log the failure and fall back to a uniform-scale
     * draw centered on the destination — the user still gets a visible
     * change instead of an opaque "huh?" result.
     */
    private fun warpWithAndroidMatrix(
        source: Bitmap,
        destinationSize: Size,
        destinationQuad: List<PointF>
    ): Bitmap {
        val outW = destinationSize.width.toInt().coerceAtLeast(1)
        val outH = destinationSize.height.toInt().coerceAtLeast(1)
        val src = floatArrayOf(
            0f, 0f,
            source.width.toFloat(), 0f,
            source.width.toFloat(), source.height.toFloat(),
            0f, source.height.toFloat()
        )
        val dst = floatArrayOf(
            destinationQuad[0].x, destinationQuad[0].y,
            destinationQuad[1].x, destinationQuad[1].y,
            destinationQuad[2].x, destinationQuad[2].y,
            destinationQuad[3].x, destinationQuad[3].y
        )
        val matrix = Matrix()
        val ok = matrix.setPolyToPoly(src, 0, dst, 0, 4)
        if (!ok) {
            Log.w(TAG, "setPolyToPoly returned false (degenerate quad). Falling back to centered uniform scale.")
            val scale = minOf(
                outW.toFloat() / source.width,
                outH.toFloat() / source.height
            )
            matrix.reset()
            matrix.postScale(scale, scale)
            matrix.postTranslate(
                (outW - source.width * scale) / 2f,
                (outH - source.height * scale) / 2f
            )
        }
        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(
            source,
            matrix,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        )
        return result
    }

    private companion object {
        const val TAG = "PerspectiveWarp"
    }
}
