package com.mawaai.love.app.design.ai.preservation

import android.graphics.Bitmap
import android.graphics.Color
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import com.mawaai.love.app.design.ai.processors.matScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlledImprovementEngine @Inject constructor() {

    suspend fun improve(sketch: ScannedSketch, analysis: SketchAnalysis): ImprovedSketch =
        withContext(Dispatchers.Default) {
            if (!OpenCVBootstrap.ensureLoaded()) {
                return@withContext ImprovedSketch(strengthenWithPixels(sketch.cleanPng, analysis), analysis)
            }

            matScope {
                val src = take(Mat())
                Utils.bitmapToMat(sketch.cleanPng, src)

                val channels = ArrayList<Mat>()
                Core.split(src, channels)
                channels.forEach { take(it) }
                val alpha = channels[3]

                val binary = take(Mat())
                Imgproc.threshold(alpha, binary, 24.0, 255.0, Imgproc.THRESH_BINARY)

                val closeKernel = take(Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0)))
                Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, closeKernel)

                if (analysis.averageStrokeWidthPx < MIN_READABLE_STROKE_PX) {
                    val dilateKernel = take(Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(2.0, 2.0)))
                    Imgproc.dilate(binary, binary, dilateKernel)
                }

                val smoothed = take(Mat())
                Imgproc.GaussianBlur(binary, smoothed, Size(3.0, 3.0), 0.0)
                Core.max(binary, smoothed, smoothed)

                val zero = take(Mat.zeros(src.size(), CvType.CV_8UC1))
                val out = take(Mat())
                Core.merge(listOf(zero, zero, zero, smoothed), out)

                val result = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(out, result)
                ImprovedSketch(result, analysis)
            }
        }

    private fun strengthenWithPixels(source: Bitmap, analysis: SketchAnalysis): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val alpha = Color.alpha(source.getPixel(x, y))
                if (alpha > 16) {
                    val boosted = if (analysis.averageStrokeWidthPx < MIN_READABLE_STROKE_PX) {
                        (alpha * 1.25f).toInt()
                    } else {
                        alpha
                    }.coerceIn(0, 255)
                    out.setPixel(x, y, Color.argb(boosted, 0, 0, 0))
                }
            }
        }
        return out
    }

    private companion object {
        const val MIN_READABLE_STROKE_PX = 3f
    }
}
