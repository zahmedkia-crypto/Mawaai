package com.mawaai.love.app.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.model.MoodEntry
import com.mawaai.love.app.data.model.MoodType
import com.mawaai.love.app.data.repository.MoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val repository: MoodRepository
) : ViewModel() {

    val moods = repository.getAllMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestMood = repository.getLatestMood()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addMood(mood: MoodType) {
        viewModelScope.launch {
            repository.addMood(MoodEntry(mood = mood))
        }
    }
}
