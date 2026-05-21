package com.mawaai.love.app.design.presentation.flow

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class PreviewState(
    val inputUri: Uri? = null,
    val isConverterFlow: Boolean = false
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val sessionStore: DesignSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(PreviewState())
    val state: StateFlow<PreviewState> = _state

    fun load(sessionId: String) {
        val session = sessionStore.get(sessionId) ?: return
        _state.value = PreviewState(
            inputUri = session.inputImageUri,
            isConverterFlow = session.isConverterFlow
        )
    }
}
