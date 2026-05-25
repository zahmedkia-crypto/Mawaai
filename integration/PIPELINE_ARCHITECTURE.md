# PIPELINE ARCHITECTURE — The 7 Phases

The full AI design pipeline as ported from Creative Studio. Each phase has a typed contract, a clear responsibility, and an explicit handoff to the next phase.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      MAWAAI AI Design Pipeline (v2)                          │
└──────────────────────────────────────────────────────────────────────────────┘

  USER SKETCH  ──┐
                 │
                 v
        ┌────────────────────┐
        │   PHASE 1          │  Sketch capture (existing CanvasEngine — keep)
        │   Sketch ingest    │  Output: Bitmap + metadata
        └─────────┬──────────┘
                  │
                  v
        ┌────────────────────┐
        │   PHASE 2          │  Look up SurfaceProfile for the chosen Template.
        │   Template         │  Output: SurfaceProfile (12 typed variants) + render
        │   Intelligence     │          direction string + constraints + masking rules.
        └─────────┬──────────┘
                  │
                  v
        ┌────────────────────┐
        │   PHASE 3          │  AI vision analysis (via ProviderRegistry).
        │   Sketch Analysis  │  Output: SketchAnalysis (typed Kotlin):
        │                    │   - art_style, cultural_origin
        │                    │   - symmetry, line_quality, composition
        │                    │   - sketch_structure (motifs, must_preserve)
        │                    │   - template_mapping (surface_type, safe_zones)
        │                    │   - template_fit (scale/density/style 0-10)
        │                    │   - findings[] up to 12 region-anchored items
        └─────────┬──────────┘
                  │ (fallback: heuristic FallbackAnalysis if AI returns bad JSON)
                  v
        ┌────────────────────┐
        │   PHASE 4          │  AI generates 4-8 region-anchored Suggestion cards.
        │   Suggestions      │  Output: List<Suggestion> with category, location,
        │                    │          title, explanation, principle, impact 0-100,
        │                    │          auto_fixable flag, preview_hint.
        │                    │  User accepts / skips each card.
        └─────────┬──────────┘
                  │
                  v
        ┌────────────────────┐
        │   PHASE 5          │  Build the render prompt:
        │   Structure        │  1) Structure preservation rule (sketch = authoritative)
        │   Preservation     │  2) Template intelligence summary (Phase 2)
        │                    │  3) Base surface direction (Phase 2)
        │                    │  4) Traditional palette
        │                    │  5) Color override (Phase 7)
        │                    │  6) Accepted refinements (Phase 4)
        │                    │  7) "Final image only — no annotations"
        └─────────┬──────────┘
                  │
                  v
        ┌────────────────────┐
        │   PHASE 6          │  Quality validation gate (2-tier).
        │   Render +         │  Tier 1: Heuristic (analysis.template_fit thresholds)
        │   Quality Gate     │  Tier 2: AI visual QA (compare sketch ↔ render):
        │                    │     - composition_preservation 0-100
        │                    │     - surface_fit 0-100
        │                    │     - lighting_realism 0-100
        │                    │     - passed (bool), issues[]
        │                    │  Auto-block if score < threshold (default 70).
        └─────────┬──────────┘
                  │ (on block: surface error to user, allow color/suggestion re-pick)
                  v
        ┌────────────────────┐
        │   PHASE 7          │  Color override (for garments + ceramics).
        │   Color Control    │  User can change template_color before re-render.
        │                    │  Re-runs Phase 5+6 with new color in prompt.
        └─────────┬──────────┘
                  │
                  v
        ┌────────────────────┐
        │   PHASE 8          │  Product mockup composition.
        │   Mockup           │  User picks one of 12 product scenes (bridal palm,
        │   Composition      │  flat-lay abaya, majlis wall, etc.).
        │                    │  OpenCV places render onto the product scene with
        │                    │  surface-appropriate warp, lighting, shadows.
        │                    │  Output: final composited image for export/share.
        └────────────────────┘
```

---

## 📥 Phase Inputs / Outputs (Contracts)

All contracts are strongly typed Kotlin (no `Map<String, Any>`).

### Phase 1 — Sketch Ingest
```kotlin
data class Sketch(
    val bitmap: Bitmap,
    val capturedAt: Instant,
    val canvasSize: Size,
    val strokeCount: Int
)
```

### Phase 2 — Template Intelligence
```kotlin
sealed interface SurfaceProfile {
    val id: String                  // e.g. "skin_palm"
    val label: String               // e.g. "open palm skin"
    val targetSurface: String
    val constraints: List<String>
    val maskingRules: List<String>
    val perspectiveRules: List<String>
    val materialResponse: String

    data class SkinPalm(...) : SurfaceProfile
    data class SkinHandFull(...) : SurfaceProfile
    data class SkinFoot(...) : SurfaceProfile
    data class FabricAbaya(...) : SurfaceProfile
    data class FabricThobe(...) : SurfaceProfile
    data class FabricToub(...) : SurfaceProfile
    data class WallStone(...) : SurfaceProfile
    data class WallPlaster(...) : SurfaceProfile
    data class WallArch(...) : SurfaceProfile
    data class CeramicPlate(...) : SurfaceProfile
    data class CeramicTile(...) : SurfaceProfile
    data class CeramicMug(...) : SurfaceProfile
}

data class TemplateIntelligence(
    val profile: SurfaceProfile,
    val zones: List<TemplateZone>,
    val lightingDirection: String,
    val material: String,
    val reflectance: String,
    val maxCoveragePct: Int
)
```

### Phase 3 — Sketch Analysis
```kotlin
data class SketchAnalysis(
    val artStyle: String,
    val culturalOrigin: String,
    val symmetry: Symmetry,
    val lineQuality: LineQuality,
    val composition: Composition,
    val sketchStructure: SketchStructure,
    val templateMapping: TemplateMapping,
    val templateFit: TemplateFit,
    val findings: List<Finding>          // max 12
) {
    data class Symmetry(val type: String, val accuracyPct: Int, val weakerSide: String, val notes: String)
    data class LineQuality(val confidence: Int, val consistency: Int, val shakiness: Int, val weightVarianceNotes: String)
    data class Composition(val visualCenterX: Float, val visualCenterY: Float, val balanceScore: Int, val negativeSpacePct: Int, val hierarchyNotes: String)
    data class SketchStructure(val primaryMotifs: List<String>, val strokeFlow: String, val proportionNotes: String, val mustPreserve: List<String>)
    data class TemplateMapping(val surfaceType: String, val primaryZone: String, val safeZones: List<String>, val lightingDirection: String, val maskingNotes: String, val surfaceFitNotes: String)
    data class TemplateFit(val scaleMatch: Int, val densityMatch: Int, val styleCompat: Int, val blockers: List<String>)
    data class Finding(
        val id: String,
        val severity: Severity,
        val region: NormalizedRect,
        val what: String,
        val why: String,
        val principle: String,
        val culturalContext: String
    ) {
        enum class Severity { INFO, WARNING, CRITICAL }
    }
}

data class NormalizedRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    init { require(x in 0f..1f && y in 0f..1f && w in 0f..1f && h in 0f..1f) }
}
```

### Phase 4 — Suggestions
```kotlin
data class Suggestion(
    val id: String,
    val category: Category,
    val location: NormalizedRect,
    val title: String,              // max 80 chars
    val explanation: String,        // max 400 chars
    val principle: String,          // max 120 chars
    val culturalContext: String,    // max 300 chars
    val impact: Int,                // 0..100
    val autoFixable: Boolean,
    val previewHint: String         // max 300 chars
) {
    enum class Category { LINE, SYMMETRY, TEMPLATE, CULTURAL, PRINT, COLOR }
}
```

### Phase 5 — Render Prompt
```kotlin
data class RenderPrompt(
    val structurePreservation: String,
    val templateIntelligence: String,
    val baseDirection: String,
    val palette: String?,
    val colorOverride: String?,
    val refinements: String?,
    val finalImageOnly: String = "Final image only — no annotations, labels, text, watermarks, or framing."
) {
    fun toPromptString(): String = listOfNotNull(
        structurePreservation,
        templateIntelligence,
        baseDirection,
        palette,
        colorOverride,
        refinements,
        finalImageOnly
    ).filter { it.isNotBlank() }.joinToString(" ")
}
```

### Phase 6 — Render Quality
```kotlin
data class RenderQuality(
    val compositionPreservation: Int,   // 0..100
    val surfaceFit: Int,                // 0..100
    val lightingRealism: Int,           // 0..100
    val passed: Boolean,
    val issues: List<String>,           // max 6
    val notes: String                   // max 500 chars
) {
    val overallScore: Int get() = (compositionPreservation + surfaceFit + lightingRealism) / 3
}
```

### Phase 7 — Color Control
```kotlin
data class ColorOverride(
    val hex: String?,                   // null = use template default palette
    val applyTo: ApplyTo
) {
    enum class ApplyTo { GARMENT_BASE, GARMENT_EMBROIDERY, DOMINANT, NONE }
}
```

### Phase 8 — Mockup Composition
```kotlin
data class ProductMockup(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val surfaceMatch: List<String>,     // which SurfaceProfile.id's this mockup accepts
    val scene: String,
    val lighting: String,
    val perspective: String,
    val accentColor: String,            // hex
    val sortOrder: Int
)

data class CompositedExport(
    val finalImage: Bitmap,
    val mockup: ProductMockup,
    val sourceRender: Bitmap,
    val createdAt: Instant
)
```

---

## 🔁 Phase Sequencing Rules

1. **Phases 1 → 2 → 3 are sequential**. Cannot start Phase 2 without Phase 1's output. Cannot start Phase 3 without Phase 2's output.

2. **Phases 4 and 7 can iterate**. User can accept suggestions (Phase 4), pick a color (Phase 7), and re-trigger Phase 5+6 multiple times. Each iteration creates a new render attempt but reuses Phase 3 analysis (cached in Room).

3. **Phase 6 is mandatory before Phase 8**. A render that fails quality gates cannot be composited onto a product mockup.

4. **Phase 8 is optional**. Some users just want the bare render. Mockup composition only runs when the user explicitly picks a mockup.

5. **Every phase output is persisted to Room** (Project entity has analysis, suggestions, accepted_suggestion_ids, render_url, render_prompt, render_quality, color_override, exported_url columns).

---

## 🛡 Phase-Level Error Handling

| Phase | Failure mode | Handler |
|---|---|---|
| 1 | User cancels | Abort, return to template selector |
| 2 | Unknown surface_type | Fall back to default profile, log warning |
| 3 | AI returns invalid JSON | Run `FallbackAnalysis.build(template)` (deterministic, heuristic-only) |
| 3 | All providers fail | Surface error: "AI is unreachable — try again or change provider in Settings" |
| 4 | AI returns invalid JSON | Run `FallbackSuggestions.build(template)` (3 default cards) |
| 5 | (no failure mode — pure data assembly) | n/a |
| 6 | Tier 1 heuristic block | Show issues to user; allow accept-suggestions or color change to retry |
| 6 | Tier 2 AI QA block | Same as Tier 1; record reason in `render_prompt` field |
| 6 | AI QA itself fails | Fall back to Tier 1 score; mark `aiValidationFallback=true` in checks |
| 7 | (no failure mode — pure data assembly) | n/a |
| 8 | OpenCV fails to composite | Surface error: "Could not place on mockup — try a different scene" |

---

## 📈 Observable State

The whole pipeline is exposed as a single `DesignSessionState` Flow that the UI observes:

```kotlin
sealed class DesignSessionState {
    object Idle : DesignSessionState()
    data class SketchCaptured(val sketch: Sketch, val template: Template) : DesignSessionState()
    data class Analyzing(val template: Template) : DesignSessionState()
    data class AnalysisReady(val analysis: SketchAnalysis) : DesignSessionState()
    data class SuggestionsReady(val suggestions: List<Suggestion>) : DesignSessionState()
    data class Rendering(val prompt: RenderPrompt) : DesignSessionState()
    data class RenderReady(val render: Bitmap, val quality: RenderQuality) : DesignSessionState()
    data class QualityBlocked(val render: Bitmap, val quality: RenderQuality) : DesignSessionState()
    data class MockupReady(val composited: CompositedExport) : DesignSessionState()
    data class Failed(val phase: Int, val reason: String) : DesignSessionState()
}
```

ViewModels in `ui/design/` collect this Flow and switch screens accordingly. Single source of truth, no implicit cross-phase coupling.
