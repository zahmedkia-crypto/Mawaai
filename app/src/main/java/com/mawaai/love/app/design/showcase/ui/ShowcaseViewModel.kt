package com.mawaai.love.app.design.showcase.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.model.Artwork
import com.mawaai.love.app.data.repository.ArtworkRepository
import com.mawaai.love.app.design.showcase.data.ShowcaseSceneRepository
import com.mawaai.love.app.design.showcase.domain.ShowcaseFrame
import com.mawaai.love.app.design.showcase.domain.ShowcaseLighting
import com.mawaai.love.app.design.showcase.domain.ShowcaseScene
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ShowcaseUiState(
    val artwork: Artwork? = null,
    val artworkBitmap: Bitmap? = null,
    val scenes: List<ShowcaseScene> = emptyList(),
    val selectedScene: ShowcaseScene? = null,
    val selectedFrame: ShowcaseFrame = ShowcaseFrame.GOLD,
    val selectedLighting: ShowcaseLighting = ShowcaseLighting.NATURAL,
    val showVisitors: Boolean = true,
    val showTitleCard: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ShowcaseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artworkRepository: ArtworkRepository,
    private val sceneRepository: ShowcaseSceneRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShowcaseUiState())
    val state: StateFlow<ShowcaseUiState> = _state

    fun load(artworkId: Long) {
        if (artworkId <= 0L) {
            _state.update {
                it.copy(
                    scenes = sceneRepository.all(),
                    selectedScene = sceneRepository.all().first(),
                    isLoading = false
                )
            }
            return
        }
        viewModelScope.launch {
            val artwork = artworkRepository.getById(artworkId)
            val bitmap = withContext(Dispatchers.IO) {
                artwork?.fullImagePath?.let { BitmapFactory.decodeFile(it) }
            }
            val scenes = sceneRepository.all()
            _state.update {
                it.copy(
                    artwork = artwork,
                    artworkBitmap = bitmap,
                    scenes = scenes,
                    selectedScene = scenes.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun selectScene(scene: ShowcaseScene) {
        _state.update { it.copy(selectedScene = scene) }
    }

    fun selectFrame(frame: ShowcaseFrame) {
        _state.update { it.copy(selectedFrame = frame) }
    }

    fun selectLighting(lighting: ShowcaseLighting) {
        _state.update { it.copy(selectedLighting = lighting) }
    }

    fun toggleVisitors() {
        _state.update { it.copy(showVisitors = !it.showVisitors) }
    }

    fun toggleTitleCard() {
        _state.update { it.copy(showTitleCard = !it.showTitleCard) }
    }

    override fun onCleared() {
        _state.value.artworkBitmap?.recycle()
        super.onCleared()
    }
}
