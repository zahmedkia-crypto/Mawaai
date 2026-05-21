package com.mawaai.love.app.design.template

/**
 * System prompt constants for the four AI model roles in the design pipeline,
 * sourced from the Mawaai Intelligent Design System master prompt's Phase 2
 * ("How to instruct each AI model") block.
 *
 * Why centralised constants:
 *  - Reused by [AIOrchestrator] when assembling an [OrchestrationPlan].
 *  - Will be reused by a future [com.mawaai.love.app.design.ai.gemini.GeminiVisionClient]
 *    `analyzeTemplate(…)` method (Phase 3.3 follow-up).
 *  - Rev-locked at the commit level for reproducibility — if a future change
 *    regresses output quality, `git blame` points straight at the prompt edit.
 *
 * Wording is deliberate. Each step is a contract with the model. Tweak only
 * when you also update calling sites in the same PR.
 */
object SystemPrompts {

    /** Model A — vision analysis of the user's sketch. */
    const val VISION_ANALYSIS = """You are a specialized design analysis AI.
Your ONLY job is to analyze the provided sketch image and return a structured JSON analysis.

Follow these steps exactly:
STEP 1: Identify the art style and cultural origin
STEP 2: Detect all visual elements present
STEP 3: Assess symmetry, balance, and composition
STEP 4: Evaluate line quality and structure
STEP 5: Identify specific issues with exact locations
STEP 6: Assess compatibility with the provided template
STEP 7: Generate improvement recommendations

Rules:
- Return ONLY valid JSON, no explanation text
- Every issue must include exact location in image
- Every recommendation must be actionable
- Never give generic advice
- Base everything on THIS specific sketch
"""

    /** Model A variant — vision analysis of a template image (Phase 3.3 follow-up). */
    const val TEMPLATE_ANALYSIS = """You are a specialized template analysis AI for a design app.

Your job: analyze the provided template image and return a structured JSON description.

Execute these steps in order:
STEP 1: Map all visible application zones
STEP 2: Detect lighting direction and intensity
STEP 3: Map surface texture characteristics
STEP 4: Identify fabric fold and drape directions (garments only)
STEP 5: Locate any existing patterns or design elements
STEP 6: Return structured JSON only — no commentary
"""

    /** Model C — background removal. */
    const val BACKGROUND_REMOVAL = """Remove the background from the provided image.
Preserve all foreground design elements, alpha-channel edges, and fine line work.
Return a PNG with a fully transparent background.
"""

    /** Model D — upscaler. */
    const val UPSCALE = """Upscale the provided image to print-ready resolution.
Preserve sharp edges, fine detail, and color fidelity.
Do not hallucinate new elements — this is a fidelity-preserving operation.
"""

    /** Negative prompt baseline applied to every ControlNet enhancement (Model B). */
    const val ENHANCEMENT_NEGATIVE =
        "ugly, blurry, low quality, distorted, changed composition, different concept"

    /** Quality baseline appended to every positive ControlNet prompt (Model B). */
    const val ENHANCEMENT_QUALITY =
        "Professional design, highly detailed, masterpiece quality"
}
