package com.mawaai.love.app.design.canvas.model

import androidx.compose.ui.graphics.Color

enum class BrushType {
    PENCIL, INK, CALLIGRAPHY, MARKER, AIRBRUSH,
    WATERCOLOR, HENNA, EMBROIDERY, PATTERN, CHARCOAL,
    ERASER_SOFT, ERASER_HARD
}

data class BrushSettings(
    val type: BrushType = BrushType.PENCIL,
    val color: Color = Color.Black,
    val size: Float = 16f,         // pixels (1f..200f)
    val opacity: Float = 1f,       // 0f..1f
    val hardness: Float = 1f,      // 0f..1f (0=very soft, 1=hard edge)
    val spacing: Float = 0.1f,     // 0.01f..2f (fraction of size between stamps)
    val scatter: Float = 0f,       // 0f..1f (fraction of size for random offset)
    val jitter: Float = 0f,        // 0f..1f (fraction of size for random size variance)
    val flow: Float = 1f           // 0f..1f (paint flow per stamp)
)
