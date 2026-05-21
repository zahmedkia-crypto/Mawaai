package com.mawaai.love.app.design.presentation.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.data.repository.DesignCatalogRepository
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.domain.model.ConversionStyle
import com.mawaai.love.app.design.domain.model.FabricTone
import com.mawaai.love.app.design.domain.model.SkinTone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TonePalette { SKIN, FABRIC, NONE }

data class SuggestionsState(
    val palette: TonePalette = TonePalette.NONE,
    val skinTones: List<SkinTone> = emptyList(),
    val fabricTones: List<FabricTone> = emptyList(),
    val styles: List<ConversionStyle> = emptyList(),
    val selectedSkinToneId: String? = null,
    val selectedFabricToneId: String? = null,
    val selectedStyleId: String? = null
) {
    val canContinue: Boolean
        get() = selectedStyleId != null && when (palette) {
            TonePalette.SKIN -> selectedSkinToneId != null
            TonePalette.FABRIC -> selectedFabricToneId != null
            TonePalette.NONE -> true
        }
}

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val catalogRepo: DesignCatalogRepository,
    private val sessionStore: DesignSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(SuggestionsState())
    val state: StateFlow<SuggestionsState> = _state

    fun load(sessionId: String) {
        val session = sessionStore.get(sessionId)
        val palette = when (session?.categoryId) {
            "henna" -> TonePalette.SKIN
            "abaya", "thob_sudani" -> TonePalette.FABRIC
            else -> TonePalette.NONE
        }
        viewModelScope.launch {
            val catalog = catalogRepo.load()
            _state.update {
                it.copy(
                    palette = palette,
                    skinTones = if (palette == TonePalette.SKIN) SkinTone.entries else emptyList(),
                    fabricTones = if (palette == TonePalette.FABRIC) FabricTone.entries else emptyList(),
                    styles = catalog.conversionStyles,
                    selectedSkinToneId = session?.skinToneId,
                    selectedFabricToneId = session?.fabricToneId,
                    selectedStyleId = session?.styleId
                )
            }
        }
    }

    fun selectSkinTone(id: String) {
        _state.update { it.copy(selectedSkinToneId = id) }
    }

    fun selectFabricTone(id: String) {
        _state.update { it.copy(selectedFabricToneId = id) }
    }

    fun selectStyle(id: String) {
        _state.update { it.copy(selectedStyleId = id) }
    }

    fun persist(sessionId: String) {
        val s = _state.value
        sessionStore.update(sessionId) {
            it.copy(
                skinToneId = s.selectedSkinToneId,
                fabricToneId = s.selectedFabricToneId,
                styleId = s.selectedStyleId
            )
        }
    }
}
