package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import com.mawaai.love.app.design.ai.processors.MatScope
import com.mawaai.love.app.design.ai.processors.matScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Final-stage polish pass that runs at the tail of every successful
 * AIEngine pipeline (cloud or local). Two cheap visual upgrades:
 *
 *  1. **Unsharp mask** — Gaussian-blur the input then subtract the blur
 *     from the original at low weight (`addWeighted(input, 1+amount,
 *     blur, -amount, 0)`). Restores micro-contrast lost by upscaling
 *     and ML stylization without introducing the halo artefacts a
 *     full-strength sharpen would.
 *  2. **Saturation lift** — convert to HSV, multiply S by 1.10, convert
 *     back. Gives stylized output a slightly punchier color cast which
 *     matches what users intuitively expect from "AI polished".
 *
 * Both passes are run on `Dispatchers.Default`. When OpenCV is not
 * loaded ([OpenCVBootstrap.ensureLoaded] returns false), the input is
 * returned untouched — same graceful-degradation contract the rest of
 * the AIEngine processors follow.
 *
 * The output is always a freshly allocated bitmap, so the caller is
 * free to recycle the input. Output config is `ARGB_8888` regardless
 * of the input config.
 */
@Singleton
class OfflineEnhancer @Inject constructor() {

    /**
     * Backwards-compatible no-context entry point. Uses the same
     * neutral tuning as before — equivalent to `enhance(input, null)`.
     */
    suspend fun enhance(input: Bitmap): Bitmap = enhance(input, categoryId = null)

    /**
     * Category-aware enhancer. The optional [categoryId] selects a
     * per-category tuning profile so the final polish matches the
     * intent of the design:
     *  - **henna**: heavier sharpening (intricate dye linework needs
     *    micro-contrast) + neutral saturation (henna ink is already
     *    saturated; bumping further pushes it orange).
     *  - **abaya** / **thob_sudani**: medium sharpening + richer
     *    saturation lift so fabric folds and gold-thread embroidery
     *    pop.
     *  - **walls**: lighter touch on both axes — large flat-painted
     *    areas amplify sharpening artefacts (banding, halos) and an
     *    over-saturated wall looks plastic.
     *  - **null / unknown**: the previous one-size-fits-all
     *    (sigma 1.4, amount 0.45, sat 1.10). Safe default.
     */
    suspend fun enhance(input: Bitmap, categoryId: String?): Bitmap = withContext(Dispatchers.Default) {
        if (input.isRecycled) return@withContext input
        if (!OpenCVBootstrap.ensureLoaded()) return@withContext input

        val profile = profileFor(categoryId)
        runCatching { applyEnhancement(input, profile) }.getOrDefault(input)
    }

    private data class Profile(
        val unsharpSigma: Double,
        val unsharpAmount: Double,
        val saturationLift: Double,
        // CLAHE (Contrast Limited Adaptive Histogram Equalization)
        // operates on local 8×8 tiles of the luma channel. Higher clip
        // limit = more aggressive local contrast lift. 0.0 disables
        // CLAHE entirely (used for the neutral default profile so the
        // backwards-compatible single-arg `enhance(input)` produces
        // pixel-identical output to the pre-Phase-12 implementation).
        val claheClipLimit: Double
    )

    private fun profileFor(categoryId: String?): Profile = when (categoryId) {
        "henna" -> Profile(unsharpSigma = 1.0, unsharpAmount = 0.65, saturationLift = 1.05, claheClipLimit = 1.6)
        "abaya" -> Profile(unsharpSigma = 1.3, unsharpAmount = 0.50, saturationLift = 1.18, claheClipLimit = 2.2)
        "thob_sudani" -> Profile(unsharpSigma = 1.2, unsharpAmount = 0.55, saturationLift = 1.20, claheClipLimit = 2.2)
        "walls" -> Profile(unsharpSigma = 1.6, unsharpAmount = 0.30, saturationLift = 1.06, claheClipLimit = 1.4)
        else -> Profile(
            unsharpSigma = UNSHARP_SIGMA,
            unsharpAmount = UNSHARP_AMOUNT,
            saturationLift = SATURATION_LIFT,
            claheClipLimit = 0.0
        )
    }

    private fun applyEnhancement(input: Bitmap, profile: Profile): Bitmap = matScope {
        val src = take(Mat())
        Utils.bitmapToMat(input, src)

        // Strip alpha into its own channel so RGB ops don't accidentally
        // smear it. We restore it at the end so transparent regions stay
        // transparent (the converter flow's segmented output relies on
        // this).
        val rgb = take(Mat())
        Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)

        // 0. (optional) CLAHE local contrast in LAB-L space. Lifts
        // shadowed regions without crushing highlights — much better
        // than a global brightness curve for outputs with uneven
        // lighting (very common after RMBG cuts which can leave faint
        // halos and uneven cast around the subject). Skipped (clip 0.0)
        // for the neutral default profile so backwards-compatible
        // single-arg `enhance(input)` is pixel-identical to pre-Phase-12.
        val preSharp = if (profile.claheClipLimit > 0.0) {
            applyClahe(rgb, profile.claheClipLimit)
        } else rgb

        // 1. Unsharp mask. Blur kernel σ tuned per-category — bigger σ
        // would over-smooth the source, smaller wouldn't lift enough
        // micro-contrast. The amount controls how strongly the high-pass
        // is mixed back in.
        val blurred = take(Mat())
        Imgproc.GaussianBlur(preSharp, blurred, UNSHARP_KERNEL, profile.unsharpSigma)
        val sharpened = take(Mat())
        Core.addWeighted(preSharp, 1.0 + profile.unsharpAmount, blurred, -profile.unsharpAmount, 0.0, sharpened)

        // 2. Saturation lift via HSV. OpenCV's 8U HSV stores S in
        // [0, 255], so we scale by the per-category factor and clamp.
        val hsv = take(Mat())
        Imgproc.cvtColor(sharpened, hsv, Imgproc.COLOR_RGB2HSV)
        val channels = ArrayList<Mat>().also { Core.split(hsv, it) }
        channels.forEach { take(it) }
        val saturationLifted = take(Mat())
        Core.multiply(channels[1], Scalar.all(profile.saturationLift), saturationLifted)
        Core.min(saturationLifted, Scalar.all(255.0), saturationLifted)
        val saturationByte = take(Mat())
        saturationLifted.convertTo(saturationByte, CvType.CV_8UC1)
        val merged = take(Mat())
        Core.merge(listOf(channels[0], saturationByte, channels[2]), merged)
        val polishedRgb = take(Mat())
        Imgproc.cvtColor(merged, polishedRgb, Imgproc.COLOR_HSV2RGB)

        // 3. Restore the original alpha channel so the polished bitmap
        // has the same transparency as the input.
        val srcChannels = ArrayList<Mat>().also { Core.split(src, it) }
        srcChannels.forEach { take(it) }
        val polishedChannels = ArrayList<Mat>().also { Core.split(polishedRgb, it) }
        polishedChannels.forEach { take(it) }
        val withAlpha = take(Mat())
        Core.merge(
            listOf(
                polishedChannels[0],
                polishedChannels[1],
                polishedChannels[2],
                srcChannels[3]
            ),
            withAlpha
        )

        val result = Bitmap.createBitmap(withAlpha.cols(), withAlpha.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(withAlpha, result)
        result
    }

    /**
     * CLAHE local contrast lift in LAB-L colour space. Returns a new
     * RGB Mat (registered with the surrounding [MatScope]) — does NOT
     * mutate [rgb]. Operating on the L channel alone preserves hue +
     * chroma, so saturated regions don't shift colour under the lift.
     *
     * Extracted from `applyEnhancement` in Phase 13 as a readability
     * win; semantics are unchanged. Pass a positive [clipLimit] — the
     * `clipLimit > 0` check still belongs at the call site so the
     * neutral-default profile skips the LAB round-trip entirely.
     */
    private fun MatScope.applyClahe(rgb: Mat, clipLimit: Double): Mat {
        val lab = take(Mat())
        Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)
        val labChannels = ArrayList<Mat>().also { Core.split(lab, it) }
        labChannels.forEach { take(it) }
        val lEqualized = take(Mat())
        Imgproc.createCLAHE(clipLimit, CLAHE_TILE_GRID).apply(labChannels[0], lEqualized)
        val mergedLab = take(Mat())
        Core.merge(listOf(lEqualized, labChannels[1], labChannels[2]), mergedLab)
        val clahed = take(Mat())
        Imgproc.cvtColor(mergedLab, clahed, Imgproc.COLOR_Lab2RGB)
        return clahed
    }

    private companion object {
        // Unsharp parameters. AMOUNT is the only knob worth exposing if
        // a future settings panel wants user control; KERNEL and SIGMA
        // pair together to define the blur strength.
        val UNSHARP_KERNEL: Size = Size(0.0, 0.0) // 0,0 → derive from sigma
        const val UNSHARP_SIGMA: Double = 1.4
        const val UNSHARP_AMOUNT: Double = 0.45

        // Saturation lift in HSV V-channel space. 1.10 is a perceptual
        // sweet spot — enough to feel "polished" but not enough to push
        // skin tones into uncanny territory.
        const val SATURATION_LIFT: Double = 1.10

        // CLAHE tile grid. 8×8 is the OpenCV default and works well at
        // our typical ~1024-px enhancer input — small enough to lift
        // local shadows, big enough not to introduce visible tile
        // boundaries in flat regions.
        val CLAHE_TILE_GRID: Size = Size(8.0, 8.0)
    }
}
