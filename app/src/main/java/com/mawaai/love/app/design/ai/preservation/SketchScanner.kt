package com.mawaai.love.app.design.ai.preservation

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import com.mawaai.love.app.design.ai.processors.matScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect as CvRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SketchScanner @Inject constructor() {

    suspend fun scan(input: Bitmap, maxSide: Int = 1024): ScannedSketch = withContext(Dispatchers.Default) {
        val source = downsize(input, maxSide)
        if (!OpenCVBootstrap.ensureLoaded()) {
            return@withContext scanWithPixels(source)
        }

        matScope {
            val src = take(Mat())
            Utils.bitmapToMat(source, src)

            val gray = take(Mat())
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)

            val blurred = take(Mat())
            Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)

            val threshold = take(Mat())
            Imgproc.adaptiveThreshold(
                blurred,
                threshold,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                31,
                8.0
            )

            val kernel = take(Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0)))
            val cleaned = take(Mat())
            Imgproc.morphologyEx(threshold, cleaned, Imgproc.MORPH_OPEN, kernel)
            Imgproc.morphologyEx(cleaned, cleaned, Imgproc.MORPH_CLOSE, kernel)

            val contours = ArrayList<MatOfPoint>()
            val hierarchy = take(Mat())
            val contourSource = take(cleaned.clone())
            Imgproc.findContours(contourSource, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            contours.forEach { take(it) }

            val contourMask = take(Mat.zeros(cleaned.size(), CvType.CV_8UC1))
            Imgproc.drawContours(contourMask, contours, -1, Scalar(255.0), 1)

            val rgba = take(Mat.zeros(src.size(), CvType.CV_8UC4))
            val zeros = take(Mat.zeros(cleaned.size(), CvType.CV_8UC1))
            Core.merge(listOf(zeros, zeros, zeros, cleaned), rgba)

            val cleanBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgba, cleanBitmap)
            val maskBitmap = Bitmap.createBitmap(cleaned.cols(), cleaned.rows(), Bitmap.Config.ARGB_8888)
            val white = take(Mat(cleaned.size(), CvType.CV_8UC1, Scalar(255.0)))
            val maskRgba = take(Mat())
            Core.merge(listOf(white, white, white, cleaned), maskRgba)
            Utils.matToBitmap(maskRgba, maskBitmap)
            val contourBitmap = Bitmap.createBitmap(contourMask.cols(), contourMask.rows(), Bitmap.Config.ARGB_8888)
            val contourRgba = take(Mat())
            Core.merge(listOf(white, white, white, contourMask), contourRgba)
            Utils.matToBitmap(contourRgba, contourBitmap)

            if (source !== input) source.recycle()
            ScannedSketch(
                cleanPng = cleanBitmap,
                inkMask = maskBitmap,
                contourMask = contourBitmap,
                backgroundRemoved = true,
                bounds = boundsFor(cleaned)
            )
        }
    }

    private fun downsize(source: Bitmap, maxSide: Int): Bitmap {
        val max = maxOf(source.width, source.height)
        if (max <= maxSide) return source
        val scale = maxSide.toFloat() / max
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun boundsFor(mask: Mat): Rect {
        val points = Mat()
        return try {
            Core.findNonZero(mask, points)
            if (points.empty()) {
                Rect(0, 0, mask.cols(), mask.rows())
            } else {
                val r: CvRect = Imgproc.boundingRect(points)
                Rect(r.x, r.y, r.x + r.width, r.y + r.height)
            }
        } finally {
            points.release()
        }
    }

    private fun scanWithPixels(source: Bitmap): ScannedSketch {
        val clean = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val mask = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val contour = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        var left = source.width
        var top = source.height
        var right = 0
        var bottom = 0
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val c = source.getPixel(x, y)
                val lum = (Color.red(c) * 0.299f + Color.green(c) * 0.587f + Color.blue(c) * 0.114f).toInt()
                val alpha = (255 - lum).coerceIn(0, 255)
                if (alpha > 48) {
                    clean.setPixel(x, y, Color.argb(alpha, 0, 0, 0))
                    mask.setPixel(x, y, Color.argb(alpha, 255, 255, 255))
                    contour.setPixel(x, y, Color.argb(alpha, 255, 255, 255))
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        if (source !== clean && source !== mask && source !== contour) {
            // Do not recycle caller-owned input; only scanner-created downsized copies reach here.
        }
        val bounds = if (right > left && bottom > top) Rect(left, top, right + 1, bottom + 1)
        else Rect(0, 0, source.width, source.height)
        return ScannedSketch(clean, mask, contour, backgroundRemoved = true, bounds = bounds)
    }
}
