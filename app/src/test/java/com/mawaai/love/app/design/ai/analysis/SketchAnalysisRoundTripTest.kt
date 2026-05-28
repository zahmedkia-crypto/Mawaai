package com.mawaai.love.app.design.ai.analysis

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * MT-018 acceptance: Gson must round-trip the full SketchAnalysis schema
 * with bit-for-bit equality across all 8 nested data classes.
 *
 * Verifies parity with the Lovable Creative Studio Zod analysisSchema
 * (lib/analysis.functions.ts) ported verbatim to Kotlin.
 */
class SketchAnalysisRoundTripTest {

    private val gson = Gson()

    @Test
    fun `full SketchAnalysis round-trips through Gson without loss`() {
        val original = sampleAnalysis()
        val json = gson.toJson(original)
        val parsed = gson.fromJson(json, SketchAnalysis::class.java)

        assertEquals(original, parsed)
    }

    @Test
    fun `each nested data class survives serialization`() {
        val original = sampleAnalysis()
        val json = gson.toJson(original)
        val parsed = gson.fromJson(json, SketchAnalysis::class.java)

        assertEquals(original.symmetry, parsed.symmetry)
        assertEquals(original.lineQuality, parsed.lineQuality)
        assertEquals(original.composition, parsed.composition)
        assertEquals(original.sketchStructure, parsed.sketchStructure)
        assertEquals(original.templateMapping, parsed.templateMapping)
        assertEquals(original.templateFit, parsed.templateFit)
        assertEquals(original.findings, parsed.findings)
    }

    @Test
    fun `Finding severity enum serializes as canonical uppercase token`() {
        val finding = SketchAnalysis.Finding(
            id = "f_001",
            severity = SketchAnalysis.Finding.Severity.WARNING,
            region = NormalizedRect(0.1f, 0.1f, 0.8f, 0.8f),
            what = "Test",
            why = "Test",
            principle = "Test",
            culturalContext = "Test",
        )
        val json = gson.toJson(finding)
        assert(json.contains("\"severity\":\"WARNING\"")) {
            "expected severity to serialize as uppercase token, got: $json"
        }
    }

    @Test
    fun `NormalizedRect rejects out-of-range coordinates`() {
        assertThrows(IllegalArgumentException::class.java) {
            NormalizedRect(x = -0.1f, y = 0.5f, w = 0.5f, h = 0.5f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NormalizedRect(x = 0.5f, y = 0.5f, w = 1.1f, h = 0.5f)
        }
    }

    private fun sampleAnalysis() = SketchAnalysis(
        artStyle = "Sudanese Toub Embroidery",
        culturalOrigin = "Sudan",
        symmetry = SketchAnalysis.Symmetry(
            type = "bilateral",
            accuracyPct = 82,
            weakerSide = "left",
            notes = "Slight drift in upper-left motif",
        ),
        lineQuality = SketchAnalysis.LineQuality(
            confidence = 8,
            consistency = 7,
            shakiness = 2,
            weightVarianceNotes = "Consistent line weight throughout",
        ),
        composition = SketchAnalysis.Composition(
            visualCenterX = 0.5f,
            visualCenterY = 0.45f,
            balanceScore = 8,
            negativeSpacePct = 38,
            hierarchyNotes = "Central medallion dominates",
        ),
        sketchStructure = SketchAnalysis.SketchStructure(
            primaryMotifs = listOf("medallion", "floral border"),
            strokeFlow = "radial outward from center",
            proportionNotes = "1:1.6 aspect, golden ratio",
            mustPreserve = listOf("central medallion", "border symmetry"),
        ),
        templateMapping = SketchAnalysis.TemplateMapping(
            surfaceType = "fabric_toub",
            primaryZone = "chest panel",
            safeZones = listOf("chest", "shoulder line"),
            lightingDirection = "soft front-top",
            maskingNotes = "Clip to visible fabric, fade at folds",
            surfaceFitNotes = "Aligns with toub drape",
        ),
        templateFit = SketchAnalysis.TemplateFit(
            scaleMatch = 8,
            densityMatch = 7,
            styleCompat = 9,
            blockers = emptyList(),
        ),
        findings = listOf(
            SketchAnalysis.Finding(
                id = "f_001",
                severity = SketchAnalysis.Finding.Severity.INFO,
                region = NormalizedRect(0.1f, 0.1f, 0.8f, 0.8f),
                what = "Composition is well-balanced",
                why = "Visual center aligns with template focal zone",
                principle = "Rule of thirds + cultural symmetry",
                culturalContext = "Traditional Sudanese radial motifs",
            ),
        ),
    )
}
