package com.mawaai.love.app.design.presentation.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.data.repository.DesignCatalogRepository
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.domain.model.ConversionStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StyleSelectionState(
    val styles: List<ConversionStyle> = emptyList(),
    val selectedStyleId: String? = null
)

@HiltViewModel
class StyleSelectionViewModel @Inject constructor(
    private val catalogRepo: DesignCatalogRepository,
    private val sessionStore: DesignSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(StyleSelectionState())
    val state: StateFlow<StyleSelectionState> = _state

    init {
        viewModelScope.launch {
            val catalog = catalogRepo.load()
            _state.update { it.copy(styles = catalog.conversionStyles) }
        }
    }

    fun select(styleId: String) {
        _state.update { it.copy(selectedStyleId = styleId) }
    }

    fun persistSelection(sessionId: String) {
        val id = _state.value.selectedStyleId ?: return
        sessionStore.update(sessionId) { it.copy(styleId = id) }
    }
}
