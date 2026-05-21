package com.mawaai.love.app.ui.intro

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resolves the "first launch?" flag exactly once per app open so the intro
 * screen can decide whether to route to Onboarding (first time) or straight
 * to Home (subsequent opens). The intro video itself plays every launch.
 */
@HiltViewModel
class IntroViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val firstLaunchKey = booleanPreferencesKey("first_launch")

    private val _isFirstLaunch = MutableStateFlow<Boolean?>(null)
    val isFirstLaunch = _isFirstLaunch.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val first = prefs[firstLaunchKey] ?: true
            _isFirstLaunch.value = first
            if (first) {
                dataStore.edit { it[firstLaunchKey] = false }
            }
        }
    }
}
