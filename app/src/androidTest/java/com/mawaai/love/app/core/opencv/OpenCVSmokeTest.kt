package com.mawaai.love.app.core.opencv

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Connected-device smoke tests that prove the OpenCV native pipeline
 * actually works on the target device — not merely that
 * `OpenCVLoader.initLocal()` returned true.
 *
 * Run with:
 *
 *     ./gradlew :app:connectedDebugAndroidTest \
 *         -Pandroid.testInstrumentationRunnerArguments.class=com.mawaai.love.app.core.opencv.OpenCVSmokeTest
 *
 * If any test fails, the `.so` is missing for the device's ABI, the AAR
 * shipped a JNI version that doesn't match the Java bindings, or the
 * loader silently lied. Cross-reference logcat (`OpenCVBootstrap` tag)
 * for the `Build.SUPPORTED_ABIS` list and the per-`.so` size dump that
 * pinpoints which failure mode triggered.
 */
@RunWith(AndroidJUnit4::class)
class OpenCVSmokeTest {

    @Test
    fun bootstrap_loadsSuccessfully() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val ok = OpenCVBootstrap.init(ctx)
        assertTrue("OpenCVBootstrap.init() returned false — see logcat", ok)
        assertTrue("isAvailable should be true after a successful init", OpenCVBootstrap.isAvailable)
    }

    @Test
    fun mat_canBeAllocatedAndReleased() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(OpenCVBootstrap.init(ctx))

        val mat = Mat(64, 64, CvType.CV_8UC4)
        try {
            assertEquals(64, mat.rows())
            assertEquals(64, mat.cols())
            assertEquals(CvType.CV_8UC4, mat.type())
            assertFalse("Mat should not be empty after explicit constructor", mat.empty())
        } finally {
            mat.release()
        }
    }

    @Test
    fun cannyEdgesPipeline_runsWithoutCrash() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(OpenCVBootstrap.init(ctx))

        // 128x128 ARGB bitmap with a centered black square — guaranteed to
        // produce a non-empty Canny result.
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
        canvas.drawRect(40f, 40f, 88f, 88f, paint)

        val src = Mat()
        val gray = Mat()
        val edges = Mat()
        try {
            Utils.bitmapToMat(bitmap, src)
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.Canny(gray, edges, 60.0, 180.0)
            assertEquals(128, edges.rows())
            assertEquals(128, edges.cols())
            // Sanity: the centered square's edges should produce at least a
            // few non-zero pixels. Anything > 0 confirms the pipeline ran.
            val nonZero = org.opencv.core.Core.countNonZero(edges)
            assertTrue("Canny output had zero edges — pipeline may have silently no-opped", nonZero > 0)
        } finally {
            src.release()
            gray.release()
            edges.release()
            bitmap.recycle()
        }
    }

    @Test
    fun bitmapMat_roundTripPreservesDimensions() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(OpenCVBootstrap.init(ctx))

        val original = Bitmap.createBitmap(64, 96, Bitmap.Config.ARGB_8888)
        val mat = Mat()
        val rebuilt = Bitmap.createBitmap(64, 96, Bitmap.Config.ARGB_8888)
        try {
            Utils.bitmapToMat(original, mat)
            Utils.matToBitmap(mat, rebuilt)
            assertNotNull(rebuilt)
            assertEquals(original.width, rebuilt.width)
            assertEquals(original.height, rebuilt.height)
        } finally {
            mat.release()
            original.recycle()
            rebuilt.recycle()
        }
    }
}
