package com.mawaai.love.app.design.template

import android.graphics.Color
import android.graphics.PointF
import com.mawaai.love.app.design.ai.processors.BlendMode

/**
 * Predefined [TemplateContext] specs for the garment family.
 *
 * Sub-types covered:
 *  - **Sudanese toub** — full-body wrap, hem-led embroidery (Nubian motifs).
 *  - **Islamic abaya** — cuffs + front-panel embroidery on chiffon.
 *  - **Saudi thobe** — men's long shirt, collar + chest embroidery.
 *  - **Bisht** — ceremonial cloak with gold trim.
 *
 * Polygon coordinates follow the same `[0, 1]` normalisation as
 * [HennaTemplates]. Refine per asset via
 * [com.mawaai.love.app.design.domain.model.TemplateMetadata.targetQuad].
 */
object GarmentTemplates {

    val SUDANESE_TOUB: TemplateContext = TemplateContext(
        type = TemplateType.SUDANESE_TOUB,
        applicationMethod = ApplicationMethod.EMBROIDERY,
        surfaceTexture = "fine_cotton_white",
        placementZones = listOf(
            PlacementZone(
                id = "border_hem",
                priority = ZonePriority.PRIMARY,
                boundaryPoints = toubHemPoints(),
                warpIntensity = 0.15f,
                textureBlendMode = BlendMode.OVERLAY,
                maxDesignCoverage = 0.85f
            ),
            PlacementZone(
                id = "chest_panel",
                priority = ZonePriority.SECONDARY,
                boundaryPoints = toubChestPanelPoints(),
                warpIntensity = 0.10f,
                textureBlendMode = BlendMode.OVERLAY,
                maxDesignCoverage = 0.50f
            )
        ),
        culturalOrigin = "Sudanese / Nubian",
        colorPalette = sudaneseColorPalette(),
        designConstraints = listOf(
            "Embroidery follows fabric drape direction",
            "Nubian geometric motifs preferred",
            "Gold or vivid colours on white fabric",
            "Border patterns repeat consistently",
            "Respect fabric fold lines"
        )
    )

    val ISLAMIC_ABAYA: TemplateContext = TemplateContext(
        type = TemplateType.ISLAMIC_ABAYA,
        applicationMethod = ApplicationMethod.EMBROIDERY,
        surfaceTexture = "chiffon_black",
        placementZones = listOf(
            PlacementZone(
                id = "cuffs",
                priority = ZonePriority.PRIMARY,
                boundaryPoints = abayaCuffPoints(),
                warpIntensity = 0.10f,
                textureBlendMode = BlendMode.SCREEN,
                maxDesignCoverage = 0.90f
            ),
            PlacementZone(
                id = "front_panel",
                priority = ZonePriority.SECONDARY,
                boundaryPoints = abayaFrontPanelPoints(),
                warpIntensity = 0.05f,
                textureBlendMode = BlendMode.SCREEN,
                maxDesignCoverage = 0.40f
            ),
            PlacementZone(
                id = "collar",
                priority = ZonePriority.ACCENT,
                boundaryPoints = abayaCollarPoints(),
                warpIntensity = 0.20f,
                textureBlendMode = BlendMode.SCREEN,
                maxDesignCoverage = 0.70f
            )
        ),
        culturalOrigin = "Gulf / Islamic",
        colorPalette = abayaColorPalette(),
        designConstraints = listOf(
            "Gold or silver thread on black fabric",
            "Arabesque and geometric Islamic motifs",
            "Embroidery has dimensional depth",
            "Luxury atelier finish quality",
            "Modesty-appropriate coverage patterns"
        )
    )

    val SAUDI_THOBE: TemplateContext = TemplateContext(
        type = TemplateType.SAUDI_THOBE,
        applicationMethod = ApplicationMethod.EMBROIDERY,
        surfaceTexture = "cotton_white",
        placementZones = listOf(
            PlacementZone(
                id = "collar",
                priority = ZonePriority.PRIMARY,
                boundaryPoints = thobeCollarPoints(),
                warpIntensity = 0.10f,
                textureBlendMode = BlendMode.OVERLAY,
                maxDesignCoverage = 0.85f
            ),
            PlacementZone(
                id = "chest_placket",
                priority = ZonePriority.SECONDARY,
                boundaryPoints = thobeChestPlacketPoints(),
                warpIntensity = 0.05f,
                textureBlendMode = BlendMode.OVERLAY,
                maxDesignCoverage = 0.60f
            ),
            PlacementZone(
                id = "cuffs",
                priority = ZonePriority.ACCENT,
                boundaryPoints = thobeCuffPoints(),
                warpIntensity = 0.10f,
                textureBlendMode = BlendMode.OVERLAY,
                maxDesignCoverage = 0.55f
            )
        ),
        culturalOrigin = "Saudi / Khaleeji",
        colorPalette = thobeColorPalette(),
        designConstraints = listOf(
            "Subtle tonal embroidery on white cotton",
            "Collar work is the focal element",
            "Vertical symmetry of the chest placket",
            "Cuff motif mirrors the collar pattern"
        )
    )

    val BISHT: TemplateContext = TemplateContext(
        type = TemplateType.BISHT,
        applicationMethod = ApplicationMethod.EMBROIDERY,
        surfaceTexture = "wool_blend_dark",
        placementZones = listOf(
            PlacementZone(
                id = "front_trim",
                priority = ZonePriority.PRIMARY,
                boundaryPoints = bishtFrontTrimPoints(),
                warpIntensity = 0.10f,
                textureBlendMode = BlendMode.SCREEN,
                maxDesignCoverage = 0.90f
            ),
            PlacementZone(
                id = "shoulder_line",
                priority = ZonePriority.SECONDARY,
                boundaryPoints = bishtShoulderPoints(),
                warpIntensity = 0.10f,
                textureBlendMode = BlendMode.SCREEN,
                maxDesignCoverage = 0.70f
            )
        ),
        culturalOrigin = "Gulf / Ceremonial",
        colorPalette = bishtColorPalette(),
        designConstraints = listOf(
            "Gold-thread trim is the signature element",
            "Trim runs the full vertical front of the cloak",
            "Shoulder line carries a secondary line of trim",
            "Background fabric stays the focal field"
        )
    )

    // ─── Normalised polygons ────────────────────────────────────────────────
    // [0, 1] of the base template bitmap, top-left origin. Rough starting
    // values — refine per asset on the runtime Template.

    private fun toubHemPoints(): List<PointF> = listOf(
        PointF(0.05f, 0.85f), PointF(0.95f, 0.85f),
        PointF(0.95f, 0.97f), PointF(0.05f, 0.97f)
    )

    private fun toubChestPanelPoints(): List<PointF> = listOf(
        PointF(0.30f, 0.18f), PointF(0.70f, 0.18f),
        PointF(0.70f, 0.40f), PointF(0.30f, 0.40f)
    )

    private fun abayaCuffPoints(): List<PointF> = listOf(
        PointF(0.05f, 0.55f), PointF(0.20f, 0.55f),
        PointF(0.20f, 0.85f), PointF(0.05f, 0.85f)
    )

    private fun abayaFrontPanelPoints(): List<PointF> = listOf(
        PointF(0.45f, 0.20f), PointF(0.55f, 0.20f),
        PointF(0.55f, 0.90f), PointF(0.45f, 0.90f)
    )

    private fun abayaCollarPoints(): List<PointF> = listOf(
        PointF(0.40f, 0.05f), PointF(0.60f, 0.05f),
        PointF(0.60f, 0.18f), PointF(0.40f, 0.18f)
    )

    private fun thobeCollarPoints(): List<PointF> = listOf(
        PointF(0.40f, 0.05f), PointF(0.60f, 0.05f),
        PointF(0.60f, 0.20f), PointF(0.40f, 0.20f)
    )

    private fun thobeChestPlacketPoints(): List<PointF> = listOf(
        PointF(0.46f, 0.20f), PointF(0.54f, 0.20f),
        PointF(0.54f, 0.55f), PointF(0.46f, 0.55f)
    )

    private fun thobeCuffPoints(): List<PointF> = listOf(
        PointF(0.05f, 0.55f), PointF(0.18f, 0.55f),
        PointF(0.18f, 0.70f), PointF(0.05f, 0.70f)
    )

    private fun bishtFrontTrimPoints(): List<PointF> = listOf(
        PointF(0.45f, 0.08f), PointF(0.55f, 0.08f),
        PointF(0.55f, 0.95f), PointF(0.45f, 0.95f)
    )

    private fun bishtShoulderPoints(): List<PointF> = listOf(
        PointF(0.15f, 0.10f), PointF(0.85f, 0.10f),
        PointF(0.85f, 0.18f), PointF(0.15f, 0.18f)
    )

    // ─── Garment palettes ──────────────────────────────────────────────

    private fun sudaneseColorPalette(): List<Int> = listOf(
        Color.rgb(0xFF, 0xFF, 0xFF), // base white fabric
        Color.rgb(0xD4, 0xAF, 0x37), // classic Sudanese gold
        Color.rgb(0xB7, 0x33, 0x2A), // deep red embroidery
        Color.rgb(0x1E, 0x39, 0x76), // Nubian indigo
        Color.rgb(0x0B, 0x69, 0x23)  // emerald accent
    )

    private fun abayaColorPalette(): List<Int> = listOf(
        Color.rgb(0x00, 0x00, 0x00), // pure black base
        Color.rgb(0xD4, 0xAF, 0x37), // gold thread
        Color.rgb(0xC0, 0xC0, 0xC0), // silver thread
        Color.rgb(0x6B, 0x48, 0x18), // bronze
        Color.rgb(0x80, 0x60, 0x00)  // antique gold
    )

    private fun thobeColorPalette(): List<Int> = listOf(
        Color.rgb(0xFF, 0xFF, 0xFF), // base white
        Color.rgb(0xEA, 0xE4, 0xCB), // ivory tonal
        Color.rgb(0xD9, 0xC9, 0x9A), // sand
        Color.rgb(0xB0, 0x96, 0x5A)  // light bronze
    )

    private fun bishtColorPalette(): List<Int> = listOf(
        Color.rgb(0x1C, 0x1A, 0x18), // near-black wool
        Color.rgb(0x4A, 0x37, 0x10), // deep brown
        Color.rgb(0xD4, 0xAF, 0x37), // signature gold
        Color.rgb(0xB0, 0x84, 0x1E)  // antique gold
    )
}
