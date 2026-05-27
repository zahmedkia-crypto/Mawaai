package com.mawaai.love.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.ai.gateway.ProviderId
import com.mawaai.love.app.design.ai.gateway.ProviderRegistry
import com.mawaai.love.app.design.ai.gateway.VisionProviderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiProviderSettingsUiState(
    val visionProviders: List<VisionProviderStatus> = emptyList(),
    val visionMode: String = ProviderRegistry.MODE_AUTO,
    val textMode: String = ProviderRegistry.MODE_AUTO
)

@HiltViewModel
class AiProviderSettingsViewModel @Inject constructor(
    private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiProviderSettingsUiState())
    val uiState: StateFlow<AiProviderSettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.value = AiProviderSettingsUiState(
                visionProviders = registry.knownVisionProviders(),
                // In a real app, we'd collect these from the registry's DataStore flow
                visionMode = ProviderRegistry.MODE_AUTO, 
                textMode = ProviderRegistry.MODE_AUTO
            )
        }
    }

    fun setVisionMode(mode: String) {
        viewModelScope.launch {
            registry.setVisionMode(mode)
            refresh()
        }
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch {
            registry.setTextMode(mode)
            refresh()
        }
    }
}
