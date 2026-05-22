package com.mawaai.love.app.design.template

import android.graphics.Color
import android.graphics.PointF
import com.mawaai.love.app.design.ai.processors.BlendMode

/**
 * Predefined [TemplateContext] specs for the henna family.
 *
 * Zone polygons are normalised to `[0, 1]` of the base template bitmap. The
 * values declared here are *rough* starting points sized to typical henna
 * template asset proportions; refine them per asset by supplying
 * [com.mawaai.love.app.design.domain.model.TemplateMetadata.targetQuad]
 * overrides on the runtime [com.mawaai.love.app.design.domain.model.Template].
 */
object HennaTemplates {

    val FOOT_TEMPLATE: TemplateContext = TemplateContext(
        type = TemplateType.HENNA_FOOT,
        applicationMethod = ApplicationMethod.HENNA_ENGRAVING,
        surfaceTexture = "human_skin_foot",
        placementZones = listOf(
            PlacementZone(
                id = "dorsum",
                priority = ZonePriority.PRIMARY,
                boundaryPoints = footDorsumPoints(),
                warpIntensity = 0.30f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.75f
            ),
            PlacementZone(
                id = "ankle",
                priority = ZonePriority.SECONDARY,
                boundaryPoints = anklePoints(),
                warpIntensity = 0.40f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.60f
            ),
            PlacementZone(
                id = "toes",
                priority = ZonePriority.ACCENT,
                boundaryPoints = toePoints(),
                warpIntensity = 0.20f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.40f
            )
        ),
        culturalOrigin = "South Asian / Middle Eastern",
        colorPalette = hennaColorPalette(),
        designConstraints = listOf(
            "Design flows from ankle downward",
            "Heavier density at the centre of the foot",
            "Lighter motifs at toe tips",
            "Avoid natural foot crease lines",
            "Maintain organic flow \u2014 no sharp geometric edges"
        )
    )

    val PALM_TEMPLATE: TemplateContext = TemplateContext(
        type = TemplateType.HENNA_PALM,
        applicationMethod = ApplicationMethod.HENNA_ENGRAVING,
        surfaceTexture = "human_skin_palm",
        placementZones = listOf(
            PlacementZone(
                id = "palm_center",
                priority = ZonePriority.PRIMARY,
                boundaryPoints = palmCenterPoints(),
                warpIntensity = 0.25f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.80f
            ),
            PlacementZone(
                id = "fingers_inner",
                priority = ZonePriority.SECONDARY,
                boundaryPoints = fingerInnerPoints(),
                warpIntensity = 0.35f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.50f
            ),
            PlacementZone(
                id = "wrist_band",
                priority = ZonePriority.ACCENT,
                boundaryPoints = wristBandPoints(),
                warpIntensity = 0.30f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.90f
            )
        ),
        culturalOrigin = "Middle Eastern / South Asian",
        colorPalette = hennaColorPalette(),
        designConstraints = listOf(
            "Mandala or focal motif at palm centre",
            "Design radiates outward from centre",
            "Fingers carry lighter, simpler patterns",
            "Respect natural palm crease lines",
            "Symmetric around the vertical palm axis"
        )
    )

    val WRIST_TEMPLATE: TemplateContext = TemplateContext(
        type = TemplateType.HENNA_WRIST,
        applicationMethod = ApplicationMethod.HENNA_ENGRAVING,
        surfaceTexture = "human_skin_wrist",
        placementZones = listOf(
            PlacementZone(
                id = "bracelet_band",
                priority = ZonePriority.PRIMARY,
                boundaryPoints = wristBandPoints(),
                warpIntensity = 0.20f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.85f
            ),
            PlacementZone(
                id = "hand_back_extension",
                priority = ZonePriority.SECONDARY,
                boundaryPoints = handBackExtensionPoints(),
                warpIntensity = 0.30f,
                textureBlendMode = BlendMode.MULTIPLY,
                maxDesignCoverage = 0.55f
            )
        ),
        culturalOrigin = "Middle Eastern / Khaleeji",
        colorPalette = hennaColorPalette(),
        designConstraints = listOf(
            "Treat the band as a circular flow",
            "Lighter detail spilling onto the hand",
            "Avoid the wrist bone \u2014 let the design wrap around it",
            "Symmetric on both edges of the band"
        )
    )

    // ─── Normalised zone polygons ───────────────────────────────────
    // All coordinates are [0, 1] of the base template bitmap, top-left origin.

    private fun footDorsumPoints(): List<PointF> = listOf(
        PointF(0.30f, 0.20f), PointF(0.70f, 0.20f),
        PointF(0.78f, 0.55f), PointF(0.70f, 0.75f),
        PointF(0.30f, 0.75f), PointF(0.22f, 0.55f)
    )

    private fun anklePoints(): List<PointF> = listOf(
        PointF(0.28f, 0.05f), PointF(0.72f, 0.05f),
        PointF(0.72f, 0.20f), PointF(0.28f, 0.20f)
    )

    private fun toePoints(): List<PointF> = listOf(
        PointF(0.25f, 0.80f), PointF(0.75f, 0.80f),
        PointF(0.78f, 0.97f), PointF(0.22f, 0.97f)
    )

    private fun palmCenterPoints(): List<PointF> = listOf(
        PointF(0.30f, 0.40f), PointF(0.70f, 0.40f),
        PointF(0.75f, 0.70f), PointF(0.50f, 0.85f),
        PointF(0.25f, 0.70f)
    )

    private fun fingerInnerPoints(): List<PointF> = listOf(
        PointF(0.20f, 0.05f), PointF(0.80f, 0.05f),
        PointF(0.78f, 0.40f), PointF(0.22f, 0.40f)
    )

    private fun wristBandPoints(): List<PointF> = listOf(
        PointF(0.15f, 0.85f), PointF(0.85f, 0.85f),
        PointF(0.85f, 0.97f), PointF(0.15f, 0.97f)
    )

    private fun handBackExtensionPoints(): List<PointF> = listOf(
        PointF(0.20f, 0.45f), PointF(0.80f, 0.45f),
        PointF(0.78f, 0.80f), PointF(0.22f, 0.80f)
    )

    // ─── Henna palette ────────────────────────────────────────────────
    // Earth-tone browns and dusty oranges authentic to natural henna paste.

    private fun hennaColorPalette(): List<Int> = listOf(
        Color.rgb(0x4A, 0x2C, 0x1A), // deep henna brown
        Color.rgb(0x6B, 0x3F, 0x21), // mid henna brown
        Color.rgb(0x8A, 0x55, 0x2D), // warm cinnamon
        Color.rgb(0xA8, 0x6E, 0x42), // light terracotta
        Color.rgb(0xC9, 0x95, 0x6B)  // pale henna tan
    )
}
