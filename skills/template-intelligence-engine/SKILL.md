---
name: template-intelligence-engine
description: Builds strongly-typed template systems for design overlay apps (henna on palms, embroidery on abayas, murals on walls, clothing print mockups). Use for any work involving placement zones, warp behavior, blend modes, cultural style constraints, or template registries. Produces Kotlin sealed class hierarchies, PlacementZone definitions, StyleProfile types, TemplateAnalyzer scoring logic, immutable TemplateContext models, and zero untyped-Map JSON shapes.
icon: layout-template
color: Teal
---

# Template Intelligence Engine

Owns stage 2 of the AI pipeline. Models the template domain with strong types. Refuses untyped JSON blobs.

## When to Use

- Designing the template engine for MAWAAI (henna, abaya, mural, mockup)
- Defining placement zones, warp, blend modes, constraints
- Adding cultural styles (Najdi, Hijazi, Modern, Minimalist, etc.)
- Building the analyzer that scores template fit against a vision analysis

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
    data class BodyArt(...) : Template()
    data class GarmentOverlay(...) : Template()
}

enum class TemplateCategory {
    PORTRAIT, LANDSCAPE, CULTURAL, PRODUCT, ABSTRACT, BODY_ART, GARMENT
}

data class PlacementZone(
    val id: ZoneId,
    val rect: NormalizedRect,
    val warp: WarpDescriptor,
    val blendMode: BlendMode,
    val constraints: ZoneConstraints,
)

data class ZoneConstraints(
    val minSize: Size,
    val aspect: AspectRule,
    val orientation: Orientation,
    val maskRef: MaskAssetRef?,
)

data class StyleProfile(
    val palette: Palette,
    val culturalStyle: CulturalStyle?,
    val motifs: List<MotifRule>,
    val moodKeywords: List<String>,
)
```

## Template Registry Rules

1. Templates load from a registry (file or remote config) — never hardcoded if/else
2. Every template fully validated at load (rect coords in 0..1, no zone overlap unless allowed, palette non-empty, asset refs resolvable)
3. New categories require new sealed subclasses — forces compile-time exhaustiveness in `when` blocks
4. Validation failures = template rejected, logged with reason

## TemplateAnalyzer

```kotlin
interface TemplateAnalyzer {
    fun score(analysis: VisionAnalysis, template: Template): TemplateScore
    fun bestMatch(analysis: VisionAnalysis): TemplateMatch
}

data class TemplateScore(
    val total: Float,
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
    val resolvedZones: List<ResolvedZone>,
    val effectivePalette: Palette,
)
```

Downstream stages (prompt synthesis, OpenCV processor) read only from `TemplateContext`. They never touch the registry directly.

## Output

Per micro-task:
- Sealed class hierarchy in `Template.kt`
- `PlacementZone.kt`, `StyleProfile.kt`, `ZoneConstraints.kt`
- `TemplateAnalyzer.kt` interface + scoring impl
- `TemplateRegistry.kt` loader with validation
- Unit tests: registry load, scoring with fixtures, exhaustive `when` block

## Anti-Patterns

- Untyped config maps with string keys for templates — banned
- String enums for category — use Kotlin enum class
- Loading templates lazily inside the pipeline — load + validate upfront
- ZoneConstraints stored as JSON strings
- Cultural styles as free-text — use sealed hierarchy or enum
- Raw transformation matrices in JSON — use typed WarpDescriptor
