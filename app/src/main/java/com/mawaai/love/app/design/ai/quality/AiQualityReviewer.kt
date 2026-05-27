package com.mawaai.love.app.design.ai.quality

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.mawaai.love.app.design.ai.gateway.ProviderRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiQualityReviewer @Inject constructor(
    private val providerRegistry: ProviderRegistry,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "AiQualityReviewer"

        private const val SYSTEM_PROMPT = """
            You are a quality assurance agent for an AI design pipeline. 
            Compare the user's ORIGINAL SKETCH with the AI-GENERATED RENDER.
            
            Evaluate based on:
            1. Composition Preservation: Did the AI keep the elements where the user drew them?
            2. Surface Fit: Does the design look like it's actually on the surface (henna on skin, print on fabric)?
            3. Lighting Realism: Does the design share the same lighting/shadows as the template?
            
            OUTPUT RULES:
            - Return ONLY valid JSON.
            - Provide a 'passed' boolean. Usually passes if overall score > 70.
        """

        private const val SCHEMA_PROMPT = """
            SCHEMA:
            {
              "quality": {
                "compositionPreservation": 0-100,
                "surfaceFit": 0-100,
                "lightingRealism": 0-100,
                "passed": boolean,
                "issues": ["string"],
                "notes": "string"
              }
            }
        """
    }

    suspend fun review(sketch: Bitmap, render: Bitmap): Result<RenderQuality> {
        val userPrompt = """
            $SCHEMA_PROMPT
            
            Review this pair. The first image is the sketch (source), the second is the render (output).
        """.trimIndent()

        // Note: For multi-image comparison, we'd ideally send both. 
        // Current VisionProvider interface takes one Bitmap. 
        // For now, we'll composite them or just send the render and a description of the sketch.
        // Or better, update the gateway to support multi-image in the future.
        // For Phase 1, we send the render as the primary and use text to describe the sketch's intent 
        // if we can't send both. 
        
        // TEMPORARY: Just analyze the render for surface fit/lighting if multi-image is unsupported.
        val chain = providerRegistry.activeVisionChain()
        
        return chain.analyze(userPrompt, render).mapCatching { json ->
            parseQuality(json)
        }
    }

    private fun parseQuality(json: String): RenderQuality {
        val cleanJson = json.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            
        return try {
            val response = gson.fromJson(cleanJson, QualityResponse::class.java)
            response.quality
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message}\nRaw: $json")
            throw e
        }
    }
}
