package com.mawaai.love.app.design.ai.quality

import com.mawaai.love.app.design.ai.analysis.NormalizedRect
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MT-030 acceptance: the heuristic pre-check is deterministic, gated on the
 * exact thresholds from Lovable's render.functions.ts heuristic block, and
 * produces a [RenderQuality] shaped exactly like the AI reviewer's output.
 */
class HeuristicQualityCheckTest {

    @Test
    fun `high-fit analysis passes with score 100 and zero blockers`() {
        val analysis = analysisWithFit(scaleMatch = 10, densityMatch = 10, styleCompat = 10)

        val quality = HeuristicQualityCheck.evaluate(analysis)

        assertTrue("passes", quality.passed)
        assertEquals("score = (30/30) * 100 = 100", 100, quality.compositionPreservation)
        assertEquals("score = 100", 100, quality.surfaceFit)
        assertEquals("score = 100", 100, quality.lightingRealism)
        assertTrue("no issues", quality.issues.isEmpty())
    }

    @Test
    fun `scale_match below 4 produces a scale blocker`() {
        val analysis = analysisWithFit(scaleMatch = 3, densityMatch = 8, styleCompat = 8)

        val quality = HeuristicQualityCheck.evaluate(analysis)

        assertFalse("blocks render", quality.passed)
        assertTrue(
            "scale blocker present",
            quality.issues.any { "Scale match too low" in it },
        )
    }

    @Test
    fun `density_match below 4 produces a density blocker`() {
        val analysis = analysisWithFit(scaleMatch = 8, densityMatch = 3, styleCompat = 8)

        val quality = HeuristicQualityCheck.evaluate(analysis)

        assertFalse("blocks render", quality.passed)
        assertTrue(
            "density blocker present",
            quality.issues.any { "density exceeds" in it },
        )
    }

    @Test
    fun `AI-supplied template_fit blockers are appended (capped at 3)`() {
        val analysis = analysisWithFit(
            scaleMatch = 8,
            densityMatch = 8,
            styleCompat = 8,
            blockers = listOf("a", "b", "c", "d", "e"),
        )

        val quality = HeuristicQualityCheck.evaluate(analysis)

        assertEquals("capped at 3", 3, quality.issues.size)
        assertEquals(listOf("a", "b", "c"), quality.issues)
    }

    @Test
    fun `same analysis produces bit-equal RenderQuality (determinism)`() {
        val analysis = analysisWithFit(scaleMatch = 7, densityMatch = 7, styleCompat = 7)

        val first = HeuristicQualityCheck.evaluate(analysis)
        val second = HeuristicQualityCheck.evaluate(analysis)

        assertEquals(first, second)
    }

    private fun analysisWithFit(
        scaleMatch: Int,
        densityMatch: Int,
        styleCompat: Int,
        blockers: List<String> = emptyList(),
    ): SketchAnalysis = SketchAnalysis(
        artStyle = "test",
        culturalOrigin = "test",
        symmetry = SketchAnalysis.Symmetry("bilateral", 70, "none", ""),
        lineQuality = SketchAnalysis.LineQuality(7, 7, 3, ""),
        composition = SketchAnalysis.Composition(0.5f, 0.5f, 7, 35, ""),
        sketchStructure = SketchAnalysis.SketchStructure(emptyList(), "", "", emptyList()),
        templateMapping = SketchAnalysis.TemplateMapping("", "", emptyList(), "", "", ""),
        templateFit = SketchAnalysis.TemplateFit(
            scaleMatch = scaleMatch,
            densityMatch = densityMatch,
            styleCompat = styleCompat,
            blockers = blockers,
        ),
        findings = listOf(
            SketchAnalysis.Finding(
                id = "f_001",
                severity = SketchAnalysis.Finding.Severity.INFO,
                region = NormalizedRect(0.1f, 0.1f, 0.8f, 0.8f),
                what = "",
                why = "",
                principle = "",
                culturalContext = "",
            ),
        ),
    )
}
