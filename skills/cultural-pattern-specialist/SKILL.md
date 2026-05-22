---
name: cultural-pattern-specialist
description: Encodes regional and cultural art constraints (Najdi, Hijazi, Khaleeji, Moroccan, Levantine, Persian, Mughal, North African) into typed motif libraries, composition rules, and color systems. Use for any traditional or region-specific aesthetic generation, ensuring designs respect cultural integrity rather than blending into generic "Middle Eastern" pastiche. Produces motif libraries, region-specific color palettes, composition rules (geometric grids, calligraphic flow, arabesque vines), and fusion declaration patterns. Pairs with henna-design-intelligence, abaya-fashion-ai, and template-intelligence-engine.
icon: globe
color: Pink
---

# Cultural Pattern Specialist

The cultural-knowledge specialist. Prevents the AI from producing "vaguely Middle Eastern" generic output by encoding region-specific rules.

## When to Use

- Designing region-specific aesthetics
- Defining `CulturalStyle` values for the template engine
- Reviewing prompts for cultural authenticity
- Resolving fusion requests with explicit declaration

## Cultural Style Hierarchy

```kotlin
sealed class CulturalStyle {
    object Najdi : CulturalStyle()         // Central Arabia, geometric, earth tones
    object Hijazi : CulturalStyle()        // Western Arabia, ornate, jewel tones
    object Khaleeji : CulturalStyle()      // Gulf, bold floral, black + gold
    object Moroccan : CulturalStyle()      // Geometric tilework, jewel + warm
    object Levantine : CulturalStyle()     // Damascene inlay, mother-of-pearl
    object Persian : CulturalStyle()       // Arabesque, fine miniature detail
    object Mughal : CulturalStyle()        // South Asian Islamic, floral arabesque
    object NorthAfrican : CulturalStyle()  // Berber, tribal geometry
    object Andalusian : CulturalStyle()    // Moorish Spain, intricate tile
    data class Fusion(val base: CulturalStyle, val accent: CulturalStyle) : CulturalStyle()
}
```

`Fusion` is the ONLY way to combine styles. Never let the LLM blend by accident.

## Motif Libraries (per style, abbreviated)

```kotlin
val NAJDI_MOTIFS = MotifLibrary(
    primary = listOf("triangular geometric", "diamond grid", "stepped pyramid"),
    secondary = listOf("zigzag border", "linear knot"),
    forbidden = listOf("paisley", "peacock", "mughal floral")
)

val MOROCCAN_MOTIFS = MotifLibrary(
    primary = listOf("8-pointed star", "zellige tile", "interlaced geometric"),
    secondary = listOf("calligraphic border", "rosette"),
    forbidden = listOf("paisley", "khaleeji floral")
)

val PERSIAN_MOTIFS = MotifLibrary(
    primary = listOf("arabesque vine", "boteh / paisley", "miniature floral"),
    secondary = listOf("calligraphic medallion", "lotus rosette"),
    forbidden = listOf("tribal geometric blocks", "khaleeji bold floral")
)
```

`forbidden` is critical — drives the negative prompt downstream.

## Palettes (canonical)

| Style | Palette |
|---|---|
| Najdi | sand, terracotta, deep red, indigo, black |
| Hijazi | emerald, gold, burgundy, ivory |
| Khaleeji | black, gold, white, deep red accent |
| Moroccan | cobalt blue, terracotta, saffron, white, jade |
| Levantine | mother-of-pearl, walnut brown, deep teal |
| Persian | turquoise, lapis, saffron, rose, ivory |
| Mughal | jade, ruby, gold, pearl |
| NorthAfrican | indigo, henna red, sand, charcoal |
| Andalusian | deep blue, white, ochre, terracotta |

## Composition Rules

| Style | Grid type | Symmetry | Flow |
|---|---|---|---|
| Najdi | triangular | bilateral | linear, axis-aligned |
| Khaleeji | free-form floral | bilateral | radial from focal flower |
| Moroccan | 8-fold star grid | radial 8-fold | tessellating tile |
| Persian | flowing arabesque | medallion radial | curvilinear vine |
| NorthAfrican | triangular | bilateral | banded horizontal |

## Prompt Block Generator

```kotlin
fun culturalPromptBlock(style: CulturalStyle): PromptBlock {
    val motifs = motifLibraryFor(style)
    return PromptBlock(
        positive = motifs.primary.joinToString(", ") + ", " +
                   paletteFor(style).joinToString(", ") + " palette, " +
                   compositionFor(style),
        negative = motifs.forbidden.joinToString(", ") + ", generic middle eastern, fusion"
    )
}
```

For `Fusion(base, accent)`: 70% weight to base motifs, 30% to accent. Always declare in the prompt.

## Output

Per micro-task:
- `CulturalStyle.kt` sealed hierarchy
- `MotifLibrary.kt` per style
- `CulturalPalette.kt`
- `CulturalPromptBuilder.kt`
- Critique heuristic for cultural authenticity

## Anti-Patterns

- Treating "Middle Eastern" as a single style
- Mixing Khaleeji floral with Moroccan geometric without `Fusion` declaration
- Letting the LLM choose `CulturalStyle` from free text
- Missing `forbidden` motifs in the negative prompt
- Hardcoded palettes outside this skill's data files
- Cultural appropriation without context (e.g., henna designs labeled as one style but using another's motifs)
