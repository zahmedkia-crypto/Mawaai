package com.mawaai.love.app.design.canvas.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.repository.ArtworkRepository
import com.mawaai.love.app.data.repository.ProjectRepository
import com.mawaai.love.app.design.canvas.engine.CanvasEngine
import com.mawaai.love.app.design.canvas.engine.ExportEngine
import com.mawaai.love.app.design.canvas.model.BrushPresetCatalog
import com.mawaai.love.app.design.canvas.model.BrushSettings
import com.mawaai.love.app.design.canvas.model.BrushType
import com.mawaai.love.app.design.canvas.model.ShapeSettings
import com.mawaai.love.app.design.canvas.model.SymmetryMode
import com.mawaai.love.app.design.canvas.model.ToolType
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DesignCanvasViewModel @Inject constructor(
    @ApplicationContext context: android.content.Context,
    private val artworkRepository: ArtworkRepository,
    private val projectRepository: ProjectRepository,
    private val sessionStore: DesignSessionStore
) : ViewModel() {

    val engine = CanvasEngine(width = 1024, height = 1024)
    private val export = ExportEngine(context, artworkRepository)

    private val _tool = MutableStateFlow(ToolType.BRUSH)
    val tool: StateFlow<ToolType> = _tool

    private val _brush = MutableStateFlow(BrushPresetCatalog.byType(BrushType.PENCIL).defaults)
    val brush: StateFlow<BrushSettings> = _brush

    private val _eraser = MutableStateFlow(
        BrushSettings(
            type = BrushType.ERASER_SOFT,
            color = Color.Transparent,
            size = 30f,
            opacity = 1f,
            hardness = 0.5f,
            spacing = 0.05f
        )
    )
    val eraser: StateFlow<BrushSettings> = _eraser

    private val _shape = MutableStateFlow(ShapeSettings())
    val shape: StateFlow<ShapeSettings> = _shape

    private val _symmetry = MutableStateFlow(SymmetryMode.OFF)
    val symmetry: StateFlow<SymmetryMode> = _symmetry

    private val _color = MutableStateFlow(Color.Black)
    val color: StateFlow<Color> = _color

    private val _recentColors = MutableStateFlow<List<Color>>(emptyList())
    val recentColors: StateFlow<List<Color>> = _recentColors

    private val _palette = MutableStateFlow(
        listOf(
            Color.Black, Color.White, Color(0xFFC8860A), Color(0xFF8B2F0F),
            Color(0xFF1B5E20), Color(0xFFE8A7B5), Color(0xFFD4AF37), Color(0xFF9B59B6),
            Color.Red, Color.Blue, Color.Green, Color.Yellow
        )
    )
    val palette: StateFlow<List<Color>> = _palette

    fun selectTool(tool: ToolType) {
        _tool.value = tool
    }

    fun selectBrush(type: BrushType) {
        _brush.value = BrushPresetCatalog.byType(type).defaults.copy(color = _color.value)
        _tool.value = ToolType.BRUSH
    }

    fun updateBrush(transform: (BrushSettings) -> BrushSettings) {
        _brush.value = transform(_brush.value)
    }

    fun updateEraser(transform: (BrushSettings) -> BrushSettings) {
        _eraser.value = transform(_eraser.value)
    }

    fun updateShape(transform: (ShapeSettings) -> ShapeSettings) {
        _shape.value = transform(_shape.value)
    }

    fun selectColor(c: Color) {
        _color.value = c
        _brush.value = _brush.value.copy(color = c)
        _shape.value = _shape.value.copy(strokeColor = c)
        val recent = (_recentColors.value.toMutableList())
        recent.remove(c)
        recent.add(0, c)
        if (recent.size > 12) recent.subList(12, recent.size).clear()
        _recentColors.value = recent
    }

    fun addPaletteColor(c: Color) {
        if (_palette.value.contains(c)) return
        _palette.value = _palette.value + c
    }

    fun setSymmetry(mode: SymmetryMode) {
        _symmetry.value = mode
    }

    fun beginBrushStroke(initial: Offset) {
        when (_tool.value) {
            ToolType.ERASER -> engine.beginBrushStroke(_eraser.value, initial, _symmetry.value, eraseMode = true)
            else -> engine.beginBrushStroke(_brush.value, initial, _symmetry.value, eraseMode = false)
        }
    }

    fun extendBrushStroke(point: Offset) = engine.extendBrushStroke(point)

    fun endBrushStroke() = engine.endBrushStroke()

    fun applyShape(start: Offset, end: Offset) =
        engine.applyShape(_shape.value, start, end)

    fun applyFill(point: Offset) =
        engine.applyFill(point, _color.value)

    fun pickColor(x: Int, y: Int) {
        engine.pickColorAt(x, y)?.let { selectColor(it) }
    }

    fun clearLayer() = engine.clearActiveLayer()
    fun undo() = engine.undo()
    fun redo() = engine.redo()

    fun saveArtwork(
        title: String,
        categoryId: String,
        subTypeId: String?,
        sessionId: String? = null,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val flat = engine.snapshotComposite()
            // Persist a cache copy of the raw drawing so the template gallery
            // and recommendations screens have something to load straight away,
            // independent of the AI processing pipeline.
            val cached = export.exportPngToCache(flat, name = "canvas")
            val id = export.saveAsArtwork(
                bitmap = flat,
                title = title,
                categoryId = categoryId,
                subTypeId = subTypeId
            )
            
            // Bridge to Project-based AI Flow (Phase 3 & 4)
            if (sessionId != null) {
                sessionStore.setProcessedImage(sessionId, android.net.Uri.fromFile(cached))
                
                // Create or find project for this session to trigger intelligence flow
                val s = sessionStore.get(sessionId)
                val templateId = s?.selectedTemplateId ?: "default"
                val projectId = projectRepository.createProject(templateId, cached.absolutePath)
                
                // Update session or return project ID for navigation
                onProjectCreated(projectId)
            } else {
                onSaved(id)
            }
            
            flat.recycle()
        }
    }

    private var onProjectCreated: (String) -> Unit = {}
    fun setOnProjectCreated(callback: (String) -> Unit) {
        onProjectCreated = callback
    }

    fun resolveSession(sessionId: String): Triple<String?, String?, Boolean> {
        val s = sessionStore.get(sessionId)
        return Triple(s?.categoryId, s?.subTypeId, s?.isConverterFlow ?: false)
    }

    override fun onCleared() {
        engine.release()
        super.onCleared()
    }
}
