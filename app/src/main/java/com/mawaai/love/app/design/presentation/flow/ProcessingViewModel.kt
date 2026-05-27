package com.mawaai.love.app.design.presentation.flow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.ai.AIEngine
import com.mawaai.love.app.design.ai.ProcessingStage
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.SkinTone
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

data class ProcessingUiState(
    val stage: ProcessingStage = ProcessingStage.Init,
    val isConverterFlow: Boolean = false,
    val modelFallbackHinted: Boolean = false
)

sealed class ProcessingNavEvent {
    object NavigateToTemplate : ProcessingNavEvent()
    object NavigateToResult : ProcessingNavEvent()
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val aiEngine: AIEngine,
    private val sessionStore: DesignSessionStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProcessingUiState())
    val state: StateFlow<ProcessingUiState> = _state

    private val _nav = Channel<ProcessingNavEvent>(Channel.BUFFERED)
    val nav = _nav.receiveAsFlow()

    private var job: Job? = null

    fun start(sessionId: String) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            // Check if it's a Project ID (Phase 5) or Session ID (Phase 1/2)
            if (sessionId.startsWith("session_") || sessionStore.get(sessionId) != null) {
                runPipeline(sessionId)
            } else {
                runProjectRender(sessionId)
            }
        }
    }

    private suspend fun runProjectRender(projectId: String) {
        _state.update { it.copy(stage = ProcessingStage.Scanning) }
        try {
            val result = aiEngine.renderProject(projectId) { stage ->
                publishStage(stage)
            }
            
            val outputUri = withContext(Dispatchers.IO) {
                persistBitmap(result, "render-$projectId")
            }
            
            // For project-based flow, we might want to store the result in the project entity too
            // and maybe navigate to a different result screen or the same one with projectId
            _state.update { it.copy(stage = ProcessingStage.Done) }
            _nav.trySend(ProcessingNavEvent.NavigateToResult)
        } catch (t: Throwable) {
            _state.update { it.copy(stage = ProcessingStage.Failed(t)) }
        }
    }

    fun retry(sessionId: String) {
        job?.cancel()
        _state.value = ProcessingUiState()
        start(sessionId)
    }

    private suspend fun runPipeline(sessionId: String) {
        val session = sessionStore.get(sessionId) ?: run {
            _state.update {
                it.copy(stage = ProcessingStage.Failed(IllegalStateException("Session not found")))
            }
            return
        }
        val isConverter = session.isConverterFlow
        _state.update { it.copy(isConverterFlow = isConverter) }

        val inputUri = session.inputImageUri
        if (inputUri == null) {
            _state.update {
                it.copy(stage = ProcessingStage.Failed(IllegalStateException("Missing input image")))
            }
            return
        }

        var inputBitmap: Bitmap? = null
        var result: Bitmap? = null
        try {
            inputBitmap = withContext(Dispatchers.IO) { decodeBitmap(inputUri) }
                ?: throw IllegalStateException("Failed to decode input image")

            result = if (isConverter) {
                aiEngine.processConverter(
                    input = inputBitmap,
                    styleId = session.styleId,
                    onProgress = ::publishStage
                )
            } else {
                aiEngine.processSpecialized(
                    input = inputBitmap,
                    categoryId = session.categoryId ?: "",
                    subTypeId = session.subTypeId,
                    styleId = session.styleId,
                    skinTone = SkinTone.fromId(session.skinToneId),
                    fabricTone = FabricTone.fromId(session.fabricToneId),
                    onProgress = ::publishStage
                )
            }

            val outputUri = withContext(Dispatchers.IO) {
                persistBitmap(result, "result-$sessionId")
            }
            sessionStore.setProcessedImage(sessionId, outputUri)
            _state.update { it.copy(stage = ProcessingStage.Done) }

            val event = if (isConverter) ProcessingNavEvent.NavigateToResult
            else ProcessingNavEvent.NavigateToTemplate
            _nav.trySend(event)
        } catch (t: Throwable) {
            _state.update { it.copy(stage = ProcessingStage.Failed(t)) }
        } finally {
            inputBitmap?.takeIf { !it.isRecycled }?.recycle()
            result?.takeIf { !it.isRecycled && it !== inputBitmap }?.recycle()
        }
    }

    private fun publishStage(stage: ProcessingStage) {
        _state.update {
            it.copy(
                stage = stage,
                modelFallbackHinted = it.modelFallbackHinted || stage is ProcessingStage.Stylizing
            )
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return when (uri.scheme) {
            "content", "file" -> appContext.contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) null else BitmapFactory.decodeStream(stream)
            }
            null -> BitmapFactory.decodeFile(uri.toString())
            else -> BitmapFactory.decodeFile(uri.path ?: return null)
        }
    }

    private fun persistBitmap(bitmap: Bitmap, name: String): Uri {
        val dir = File(appContext.cacheDir, "design_results").apply { mkdirs() }
        val file = File(dir, "$name-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.toUri()
    }
}
