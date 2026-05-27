package com.mawaai.love.app.design.ai.suggestions

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.ai.gateway.ProviderRegistry
import com.mawaai.love.app.design.ai.intelligence.templateIntelligencePrompt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuggestionsClient @Inject constructor(
    private val providerRegistry: ProviderRegistry,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "SuggestionsClient"

        private const val SYSTEM_PROMPT = """
            You are a senior design mentor. Based on a SketchAnalysis and the original sketch, generate 4-8 specific Suggestion cards.
            Each suggestion should be actionable and culturally relevant.
            
            OUTPUT RULES:
            - Return ONLY valid JSON matching the schema.
            - Ensure regions correspond to the findings in the analysis.
        """

        private const val SCHEMA_PROMPT = """
            SCHEMA:
            {
              "suggestions": [
                {
                  "id": "string",
                  "category": "LINE|SYMMETRY|TEMPLATE|CULTURAL|PRINT|COLOR",
                  "location": { "x": 0-1, "y": 0-1, "w": 0-1, "h": 0-1 },
                  "title": "string (max 80)",
                  "explanation": "string (max 400)",
                  "principle": "string (max 120)",
                  "culturalContext": "string (max 300)",
                  "impact": 0-100,
                  "autoFixable": boolean,
                  "previewHint": "string (max 300)"
                }
              ]
            }
        """
    }

    suspend fun generateSuggestions(
        sketch: Bitmap,
        template: TemplateEntity,
        analysis: SketchAnalysis
    ): Result<List<Suggestion>> {
        val intelligence = templateIntelligencePrompt(template)
        val analysisJson = gson.toJson(analysis)
        
        val userPrompt = """
            $intelligence
            
            ANALYSIS:
            $analysisJson
            
            $SCHEMA_PROMPT
            
            Generate suggestions to improve this design for the specified surface.
        """.trimIndent()

        val chain = providerRegistry.activeVisionChain()
        
        return chain.analyze(userPrompt, sketch).mapCatching { json ->
            parseSuggestions(json)
        }
    }

    private fun parseSuggestions(json: String): List<Suggestion> {
        val cleanJson = json.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
            
        return try {
            val response = gson.fromJson(cleanJson, SuggestionsResponse::class.java)
            response.suggestions
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message}\nRaw: $json")
            throw e
        }
    }
}
