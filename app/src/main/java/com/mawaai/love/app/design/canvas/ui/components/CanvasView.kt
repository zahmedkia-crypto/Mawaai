package com.mawaai.love.app.design.canvas.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaai.love.app.design.canvas.engine.CanvasEngine
import com.mawaai.love.app.design.canvas.model.ToolType

@Composable
fun CanvasView(
    engine: CanvasEngine,
    tool: ToolType,
    onBrushBegin: (Offset) -> Unit,
    onBrushExtend: (Offset) -> Unit,
    onBrushEnd: () -> Unit,
    onShapeCommit: (Offset, Offset) -> Unit,
    onFillTap: (Offset) -> Unit,
    onEyedropTap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val invalidations by engine.invalidations.collectAsStateWithLifecycle()
    val canvasSize = engine.canvasSize

    // Float-state for the gesture scale to avoid autoboxing each pinch
    // tick. `pan` and `viewSize` are non-primitive so they stay on the
    // generic mutableStateOf.
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .onSizeChanged { viewSize = it }
            .pointerInput(canvasSize) {
                detectTransformGestures(panZoomLock = false) { _, panChange, zoomChange, _ ->
                    if (zoomChange != 1f) {
                        scale = (scale * zoomChange).coerceIn(0.5f, 8f)
                    }
                    pan += panChange
                }
            }
            .pointerInput(tool, scale, pan, viewSize) {
                if (tool == ToolType.FILL || tool == ToolType.EYEDROPPER) {
                    detectTapGestures { tap ->
                        val canvasP = viewToCanvas(tap, viewSize, canvasSize, scale, pan)
                        if (tool == ToolType.FILL) onFillTap(canvasP)
                        else onEyedropTap(canvasP.x.toInt(), canvasP.y.toInt())
                    }
                }
            }
            .pointerInput(tool, scale, pan, viewSize) {
                if (tool == ToolType.BRUSH || tool == ToolType.ERASER) {
                    awaitEachGesture {
                        val down = awaitFirstSingleDown() ?: return@awaitEachGesture
                        val initial = viewToCanvas(down.position, viewSize, canvasSize, scale, pan)
                        onBrushBegin(initial)
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!pointer.pressed) break
                                if (pointer.positionChange() != Offset.Zero) {
                                    val cur = viewToCanvas(pointer.position, viewSize, canvasSize, scale, pan)
                                    onBrushExtend(cur)
                                }
                                pointer.consume()
                            }
                        } finally {
                            onBrushEnd()
                        }
                    }
                } else if (tool == ToolType.SHAPE) {
                    awaitEachGesture {
                        val down = awaitFirstSingleDown() ?: return@awaitEachGesture
                        val start = viewToCanvas(down.position, viewSize, canvasSize, scale, pan)
                        var lastEnd = start
                        var change = down
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!pointer.pressed) break
                            lastEnd = viewToCanvas(pointer.position, viewSize, canvasSize, scale, pan)
                            pointer.consume()
                            change = pointer
                        }
                        onShapeCommit(start, lastEnd)
                    }
                }
            }
    ) {
        @Suppress("UNUSED_VARIABLE")
        val tick = invalidations
        drawIntoCanvas { composeCanvas ->
            val nc = composeCanvas.nativeCanvas
            nc.save()
            // Centered fit + manual zoom/pan
            val fitScale = scaleToFit(viewSize, canvasSize)
            val drawW = canvasSize.width * fitScale * scale
            val drawH = canvasSize.height * fitScale * scale
            val tx = (viewSize.width - drawW) / 2f + pan.x
            val ty = (viewSize.height - drawH) / 2f + pan.y
            nc.translate(tx, ty)
            nc.scale(fitScale * scale, fitScale * scale)

            nc.drawRect(
                0f, 0f,
                canvasSize.width.toFloat(), canvasSize.height.toFloat(),
                CheckerboardCache.paint()
            )
            // Cached composite — do NOT recycle (engine owns it).
            nc.drawBitmap(engine.composite(), 0f, 0f, null)
            nc.restore()
        }
    }
}

/** A tiny 32x32 checkerboard tile + shader Paint, lazily built and reused across frames. */
private object CheckerboardCache {
    private val lock = Any()
    private var cachedPaint: Paint? = null

    fun paint(): Paint {
        cachedPaint?.let { return it }
        synchronized(lock) {
            cachedPaint?.let { return it }
            val cell = 16
            val tile = Bitmap.createBitmap(cell * 2, cell * 2, Bitmap.Config.ARGB_8888)
            val c = AndroidCanvas(tile)
            val light = Paint().apply { color = android.graphics.Color.rgb(220, 220, 220) }
            val dark = Paint().apply { color = android.graphics.Color.rgb(180, 180, 180) }
            c.drawRect(0f, 0f, (cell * 2).toFloat(), (cell * 2).toFloat(), light)
            c.drawRect(0f, 0f, cell.toFloat(), cell.toFloat(), dark)
            c.drawRect(cell.toFloat(), cell.toFloat(), (cell * 2).toFloat(), (cell * 2).toFloat(), dark)
            val p = Paint().apply {
                shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                isFilterBitmap = false
            }
            cachedPaint = p
            return p
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitFirstSingleDown(): androidx.compose.ui.input.pointer.PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val pressed = event.changes.filter { it.pressed }
        if (pressed.size == 1 && pressed.first().changedToDownOrFirstFrame()) {
            return pressed.first().also { it.consume() }
        }
        if (pressed.size > 1) return null
    }
}

private fun androidx.compose.ui.input.pointer.PointerInputChange.changedToDownOrFirstFrame(): Boolean =
    pressed && (!previousPressed)

private fun viewToCanvas(
    p: Offset,
    viewSize: IntSize,
    canvasSize: IntSize,
    scale: Float,
    pan: Offset
): Offset {
    val fitScale = scaleToFit(viewSize, canvasSize)
    val totalScale = fitScale * scale
    val drawW = canvasSize.width * totalScale
    val drawH = canvasSize.height * totalScale
    val tx = (viewSize.width - drawW) / 2f + pan.x
    val ty = (viewSize.height - drawH) / 2f + pan.y
    val cx = ((p.x - tx) / totalScale).coerceIn(0f, canvasSize.width.toFloat() - 1f)
    val cy = ((p.y - ty) / totalScale).coerceIn(0f, canvasSize.height.toFloat() - 1f)
    return Offset(cx, cy)
}

private fun scaleToFit(viewSize: IntSize, canvasSize: IntSize): Float {
    if (viewSize.width <= 0 || viewSize.height <= 0) return 1f
    val sx = viewSize.width.toFloat() / canvasSize.width.toFloat()
    val sy = viewSize.height.toFloat() / canvasSize.height.toFloat()
    return minOf(sx, sy)
}


