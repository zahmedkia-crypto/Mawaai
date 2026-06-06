package com.mawaai.love.app.design.ai.render

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MT-027 acceptance: the renderer's flattenPrompt collapses RenderPrompt
 * into the expected Lovable-equivalent flat prompt and silently drops null
 * or blank fields.
 *
 * Pure-function test against [ImageEditRenderer.flattenPrompt] in the
 * companion object -- no constructor invocation, no mocking framework,
 * no network. Image generation is exercised separately by instrumentation
 * tests that hit the live HuggingFace endpoint.
 */
class ImageEditRendererTest {

    @Test
    fun `flatten includes structure intelligence direction realism palette color refinements terminator`() {
        val prompt = sampleRenderPrompt(
            palette = "deep red, gold, ivory",
            colorOverride = "OVERRIDE COLOR: Use the specific hex color #B8860B for the embroidery.",
            refinements = "USER-ACCEPTED REFINEMENTS: smooth the line quality",
        )

        val flat = ImageEditRenderer.flattenPrompt(prompt)

        assertTrue("structure preserved", "CRITICAL RULE" in flat)
        assertTrue("intelligence preserved", "template intelligence" in flat)
        assertTrue("base direction preserved", "henna paste" in flat)
        assertTrue("realism preserved", "PHOTOREALISM TARGET" in flat)
        assertTrue("material preserved", "MATERIAL PHYSICS" in flat)
        assertTrue("camera preserved", "CAMERA + LIGHTING" in flat)
        assertTrue("negative prompt preserved", "AVOID" in flat)
        assertTrue(
            "palette wrapped",
            "Honor the traditional palette where natural: deep red, gold, ivory." in flat,
        )
        assertTrue("color override preserved", "OVERRIDE COLOR" in flat)
        assertTrue("refinements preserved", "USER-ACCEPTED REFINEMENTS" in flat)
        assertTrue("terminator preserved", flat.endsWith("before/after panels."))
    }

    @Test
    fun `flatten drops null and blank optional fields`() {
        val prompt = sampleRenderPrompt(
            palette = null,
            colorOverride = null,
            refinements = null,
        )

        val flat = ImageEditRenderer.flattenPrompt(prompt)

        assertFalse("no palette wrapper", "Honor the traditional palette" in flat)
        assertFalse("no color override token", "OVERRIDE COLOR" in flat)
        assertFalse("no refinements token", "USER-ACCEPTED REFINEMENTS" in flat)
        assertTrue("structure still present", "CRITICAL RULE" in flat)
        assertTrue("realism still present", "PHOTOREALISM TARGET" in flat)
        assertTrue("terminator still present", flat.endsWith("before/after panels."))
    }

    @Test
    fun `field order keeps realism before user-selected refinements`() {
        val prompt = sampleRenderPrompt(
            palette = "deep red",
            colorOverride = "OVERRIDE COLOR: black",
            refinements = "USER-ACCEPTED REFINEMENTS: refine",
        )

        val flat = ImageEditRenderer.flattenPrompt(prompt)

        val iStructure = flat.indexOf("CRITICAL RULE")
        val iIntelligence = flat.indexOf("template intelligence")
        val iDirection = flat.indexOf("henna paste")
        val iRealism = flat.indexOf("PHOTOREALISM TARGET")
        val iMaterial = flat.indexOf("MATERIAL PHYSICS")
        val iCamera = flat.indexOf("CAMERA + LIGHTING")
        val iPalette = flat.indexOf("Honor the traditional palette")
        val iColor = flat.indexOf("OVERRIDE COLOR")
        val iRefinements = flat.indexOf("USER-ACCEPTED REFINEMENTS")
        val iNegative = flat.indexOf("AVOID")
        val iTerminator = flat.indexOf("Final image only")

        assertTrue("structure before intelligence", iStructure < iIntelligence)
        assertTrue("intelligence before direction", iIntelligence < iDirection)
        assertTrue("direction before realism", iDirection < iRealism)
        assertTrue("realism before material", iRealism < iMaterial)
        assertTrue("material before camera", iMaterial < iCamera)
        assertTrue("camera before palette", iCamera < iPalette)
        assertTrue("palette before color override", iPalette < iColor)
        assertTrue("color before refinements", iColor < iRefinements)
        assertTrue("refinements before negative prompt", iRefinements < iNegative)
        assertTrue("negative prompt before terminator", iNegative < iTerminator)
    }

    private fun sampleRenderPrompt(
        palette: String?,
        colorOverride: String?,
        refinements: String?,
    ): RenderPrompt = RenderPrompt(
        structurePreservation = "CRITICAL RULE: Preserve the exact spatial structure.",
        templateIntelligence = "template intelligence block",
        baseDirection = "Render this sketch as authentic henna paste on the open palm.",
        realismDirection = "PHOTOREALISM TARGET: Make it real.",
        materialPhysics = "MATERIAL PHYSICS: Blend into surface texture.",
        cameraAndLighting = "CAMERA + LIGHTING: Product photography.",
        palette = palette,
        colorOverride = colorOverride,
        refinements = refinements,
        negativePrompt = "AVOID: pasted overlay."
    )
}