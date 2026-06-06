package com.mawaai.love.app.ui.design.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mawaai.love.app.data.repository.ProjectRepository
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import com.mawaai.love.app.design.ai.suggestions.SuggestionsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MT-025: state for [SuggestionCardsScreen].
 *
 * Reads the suggestions JSON that the upstream `SuggestionsClient` (MT-023)
 * persisted to [com.mawaai.love.app.data.database.entities.ProjectEntity.suggestionsJson]
 * and tracks which of them the user has accepted. Acceptance survives a
 * config change because it's persisted back to the project row on submit.
 */
data class SuggestionCardsUiState(
    val isLoading: Boolean = true,
    val suggestions: List<Suggestion> = emptyList(),
    val acceptedIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@HiltViewModel
class SuggestionCardsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val gson: Gson,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionCardsUiState())
    val uiState: StateFlow<SuggestionCardsUiState> = _uiState.asStateFlow()

    private var currentProjectId: String? = null

    /**
     * Load suggestions for [projectId]. Idempotent -- repeated calls with the
     * same id are a no-op so the screen survives recomposition without
     * thrashing the database.
     */
    fun load(projectId: String) {
        if (currentProjectId == projectId && !_uiState.value.isLoading) return
        currentProjectId = projectId

        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                _uiState.value = SuggestionCardsUiState(
                    isLoading = false,
                    errorMessage = "Project not found.",
                )
                return@launch
            }
            val suggestions = parseSuggestions(project.suggestionsJson)
            val accepted = parseAcceptedCsv(project.acceptedSuggestionIds)
            _uiState.value = SuggestionCardsUiState(
                isLoading = false,
                suggestions = suggestions,
                acceptedIds = accepted,
                errorMessage = if (suggestions.isEmpty()) {
                    "No suggestions yet -- run analysis first."
                } else {
                    null
                },
            )
        }
    }

    /** Flip the accepted/skipped state of [id]. */
    fun toggle(id: String) {
        _uiState.update { state ->
            val next = if (id in state.acceptedIds) {
                state.acceptedIds - id
            } else {
                state.acceptedIds + id
            }
            state.copy(acceptedIds = next)
        }
    }

    /**
     * Persist the current acceptance set back onto the project row so the
     * renderer (MT-027) picks it up on the next render.
     */
    fun submit(onDone: () -> Unit = {}) {
        val id = currentProjectId ?: return
        val csv = _uiState.value.acceptedIds.joinToString(",")
        viewModelScope.launch {
            projectRepository.saveAcceptedSuggestions(id, csv)
            onDone()
        }
    }

    private fun parseSuggestions(json: String?): List<Suggestion> {
        if (json.isNullOrBlank()) return emptyList()
        val listType = object : TypeToken<List<Suggestion>>() {}.type
        return runCatching {
            gson.fromJson<List<Suggestion>>(json, listType)
        }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: runCatching {
                gson.fromJson(json, SuggestionsResponse::class.java).suggestions
            }.getOrDefault(emptyList())
    }

    private fun parseAcceptedCsv(csv: String?): Set<String> {
        if (csv.isNullOrBlank()) return emptySet()
        return csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}