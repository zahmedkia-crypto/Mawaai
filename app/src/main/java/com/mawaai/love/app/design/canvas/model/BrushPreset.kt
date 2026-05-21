package com.mawaai.love.app.design.canvas.model

import androidx.compose.ui.graphics.Color
import com.mawaai.love.app.R

data class BrushPreset(
    val type: BrushType,
    val nameRes: Int,
    val defaults: BrushSettings
) {
    fun applyToColor(color: Color): BrushSettings = defaults.copy(color = color)
}

object BrushPresetCatalog {
    val all: List<BrushPreset> = listOf(
        BrushPreset(
            type = BrushType.PENCIL,
            nameRes = R.string.brush_pencil,
            defaults = BrushSettings(
                type = BrushType.PENCIL, size = 6f, opacity = 0.85f,
                hardness = 0.6f, spacing = 0.05f, scatter = 0.05f, jitter = 0.1f, flow = 0.9f
            )
        ),
        BrushPreset(
            type = BrushType.INK,
            nameRes = R.string.brush_ink,
            defaults = BrushSettings(
                type = BrushType.INK, size = 8f, opacity = 1f,
                hardness = 1f, spacing = 0.04f, scatter = 0f, jitter = 0f, flow = 1f
            )
        ),
        BrushPreset(
            type = BrushType.CALLIGRAPHY,
            nameRes = R.string.brush_calligraphy,
            defaults = BrushSettings(
                type = BrushType.CALLIGRAPHY, size = 18f, opacity = 1f,
                hardness = 0.95f, spacing = 0.03f, scatter = 0f, jitter = 0.3f, flow = 1f
            )
        ),
        BrushPreset(
            type = BrushType.MARKER,
            nameRes = R.string.brush_marker,
            defaults = BrushSettings(
                type = BrushType.MARKER, size = 22f, opacity = 0.7f,
                hardness = 0.85f, spacing = 0.04f, scatter = 0f, jitter = 0f, flow = 0.85f
            )
        ),
        BrushPreset(
            type = BrushType.AIRBRUSH,
            nameRes = R.string.brush_airbrush,
            defaults = BrushSettings(
                type = BrushType.AIRBRUSH, size = 60f, opacity = 0.25f,
                hardness = 0.1f, spacing = 0.05f, scatter = 0.1f, jitter = 0f, flow = 0.5f
            )
        ),
        BrushPreset(
            type = BrushType.WATERCOLOR,
            nameRes = R.string.brush_watercolor,
            defaults = BrushSettings(
                type = BrushType.WATERCOLOR, size = 48f, opacity = 0.35f,
                hardness = 0.2f, spacing = 0.08f, scatter = 0.15f, jitter = 0.4f, flow = 0.6f
            )
        ),
        BrushPreset(
            type = BrushType.HENNA,
            nameRes = R.string.brush_henna,
            defaults = BrushSettings(
                type = BrushType.HENNA, color = Color(0xFF8B2F0F),
                size = 14f, opacity = 0.95f,
                hardness = 0.85f, spacing = 0.05f, scatter = 0.04f, jitter = 0.2f, flow = 1f
            )
        ),
        BrushPreset(
            type = BrushType.EMBROIDERY,
            nameRes = R.string.brush_embroidery,
            defaults = BrushSettings(
                type = BrushType.EMBROIDERY, size = 10f, opacity = 1f,
                hardness = 1f, spacing = 0.5f, scatter = 0.05f, jitter = 0.15f, flow = 1f
            )
        ),
        BrushPreset(
            type = BrushType.PATTERN,
            nameRes = R.string.brush_pattern,
            defaults = BrushSettings(
                type = BrushType.PATTERN, color = Color(0xFFC8860A),
                size = 36f, opacity = 0.9f,
                hardness = 0.9f, spacing = 1f, scatter = 0f, jitter = 0.25f, flow = 1f
            )
        ),
        BrushPreset(
            type = BrushType.CHARCOAL,
            nameRes = R.string.brush_charcoal,
            defaults = BrushSettings(
                type = BrushType.CHARCOAL, size = 32f, opacity = 0.7f,
                hardness = 0.45f, spacing = 0.06f, scatter = 0.25f, jitter = 0.5f, flow = 0.7f
            )
        )
    )

    fun byType(type: BrushType): BrushPreset =
        all.firstOrNull { it.type == type } ?: all.first()
}
