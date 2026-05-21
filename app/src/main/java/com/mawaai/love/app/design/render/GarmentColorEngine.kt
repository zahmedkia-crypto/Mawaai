package com.mawaai.love.app.design.render

import android.graphics.Bitmap
import android.graphics.PointF
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import com.mawaai.love.app.design.ai.processors.BlendMode
import com.mawaai.love.app.design.ai.processors.BlendModeProcessor
import com.mawaai.love.app.design.ai.processors.MatScope
import com.mawaai.love.app.design.ai.processors.PerspectiveWarpProcessor
import com.mawaai.love.app.design.ai.processors.complement
import com.mawaai.love.app.design.ai.processors.matScope
import com.mawaai.love.app.design.domain.model.HslColor
import com.mawaai.love.app.design.domain.model.Template
import com.mawaai.love.app.design.domain.model.TemplateMetadata
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
 * Replaces the garment color of an abaya / Sudanese thob template while
 * preserving fabric folds, shadows, and highlights, then re-composites the
 * already-processed design overlay on top. Driven by [HslColor] from the
 * Customize screen.
 *
 * Pipeline per call:
 *  1. Look up (or build & cache) the warped design overlay for this
 *     `(template, design)` pair. Warping is the heaviest single op
 *     (~50 ms at 1024 px) and depends only on the template's quad — never
 *     on the user's color choice — so it's reused across every slider tick.
 *  2. Look up (or build & cache) the fabric mask. Prefers
 *     `templates/<categoryId>/<id>.mask.png` if it ships; otherwise derives
 *     a heuristic mask from the dominant hue in a centered fabric band of
 *     the base template.
 *  3. Convert the base template to HSV float, replace H + S inside the mask
 *     using the user-picked target color, scale V by the target lightness.
 *     The original V's spatial variation (folds, shadows, embroidery) is
 *     preserved — that's what makes the recolored garment still look like
 *     fabric instead of a flat fill.
 *  4. Blend the cached warped design overlay on top of the recolored base
 *     using the same category-driven blend rules as [TemplateCompositor]
 *     (FABRIC_REALISTIC for cloth, MULTIPLY for skin, NORMAL for walls).
 *
 * On a hard OpenCV failure ([OpenCVBootstrap.ensureLoaded] returning false),
 * the engine returns a non-recolored composite — same path as
 * [TemplateCompositor.compose] — so the user still sees the design on the
 * template, just without the color shift. Never throws to the UI.
 *
 * Cache lifecycle: the cache survives across slider ticks but is cleared
 * on [invalidate]. The Customize screen calls [invalidate] from
 * `onCleared` so a fresh navigation into a different template starts from
 * a clean slate.
 */
@Singleton
class GarmentColorEngine @Inject constructor(
    private val templates: TemplateAssetManager,
    private val warp: PerspectiveWarpProcessor,
    private val blender: BlendModeProcessor
) {

    private data class WarpKey(val templateId: String, val designIdentity: Int)

    // All cache access goes through `synchronized(cacheLock) { ... }`
    // blocks. Plain blocking sync is fine here — the operations inside
    // are constant-time map reads/writes (no I/O, no JNI work) so the
    // contention window is negligible. Using a `Mutex` instead would
    // bar the non-suspend [invalidate] call from `ViewModel.onCleared`,
    // and `runBlocking` on Main is worse than this microsecond block.
    private val cacheLock = Any()
    private val warpedDesignCache = mutableMapOf<WarpKey, Bitmap>()
    // Heuristic-derived masks only. Asset-backed `<id>.mask.png` bitmaps
    // are owned + cached by [TemplateAssetManager] and reloaded each call;
    // double-caching them here would risk a use-after-recycle when the
    // shared LRU evicts the bitmap underneath us.
    private val heuristicMaskCache = mutableMapOf<String, Bitmap>()

    /**
     * Returns a fresh composite where the garment color has been replaced
     * with [target]. The caller owns the returned bitmap and must recycle
     * it — either by handing it to a Coil `AsyncImage` (which holds a
     * reference) or via [Bitmap.recycle] after persisting. The cached
     * intermediates (warped design, mask) survive the call and are reused
     * on the next [recolor] for the same `(template, design)` pair.
     *
     * [blendIntensity] in `[0, 1]` controls how strongly the fabric's
     * original tonal variation is preserved. 1 = full replacement of H+S;
     * 0 = no change at all (the template's original color is preserved
     * exactly). The Customize screen pins this at 1 today; the parameter
     * is exposed so a future "intensity" knob can be wired without a
     * breaking change.
     */
    suspend fun recolor(
        template: Template,
        design: Bitmap,
        target: HslColor,
        blendIntensity: Float = 1f
    ): Bitmap = withContext(Dispatchers.Default) {
        val base = templates.loadBitmap(template)
        val rules = blendRulesFor(template.categoryId, template.metadata)

        if (!OpenCVBootstrap.ensureLoaded()) {
            // OpenCV isn't usable on this device: skip the recolor pass
            // entirely and just return a normal composite. Better to show
            // the original-color garment than to crash mid-Customize.
            val warped = obtainWarpedDesign(template, design, base, rules.insetFraction)
            return@withContext blender.blend(base, warped, rules.mode, rules.overlayAlpha)
        }

        val mask = obtainMaskBitmap(template, base)
        val recoloredBase = recolorBaseInPlace(base, mask, target, blendIntensity.coerceIn(0f, 1f))
        val warpedDesign = obtainWarpedDesign(template, design, base, rules.insetFraction)
        val composite = blender.blend(recoloredBase, warpedDesign, rules.mode, rules.overlayAlpha)
        if (composite !== recoloredBase && !recoloredBase.isRecycled) recoloredBase.recycle()
        composite
    }

    /**
     * Returns the dominant fabric color of [template] as the seed value
     * for the Customize screen's color sliders. When the template ships
     * a mask, the sample is taken inside the masked region; otherwise
     * the heuristic centered-band fallback is used. Falls back to the
     * `argb` of the template's category fabric default when OpenCV is
     * unavailable.
     */
    suspend fun sampleSeedColor(template: Template): HslColor = withContext(Dispatchers.Default) {
        if (!OpenCVBootstrap.ensureLoaded()) return@withContext DEFAULT_FALLBACK_COLOR
        val base = templates.loadBitmap(template)
        val mask = obtainMaskBitmap(template, base)
        runCatching { sampleColorFromMaskedRegion(base, mask) }
            .getOrDefault(DEFAULT_FALLBACK_COLOR)
    }

    private fun sampleColorFromMaskedRegion(base: Bitmap, mask: Bitmap): HslColor = matScope {
        val src = take(Mat())
        Utils.bitmapToMat(base, src)
        val rgb = take(Mat())
        Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
        val rgbF = take(Mat())
        rgb.convertTo(rgbF, CvType.CV_32FC3, 1.0 / 255.0)
        val hsvF = take(Mat())
        Imgproc.cvtColor(rgbF, hsvF, Imgproc.COLOR_RGB2HSV)

        val maskBitmap = ensureArgb(mask, base.width, base.height)
        val maskMat = take(Mat())
        Utils.bitmapToMat(maskBitmap, maskMat)
        if (maskBitmap !== mask) maskBitmap.recycle()
        val maskWeight = take(Mat())
        extractMaskWeight(maskMat, maskWeight)
        // Treat any non-zero mask pixel as "include in sample"; binarize
        // for Core.mean's mask argument which expects CV_8UC1 with
        // strictly 0 / non-zero semantics.
        val binMask = take(Mat())
        Imgproc.threshold(maskWeight, binMask, 1.0, 255.0, Imgproc.THRESH_BINARY)

        val mean = Core.mean(hsvF, binMask)
        val h = mean.`val`[0].toFloat()
        val s = mean.`val`[1].toFloat().coerceIn(0f, 1f)
        val v = mean.`val`[2].toFloat().coerceIn(0f, 1f)
        // HSV V → HSL L using the standard conversion that holds the
        // fabric's perceptual brightness while letting the slider lean
        // toward white/black around it.
        val l = v * (1f - s / 2f)
        val sl = if (l == 0f || l == 1f) 0f else (v - l) / minOf(l, 1f - l)
        HslColor(hue = h, saturation = sl.coerceIn(0f, 1f), lightness = l.coerceIn(0f, 1f))
    }

    /**
     * Drops every cached warped design and heuristic mask. Synchronized
     * so the Customize ViewModel can call it from `onCleared` (where
     * `viewModelScope` is already cancelled and a `suspend` API isn't
     * runnable). Asset-backed masks are owned by [TemplateAssetManager]'s
     * LRU and are intentionally not touched here.
     */
    fun invalidate() = synchronized(cacheLock) {
        warpedDesignCache.values.forEach { if (!it.isRecycled) it.recycle() }
        warpedDesignCache.clear()
        heuristicMaskCache.values.forEach { if (!it.isRecycled) it.recycle() }
        heuristicMaskCache.clear()
    }

    /**
     * Returns the fabric mask for [template]. Prefers the per-template
     * `<id>.mask.png` asset (re-fetched from [TemplateAssetManager]'s
     * LRU on every call — fast hit, sound lifetime) and falls back to a
     * heuristic mask derived from the centered fabric band of [base].
     * Heuristic results are cached locally because the derivation is
     * heavier than the asset path.
     */
    private suspend fun obtainMaskBitmap(template: Template, base: Bitmap): Bitmap {
        val asset = runCatching { templates.loadMaskBitmap(template) }.getOrNull()
        if (asset != null) return asset
        synchronized(cacheLock) {
            heuristicMaskCache[template.id]?.takeIf { !it.isRecycled }?.let { return it }
        }
        val derived = deriveHeuristicMask(base)
        return synchronized(cacheLock) {
            // Race: another slider tick may have computed and cached its
            // own mask while we were deriving. Use the existing entry if
            // present and recycle ours; first-write-wins is fine since
            // any heuristic mask for this template is equivalent.
            val existing = heuristicMaskCache[template.id]
            if (existing != null && !existing.isRecycled) {
                if (!derived.isRecycled) derived.recycle()
                existing
            } else {
                heuristicMaskCache[template.id] = derived
                derived
            }
        }
    }

    /**
     * Cached warped-design lookup. The key is `(template.id, identityHash)`
     * — two different design bitmaps for the same template will not
     * collide. The Customize screen passes the same processed-design
     * bitmap reference on every slider tick, so the warp runs exactly
     * once per screen visit.
     */
    private suspend fun obtainWarpedDesign(
        template: Template,
        design: Bitmap,
        base: Bitmap,
        insetFraction: Float
    ): Bitmap {
        val key = WarpKey(template.id, System.identityHashCode(design))
        synchronized(cacheLock) {
            warpedDesignCache[key]?.takeIf { !it.isRecycled }?.let { return it }
        }
        val quad = quadFor(
            normalizedQuad = template.metadata?.targetQuad,
            categoryDefault = TemplateQuadDefaults.forCategory(template.categoryId),
            width = base.width,
            height = base.height,
            insetFraction = insetFraction
        )
        val warped = warp.warp(
            source = design,
            destinationSize = Size(base.width.toDouble(), base.height.toDouble()),
            destinationQuad = quad
        )
        return synchronized(cacheLock) {
            val existing = warpedDesignCache[key]
            if (existing != null && !existing.isRecycled) {
                if (!warped.isRecycled) warped.recycle()
                existing
            } else {
                warpedDesignCache[key] = warped
                warped
            }
        }
    }

    /**
     * Returns a NEW bitmap holding the recolored base. The base passed in
     * is owned by [TemplateAssetManager]'s LRU and must not be mutated.
     * Recolors only pixels where the mask alpha > 0; in those pixels:
     *  - hue replaced by [target.hue]
     *  - saturation lerped toward [target.saturation] by [intensity]
     *  - value (brightness) scaled by `target.lightness × 2` so the
     *    original fold/shadow variation in V is preserved while the
     *    overall lightness shifts with the slider.
     *
     * Operates in 32F HSV space throughout (RGB pre-scaled to `[0, 1]`
     * before the conversion) so hue is read in degrees and saturation /
     * value in `[0, 1]`. Mixing 8U HSV — where hue compresses to
     * `[0, 180]` — with the user's `[0, 360)` hue would silently double
     * every color the slider produces.
     */
    private fun recolorBaseInPlace(
        base: Bitmap,
        mask: Bitmap,
        target: HslColor,
        intensity: Float
    ): Bitmap {
        // Defensive guard: if intensity == 0 there's nothing to do — return
        // a pristine copy so the caller's `recycle` contract is uniform.
        if (intensity <= 0f) return base.copy(Bitmap.Config.ARGB_8888, false)

        return matScope {
            val src = take(Mat())
            Utils.bitmapToMat(base, src)

            val rgb = take(Mat())
            Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
            val rgbF = take(Mat())
            rgb.convertTo(rgbF, CvType.CV_32FC3, 1.0 / 255.0)

            val hsvF = take(Mat())
            Imgproc.cvtColor(rgbF, hsvF, Imgproc.COLOR_RGB2HSV)

            val channels = ArrayList<Mat>().also { Core.split(hsvF, it) }
            channels.forEach { take(it) }
            val h = channels[0]
            val s = channels[1]
            val v = channels[2]

            val maskBitmap = ensureArgb(mask, base.width, base.height)
            val maskMat = take(Mat())
            Utils.bitmapToMat(maskBitmap, maskMat)
            if (maskBitmap !== mask) maskBitmap.recycle()
            // The fabric mask carries its weight in the alpha channel
            // (per `Bitmap.extractAlpha + ARGB_8888` convention) when it
            // came from `<id>.mask.png`, but a single-channel grayscale
            // mask copied to ARGB has its weight in any channel. Use the
            // max channel so both authoring conventions work.
            val maskAlpha = take(Mat())
            extractMaskWeight(maskMat, maskAlpha)

            // Convert mask weight 0..255 → 0..1 float, then multiply by
            // intensity to get the per-pixel mix weight w in [0, 1].
            val w = take(Mat())
            maskAlpha.convertTo(w, CvType.CV_32F, intensity / 255.0)

            // Hue is in [0, 360) for 32F HSV. Replace H wholesale inside
            // the mask:  H' = H * (1 - w) + targetHue * w.
            val targetHue = ((target.hue % 360f) + 360f) % 360f
            blendChannel(h, targetHue.toDouble(), w)

            // Saturation: lerp toward target.saturation in [0, 1].
            blendChannel(s, target.saturation.toDouble().coerceIn(0.0, 1.0), w)

            // Value (brightness): scale by `target.lightness × 2`. L=0.5 →
            // V unchanged, L=1 → V doubled (clamped to 1), L=0 → V → 0.
            // Preserves the relative fold/shadow structure while the
            // overall garment lightness tracks the slider.
            scaleChannel(v, w, target.lightness.toDouble() * 2.0, ceiling = 1.0)

            val merged = take(Mat())
            Core.merge(channels, merged)

            val recoloredRgbF = take(Mat())
            Imgproc.cvtColor(merged, recoloredRgbF, Imgproc.COLOR_HSV2RGB)

            val recoloredRgb = take(Mat())
            recoloredRgbF.convertTo(recoloredRgb, CvType.CV_8UC3, 255.0)

            val recoloredRgba = take(Mat())
            Imgproc.cvtColor(recoloredRgb, recoloredRgba, Imgproc.COLOR_RGB2RGBA)
            // Restore the original alpha channel (the base is opaque, so
            // this is a no-op for shipped templates, but keeps the
            // function correct for future transparent assets).
            val srcChannels = ArrayList<Mat>().also { Core.split(src, it) }
            srcChannels.forEach { take(it) }
            val outChannels = ArrayList<Mat>().also { Core.split(recoloredRgba, it) }
            outChannels.forEach { take(it) }
            val outWithAlpha = take(Mat())
            Core.merge(listOf(outChannels[0], outChannels[1], outChannels[2], srcChannels[3]), outWithAlpha)

            val result = Bitmap.createBitmap(outWithAlpha.cols(), outWithAlpha.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(outWithAlpha, result)
            result
        }
    }

    /**
     * Heuristic fabric mask used when the template doesn't ship a
     * hand-authored `<id>.mask.png`. Samples the dominant hue in a
     * centered band (40–80% Y, 20–80% X) — empirically the area where
     * the abaya / thob fabric is most likely to fill the frame across
     * the bundled photos — then builds a soft mask of pixels within an
     * HSV tolerance of that median color.
     *
     * Returns an ARGB_8888 bitmap with weight encoded in all channels.
     * Skin tones (S < 0.25, V > 0.4) are explicitly excluded so the
     * recolor doesn't bleed onto faces / hands.
     */
    private fun deriveHeuristicMask(base: Bitmap): Bitmap = matScope {
        val src = take(Mat())
        Utils.bitmapToMat(base, src)
        val rgb = take(Mat())
        Imgproc.cvtColor(src, rgb, Imgproc.COLOR_RGBA2RGB)
        val rgbF = take(Mat())
        rgb.convertTo(rgbF, CvType.CV_32FC3, 1.0 / 255.0)
        val hsvF = take(Mat())
        Imgproc.cvtColor(rgbF, hsvF, Imgproc.COLOR_RGB2HSV)

        val (centerH, centerS) = sampleDominantFabricHs(hsvF)

        val width = base.width
        val height = base.height

        val channels = ArrayList<Mat>().also { Core.split(hsvF, it) }
        channels.forEach { take(it) }
        val hueMat = channels[0]
        val satMat = channels[1]
        val valMat = channels[2]

        // Hue is angular: distance must wrap at 360°. Compute |H - centerH|
        // as min(d, 360 - d), then threshold by HUE_TOLERANCE.
        val hueDiff = take(Mat())
        Core.subtract(hueMat, Scalar.all(centerH), hueDiff)
        Core.absdiff(hueDiff, Scalar.all(0.0), hueDiff)
        val wrap = take(Mat())
        // wrap = 360 - hueDiff. OpenCV 4.9.0 has no `subtract(Scalar, Mat,
        // Mat)` overload, so compute via `convertTo(dst, -1, -1, 360)`
        // which yields `dst = src * -1 + 360 = 360 - src`, preserving src's
        // depth (same trick as the blend processor's `complement`).
        hueDiff.convertTo(wrap, -1, -1.0, 360.0)
        Core.min(hueDiff, wrap, hueDiff)

        val hueOk = take(Mat())
        Core.compare(hueDiff, Scalar.all(HUE_TOLERANCE_DEG), hueOk, Core.CMP_LE)

        // 32F HSV: S is in [0, 1]. Reject sub-skin saturation; that's
        // primarily skin / hair / shadow which we never want to recolor.
        val satOk = take(Mat())
        Core.compare(satMat, Scalar.all(SKIN_SAT_MAX_F), satOk, Core.CMP_GE)

        val valOk = take(Mat())
        val valLow = take(Mat())
        Core.compare(valMat, Scalar.all(VAL_MIN_F), valLow, Core.CMP_GE)
        val valHigh = take(Mat())
        Core.compare(valMat, Scalar.all(VAL_MAX_F), valHigh, Core.CMP_LE)
        Core.bitwise_and(valLow, valHigh, valOk)

        val maskMat = take(Mat(height, width, CvType.CV_8UC1, Scalar.all(0.0)))
        Core.bitwise_and(hueOk, satOk, maskMat)
        Core.bitwise_and(maskMat, valOk, maskMat)

        // Saturation similarity: pixels with similar S to the sampled
        // fabric are more likely to BE fabric.
        val satDiff = take(Mat())
        Core.subtract(satMat, Scalar.all(centerS), satDiff)
        Core.absdiff(satDiff, Scalar.all(0.0), satDiff)
        val satNear = take(Mat())
        Core.compare(satDiff, Scalar.all(SAT_TOLERANCE_F), satNear, Core.CMP_LE)
        Core.bitwise_and(maskMat, satNear, maskMat)

        // Soften the mask edge so the recolor doesn't show a hard
        // boundary. Larger kernel = smoother but may bleed onto
        // adjacent skin.
        Imgproc.GaussianBlur(maskMat, maskMat, MASK_BLUR_KERNEL, 0.0)

        val rgba = take(Mat())
        Core.merge(listOf(maskMat, maskMat, maskMat, maskMat), rgba)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, result)
        result
    }

    /**
     * Samples the median hue and saturation of pixels inside a centered
     * fabric band, excluding skin-tone-ish regions. Returns
     * `(hue [0, 360), sat [0, 1])`.
     */
    private fun MatScope.sampleDominantFabricHs(hsvFloat: Mat): Pair<Double, Double> {
        val rows = hsvFloat.rows()
        val cols = hsvFloat.cols()
        val yStart = (rows * 0.40).toInt().coerceAtLeast(0)
        val yEnd = (rows * 0.80).toInt().coerceAtMost(rows)
        val xStart = (cols * 0.20).toInt().coerceAtLeast(0)
        val xEnd = (cols * 0.80).toInt().coerceAtMost(cols)
        val rect = org.opencv.core.Rect(xStart, yStart, xEnd - xStart, yEnd - yStart)
        val band = take(Mat(hsvFloat, rect))

        val channels = ArrayList<Mat>().also { Core.split(band, it) }
        channels.forEach { take(it) }
        // Median is robust against the hand/face/background pixels
        // sneaking into the band; use the meanStdDev mean as a good-
        // enough approximation that's far cheaper than a true median.
        val meanH = Core.mean(channels[0]).`val`[0]
        val meanS = Core.mean(channels[1]).`val`[0]
        return meanH to meanS
    }

    /**
     * Linear interpolation: `dst = dst * (1 - w) + targetScalar * w`,
     * applied per-channel. Uses the shared
     * [com.mawaai.love.app.design.ai.processors.complement] helper for
     * the `1 - w` step (OpenCV 4.9.0 lacks a `subtract(Scalar, Mat, Mat)`
     * overload, so all three call sites in the codebase route through
     * the same `convertTo(-1, 1)` recipe).
     */
    private fun MatScope.blendChannel(channel: Mat, targetScalar: Double, w: Mat) {
        val invW = take(Mat())
        complement(w, invW)
        Core.multiply(channel, invW, channel)
        val targetW = take(Mat())
        Core.multiply(w, Scalar.all(targetScalar), targetW)
        Core.add(channel, targetW, channel)
    }

    /**
     * Scales a channel by `(1 - w) + factor * w`. Used for V — the
     * lightness slider multiplies the original V by the user-picked
     * factor, but only inside the mask. Result clamped to [ceiling]
     * (`1.0` for 32F-normalized HSV, `255.0` for 8U HSV).
     */
    private fun MatScope.scaleChannel(channel: Mat, w: Mat, factor: Double, ceiling: Double) {
        val multiplier = take(Mat())
        Core.multiply(w, Scalar.all(factor - 1.0), multiplier)
        Core.add(multiplier, Scalar.all(1.0), multiplier)
        Core.multiply(channel, multiplier, channel)
        Core.min(channel, Scalar.all(ceiling), channel)
    }

    /**
     * Pulls a single-channel mask weight from a multi-channel mask Mat.
     *
     * Authoring convention: shipped `<id>.mask.png` files are grayscale
     * (or RGB) where pixel value > 0 means "fabric here, recolor". The
     * R channel is sampled — for grayscale assets it equals G and B, so
     * the choice is arbitrary; for RGB-encoded masks the author should
     * use white pixels in the fabric region. The alpha channel is
     * ignored to keep the convention robust against `BitmapFactory`'s
     * implicit ARGB_8888 promotion of grayscale PNGs (which sets A=255
     * uniformly and would otherwise be misread as "everything is
     * fabric").
     *
     * Result is CV_8UC1 in `[0, 255]`.
     */
    private fun MatScope.extractMaskWeight(maskMat: Mat, dst: Mat) {
        val channels = ArrayList<Mat>().also { Core.split(maskMat, it) }
        channels.forEach { take(it) }
        channels[0].copyTo(dst)
    }

    private fun ensureArgb(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val sized = if (bitmap.width == width && bitmap.height == height) bitmap
        else Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (sized.config == Bitmap.Config.ARGB_8888) return sized
        val converted = sized.copy(Bitmap.Config.ARGB_8888, false)
        if (sized !== bitmap) sized.recycle()
        return converted
    }

    private fun blendRulesFor(categoryId: String, metadata: TemplateMetadata?): BlendRules {
        val defaults = when (categoryId) {
            "henna" -> BlendRules(BlendMode.MULTIPLY, 0.85, 0.18f)
            "abaya" -> BlendRules(BlendMode.FABRIC_REALISTIC, 0.85, 0.22f)
            "walls" -> BlendRules(BlendMode.NORMAL, 0.95, 0.12f)
            "thob_sudani" -> BlendRules(BlendMode.FABRIC_REALISTIC, 0.85, 0.20f)
            else -> BlendRules(BlendMode.NORMAL, 0.8, 0.15f)
        }
        if (metadata == null) return defaults
        return defaults.copy(
            mode = metadata.blendMode?.let { runCatching { BlendMode.valueOf(it.uppercase()) }.getOrNull() } ?: defaults.mode,
            overlayAlpha = metadata.overlayAlpha ?: defaults.overlayAlpha
        )
    }

    private fun quadFor(
        normalizedQuad: List<PointF>?,
        categoryDefault: List<PointF>?,
        width: Int,
        height: Int,
        insetFraction: Float
    ): List<PointF> {
        val source = normalizedQuad?.takeIf { it.size == 4 } ?: categoryDefault
        if (source != null && source.size == 4) {
            return source.map { pt ->
                PointF(
                    (pt.x * width).coerceIn(0f, width.toFloat()),
                    (pt.y * height).coerceIn(0f, height.toFloat())
                )
            }
        }
        val dx = width * insetFraction
        val dy = height * insetFraction
        return listOf(
            PointF(dx, dy),
            PointF(width - dx, dy),
            PointF(width - dx, height - dy),
            PointF(dx, height - dy)
        )
    }

    private data class BlendRules(
        val mode: BlendMode,
        val overlayAlpha: Double,
        val insetFraction: Float
    )

    private companion object {
        // 32F HSV pipeline: H in [0, 360), S/V in [0, 1].
        const val HUE_TOLERANCE_DEG = 28.0   // degrees of hue half-width
        const val SAT_TOLERANCE_F = 0.43     // saturation half-width
        const val SKIN_SAT_MAX_F = 0.24      // exclude S < 0.24 (skin / wall / hair)
        const val VAL_MIN_F = 0.07           // exclude near-black background
        const val VAL_MAX_F = 0.96           // exclude near-white sky / highlights

        // Soft mask edge — kernel is small so we don't blur away
        // detail at thin garment boundaries.
        val MASK_BLUR_KERNEL: Size = Size(11.0, 11.0)

        // Fallback when OpenCV isn't loaded and we can't sample the
        // template — picks the deep midnight-blue from MawaaiColors that
        // shows up across the rest of the design feature.
        val DEFAULT_FALLBACK_COLOR: HslColor = HslColor(
            hue = 240f,
            saturation = 0.38f,
            lightness = 0.16f
        )
    }
}


