package com.mawaai.love.app.design.presentation.flow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.ai.huggingface.HuggingFaceClient
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.render.ImageExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ResultState(
    val imageUri: Uri? = null,
    val isSaving: Boolean = false,
    val savedUri: Uri? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val sessionStore: DesignSessionStore,
    private val exporter: ImageExporter,
    private val huggingFace: HuggingFaceClient,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ResultState())
    val state: StateFlow<ResultState> = _state

    fun load(sessionId: String) {
        val session = sessionStore.get(sessionId) ?: return
        _state.update { it.copy(imageUri = session.processedImageUri) }
    }

    /**
     * Saves the current result to the device gallery. Phase 22: when the
     * user has a HuggingFace API key configured, the bitmap is sent to
     * Real-ESRGAN for a 4× upscale BEFORE being persisted — the gallery
     * receives a high-resolution print-ready PNG instead of the
     * mid-pipeline 1024-px intermediate.
     *
     * The cloud upscale is best-effort: any failure (no key, network,
     * 503 cold start, decode error) cleanly falls through to saving
     * the original. The user always sees a saved file; they only
     * notice the difference when they zoom in or print.
     */
    fun saveToGallery(sessionId: String, displayName: String) {
        val uri = _state.value.imageUri ?: return
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val original = withContext(Dispatchers.IO) { decode(uri) }
                    ?: error("Failed to decode result image")
                val toSave = upscaleOrOriginal(original)
                try {
                    exporter.saveToGallery(
                        bitmap = toSave,
                        displayName = "mawaai-$displayName-$sessionId",
                        format = ImageExporter.Format.JPEG,
                        quality = 92
                    )
                } finally {
                    if (toSave !== original && !toSave.isRecycled) toSave.recycle()
                    if (!original.isRecycled) original.recycle()
                }
            }.onSuccess { saved ->
                _state.update { it.copy(isSaving = false, savedUri = saved) }
            }.onFailure { t ->
                _state.update { it.copy(isSaving = false, errorMessage = t.message) }
            }
        }
    }

    /**
     * Best-effort cloud upscale on the final save. Returns the upscaled
     * bitmap when HuggingFace returned a result, or [original] when the
     * call falls through. Catches every exception path because Save
     * must never fail just because the cloud is offline.
     */
    private suspend fun upscaleOrOriginal(original: Bitmap): Bitmap {
        if (!huggingFace.isConfigured) return original
        return runCatching {
            val upscaled = huggingFace.cloudUpscale(original)
            if (upscaled != null && !upscaled.isRecycled) {
                Log.i(TAG, "Cloud upscale returned ${upscaled.width}×${upscaled.height}")
                upscaled
            } else {
                original
            }
        }.onFailure { Log.w(TAG, "Cloud upscale threw — saving original", it) }
            .getOrDefault(original)
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

    private companion object {
        const val TAG = "ResultViewModel"
    }
}
