package com.mawaai.love.app.design.ai.analysis

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.gateway.ProviderRegistry
import com.mawaai.love.app.design.ai.intelligence.templateIntelligencePrompt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StructuredAnalysisClient @Inject constructor(
    private val providerRegistry: ProviderRegistry,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "StructuredAnalysisClient"

        private const val SYSTEM_PROMPT = """
            You are a senior design analyst specializing in Middle Eastern traditional arts (Henna, Arabic Calligraphy, Islamic Geometry).
            Your task is to analyze a user's sketch and explain how it should be mapped onto a specific product surface (template).
            
            OUTPUT RULES:
            - Return ONLY valid JSON.
            - Follow the schema precisely.
            - Use 0-1.0 for normalized coordinates (x, y, w, h).
            - Identify up to 12 findings (strengths, issues, or cultural notes).
            - Be technical and constructive.
        """

        private const val SCHEMA_PROMPT = """
            SCHEMA:
            {
              "artStyle": "string",
              "culturalOrigin": "string",
              "symmetry": { "type": "radial|bilateral|none", "accuracyPct": 0-100, "weakerSide": "string", "notes": "string" },
              "lineQuality": { "confidence": 0-10, "consistency": 0-10, "shakiness": 0-10, "weightVarianceNotes": "string" },
              "composition": { "visualCenterX": 0-1, "visualCenterY": 0-1, "balanceScore": 0-10, "negativeSpacePct": 0-100, "hierarchyNotes": "string" },
              "sketchStructure": { "primaryMotifs": ["string"], "strokeFlow": "string", "proportionNotes": "string", "mustPreserve": ["string"] },
              "templateMapping": { "surfaceType": "string", "primaryZone": "string", "safeZones": ["string"], "lightingDirection": "string", "maskingNotes": "string", "surfaceFitNotes": "string" },
              "templateFit": { "scaleMatch": 0-10, "densityMatch": 0-10, "styleCompat": 0-10, "blockers": ["string"] },
              "findings": [
                {
                  "id": "string",
                  "severity": "INFO|WARNING|CRITICAL",
                  "region": { "x": 0-1, "y": 0-1, "w": 0-1, "h": 0-1 },
                  "what": "string",
                  "why": "string",
                  "principle": "string",
                  "culturalContext": "string"
                }
              ]
            }
        """
    }

    suspend fun analyze(sketch: Bitmap, template: TemplateEntity): Result<SketchAnalysis> {
        val intelligence = templateIntelligencePrompt(template)
        val userPrompt = """
            $intelligence
            
            $SCHEMA_PROMPT
            
            Analyze the attached sketch relative to this template.
        """.trimIndent()

        val chain = providerRegistry.activeVisionChain()
        
        return chain.analyze(userPrompt, sketch).mapCatching { json ->
            parseAnalysis(json)
        }
    }

    private fun parseAnalysis(json: String): SketchAnalysis {
        // Strip Markdown code blocks if present
        val cleanJson = json.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            
        return try {
            gson.fromJson(cleanJson, SketchAnalysis::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message}\nRaw: $json")
            throw e
        }
    }
}
