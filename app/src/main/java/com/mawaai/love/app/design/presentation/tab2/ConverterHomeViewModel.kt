package com.mawaai.love.app.design.presentation.tab2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.ai.gemini.GeminiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConverterHomeState(
    val prompts: List<String> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ConverterHomeViewModel @Inject constructor(
    private val gemini: GeminiClient
) : ViewModel() {

    private val _state = MutableStateFlow(ConverterHomeState())
    val state: StateFlow<ConverterHomeState> = _state

    init {
        if (gemini.isConfigured) loadPrompts()
    }

    fun loadPrompts() {
        if (!gemini.isConfigured) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val prompts = gemini.inspirationPrompts()
            _state.value = ConverterHomeState(prompts = prompts, isLoading = false)
        }
    }
}
