package com.mawaai.love.app.design.presentation.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaai.love.app.data.repository.ProjectRepository
import com.mawaai.love.app.design.ai.AIEngine
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.ai.suggestions.AiSuggestionEngine
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreativeAnalysisState(
    val isLoading: Boolean = false,
    val analysis: SketchAnalysis? = null,
    val suggestions: List<Suggestion> = emptyList(),
    val acceptedSuggestionIds: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class CreativeAnalysisViewModel @Inject constructor(
    private val aiEngine: AIEngine,
    private val suggestionEngine: AiSuggestionEngine,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreativeAnalysisState())
    val state: StateFlow<CreativeAnalysisState> = _state.asStateFlow()

    fun load(projectId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val analysis = aiEngine.analyzeProject(projectId)
                val iteration = suggestionEngine.afterAnalysis(analysis)
                projectRepository.saveSuggestions(projectId, iteration.suggestions)
                _state.update {
                    it.copy(
                        isLoading = false,
                        analysis = analysis,
                        suggestions = iteration.suggestions,
                        acceptedSuggestionIds = emptySet()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error") }
            }
        }
    }

    fun toggleSuggestion(suggestionId: String) {
        _state.update { current ->
            val newAccepted = if (current.acceptedSuggestionIds.contains(suggestionId)) {
                current.acceptedSuggestionIds - suggestionId
            } else {
                current.acceptedSuggestionIds + suggestionId
            }
            current.copy(acceptedSuggestionIds = newAccepted)
        }
    }

    fun applyAndContinue(projectId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                projectRepository.saveSuggestions(
                    id = projectId,
                    suggestions = _state.value.suggestions,
                    acceptedSuggestionIds = _state.value.acceptedSuggestionIds
                )
                onComplete()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to apply suggestions: ${e.message}") }
            }
        }
    }
}