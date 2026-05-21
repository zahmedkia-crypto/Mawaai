package com.mawaai.love.app.design.canvas.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.canvas.ui.components.BrushOptionsPanel
import com.mawaai.love.app.design.canvas.ui.components.BrushPanel
import com.mawaai.love.app.design.canvas.ui.components.CanvasToolbar
import com.mawaai.love.app.design.canvas.ui.components.CanvasTopBar
import com.mawaai.love.app.design.canvas.ui.components.CanvasView
import com.mawaai.love.app.design.canvas.ui.components.ColorPickerDialog
import com.mawaai.love.app.design.canvas.ui.components.LayerPanel
import com.mawaai.love.app.design.canvas.ui.components.ShapePanel
import com.mawaai.love.app.design.canvas.ui.components.Sheet
import com.mawaai.love.app.design.canvas.ui.components.SymmetryPanel
import com.mawaai.love.app.design.presentation.main.DesignRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignCanvasScreen(
    nav: NavController,
    sessionId: String,
    viewModel: DesignCanvasViewModel = hiltViewModel()
) {
    val tool by viewModel.tool.collectAsStateWithLifecycle()
    val brush by viewModel.brush.collectAsStateWithLifecycle()
    val symmetry by viewModel.symmetry.collectAsStateWithLifecycle()
    val color by viewModel.color.collectAsStateWithLifecycle()
    val palette by viewModel.palette.collectAsStateWithLifecycle()
    val recents by viewModel.recentColors.collectAsStateWithLifecycle()
    val shapeSettings by viewModel.shape.collectAsStateWithLifecycle()
    val layers by viewModel.engine.layers.layers.collectAsStateWithLifecycle()
    val activeLayerId by viewModel.engine.layers.activeLayerId.collectAsStateWithLifecycle()
    val canUndo by viewModel.engine.history.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.engine.history.canRedo.collectAsStateWithLifecycle()

    var sheet by remember { mutableStateOf(Sheet.NONE) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var artworkTitle by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val (categoryId, subTypeId, _) = remember(sessionId) {
        viewModel.resolveSession(sessionId)
    }

    Scaffold(
        topBar = {
            CanvasTopBar(
                onBack = { nav.popBackStack() },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onClear = { viewModel.clearLayer() },
                onAiTips = {
                    // Save quickly under an "AI Tips" working title, then route to
                    // the recommendations screen with the artwork id.
                    viewModel.saveArtwork(
                        title = artworkTitle.ifBlank { "AI Tips" },
                        categoryId = categoryId ?: "general",
                        subTypeId = subTypeId
                    ) { id ->
                        nav.navigate(DesignRoute.Recommendations.create(id))
                    }
                },
                onPickTemplate = {
                    // Save the drawing AND pin it as the session's processed
                    // image so the template gallery can composite it onto any
                    // wall / hand / abaya template right away.
                    viewModel.saveArtwork(
                        title = artworkTitle.ifBlank { "Template Pick" },
                        categoryId = categoryId ?: "general",
                        subTypeId = subTypeId,
                        sessionId = sessionId
                    ) { _ ->
                        nav.navigate(DesignRoute.TemplateGallery.create(sessionId))
                    }
                },
                onSave = {
                    artworkTitle = ""
                    showSaveDialog = true
                },
                canUndo = canUndo,
                canRedo = canRedo
            )
        },
        bottomBar = {
            CanvasToolbar(
                tool = tool,
                onTool = { viewModel.selectTool(it) },
                color = color,
                onColorClick = { showColorPicker = true },
                onSheet = { sheet = it }
            )
        },
        containerColor = MawaaiColors.DesignBgDark
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            CanvasView(
                engine = viewModel.engine,
                tool = tool,
                onBrushBegin = viewModel::beginBrushStroke,
                onBrushExtend = viewModel::extendBrushStroke,
                onBrushEnd = viewModel::endBrushStroke,
                onShapeCommit = { s, e -> viewModel.applyShape(s, e) },
                onFillTap = { viewModel.applyFill(it) },
                onEyedropTap = { x, y -> viewModel.pickColor(x, y) }
            )
        }
    }

    if (sheet != Sheet.NONE) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { sheetState.hide() }.invokeOnCompletion { sheet = Sheet.NONE } },
            sheetState = sheetState,
            containerColor = MawaaiColors.DesignSurface
        ) {
            when (sheet) {
                Sheet.BRUSHES -> BrushPanel(selected = brush.type, onSelect = {
                    viewModel.selectBrush(it)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { sheet = Sheet.NONE }
                })
                Sheet.BRUSH_OPTIONS -> BrushOptionsPanel(brush = brush, onChange = { newBrush -> viewModel.updateBrush { newBrush } })
                Sheet.LAYERS -> LayerPanel(
                    layers = layers,
                    activeId = activeLayerId,
                    onSelect = { viewModel.engine.layers.setActive(it) },
                    onAdd = { viewModel.engine.layers.addLayer("Layer ${layers.size + 1}") },
                    onDelete = { viewModel.engine.layers.deleteLayer(it) },
                    onDuplicate = { viewModel.engine.layers.duplicateLayer(it) },
                    onMergeDown = { viewModel.engine.layers.mergeDown(it) },
                    onToggleVisible = { id, v -> viewModel.engine.layers.setVisible(id, v) },
                    onOpacity = { id, op -> viewModel.engine.layers.setOpacity(id, op) },
                    onBlend = { id, mode -> viewModel.engine.layers.setBlendMode(id, mode) }
                )
                Sheet.SYMMETRY -> SymmetryPanel(selected = symmetry, onSelect = {
                    viewModel.setSymmetry(it)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { sheet = Sheet.NONE }
                })
                Sheet.SHAPES -> ShapePanel(settings = shapeSettings, onChange = { viewModel.updateShape { _ -> it } })
                else -> Unit
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initial = color,
            palette = palette,
            recents = recents,
            onConfirm = { viewModel.selectColor(it); showColorPicker = false },
            onAddToPalette = { viewModel.addPaletteColor(it) },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = MawaaiColors.DesignSurface,
            title = {
                Text(
                    stringResource(R.string.canvas_save_artwork),
                    fontFamily = CairoFamily,
                    fontWeight = FontWeight.Bold,
                    color = MawaaiColors.DesignTextLight
                )
            },
            text = {
                OutlinedTextField(
                    value = artworkTitle,
                    onValueChange = { artworkTitle = it },
                    placeholder = { Text(stringResource(R.string.canvas_artwork_title_hint), fontFamily = CairoFamily) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val title = artworkTitle.ifBlank { "Untitled" }
                    showSaveDialog = false
                    viewModel.saveArtwork(
                        title = title,
                        categoryId = categoryId ?: "general",
                        subTypeId = subTypeId,
                        sessionId = sessionId
                    ) { artworkId ->
                        // After Save we always offer AI recommendations on the
                        // freshly persisted artwork. The "Pick Template" button
                        // stays available in the top bar so the user can still
                        // jump to compositing without leaving this flow.
                        nav.navigate(DesignRoute.Recommendations.create(artworkId))
                    }
                }) {
                    Text(stringResource(R.string.canvas_done), fontFamily = CairoFamily, color = MawaaiColors.DesignGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.action_close), fontFamily = CairoFamily, color = MawaaiColors.DesignTextLight)
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun DesignCanvasScreenPreview() {
    DesignCanvasScreen(nav = rememberNavController(), sessionId = "preview")
}
