package com.mawaai.love.app.ui.memories

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.model.Memory
import com.mawaai.love.app.data.model.MemoryCategory
import com.mawaai.love.app.data.repository.MemoryRepository
import com.mawaai.love.app.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoriesViewModel @Inject constructor(
    private val repository: MemoryRepository,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<MemoryCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    val memories = _selectedCategory.flatMapLatest { category ->
        if (category == null) {
            repository.getAllMemories()
        } else {
            repository.getByCategory(category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteMemories = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: MemoryCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Persists a memory on the application-scoped coroutine so the work
     * survives if the user navigates away from AddMemory before the insert
     * settles. The callback is dispatched on the main thread; callers must
     * tolerate it running after the screen is gone (use applicationContext
     * for any Toasts).
     */
    fun addMemory(
        memory: Memory,
        imageUri: Uri?,
        onResult: (Result<Long>) -> Unit
    ) {
        appScope.launch {
            onResult(repository.addMemory(memory, imageUri))
        }
    }

    fun deleteMemory(memory: Memory) {
        appScope.launch {
            repository.deleteMemory(memory)
        }
    }

    suspend fun getMemoryById(id: Long): Memory? {
        return repository.getAllMemories().first().find { it.id == id }
    }
}
