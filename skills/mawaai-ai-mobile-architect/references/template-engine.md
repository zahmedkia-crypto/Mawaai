# Template Intelligence Layer

Strongly typed template system. No `Map<String, Any>`, ever.

## Core Types

```kotlin
sealed class Template {
    abstract val id: TemplateId
    abstract val category: TemplateCategory
    abstract val styleProfile: StyleProfile
    abstract val zones: List<PlacementZone>
    abstract val constraints: TemplateConstraints

    data class Portrait(...) : Template()
    data class Landscape(...) : Template()
    data class CulturalMotif(...) : Template()
    data class ProductMockup(...) : Template()
}

enum class TemplateCategory { PORTRAIT, LANDSCAPE, CULTURAL, PRODUCT, ABSTRACT }

data class PlacementZone(
    val id: ZoneId,
    val rect: NormalizedRect,           // 0..1 coords, resolution-independent
    val warp: WarpDescriptor,           // perspective / affine / none
    val blendMode: BlendMode,           // NORMAL, MULTIPLY, SCREEN, OVERLAY
    val constraints: ZoneConstraints,   // min size, aspect, orientation
)

data class StyleProfile(
    val palette: Palette,
    val culturalStyle: CulturalStyle?,  // e.g., Najdi, Hijazi, Modern, Minimalist
    val motifs: List<MotifRule>,
    val moodKeywords: List<String>,
)
```

## TemplateAnalyzer

Consumes `VisionAnalysis`, scores all templates, returns the best match with rationale.

```kotlin
interface TemplateAnalyzer {
    fun score(analysis: VisionAnalysis, template: Template): TemplateScore
    fun bestMatch(analysis: VisionAnalysis): TemplateMatch
}

data class TemplateScore(
    val total: Float,           // 0..1
    val styleFit: Float,
    val structureFit: Float,
    val zoneFit: Float,
    val rationale: String,
)
```

## TemplateContext (immutable, passed downstream)

```kotlin
data class TemplateContext(
    val template: Template,
    val match: TemplateMatch,
    val resolvedZones: List<ResolvedZone>,   // pixel coords once canvas size known
    val effectivePalette: Palette,
)
```

## Rules

1. Templates are loaded from a registry, not hardcoded — supports remote config
2. Every template is fully validated at load (rects in 0..1, palette non-empty, zones non-overlapping unless explicitly allowed)
3. Cultural styles live in their own sealed hierarchy with metadata (no string matching)
4. New template categories require new sealed subclasses — forces compile-time exhaustiveness in `when` blocks
5. Zone warp parameters are typed, never raw matrices in JSON

## Anti-Patterns

- `Template(name="...", config=mapOf(...))` — banned
- String enums for category — use Kotlin `enum class`
- Loading templates lazily inside the pipeline — load + validate upfront
