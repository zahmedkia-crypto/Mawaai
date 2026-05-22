---
name: henna-design-intelligence
description: Encodes authentic mehndi / henna composition rules for AI design generation. Use whenever generating henna designs on palms, feet, arms, or back-of-hand, or scoring/critiquing generated henna designs. Produces cultural pattern rules (Khaleeji, Indian, Moroccan, Sudanese styles), density maps per region, symmetry constraints, flow guidance from fingertip to wrist, motif hierarchies (mandala, paisley, vines, geometric), and prompt building blocks that respect tradition. Pairs with stable-diffusion-pipeline-builder for prompt synthesis and cultural-pattern-specialist for broader cultural rules.
icon: hand
color: Pink
---

# Henna Design Intelligence

Domain knowledge for authentic mehndi composition. Drives prompt synthesis and template scoring for body-art templates.

## When to Use

- Generating henna / mehndi designs
- Scoring or critiquing generated designs for authenticity
- Building palm / foot template definitions
- Adding cultural style constraints to the prompt synthesizer

## Style Profiles

```kotlin
enum class HennaStyle {
    KHALEEJI,   // bold, floral, large negative space, finger tips emphasized
    INDIAN,     // dense, detailed, paisley + peacock + mandala
    MOROCCAN,   // geometric, lines, diamonds, less floral
    SUDANESE,   // black jagua-influenced, bold geometric blocks
    MINIMALIST, // single motif, large negative space, contemporary
    FUSION      // explicit blend, must declare base + accent
}
```

## Composition Rules

### Density Map (palm, normalized 0..1 from wrist [0] to fingertip [1])

| Style | Wrist | Mid-palm | Fingertip |
|---|---|---|---|
| KHALEEJI | low (0.2) | medium (0.4) | high (0.85) |
| INDIAN | high (0.8) | high (0.9) | very high (0.95) |
| MOROCCAN | medium (0.5) | high (0.7) | medium (0.5) |
| SUDANESE | medium (0.6) | high (0.75) | medium (0.5) |
| MINIMALIST | none (0.0) | one focal motif | low (0.2) |

### Symmetry

- Palm front: bilateral symmetry along the middle finger axis
- Back of hand: free-form allowed
- Foot: bilateral along the central toe
- Wrist band: rotational symmetry around the wrist

### Flow

- Lines move FROM wrist TO fingertips in Khaleeji + Minimalist
- Lines radiate FROM center IN Indian (mandala) and Moroccan (geometric)
- Never break flow at joint creases

## Motif Hierarchy (by style)

```kotlin
data class MotifSet(
    val primary: List<Motif>,   // dominant, large
    val secondary: List<Motif>, // accents
    val filler: List<Motif>,    // negative-space dots, fine lines
)

val KHALEEJI_MOTIFS = MotifSet(
    primary = listOf(Motif.LargeFlower, Motif.Crescent),
    secondary = listOf(Motif.Vine, Motif.Leaf),
    filler = listOf(Motif.Dot, Motif.FineLine)
)
// ... per style
```

## Prompt Building Blocks

For `prompt-system-architect` to consume:

```
[STYLE: KHALEEJI]
Positive keywords: bold floral henna, large flower on palm, finger-tip emphasis,
  flowing vines from wrist, generous negative space, deep burgundy stain
Negative: tribal lines, sharp geometric, dense filling, paisley
Density: low wrist, medium palm, high fingertips
Symmetry: bilateral along middle finger
```

## Critique Heuristics

When scoring a generated design:
1. Style match (motif hierarchy adherence)
2. Density profile match (use density map)
3. Symmetry compliance
4. Flow continuity (no broken lines at joints)
5. Cultural authenticity (no mixing of incompatible motifs unless FUSION)

## Output

Per micro-task:
- `HennaStyle.kt` enum + `MotifSet.kt` data classes
- `HennaDensityMap.kt` per style
- `HennaPromptBlocks.kt` (positive / negative builders)
- `HennaCritic.kt` for scoring generated outputs
- Reference table of motifs per style in `references/` if expanding

## Anti-Patterns

- Generating "henna" without style declaration (drifts to generic)
- Bilateral symmetry on back-of-hand designs
- Khaleeji density profile applied to Indian style
- Mixing Sudanese geometric blocks with paisley
- Letting the LLM pick the style from free text — always enum-mapped
