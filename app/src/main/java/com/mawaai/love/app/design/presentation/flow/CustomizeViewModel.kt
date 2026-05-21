package com.mawaai.love.app.design.presentation.flow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.HslColor
import com.mawaai.love.app.design.domain.model.Template
import com.mawaai.love.app.design.render.GarmentColorEngine
import com.mawaai.love.app.design.render.TemplateAssetManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class CustomizeUiState(
    val isLoading: Boolean = true,
    val template: Template? = null,
    val color: HslColor = HslColor(0f, 0f, 0.5f),
    val previewUri: Uri? = null,
    val isRecoloring: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /**
     * Tracks the last fabric preset the user explicitly picked. The
     * Customize screen highlights that preset's swatch with a gold ring.
     * Cleared whenever the color is updated through any other path
     * (slider, hex input) so the highlight tracks user intent, not raw
     * ARGB equality (which was brittle: rounding the HSL → RGB
     * conversion by ±1 dropped the ring on the first slider tick).
     */
    val lastPickedPreset: FabricTone? = null
)

/**
 * Drives the Customize screen for abaya / Sudanese thob templates. Holds
 * the user-picked [HslColor], debounces slider drags through a
 * [MutableSharedFlow] (~12 fps recolor updates), and produces a fresh
 * preview file on every settled tick. Save commits the latest preview
 * to `cacheDir/design_results/customize-…png`, repoints
 * [DesignSessionStore.setProcessedImage], and emits a navigation event
 * so the UI advances to [com.mawaai.love.app.design.presentation.main.DesignRoute.Result].
 *
 * Concurrency: `setColor` is called per slider tick on the Main thread
 * and is non-blocking — it pushes into [colorRequests] which a single
 * [collectLatest] consumer drains. `runRecolor` is suspend; only one
 * runs at a time because of `collectLatest`. Save runs on a fresh
 * `viewModelScope.launch` and tolerates concurrent recolor (the engine
 * caches the warped design so two parallel recolors share the warp).
 *
 * Bitmap lifecycle (post-2026-05-13 audit fix #7): previews used to
 * be passed into the screen as raw `Bitmap`s and recycled on the next
 * slider tick — fast but theoretically racy because Coil could be
 * holding the previous reference during recompose. The VM now writes
 * each settled preview to a unique cache file as JPEG @ 85 and keeps
 * only the most recent two on disk. The screen consumes file URIs via
 * Coil; bitmap recycling happens immediately on this side. Trades ~10
 * ms of extra disk I/O per tick for a robust contract.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val sessionStore: DesignSessionStore,
    private val templates: TemplateAssetManager,
    private val engine: GarmentColorEngine,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CustomizeUiState())
    val state: StateFlow<CustomizeUiState> = _state.asStateFlow()

    private val _nav = Channel<Unit>(Channel.BUFFERED)
    val nav = _nav.receiveAsFlow()

    private val colorRequests = MutableSharedFlow<HslColor>(extraBufferCapacity = 1)

    private var design: Bitmap? = null
    private var sessionId: String? = null
    private var loaded = false

    // FIFO of preview cache files. We keep at most [PREVIEW_RETENTION]
    // most-recent files on disk to give Coil a brief window to finish
    // decoding the previous preview before its bytes are deleted.
    private val previewFiles = ArrayDeque<File>()
    private var previewSeq: Long = 0L

    init {
        viewModelScope.launch {
            colorRequests
                .debounce(SLIDER_DEBOUNCE_MS)
                .collectLatest { color ->
                    runRecolor(color, intensityOverride = null)
                }
        }
    }

    fun load(sessionId: String) {
        if (this.sessionId == sessionId && loaded) return
        this.sessionId = sessionId
        viewModelScope.launch {
            val session = sessionStore.get(sessionId)
            val templateId = session?.selectedTemplateId
            val categoryId = session?.categoryId
            val processedUri = session?.processedImageUri
            if (session == null || templateId == null || categoryId == null || processedUri == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Missing session data for customize"
                    )
                }
                return@launch
            }

            val tpl = runCatching { templates.forCategory(categoryId).firstOrNull { it.id == templateId } }
                .getOrNull()
            if (tpl == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Template not found"
                    )
                }
                return@launch
            }

            val designBitmap = withContext(Dispatchers.IO) { decode(processedUri) }
            if (designBitmap == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not decode design"
                    )
                }
                return@launch
            }
            design = designBitmap

            val seed = engine.sampleSeedColor(tpl)
            _state.update {
                it.copy(
                    isLoading = false,
                    template = tpl,
                    color = seed,
                    lastPickedPreset = null
                )
            }
            loaded = true
            // Prime the preview with the seed color so the screen never
            // shows an empty box.
            runRecolor(seed, intensityOverride = null)
        }
    }

    /**
     * Updates the user's color choice immediately and schedules a
     * debounced recolor pass. Slider thumbs read [state]; the preview
     * catches up after [SLIDER_DEBOUNCE_MS]. Clears [lastPickedPreset]
     * — the user is editing through a non-preset path now, so the
     * preset highlight should drop.
     */
    fun setColor(color: HslColor) {
        _state.update { it.copy(color = color, lastPickedPreset = null) }
        colorRequests.tryEmit(color)
    }

    /**
     * Sets the color from a fabric preset and remembers which preset
     * was picked so the screen can keep its swatch highlighted while
     * the user inspects the result. Subsequent slider/hex edits clear
     * the marker via [setColor].
     */
    fun setPreset(tone: FabricTone) {
        val color = HslColor.fromColor(tone.argb)
        _state.update { it.copy(color = color, lastPickedPreset = tone) }
        colorRequests.tryEmit(color)
    }

    fun save() {
        val sid = sessionId ?: return
        val tpl = _state.value.template ?: return
        val d = design ?: return
        val color = _state.value.color
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val finalBmp = engine.recolor(tpl, d, color)
                val uri = withContext(Dispatchers.IO) { persistFinal(finalBmp, sid) }
                sessionStore.setProcessedImage(sid, uri)
                if (!finalBmp.isRecycled) finalBmp.recycle()
            }.onSuccess {
                _state.update { it.copy(isSaving = false) }
                _nav.trySend(Unit)
            }.onFailure { t ->
                _state.update { it.copy(isSaving = false, errorMessage = t.message) }
            }
        }
    }

    private suspend fun runRecolor(color: HslColor, intensityOverride: Float?) {
        val tpl = _state.value.template ?: return
        val d = design ?: return
        _state.update { it.copy(isRecoloring = true, errorMessage = null) }
        runCatching {
            engine.recolor(
                template = tpl,
                design = d,
                target = color,
                blendIntensity = intensityOverride ?: 1f
            )
        }.onSuccess { newPreview ->
            val uri = withContext(Dispatchers.IO) { persistPreview(newPreview) }
            if (!newPreview.isRecycled) newPreview.recycle()
            _state.update { it.copy(previewUri = uri, isRecoloring = false) }
        }.onFailure { t ->
            _state.update { it.copy(isRecoloring = false, errorMessage = t.message) }
        }
    }

    /**
     * Persists [bitmap] as a unique JPEG file under
     * `cacheDir/customize_preview/`. JPEG @ 85 is visually
     * indistinguishable from the recolor output at preview resolution
     * and trims ~80% off the per-tick disk I/O vs. PNG. Returns the
     * file's `Uri`; trims the on-disk FIFO to [PREVIEW_RETENTION].
     */
    private fun persistPreview(bitmap: Bitmap): Uri {
        val dir = File(appContext.cacheDir, "customize_preview").apply { mkdirs() }
        val sid = sessionId ?: "unknown"
        val seq = ++previewSeq
        val file = File(dir, "preview-$sid-$seq.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        previewFiles.addLast(file)
        while (previewFiles.size > PREVIEW_RETENTION) {
            runCatching { previewFiles.removeFirst().delete() }
        }
        return file.toUri()
    }

    private fun persistFinal(bitmap: Bitmap, sessionId: String): Uri {
        val dir = File(appContext.cacheDir, "design_results").apply { mkdirs() }
        val file = File(dir, "customize-$sessionId-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.toUri()
    }

    override fun onCleared() {
        super.onCleared()
        engine.invalidate()
        // Drop the design bitmap eagerly. Compose's last frame consumed
        // file URIs (not bitmap references), so nothing downstream still
        // holds a pointer here. Different from the prior bitmap-direct
        // version, which had to wait for finalizers.
        design?.takeIf { !it.isRecycled }?.recycle()
        design = null
        // Sweep all preview cache files for this session — any lingering
        // entries are useless on the next visit.
        previewFiles.forEach { runCatching { it.delete() } }
        previewFiles.clear()
    }

    private fun decode(uri: Uri): Bitmap? = when (uri.scheme) {
        "content", "file" -> appContext.contentResolver.openInputStream(uri).use { stream ->
            if (stream == null) null else BitmapFactory.decodeStream(stream)
        }
        null -> BitmapFactory.decodeFile(uri.toString())
        else -> uri.path?.let { BitmapFactory.decodeFile(it) }
    }

    private companion object {
        // Slider debounce — picks the first emission after the user
        // settles for ~80 ms. Net latency to a visible preview is
        // (debounce + recolor + persist) ≈ 80 + 150 + 10 = 240 ms on a
        // mid-tier arm64, comfortably inside the 300 ms target spec.
        const val SLIDER_DEBOUNCE_MS = 80L
        // Keep at most this many preview files on disk so Coil has a
        // window to read a previous file even if the user drags fast.
        const val PREVIEW_RETENTION = 2
    }
}
