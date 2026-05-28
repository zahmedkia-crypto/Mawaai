package com.mawaai.love.app.design.ai.render

import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MT-027 acceptance: the renderer flattens RenderPrompt into the expected
 * Lovable-equivalent flat prompt and silently drops null / blank fields.
 *
 * Image generation itself is not exercised here (no network, no fixtures);
 * the chain wiring is tested separately in instrumentation tests.
 */
class ImageEditRendererTest {

    private val renderer = ImageEditRenderer(
        promptBuilder = mockk(relaxed = true),
        chain = mockk(relaxed = true),
    )

    @Test
    fun `flatten includes structure, intelligence, direction, palette, color, refinements, terminator`() {
        val prompt = sampleRenderPrompt(
            palette = "deep red, gold, ivory",
            colorOverride = "OVERRIDE COLOR: Use the specific hex color #B8860B for the embroidery.",
            refinements = "ACCEPTED REFINEMENTS: smooth the line quality",
        )

        val flat = renderer.flattenPrompt(prompt)

        assertTrue("structure preserved", "CRITICAL RULE" in flat)
        assertTrue("intelligence preserved", "template intelligence" in flat)
        assertTrue("base direction preserved", "henna paste" in flat)
        assertTrue("palette wrapped", "Honor the traditional palette where natural: deep red, gold, ivory." in flat)
        assertTrue("color override preserved", "OVERRIDE COLOR" in flat)
        assertTrue("refinements preserved", "ACCEPTED REFINEMENTS" in flat)
        assertTrue("terminator preserved", flat.endsWith("watermarks, or framing."))
    }

    @Test
    fun `flatten drops null and blank optional fields`() {
        val prompt = sampleRenderPrompt(
            palette = null,
            colorOverride = null,
            refinements = null,
        )

        val flat = renderer.flattenPrompt(prompt)

        assertFalse("no palette wrapper", "Honor the traditional palette" in flat)
        assertFalse("no color override token", "OVERRIDE COLOR" in flat)
        assertFalse("no refinements token", "ACCEPTED REFINEMENTS" in flat)
        assertTrue("structure still present", "CRITICAL RULE" in flat)
        assertTrue("terminator still present", flat.endsWith("watermarks, or framing."))
    }

    @Test
    fun `field order matches Lovable render pipeline (structure first, terminator last)`() {
        val prompt = sampleRenderPrompt(
            palette = "deep red",
            colorOverride = "OVERRIDE COLOR: black",
            refinements = "ACCEPTED REFINEMENTS: refine",
        )

        val flat = renderer.flattenPrompt(prompt)

        val iStructure = flat.indexOf("CRITICAL RULE")
        val iIntelligence = flat.indexOf("template intelligence")
        val iDirection = flat.indexOf("henna paste")
        val iPalette = flat.indexOf("Honor the traditional palette")
        val iColor = flat.indexOf("OVERRIDE COLOR")
        val iRefinements = flat.indexOf("ACCEPTED REFINEMENTS")
        val iTerminator = flat.indexOf("Final image only")

        assertTrue("structure before intelligence", iStructure < iIntelligence)
        assertTrue("intelligence before direction", iIntelligence < iDirection)
        assertTrue("direction before palette", iDirection < iPalette)
        assertTrue("palette before color override", iPalette < iColor)
        assertTrue("color before refinements", iColor < iRefinements)
        assertTrue("refinements before terminator", iRefinements < iTerminator)
    }

    private fun sampleRenderPrompt(
        palette: String?,
        colorOverride: String?,
        refinements: String?,
    ): RenderPrompt = RenderPrompt(
        structurePreservation = "CRITICAL RULE: Preserve the exact spatial structure.",
        templateIntelligence = "template intelligence block",
        baseDirection = "Render this sketch as authentic henna paste on the open palm.",
        palette = palette,
        colorOverride = colorOverride,
        refinements = refinements,
    )

    // Unused parameters silenced -- the renderer test does not invoke render().
    @Suppress("unused")
    private fun sampleTemplate(): TemplateEntity = mockk(relaxed = true)

    @Suppress("unused")
    private fun sampleSuggestion(): Suggestion = mockk(relaxed = true)
}
