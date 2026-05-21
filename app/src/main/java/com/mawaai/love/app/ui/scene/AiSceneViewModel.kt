package com.mawaai.love.app.ui.scene

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.ai.AIEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel backing the AI Scene Generator dialog.
 *
 * Pipeline:
 *  1. User types an English prompt describing a romantic scene.
 *  2. [generate] hands the prompt to [AIEngine.generateRomanticImage]
 *     which routes to Cloudflare Workers AI (SDXL Lightning).
 *  3. The resulting [Bitmap] lands in [state] for the dialog to render.
 *  4. [saveToGallery] persists the bitmap to MediaStore under the
 *     "Mawaai" album so the user can share/use it elsewhere.
 *
 * All cloud-call failure modes (no key, network, decode) collapse to
 * an [SceneState.Error] state with a localised message — no exceptions
 * escape the ViewModel.
 */
@HiltViewModel
class AiSceneViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val aiEngine: AIEngine
) : ViewModel() {

    private val _state = MutableStateFlow<SceneState>(SceneState.Idle)
    val state = _state.asStateFlow()

    /** True when the cloud T2I provider is configured. UI gates the
     *  whole feature on this so it hides when the user has no keys. */
    val isConfigured: Boolean get() = aiEngine.cloudTextToImageAvailable

    fun generate(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            _state.value = SceneState.Error(reasonRes = ERROR_EMPTY_PROMPT)
            return
        }
        _state.value = SceneState.Generating
        viewModelScope.launch {
            val bitmap = aiEngine.generateRomanticImage(trimmed)
            _state.value = if (bitmap != null) {
                SceneState.Ready(bitmap = bitmap, prompt = trimmed)
            } else {
                SceneState.Error(reasonRes = ERROR_GENERATION_FAILED)
            }
        }
    }

    /** Reset to the empty state. Used when the user closes/reopens the dialog. */
    fun reset() {
        _state.value = SceneState.Idle
    }

    /**
     * Persists the current generated bitmap to the device gallery via
     * MediaStore. No runtime permission is required on API 29+ because
     * we write to the shared Pictures collection through the system
     * content resolver. Returns the saved [Uri] via [onSaved] when
     * successful, or null on failure.
     */
    fun saveToGallery(onSaved: (Uri?) -> Unit) {
        val ready = (_state.value as? SceneState.Ready) ?: run {
            onSaved(null)
            return
        }
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                runCatching { writeToMediaStore(ready.bitmap) }.getOrNull()
            }
            onSaved(uri)
        }
    }

    private fun writeToMediaStore(bitmap: Bitmap): Uri? {
        val resolver = appContext.contentResolver
        val displayName = "Mawaai-Scene-${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Mawaai")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri).use { stream ->
            if (stream == null) return null
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    private companion object {
        // String resource IDs for error messages. `val` (not `const val`)
        // because R.string IDs are runtime-computed final ints, not
        // compile-time constants — Kotlin's `const` requires a literal.
        // The dialog resolves these to localised text at render time.
        val ERROR_EMPTY_PROMPT = com.mawaai.love.app.R.string.ai_scene_error_empty_prompt
        val ERROR_GENERATION_FAILED = com.mawaai.love.app.R.string.ai_scene_error_generation_failed
    }
}

/**
 * State machine for the AI Scene dialog. Only one of [Idle],
 * [Generating], [Ready], or [Error] is ever active.
 */
sealed interface SceneState {
    object Idle : SceneState
    object Generating : SceneState
    data class Ready(val bitmap: Bitmap, val prompt: String) : SceneState
    data class Error(val reasonRes: Int) : SceneState
}
