package com.mawaai.love.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.model.UserProfile
import com.mawaai.love.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    val profile = repository.getProfile()
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }
}
