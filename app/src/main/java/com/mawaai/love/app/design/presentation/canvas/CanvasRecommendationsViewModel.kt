package com.mawaai.love.app.design.presentation.canvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.repository.ArtworkRepository
import com.mawaai.love.app.design.ai.DrawingActionEngine
import com.mawaai.love.app.design.ai.LocalDrawingAnalyzer
import com.mawaai.love.app.design.ai.gemini.GeminiVisionClient
import com.mawaai.love.app.design.domain.model.DrawingAction
import com.mawaai.love.app.design.domain.model.DrawingAnalysis
import com.mawaai.love.app.design.domain.model.DrawingSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class RecommendationsState(
    val isLoading: Boolean = true,
    val analysis: DrawingAnalysis = DrawingAnalysis.EMPTY,
    val artworkUri: Uri? = null,
    /**
     * Tracks which suggestion is currently in-flight by **message string**
     * rather than list index. The analyzer's heuristics produce
     * deterministic ordering today, but `rerunAnalysis` after Apply can
     * still reshuffle entries — keying by message is invariant against
     * reorder so the spinner stays on the right row. Audit fix #1.
     */
    val applyingMessage: String? = null,
    val isReverting: Boolean = false,
    val canUndo: Boolean = false
)

sealed interface RecommendationsEvent {
    object ApplySuccess : RecommendationsEvent
    object RevertSuccess : RecommendationsEvent
    data class ApplyFailed(val message: String?) : RecommendationsEvent
    data class RevertFailed(val message: String?) : RecommendationsEvent
}

@HiltViewModel
class CanvasRecommendationsViewModel @Inject constructor(
    private val artworkRepository: ArtworkRepository,
    private val vision: GeminiVisionClient,
    private val analyzer: LocalDrawingAnalyzer,
    private val actionEngine: DrawingActionEngine
) : ViewModel() {

    private val _state = MutableStateFlow(RecommendationsState())
    val state: StateFlow<RecommendationsState> = _state

    private val _events = Channel<RecommendationsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var artworkId: Long = -1L
    private var categoryId: String? = null

    fun load(artworkId: Long) {
        if (this.artworkId == artworkId && _state.value.analysis.suggestions.isNotEmpty()) return
        if (this.artworkId != artworkId) {
            // Different artwork → drop any pending undo from a previous one
            // before kicking off the fresh analysis.
            actionEngine.clearUndo()
        }
        this.artworkId = artworkId
        refresh()
    }

    /**
     * Re-analyzes the artwork and replaces the suggestions list. Guarded
     * by the same in-flight check `apply()` and `revert()` use — firing
     * Refresh during an Apply would otherwise re-analyze the *stale*
     * bitmap (Apply hasn't written its result yet) and confuse the user
     * with phantom suggestions. Audit fix #4.
     */
    fun refresh() {
        val current = _state.value
        if (current.applyingMessage != null || current.isReverting) return
        val id = artworkId
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val analysis = analyzeArtwork(id)
            if (analysis == null) {
                _state.value = RecommendationsState(isLoading = false)
                return@launch
            }
            val artwork = artworkRepository.getById(id)
            val uri = artwork?.fullImagePath?.let { File(it).toUri() }
            _state.update {
                it.copy(
                    isLoading = false,
                    analysis = analysis,
                    artworkUri = uri ?: it.artworkUri,
                    canUndo = actionEngine.canUndo(id)
                )
            }
        }
    }

    /**
     * Applies the suggestion identified by [message]. The screen looks up
     * the row by message, so Apply is invariant against list reorder
     * during async re-analysis. Returns silently if the message no
     * longer maps to an actionable suggestion (e.g. the analysis was
     * refreshed before the user tapped). Audit fix #1.
     */
    fun apply(message: String) {
        val current = _state.value
        if (current.applyingMessage != null || current.isReverting) return
        val suggestion = current.analysis.suggestions.firstOrNull { it.message == message }
            ?: return
        val action: DrawingAction = suggestion.action ?: return
        val id = artworkId
        if (id <= 0L) return

        _state.update { it.copy(applyingMessage = message) }
        viewModelScope.launch {
            val result = actionEngine.apply(id, action)
            result.onSuccess {
                val freshUri = bustCache(it.fullImagePath)
                _state.update { st ->
                    st.copy(
                        applyingMessage = null,
                        artworkUri = freshUri,
                        canUndo = actionEngine.canUndo(id)
                    )
                }
                _events.trySend(RecommendationsEvent.ApplySuccess)
                rerunAnalysis(id)
            }.onFailure { t ->
                _state.update { it.copy(applyingMessage = null) }
                _events.trySend(RecommendationsEvent.ApplyFailed(t.message))
            }
        }
    }

    /**
     * Restores the artwork to its state before the most recent [apply].
     * No-op if no undo is available. On success, reloads the preview +
     * re-runs analysis.
     */
    fun revert() {
        val current = _state.value
        if (current.isReverting || current.applyingMessage != null) return
        if (!current.canUndo) return
        val id = artworkId
        if (id <= 0L) return

        _state.update { it.copy(isReverting = true) }
        viewModelScope.launch {
            val result = actionEngine.revert(id)
            result.onSuccess {
                val freshUri = bustCache(it.fullImagePath)
                _state.update { st ->
                    st.copy(
                        isReverting = false,
                        artworkUri = freshUri,
                        canUndo = false
                    )
                }
                _events.trySend(RecommendationsEvent.RevertSuccess)
                rerunAnalysis(id)
            }.onFailure { t ->
                _state.update { it.copy(isReverting = false) }
                _events.trySend(RecommendationsEvent.RevertFailed(t.message))
            }
        }
    }

    /**
     * Re-analyzes after a successful Apply or Revert. Briefly raises
     * `isLoading` so the user sees a spinner while the new bitmap is
     * decoded — addresses the audit's "stale suggestions until new
     * analysis arrives" gap (#4).
     */
    private suspend fun rerunAnalysis(id: Long) {
        _state.update { it.copy(isLoading = true) }
        val analysis = analyzeArtwork(id)
        _state.update {
            it.copy(
                isLoading = false,
                analysis = analysis ?: it.analysis
            )
        }
    }

    /**
     * Single source of truth for "decode artwork → ask vision → fall
     * back to local analyzer". Used by both [refresh] and
     * [rerunAnalysis] — the previous duplication was the audit's #5
     * concern (the two branches could drift if one was touched and the
     * other wasn't). Returns null if the artwork or its bitmap is
     * unavailable.
     */
    private suspend fun analyzeArtwork(id: Long): DrawingAnalysis? {
        val artwork = artworkRepository.getById(id) ?: return null
        categoryId = artwork.categoryId
        val bitmap = withContext(Dispatchers.IO) { decode(artwork.fullImagePath) }
            ?: return null
        return try {
            if (vision.isConfigured) {
                val remote = vision.suggestionsForDrawing(bitmap, artwork.categoryId)
                if (remote.isNotEmpty()) {
                    DrawingAnalysis(
                        suggestions = remote.map { DrawingSuggestion(message = it, action = null) },
                        source = DrawingAnalysis.Source.GEMINI
                    )
                } else {
                    analyzer.analyze(bitmap, artwork.categoryId)
                }
            } else {
                analyzer.analyze(bitmap, artwork.categoryId)
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    /**
     * Adds a `?v=<timestamp>` query param so Coil's URI-keyed cache
     * misses the post-Apply / post-Revert reload and re-decodes the
     * file. The decoded-bitmap memory cache is also invalidated by the
     * fresh URI string.
     */
    private fun bustCache(fullImagePath: String): Uri {
        return File(fullImagePath).toUri()
            .buildUpon()
            .appendQueryParameter("v", System.currentTimeMillis().toString())
            .build()
    }

    override fun onCleared() {
        super.onCleared()
        // Surrender the undo buffer when leaving the screen — the engine's
        // 1-step undo contract doesn't survive navigation. Re-entering
        // recommendations on the same artwork will start fresh.
        actionEngine.clearUndo()
    }

    private fun decode(path: String): Bitmap? = runCatching {
        BitmapFactory.decodeFile(path)
    }.getOrNull()
}
