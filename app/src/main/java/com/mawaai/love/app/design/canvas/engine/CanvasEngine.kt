package com.mawaai.love.app.design.canvas.engine

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.mawaai.love.app.design.canvas.model.BrushSettings
import com.mawaai.love.app.design.canvas.model.ShapeSettings
import com.mawaai.love.app.design.canvas.model.SymmetryMode
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Top-level engine that ties layer/history/brush/shape/fill/symmetry together.
 * The view layer should drive only this class — never the sub-engines directly.
 *
 * The engine maintains a cached composite bitmap so the view can read it on every
 * recomposition without allocating a new full-size bitmap per frame. The cache is
 * invalidated (re-composited on next read) whenever a mutating op fires [bump].
 *
 * All mutating brush/shape/fill operations are dispatched onto a single-threaded
 * worker dispatcher so the UI gesture loop never blocks on `BlurMaskFilter`,
 * symmetry fan-out, or WEBP history compression. Operations remain strictly
 * ordered because the dispatcher is single-threaded; the spacing carry inside
 * [BrushEngine] therefore stays correct across rapid pointer events.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CanvasEngine(
    width: Int = 1024,
    height: Int = 1024
) {
    val canvasSize: IntSize = IntSize(width, height)
    val layers = LayerManager(width, height)
    val history = HistoryManager()
    private val brush = BrushEngine()

    private val _invalidations = MutableStateFlow(0)
    val invalidations: StateFlow<Int> = _invalidations
    private fun bump() {
        compositeDirty = true
        _invalidations.value = _invalidations.value + 1
    }

    private val compositeBuffer: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    @Volatile private var compositeDirty: Boolean = true

    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    @Volatile private var released = false

    init {
        layers.setInvalidationListener { bump() }
    }

    /**
     * Open a new freehand brush stroke. Pushes the active layer onto history
     * (so undo restores the pre-stroke bitmap) and hands [initialPoint] to the
     * stateful [BrushEngine] which keeps the spacing carry continuous across
     * subsequent [extendBrushStroke] calls. Symmetry fan-out happens inside
     * the brush engine per stamp.
     */
    fun beginBrushStroke(
        brushSettings: BrushSettings,
        initialPoint: Offset,
        symmetry: SymmetryMode,
        eraseMode: Boolean
    ) {
        workerScope.launch {
            val layer = layers.activeLayer() ?: return@launch
            history.push(layer.id, layer.bitmap)
            brush.beginStroke(
                bitmap = layer.bitmap,
                brush = brushSettings,
                initialPoint = initialPoint,
                symmetry = symmetry,
                canvasSize = canvasSize,
                eraseMode = eraseMode
            )
            bump()
        }
    }

    fun extendBrushStroke(point: Offset) {
        workerScope.launch {
            val layer = layers.activeLayer() ?: return@launch
            brush.extendStroke(layer.bitmap, point)
            bump()
        }
    }

    fun endBrushStroke() {
        workerScope.launch {
            val layer = layers.activeLayer()
            brush.endStroke(layer?.bitmap)
            if (layer != null) bump()
        }
    }

    fun applyShape(settings: ShapeSettings, start: Offset, end: Offset) {
        workerScope.launch {
            val layer = layers.activeLayer() ?: return@launch
            history.push(layer.id, layer.bitmap)
            ShapeEngine.render(layer.bitmap, settings, start, end)
            bump()
        }
    }

    fun applyFill(point: Offset, color: Color, tolerance: Int = 32) {
        workerScope.launch {
            val layer = layers.activeLayer() ?: return@launch
            history.push(layer.id, layer.bitmap)
            FillEngine.fill(layer.bitmap, point.x.toInt(), point.y.toInt(), color, tolerance)
            bump()
        }
    }

    fun pickColorAt(x: Int, y: Int): Color? {
        if (x !in 0 until canvasSize.width || y !in 0 until canvasSize.height) return null
        ensureComposite()
        return Color(compositeBuffer.getPixel(x, y))
    }

    fun clearActiveLayer() {
        workerScope.launch {
            val layer = layers.activeLayer() ?: return@launch
            history.push(layer.id, layer.bitmap)
            val canvas = android.graphics.Canvas(layer.bitmap)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            bump()
        }
    }

    fun undo() {
        workerScope.launch {
            val snap = history.undo(::bitmapForLayer) ?: return@launch
            copyInto(snap.layerId, snap.bitmap)
            snap.bitmap.recycle()
            bump()
        }
    }

    fun redo() {
        workerScope.launch {
            val snap = history.redo(::bitmapForLayer) ?: return@launch
            copyInto(snap.layerId, snap.bitmap)
            snap.bitmap.recycle()
            bump()
        }
    }

    /**
     * Returns the shared cached composite. Callers MUST NOT recycle or mutate it.
     * For a one-shot copy (e.g. export), use [snapshotComposite].
     */
    fun composite(): Bitmap {
        ensureComposite()
        return compositeBuffer
    }

    /**
     * Allocates and returns a fresh independent copy of the current composite.
     * Caller owns the returned bitmap and is responsible for recycling it.
     */
    fun snapshotComposite(): Bitmap {
        ensureComposite()
        return compositeBuffer.copy(Bitmap.Config.ARGB_8888, false)
    }

    /**
     * Re-composites layers into [compositeBuffer] if dirty or if the layer stack
     * structure changed. Cheap when clean (bool check only).
     */
    @Synchronized
    private fun ensureComposite() {
        if (!compositeDirty) return
        layers.compositeInto(compositeBuffer)
        compositeDirty = false
    }

    private fun bitmapForLayer(id: Int): Bitmap? =
        layers.layers.value.firstOrNull { it.id == id }?.bitmap

    private fun copyInto(layerId: Int, source: Bitmap) {
        val target = bitmapForLayer(layerId) ?: return
        val canvas = android.graphics.Canvas(target)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(source, 0f, 0f, null)
    }

    /**
     * Releases every native resource owned by this engine: the worker
     * coroutine scope, the cached composite buffer, every layer bitmap, and
     * the encoded history snapshots. Must be called from the owning
     * ViewModel's `onCleared()` — without it, ~8 MB of bitmap plus a live
     * coroutine job leak per design session. Idempotent.
     */
    fun release() {
        if (released) return
        released = true
        workerScope.cancel()
        if (!compositeBuffer.isRecycled) compositeBuffer.recycle()
        layers.releaseAll()
        history.clear()
    }
}
