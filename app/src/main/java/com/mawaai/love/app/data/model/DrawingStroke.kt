package com.mawaai.love.app.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float = 1f,
    val layerIndex: Int = 1
)

data class DrawingState(
    val strokes: List<DrawingStroke> = emptyList(),
    val currentColor: Color = Color(0xFFE8A7B5),
    val currentStrokeWidth: Float = 5f,
    val currentAlpha: Float = 1f,
    val currentLayer: Int = 1,
    val canvasBackground: Color = Color.White
)
