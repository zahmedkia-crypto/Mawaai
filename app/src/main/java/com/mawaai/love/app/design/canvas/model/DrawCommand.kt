package com.mawaai.love.app.design.canvas.model

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed class DrawCommand {
    abstract val layerId: Int

    data class Stroke(
        override val layerId: Int,
        val brush: BrushSettings,
        val points: List<Offset>,
        val symmetry: SymmetryMode,
        val canvasSize: androidx.compose.ui.unit.IntSize
    ) : DrawCommand()

    data class Shape(
        override val layerId: Int,
        val settings: ShapeSettings,
        val start: Offset,
        val end: Offset
    ) : DrawCommand()

    data class Fill(
        override val layerId: Int,
        val point: Offset,
        val color: Color,
        val tolerance: Int = 32
    ) : DrawCommand()

    data class Erase(
        override val layerId: Int,
        val brush: BrushSettings,
        val points: List<Offset>,
        val symmetry: SymmetryMode,
        val canvasSize: androidx.compose.ui.unit.IntSize
    ) : DrawCommand()

    data class ClearLayer(override val layerId: Int) : DrawCommand()

    data class StampBitmap(
        override val layerId: Int,
        val bitmap: Bitmap,
        val target: androidx.compose.ui.geometry.Rect
    ) : DrawCommand()
}
