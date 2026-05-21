package com.mawaai.love.app.design.ai.processors

import android.graphics.Bitmap
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeDetectionProcessor @Inject constructor() {

    suspend fun cannyEdges(
        input: Bitmap,
        lowThreshold: Double = 60.0,
        highThreshold: Double = 180.0,
        dilateKernel: Int = 1
    ): Bitmap = withContext(Dispatchers.Default) {
        // OpenCVBootstrap is the single source of truth for native availability.
        // When unavailable we return the input untouched — callers (AIEngine)
        // already treat edge-detection as a quality enhancement, not required.
        if (!OpenCVBootstrap.ensureLoaded()) return@withContext input

        matScope {
            val src = take(Mat())
            Utils.bitmapToMat(input, src)

            val gray = take(Mat())
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 1.4)

            val edges = take(Mat())
            Imgproc.Canny(gray, edges, lowThreshold, highThreshold)

            if (dilateKernel > 0) {
                val k = take(Imgproc.getStructuringElement(
                    Imgproc.MORPH_RECT,
                    Size((dilateKernel * 2 + 1).toDouble(), (dilateKernel * 2 + 1).toDouble())
                ))
                Imgproc.dilate(edges, edges, k)
            }

            Imgproc.cvtColor(edges, edges, Imgproc.COLOR_GRAY2RGBA)
            val out = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(edges, out)
            out
        }
    }
}
