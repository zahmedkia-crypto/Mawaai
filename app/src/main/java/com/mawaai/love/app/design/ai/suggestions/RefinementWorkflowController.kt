package com.mawaai.love.app.design.ai.suggestions

import com.mawaai.love.app.data.repository.ProjectRepository
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.rendering.RenderAssessment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefinementWorkflowController @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val suggestionEngine: AiSuggestionEngine
) {

    suspend fun startAfterAnalysis(
        projectId: String,
        analysis: SketchAnalysis
    ): SuggestionIteration {
        projectRepository.saveAnalysis(projectId, analysis)
        val iteration = suggestionEngine.afterAnalysis(analysis)
        projectRepository.saveSuggestions(projectId, iteration.suggestions)
        return iteration
    }

    suspend fun acceptSuggestions(
        projectId: String,
        iteration: SuggestionIteration,
        selectedIds: Set<String>
    ): SuggestionIteration {
        val accepted = suggestionEngine.accept(iteration, selectedIds)
        projectRepository.saveSuggestions(
            id = projectId,
            suggestions = accepted.suggestions,
            acceptedSuggestionIds = accepted.acceptedSuggestionIds
        )
        return accepted
    }

    suspend fun continueAfterRender(
        projectId: String,
        previousIteration: SuggestionIteration,
        assessment: RenderAssessment,
        feedback: SatisfactionFeedback? = null
    ): SuggestionIteration {
        projectRepository.saveRenderAssessment(projectId, assessment)
        if (feedback != null) {
            projectRepository.saveSatisfactionFeedback(projectId, feedback)
        }
        val next = suggestionEngine.afterRender(
            previous = previousIteration,
            assessment = assessment,
            feedback = feedback
        )
        projectRepository.saveSuggestions(projectId, next.suggestions)
        return next
    }

    suspend fun markUserSatisfied(
        projectId: String,
        feedback: SatisfactionFeedback
    ) {
        projectRepository.saveSatisfactionFeedback(
            id = projectId,
            feedback = feedback.copy(closenessToIntentPct = feedback.closenessToIntentPct.coerceAtLeast(95))
        )
    }
}
