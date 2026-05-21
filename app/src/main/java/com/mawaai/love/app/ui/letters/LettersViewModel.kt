package com.mawaai.love.app.ui.letters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.model.LoveLetter
import com.mawaai.love.app.data.repository.LoveLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LettersViewModel @Inject constructor(
    private val repository: LoveLetterRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: All, 1: Favorites
    val selectedTab = _selectedTab.asStateFlow()

    val letters = _selectedTab.flatMapLatest { tab ->
        if (tab == 0) {
            repository.getAllLetters()
        } else {
            repository.getFavorites()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun addLetter(letter: LoveLetter, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addLetter(letter)
            onSuccess()
        }
    }

    fun updateLetter(letter: LoveLetter) {
        viewModelScope.launch {
            repository.updateLetter(letter)
        }
    }

    fun deleteLetter(letter: LoveLetter) {
        viewModelScope.launch {
            repository.deleteLetter(letter)
        }
    }

    suspend fun getLetterById(id: Long): LoveLetter? {
        return repository.getLetterById(id)
    }
}
