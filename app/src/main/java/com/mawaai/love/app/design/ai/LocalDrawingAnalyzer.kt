package com.mawaai.love.app.design.ai

import android.graphics.Bitmap
import com.mawaai.love.app.design.domain.model.DrawingAction
import com.mawaai.love.app.design.domain.model.DrawingAnalysis
import com.mawaai.love.app.design.domain.model.DrawingSuggestion
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cheap, offline heuristics that look at a flattened drawing bitmap and
 * produce 2-5 Arabic [DrawingSuggestion]s. Used as a fallback when Gemini
 * Vision is not configured or its network call failed. Never blocks —
 * completes in < 50 ms on a 1024×1024 canvas because it samples a 32×32
 * grid via [sampleDrawingStats].
 *
 * Phase 4 rewrite: previously returned `List<String>`. Now returns a
 * structured [DrawingAnalysis] so each suggestion can carry an optional
 * one-tap [DrawingAction] that the Recommendations screen surfaces as
 * an "Apply" button.
 */
@Singleton
class LocalDrawingAnalyzer @Inject constructor() {

    fun analyze(bitmap: Bitmap, categoryId: String?): DrawingAnalysis {
        if (bitmap.isRecycled) return DrawingAnalysis.EMPTY
        val stats = sampleDrawingStats(bitmap)
        val out = mutableListOf<DrawingSuggestion>()

        // Density.
        when {
            stats.coverage < 0.05f -> out += DrawingSuggestion(
                message = "الرسمة فيها مساحة فارغة كبيرة — جرّبي إضافة عناصر صغيرة في الزوايا."
            )
            stats.coverage > 0.65f -> out += DrawingSuggestion(
                message = "التصميم ممتلئ جداً — يمكنكِ تفريغ بعض المساحات لراحة العين."
            )
        }

        // Color variety.
        when {
            stats.uniqueHues < 2 -> out += DrawingSuggestion(
                message = "اللون واحد فقط — لمسة وردية ناعمة تضفي توازناً.",
                action = DrawingAction.AddAccentColor
            )
            stats.uniqueHues > 6 -> out += DrawingSuggestion(
                message = "عدد الألوان كبير — اختاري ٣ إلى ٤ ألوان رئيسية فقط."
            )
        }

        // Symmetry hints. Two tiers based on asymmetry magnitude:
        //  - moderate (0.25..0.45): user *probably* meant it symmetric
        //    but drift crept in. Soft-mirror via FixSymmetry preserves
        //    most of the original strokes.
        //  - large (>0.45): destructive MirrorHorizontally is the right
        //    fix; the offer "tap to mirror" is the actual user intent.
        when {
            stats.asymmetryScore in SOFT_SYMMETRY_LOW..SOFT_SYMMETRY_HIGH -> out += DrawingSuggestion(
                message = "اختلال طفيف في التماثل — اضبطيه بنعومة دون فقدان رسمتك.",
                action = DrawingAction.FixSymmetry
            )
            stats.asymmetryScore > SOFT_SYMMETRY_HIGH -> out += DrawingSuggestion(
                message = "التصميم غير متوازن — تماثل أفقي بنقرة واحدة.",
                action = DrawingAction.MirrorHorizontally
            )
        }

        // Phase 20 — print readiness. A high fraction of "thin" sample
        // points (inked but with ≤ 1 of 4 cardinal neighbours also
        // inked) means the drawing has many 1-pixel-wide strokes that
        // a fabric printer at 12 cm garment scale will lose. Offer to
        // thicken them with a 3×3 dilation.
        if (stats.coverage > 0.05f && stats.thinStrokeFraction > 0.55f) {
            out += DrawingSuggestion(
                message = "خطوط رفيعة قد لا تظهر عند الطباعة — كثّفيها قليلاً.",
                action = DrawingAction.ThickenThinStrokes
            )
        }

        // Phase 20 — palette coherence. When more than 4 distinct hue
        // buckets each carry ≥ 5% of the inked area, the palette is
        // visually busy. Offer to nudge outliers toward the top 3
        // dominant hues. Sub-2-hue drawings are handled by the existing
        // [AddAccentColor] path above; this one targets the chaotic
        // middle-ground.
        if (stats.coverage > 0.10f && stats.paletteCoherence < CHAOTIC_PALETTE_THRESHOLD) {
            out += DrawingSuggestion(
                message = "الباليتة متفرقة — قرّبي الألوان إلى ثلاث درجات رئيسية.",
                action = DrawingAction.BalancePalette
            )
        }

        // Composition — rule of thirds. The "focal offset" is the
        // distance from the inked centroid to the nearest of the four
        // rule-of-thirds intersection points (in normalized units, where
        // 1.0 == half the diagonal). Anything > 0.18 means the subject
        // is significantly off-axis from both the centre AND the four
        // power-points — a strong signal to nudge the focal element.
        if (stats.coverage > 0.05f && stats.focusOffset > 0.18f) {
            out += DrawingSuggestion(
                message = "حرّكي العنصر الرئيسي قليلاً نحو إحدى نقاط التقاطع لتكوين أفضل."
            )
        }

        // Edge quality — jagged vs smooth. Strokes that flip between
        // dark and light at every sample neighbour read as "shaky" /
        // "pixelated". Suggest a softer brush so the rendered output
        // (especially after ControlNet) keeps clean lines.
        if (stats.coverage > 0.10f && stats.edgeJaggedness > 0.62f) {
            out += DrawingSuggestion(
                message = "الخطوط متقطعة قليلاً — جرّبي فرشاة أنعم لخطوط أنظف."
            )
        }

        // Brightness — too dark / too washed-out.
        when {
            stats.averageBrightness < 0.25f -> out += DrawingSuggestion(
                message = "الإضاءة قاتمة قليلاً — تفتيح خفيف يُبرز التفاصيل.",
                action = DrawingAction.LightenCanvas
            )
            stats.averageBrightness > 0.85f -> out += DrawingSuggestion(
                message = "الخلفية فاتحة جداً — تدرّج الحواف بظل خفيف.",
                action = DrawingAction.DarkenEdges
            )
        }

        // Polish: offer a background when the canvas has moderate
        // coverage. Pick solid cream for already-bright drawings (won't
        // over-brighten) and a rose-gold gradient otherwise (warms up
        // darker / mid-tone compositions).
        if (stats.coverage in 0.10f..0.55f) {
            out += if (stats.averageBrightness > 0.55f) {
                DrawingSuggestion(
                    message = "خلفية كريمية ناعمة تُبرز التفاصيل الدقيقة.",
                    action = DrawingAction.AddSolidBackground
                )
            } else {
                DrawingSuggestion(
                    message = "تدرّج وردي ذهبي يضفي لمسة رومانسية.",
                    action = DrawingAction.AddGradientBackground
                )
            }
        }

        // Category-specific extras. No structured action — category-
        // guided creative changes are too varied to auto-apply.
        when (categoryId) {
            "henna" -> out += DrawingSuggestion("للحناء: زيدي تفاصيل دقيقة عند أطراف الأصابع.")
            "abaya" -> out += DrawingSuggestion("للعباية: ركّزي على الياقة والأكمام أكثر.")
            "walls" -> out += DrawingSuggestion("للجدارية: اتركي إطاراً جانبياً فارغاً لتظهر مركزية التصميم.")
            "thob_sudani" -> out += DrawingSuggestion("للتوب السوداني: ركّزي على الفتلة والرقمة عند الأطراف.")
        }

        return DrawingAnalysis(
            suggestions = out.distinctBy { it.message }.take(MAX_SUGGESTIONS),
            source = DrawingAnalysis.Source.LOCAL
        )
    }

    private companion object {
        const val MAX_SUGGESTIONS = 5

        // Phase 20 — soft-symmetry threshold band. Asymmetry < 0.25
        // reads as "user meant this asymmetric, leave it alone".
        // Above 0.45 the user is unambiguously off-axis; the
        // destructive `MirrorHorizontally` is the right action there.
        // The middle band (0.25..0.45) is "drift on a meant-symmetric
        // piece" → soft-mirror via [DrawingAction.FixSymmetry].
        const val SOFT_SYMMETRY_LOW = 0.25f
        const val SOFT_SYMMETRY_HIGH = 0.45f

        // Palette coherence below this threshold triggers the
        // [DrawingAction.BalancePalette] suggestion. 0.55 means the
        // top-3 hue buckets must cover ≥ 55% of saturated pixels;
        // anything less is "spread too thin across the wheel".
        const val CHAOTIC_PALETTE_THRESHOLD = 0.55f
    }
}
