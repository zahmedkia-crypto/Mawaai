package com.mawaai.love.app.design.ai.suggestions

import com.mawaai.love.app.design.rendering.RenderAssessment

/**
 * Session-level state for the Draw -> Analyze -> Suggest -> Render -> Refine
 * loop. UI/ViewModels can keep one instance per project and append rounds until
 * the user says the result matches what they imagined.
 */
data class RefinementSession(
    val projectId: String,
    val userIntent: String = "",
    val rounds: List<SuggestionIteration> = emptyList(),
    val renderAssessments: List<RenderAssessment> = emptyList(),
    val satisfactionHistory: List<SatisfactionFeedback> = emptyList(),
    val favoriteVersionId: String? = null,
) {
    val currentRound: SuggestionIteration? get() = rounds.lastOrNull()
    val latestAssessment: RenderAssessment? get() = renderAssessments.lastOrNull()
    val latestSatisfaction: SatisfactionFeedback? get() = satisfactionHistory.lastOrNull()

    val acceptedSuggestions: List<Suggestion>
        get() = rounds.flatMap { it.acceptedSuggestions }

    val shouldKeepRefining: Boolean
        get() {
            val satisfaction = latestSatisfaction?.closenessToIntentPct ?: return true
            val assessmentReady = latestAssessment?.isProductionReady ?: false
            return satisfaction < TARGET_SATISFACTION_PCT || !assessmentReady
        }

    fun withRound(round: SuggestionIteration): RefinementSession =
        copy(rounds = rounds + round)

    fun withAssessment(assessment: RenderAssessment): RefinementSession =
        copy(renderAssessments = renderAssessments + assessment)

    fun withSatisfaction(feedback: SatisfactionFeedback): RefinementSession =
        copy(satisfactionHistory = satisfactionHistory + feedback)

    companion object {
        const val TARGET_SATISFACTION_PCT = 95
    }
}

data class SatisfactionFeedback(
    val closenessToIntentPct: Int,
    val whatIsMissing: String = "",
    val keepFromCurrent: String = "",
    val changeNext: String = "",
) {
    init {
        require(closenessToIntentPct in 0..100) {
            "closenessToIntentPct must be in 0..100, was $closenessToIntentPct"
        }
    }

    fun toSuggestionHint(): String = buildString {
        append("User satisfaction is $closenessToIntentPct%. ")
        if (keepFromCurrent.isNotBlank()) append("Keep: $keepFromCurrent. ")
        if (changeNext.isNotBlank()) append("Change next: $changeNext. ")
        if (whatIsMissing.isNotBlank()) append("Missing: $whatIsMissing. ")
    }.trim()
}

data class VersionPreference(
    val versionId: String,
    val likedElements: List<String> = emptyList(),
    val dislikedElements: List<String> = emptyList(),
    val mergeInstruction: String = "",
) {
    fun toPromptHint(): String = buildString {
        append("Use version $versionId as a preference reference. ")
        if (likedElements.isNotEmpty()) append("Keep ${likedElements.joinToString()}. ")
        if (dislikedElements.isNotEmpty()) append("Avoid ${dislikedElements.joinToString()}. ")
        if (mergeInstruction.isNotBlank()) append(mergeInstruction)
    }.trim()
}
