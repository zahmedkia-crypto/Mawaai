package com.mawaai.love.app.design.presentation.flow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.ai.AIEngine
import com.mawaai.love.app.design.ai.OfflineEnhancer
import com.mawaai.love.app.design.ai.RefinementStage
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.domain.model.Template
import com.mawaai.love.app.design.render.TemplateAssetManager
import com.mawaai.love.app.design.render.TemplateCompositor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class TemplateGalleryState(
    val isLoading: Boolean = true,
    val templates: List<Template> = emptyList(),
    val selectedTemplateId: String? = null,
    val isApplying: Boolean = false,
    /**
     * Phase 25 — current stage of the multi-step apply pipeline. The UI
     * binds to this to show a progress bar / stage label per step:
     *  - [RefinementStage.Compositing]: drafting the warp + blend
     *  - [RefinementStage.Refining]: AI img2img integration pass
     *  - [RefinementStage.Polishing]: final unsharp + saturation polish
     *  - [RefinementStage.Done]: ready to navigate
     */
    val applyStage: RefinementStage = RefinementStage.Idle,
    val errorMessage: String? = null
)

@HiltViewModel
class TemplateGalleryViewModel @Inject constructor(
    private val sessionStore: DesignSessionStore,
    private val assetManager: TemplateAssetManager,
    private val compositor: TemplateCompositor,
    private val aiEngine: AIEngine,
    private val offlineEnhancer: OfflineEnhancer,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(TemplateGalleryState())
    val state: StateFlow<TemplateGalleryState> = _state

    private val _nav = Channel<Unit>(Channel.BUFFERED)
    val nav = _nav.receiveAsFlow()

    fun load(sessionId: String) {
        val session = sessionStore.get(sessionId) ?: return
        val categoryId = session.categoryId ?: return
        viewModelScope.launch {
            val list = assetManager.forCategory(categoryId)
            _state.update {
                it.copy(
                    isLoading = false,
                    templates = list,
                    selectedTemplateId = list.firstOrNull()?.id
                )
            }
        }
    }

    fun select(templateId: String) {
        _state.update { it.copy(selectedTemplateId = templateId) }
    }

    /**
     * Phase 25 — multi-step apply pipeline.
     *
     *   1. **Compositing**: simple warp + blend produces a draft.
     *   2. **Refining**: AI img2img integrates the draft into the
     *      template (fabric folds, lighting, edges) using
     *      [AIEngine.refineComposite]. Skipped when the cloud
     *      provider isn't configured — the un-refined composite
     *      flows straight to polish.
     *   3. **Polishing**: category-aware OfflineEnhancer pass
     *      restores sharpness lost to img2img's softening, lifts
     *      saturation, and applies CLAHE for richer local contrast.
     *
     * Each stage publishes to [TemplateGalleryState.applyStage] so the
     * UI can show meaningful progress instead of an opaque spinner.
     */
    fun apply(sessionId: String) {
        val current = _state.value
        val template = current.templates.firstOrNull { it.id == current.selectedTemplateId } ?: return
        val session = sessionStore.get(sessionId) ?: return
        val processedUri = session.processedImageUri ?: run {
            _state.update { it.copy(errorMessage = "Missing processed image") }
            return
        }
        if (current.isApplying) return
        _state.update {
            it.copy(
                isApplying = true,
                errorMessage = null,
                applyStage = RefinementStage.Compositing
            )
        }
        viewModelScope.launch {
            runCatching {
                val artwork = withContext(Dispatchers.IO) { decode(processedUri) }
                    ?: error("Failed to decode processed image")

                // Stage 1 — simple compose (warp + blend). Fast, local.
                val composite = compositor.compose(template, artwork)

                // Stage 2 — AI img2img refinement (cloud, optional).
                _state.update { it.copy(applyStage = RefinementStage.Refining) }
                val refined = aiEngine.refineComposite(
                    composite = composite,
                    categoryId = template.categoryId,
                    subTypeId = session.subTypeId
                ) ?: composite  // graceful fallback when cloud unavailable

                // Stage 3 — final polish: unsharp + saturation, category-aware.
                _state.update { it.copy(applyStage = RefinementStage.Polishing) }
                val polished = offlineEnhancer.enhance(refined, template.categoryId)

                val outputUri = withContext(Dispatchers.IO) { persist(polished, sessionId) }
                sessionStore.setProcessedImage(sessionId, outputUri)
                sessionStore.setSelectedTemplate(sessionId, template.id)

                // Recycle intermediates we don't need anymore. The
                // `polished` bitmap stays alive until next GC.
                if (artwork !== polished && !artwork.isRecycled) artwork.recycle()
                if (composite !== refined && composite !== polished && !composite.isRecycled) {
                    composite.recycle()
                }
                if (refined !== polished && refined !== composite && !refined.isRecycled) {
                    refined.recycle()
                }
            }.onSuccess {
                _state.update {
                    it.copy(isApplying = false, applyStage = RefinementStage.Done)
                }
                _nav.trySend(Unit)
            }.onFailure { t ->
                _state.update {
                    it.copy(
                        isApplying = false,
                        applyStage = RefinementStage.Failed(t),
                        errorMessage = t.message
                    )
                }
            }
        }
    }

    private fun decode(uri: Uri): Bitmap? {
        return when (uri.scheme) {
            "content", "file" -> appContext.contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) null else BitmapFactory.decodeStream(stream)
            }
            null -> BitmapFactory.decodeFile(uri.toString())
            else -> BitmapFactory.decodeFile(uri.path ?: return null)
        }
    }

    private fun persist(bitmap: Bitmap, sessionId: String): Uri {
        val dir = File(appContext.cacheDir, "design_results").apply { mkdirs() }
        val file = File(dir, "composite-$sessionId-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.toUri()
    }
}
