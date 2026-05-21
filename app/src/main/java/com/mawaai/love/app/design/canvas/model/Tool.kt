package com.mawaai.love.app.design.canvas.model

import androidx.compose.ui.graphics.Color

enum class ToolType { BRUSH, ERASER, FILL, SHAPE, SELECT, EYEDROPPER, MOVE }

enum class ShapeType { LINE, RECT, CIRCLE, POLYGON, STAR }

data class ShapeSettings(
    val shape: ShapeType = ShapeType.LINE,
    val strokeColor: Color = Color.Black,
    val fillColor: Color? = null,
    val strokeWidth: Float = 6f,
    val polygonSides: Int = 5,
    val starPoints: Int = 5
)

enum class SymmetryMode {
    OFF, VERTICAL, HORIZONTAL, RADIAL_2, RADIAL_4, RADIAL_6, RADIAL_8
}

enum class BlendMode {
    NORMAL, MULTIPLY, OVERLAY, SCREEN, SOFT_LIGHT
}
