package com.mawaai.love.app.design.canvas.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.mawaai.love.app.design.canvas.model.BlendMode
import com.mawaai.love.app.design.canvas.model.Layer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the ordered list of layers (bottom-first) and the active layer index.
 * All bitmap mutations happen through here.
 */
class LayerManager(
    private val canvasWidth: Int,
    private val canvasHeight: Int
) {
    private val _layers = MutableStateFlow<List<Layer>>(emptyList())
    val layers: StateFlow<List<Layer>> = _layers

    private val _activeLayerId = MutableStateFlow(0)
    val activeLayerId: StateFlow<Int> = _activeLayerId

    private var nextId = 0
    private var onInvalidate: (() -> Unit)? = null

    /** Wired by [CanvasEngine] so structural/visual layer changes invalidate the cached composite. */
    internal fun setInvalidationListener(listener: () -> Unit) {
        onInvalidate = listener
    }

    init {
        addLayer("Background")
    }

    fun addLayer(name: String): Layer {
        val bmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val layer = Layer(id = nextId++, name = name, bitmap = bmp)
        _layers.value = _layers.value + layer
        _activeLayerId.value = layer.id
        invalidate()
        return layer
    }

    fun deleteLayer(id: Int) {
        val list = _layers.value.toMutableList()
        if (list.size <= 1) return
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val removed = list.removeAt(idx)
        removed.bitmap.recycle()
        _layers.value = list
        if (_activeLayerId.value == id) {
            _activeLayerId.value = list[(idx).coerceAtMost(list.lastIndex).coerceAtLeast(0)].id
        }
        invalidate()
    }

    fun duplicateLayer(id: Int) {
        val list = _layers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val src = list[idx]
        val copy = src.bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return
        val dup = src.copy(id = nextId++, name = "${src.name} copy", bitmap = copy)
        list.add(idx + 1, dup)
        _layers.value = list
        _activeLayerId.value = dup.id
        invalidate()
    }

    fun setActive(id: Int) {
        if (_layers.value.any { it.id == id }) _activeLayerId.value = id
    }

    fun setVisible(id: Int, visible: Boolean) =
        update(id) { it.copy(visible = visible) }.also { invalidate() }

    fun setOpacity(id: Int, opacity: Float) =
        update(id) { it.copy(opacity = opacity.coerceIn(0f, 1f)) }.also { invalidate() }

    fun setBlendMode(id: Int, mode: BlendMode) =
        update(id) { it.copy(blend = mode) }.also { invalidate() }

    fun rename(id: Int, name: String) = update(id) { it.copy(name = name) }

    fun moveLayer(id: Int, toIndex: Int) {
        val list = _layers.value.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return
        val item = list.removeAt(from)
        list.add(toIndex.coerceIn(0, list.size), item)
        _layers.value = list
        invalidate()
    }

    fun mergeDown(id: Int) {
        val list = _layers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx <= 0) return
        val top = list[idx]
        val below = list[idx - 1]
        val canvas = Canvas(below.bitmap)
        val paint = Paint().apply {
            alpha = (top.opacity * 255f).toInt().coerceIn(0, 255)
            xfermode = PorterDuffXfermode(blendMode(top.blend))
        }
        canvas.drawBitmap(top.bitmap, 0f, 0f, paint)
        list.removeAt(idx)
        top.bitmap.recycle()
        _layers.value = list
        _activeLayerId.value = below.id
        invalidate()
    }

    fun activeLayer(): Layer? =
        _layers.value.firstOrNull { it.id == _activeLayerId.value }

    private fun update(id: Int, transform: (Layer) -> Layer) {
        _layers.value = _layers.value.map { if (it.id == id) transform(it) else it }
    }

    private fun invalidate() { onInvalidate?.invoke() }

    fun blendMode(mode: BlendMode): PorterDuff.Mode = when (mode) {
        BlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
        BlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
        BlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
        BlendMode.SCREEN -> PorterDuff.Mode.SCREEN
        // PorterDuff has no SOFT_LIGHT — fall back to OVERLAY which is visually similar.
        BlendMode.SOFT_LIGHT -> PorterDuff.Mode.OVERLAY
    }

    fun composite(): Bitmap {
        val out = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        compositeInto(out)
        return out
    }

    /**
     * Recycles every layer bitmap and empties the stack. Call this from the
     * owner's lifecycle teardown (e.g. ViewModel.onCleared) — the manager is
     * unusable afterwards. Idempotent: safe to call multiple times.
     */
    fun releaseAll() {
        for (layer in _layers.value) {
            if (!layer.bitmap.isRecycled) layer.bitmap.recycle()
        }
        _layers.value = emptyList()
        onInvalidate = null
    }

    /**
     * Composites all visible layers into [target] (must be the canvas size + ARGB_8888).
     * Clears the target first. No allocations besides the per-call Paint.
     */
    fun compositeInto(target: Bitmap) {
        val canvas = Canvas(target)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val paint = Paint()
        for (layer in _layers.value) {
            if (!layer.visible) continue
            paint.alpha = (layer.opacity * 255f).toInt().coerceIn(0, 255)
            paint.xfermode = PorterDuffXfermode(blendMode(layer.blend))
            canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
        }
    }
}
