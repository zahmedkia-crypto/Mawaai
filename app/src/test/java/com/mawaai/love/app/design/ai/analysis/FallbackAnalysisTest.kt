package com.mawaai.love.app.design.ai.analysis

import com.mawaai.love.app.data.database.entities.TemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MT-020 acceptance: [FallbackAnalysis.build] must be deterministic and
 * always return a valid [SketchAnalysis] the renderer can safely consume
 * when the AI provider chain is unreachable.
 */
class FallbackAnalysisTest {

    @Test
    fun `same template produces bit-equal fallback (determinism)`() {
        val template = sampleTemplate(id = "t_palm_001", name = "Palm Henna", category = "henna")

        val first = FallbackAnalysis.build(template)
        val second = FallbackAnalysis.build(template)

        assertEquals(first, second)
    }

    @Test
    fun `different templates produce different surface mappings`() {
        val palm = sampleTemplate(id = "t_palm", name = "Palm Henna", category = "henna")
        val abaya = sampleTemplate(
            id = "t_abaya",
            name = "Abaya Chest Panel",
            category = "garment",
            primaryLight = "soft side",
        )

        val palmAnalysis = FallbackAnalysis.build(palm)
        val abayaAnalysis = FallbackAnalysis.build(abaya)

        assertNotEquals(
            palmAnalysis.templateMapping.surfaceType,
            abayaAnalysis.templateMapping.surfaceType,
        )
        assertNotEquals(
            palmAnalysis.templateMapping.lightingDirection,
            abayaAnalysis.templateMapping.lightingDirection,
        )
    }

    @Test
    fun `fallback always satisfies NormalizedRect 0_1 invariants`() {
        val template = sampleTemplate(id = "t_thobe", name = "Thobe Collar", category = "garment")

        val analysis = FallbackAnalysis.build(template)

        analysis.findings.forEach { finding ->
            // Constructor would have thrown if any coord was out of range.
            assertTrue("region.x in 0..1", finding.region.x in 0f..1f)
            assertTrue("region.y in 0..1", finding.region.y in 0f..1f)
            assertTrue("region.w in 0..1", finding.region.w in 0f..1f)
            assertTrue("region.h in 0..1", finding.region.h in 0f..1f)
        }
    }

    @Test
    fun `seeded fallback finding carries INFO severity and cultural context`() {
        val template = sampleTemplate(id = "t_toub", name = "Toub Border", category = "garment")

        val analysis = FallbackAnalysis.build(template)

        assertEquals(1, analysis.findings.size)
        val finding = analysis.findings.first()
        assertEquals(SketchAnalysis.Finding.Severity.INFO, finding.severity)
        assertNotNull(finding.culturalContext)
        assertTrue(
            "cultural context should be non-empty",
            finding.culturalContext.isNotBlank(),
        )
    }

    private fun sampleTemplate(
        id: String,
        name: String,
        category: String,
        primaryLight: String = "natural daylight",
    ): TemplateEntity = TemplateEntity(
        id = id,
        category = category,
        name = name,
        surfaceType = surfaceForName(name, category),
        description = null,
        referenceImageUrl = null,
        traditionalPaletteCsv = "",
        maxCoveragePct = 75,
        primaryLight = primaryLight,
    )

    /**
     * Minimal heuristic mirroring SurfaceCatalog.forTemplate() to ensure the
     * fallback receives a distinct surfaceType across the test templates
     * without coupling this test to the real catalog implementation.
     */
    private fun surfaceForName(name: String, category: String): String {
        val lower = name.lowercase()
        return when {
            category == "henna" && "palm" in lower -> "skin_palm"
            category == "henna" && "hand" in lower -> "skin_hand_full"
            category == "garment" && "abaya" in lower -> "fabric_abaya"
            category == "garment" && "thobe" in lower -> "fabric_thobe"
            category == "garment" && "toub" in lower -> "fabric_toub"
            else -> "unknown"
        }
    }
}
