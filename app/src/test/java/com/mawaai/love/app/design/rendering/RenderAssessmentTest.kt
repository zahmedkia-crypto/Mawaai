package com.mawaai.love.app.design.rendering

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderAssessmentTest {

    @Test
    fun `production ready requires every quality threshold`() {
        val ready = RenderAssessmentPolicy.manual(
            realism = 8.5f,
            structurePreservation = 9f,
            materialIntegration = 8f,
            lightingConsistency = 8f
        )

        assertTrue(ready.isProductionReady)
    }

    @Test
    fun `low realism blocks production readiness and creates retry instruction`() {
        val assessment = RenderAssessmentPolicy.manual(
            realism = 6f,
            structurePreservation = 9.5f,
            materialIntegration = 8.5f,
            lightingConsistency = 8.5f
        )

        assertFalse(assessment.isProductionReady)
        assertTrue("retry mentions product photography", "product photography" in assessment.retryInstruction())
    }

    @Test
    fun `manual scores are clamped to zero through ten`() {
        val assessment = RenderAssessmentPolicy.manual(
            realism = -4f,
            structurePreservation = 14f,
            materialIntegration = 12f,
            lightingConsistency = -1f
        )

        assertTrue(assessment.realism == 0f)
        assertTrue(assessment.structurePreservation == 10f)
        assertTrue(assessment.materialIntegration == 10f)
        assertTrue(assessment.lightingConsistency == 0f)
    }
}
