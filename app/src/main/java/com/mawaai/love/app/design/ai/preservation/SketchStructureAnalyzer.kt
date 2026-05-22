package com.mawaai.love.app.design.ai.preservation

import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Singleton
class SketchStructureAnalyzer @Inject constructor() {

    suspend fun analyze(sketch: ScannedSketch): SketchAnalysis = withContext(Dispatchers.Default) {
        val mask = sketch.inkMask
        val bounds = sketch.bounds
        val grid = Array(GRID) { IntArray(GRID) }
        var inkPixels = 0
        var edgeTransitions = 0
        var totalRuns = 0
        var runWidthSum = 0
        var leftInk = 0
        var rightInk = 0

        val centerX = bounds.exactCenterX()
        for (y in 0 until mask.height) {
            var inRun = false
            var runStart = 0
            var previousInk = false
            for (x in 0 until mask.width) {
                val ink = alphaAt(mask, x, y) > ALPHA_THRESHOLD
                if (ink) {
                    inkPixels++
                    if (x < centerX) leftInk++ else rightInk++
                    val row = ((y.toFloat() / mask.height) * GRID).toInt().coerceIn(0, GRID - 1)
                    val col = ((x.toFloat() / mask.width) * GRID).toInt().coerceIn(0, GRID - 1)
                    grid[row][col]++
                    if (!inRun) {
                        inRun = true
                        runStart = x
                    }
                } else if (inRun) {
                    totalRuns++
                    runWidthSum += x - runStart
                    inRun = false
                }
                if (x > 0 && ink != previousInk) edgeTransitions++
                previousInk = ink
            }
            if (inRun) {
                totalRuns++
                runWidthSum += mask.width - runStart
            }
        }

        val densityMap = buildList {
            val cellArea = (mask.width / GRID.toFloat()) * (mask.height / GRID.toFloat())
            for (row in 0 until GRID) {
                for (col in 0 until GRID) {
                    add(StrokeDensityCell(row, col, (grid[row][col] / cellArea).coerceIn(0f, 1f)))
                }
            }
        }

        val balanceDenominator = max(1, leftInk + rightInk).toFloat()
        val emptySpaceBalance = 1f - (abs(leftInk - rightInk) / balanceDenominator).coerceIn(0f, 1f)
        val averageStrokeWidth = if (totalRuns == 0) 1f else (runWidthSum / totalRuns.toFloat()).coerceAtLeast(1f)
        val shakiness = (edgeTransitions / max(1f, inkPixels.toFloat() * 0.28f)).coerceIn(0f, 1f)

        SketchAnalysis(
            bounds = bounds,
            symmetryAxisX = centerX / mask.width,
            symmetryScore = estimateSymmetry(mask, bounds),
            dominantContourCount = estimateContourCount(sketch.contourMask),
            averageStrokeWidthPx = averageStrokeWidth,
            densityMap = densityMap,
            emptySpaceBalance = emptySpaceBalance,
            curveConfidence = (1f - shakiness).coerceIn(0f, 1f),
            lineShakiness = shakiness
        )
    }

    private fun estimateSymmetry(mask: android.graphics.Bitmap, bounds: android.graphics.Rect): Float {
        var compared = 0
        var matched = 0
        val center = bounds.centerX()
        val maxDelta = min(center - bounds.left, bounds.right - center).coerceAtLeast(0)
        for (y in bounds.top until bounds.bottom) {
            for (d in 0 until maxDelta) {
                val left = alphaAt(mask, center - d, y) > ALPHA_THRESHOLD
                val right = alphaAt(mask, center + d, y) > ALPHA_THRESHOLD
                compared++
                if (left == right) matched++
            }
        }
        return if (compared == 0) 0f else matched / compared.toFloat()
    }

    private fun estimateContourCount(contourMask: android.graphics.Bitmap): Int {
        var transitions = 0
        for (y in 0 until contourMask.height step 2) {
            var previous = false
            for (x in 0 until contourMask.width step 2) {
                val ink = alphaAt(contourMask, x, y) > ALPHA_THRESHOLD
                if (ink && !previous) transitions++
                previous = ink
            }
        }
        return max(1, transitions / 8)
    }

    private fun alphaAt(bitmap: android.graphics.Bitmap, x: Int, y: Int): Int =
        Color.alpha(bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1)))

    private companion object {
        const val GRID = 4
        const val ALPHA_THRESHOLD = 32
    }
}
