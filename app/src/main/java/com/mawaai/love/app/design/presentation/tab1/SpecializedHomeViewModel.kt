package com.mawaai.love.app.design.presentation.tab1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.design.data.repository.DesignCatalogRepository
import com.mawaai.love.app.design.data.repository.DesignSessionStore
import com.mawaai.love.app.design.domain.model.DesignCategory
import com.mawaai.love.app.design.domain.model.DesignSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpecializedHomeState(
    val categories: List<DesignCategory> = emptyList(),
    val selectedCategory: DesignCategory? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SpecializedHomeViewModel @Inject constructor(
    private val catalogRepo: DesignCatalogRepository,
    private val sessionStore: DesignSessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(SpecializedHomeState())
    val state: StateFlow<SpecializedHomeState> = _state

    init {
        viewModelScope.launch {
            val catalog = catalogRepo.load()
            _state.update { it.copy(categories = catalog.categories, isLoading = false) }
        }
    }

    fun selectCategory(category: DesignCategory) {
        _state.update { it.copy(selectedCategory = category) }
    }

    fun dismissSheet() {
        _state.update { it.copy(selectedCategory = null) }
    }

    fun createSession(categoryId: String, subTypeId: String): String {
        val session = sessionStore.create(
            DesignSession(
                id = java.util.UUID.randomUUID().toString(),
                categoryId = categoryId,
                subTypeId = subTypeId,
                isConverterFlow = false
            )
        )
        return session.id
    }
}
