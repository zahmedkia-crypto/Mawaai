package com.mawaai.love.app.core.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.model.BackgroundTheme
import com.mawaai.love.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    profileRepository: ProfileRepository
) : ViewModel() {

    /**
     * Live preference. Defaults to AUTO until the profile row loads — this
     * means a fresh install lands in MORNING during the day and NIGHT after
     * 18:00 without the user touching settings.
     */
    val themeMode = profileRepository.getProfile()
        .map { it?.themeMode ?: BackgroundTheme.AUTO }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundTheme.AUTO)
}
