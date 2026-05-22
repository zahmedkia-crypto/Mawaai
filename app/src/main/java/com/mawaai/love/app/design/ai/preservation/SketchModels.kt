package com.mawaai.love.app.design.ai.preservation

import android.graphics.Bitmap
import android.graphics.Rect

data class ScannedSketch(
    val cleanPng: Bitmap,
    val inkMask: Bitmap,
    val contourMask: Bitmap,
    val backgroundRemoved: Boolean,
    val bounds: Rect
)

data class SketchAnalysis(
    val bounds: Rect,
    val symmetryAxisX: Float,
    val symmetryScore: Float,
    val dominantContourCount: Int,
    val averageStrokeWidthPx: Float,
    val densityMap: List<StrokeDensityCell>,
    val emptySpaceBalance: Float,
    val curveConfidence: Float,
    val lineShakiness: Float
)

data class StrokeDensityCell(
    val row: Int,
    val column: Int,
    val density: Float
)

data class ImprovedSketch(
    val bitmap: Bitmap,
    val analysis: SketchAnalysis
)

enum class MaterialTarget {
    EMBROIDERY_GOLD,
    EMBROIDERY_DARK
}

data class RenderedDesign(
    val bitmap: Bitmap,
    val material: MaterialTarget
)
