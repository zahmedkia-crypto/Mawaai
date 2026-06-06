package com.mawaai.love.app.data.repository

import com.mawaai.love.app.data.database.dao.ProductMockupDao
import com.mawaai.love.app.data.database.dao.ProjectDao
import com.mawaai.love.app.data.database.dao.TemplateDao
import com.mawaai.love.app.data.database.entities.ProductMockupEntity
import com.mawaai.love.app.data.database.entities.ProjectEntity
import com.mawaai.love.app.data.database.entities.TemplateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.google.gson.Gson
import com.mawaai.love.app.design.ai.analysis.SketchAnalysis
import com.mawaai.love.app.design.ai.suggestions.SatisfactionFeedback
import com.mawaai.love.app.design.ai.suggestions.Suggestion
import com.mawaai.love.app.design.ai.suggestions.SuggestionsResponse
import com.mawaai.love.app.design.rendering.RenderAssessment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@Singleton
class TemplateRepository @Inject constructor(
    private val dao: TemplateDao
) {
    fun getAllTemplates(): Flow<List<TemplateEntity>> = dao.getAllTemplates()
    fun getTemplatesByCategory(category: String): Flow<List<TemplateEntity>> = dao.getTemplatesByCategory(category)
    suspend fun getTemplateById(id: String): TemplateEntity? = dao.getTemplateById(id)
    suspend fun insertTemplates(templates: List<TemplateEntity>) = dao.insertTemplates(templates)
}

@Singleton
class ProjectRepository @Inject constructor(
    private val dao: ProjectDao,
    private val gson: Gson
) {
    fun getAllProjects(): Flow<List<ProjectEntity>> = dao.getAllProjects()
    suspend fun getProjectById(id: String): ProjectEntity? = dao.getProjectById(id)
    fun observeProjectById(id: String): Flow<ProjectEntity?> = dao.observeProjectById(id)
    
    suspend fun createProject(templateId: String, sketchPath: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val project = ProjectEntity(
            id = id,
            templateId = templateId,
            title = null,
            sketchPath = sketchPath,
            sketchCapturedAt = System.currentTimeMillis(),
            analysisJson = null,
            suggestionsJson = null,
            acceptedSuggestionIds = "",
            colorOverride = null,
            templateColor = null,
            renderedPath = null,
            renderPrompt = null,
            renderedAt = null,
            renderQualityJson = null,
            exportPath = null,
            exportedAt = null,
            exportMockupId = null,
            isPublic = false,
            notes = null,
            status = "DRAFT",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertProject(project)
        id
    }

    suspend fun saveAnalysis(id: String, analysis: SketchAnalysis) = withContext(Dispatchers.IO) {
        val project = dao.getProjectById(id) ?: return@withContext
        val updated = project.copy(
            analysisJson = gson.toJson(analysis),
            status = "ANALYZED",
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProject(updated)
    }

    suspend fun saveSuggestions(
        id: String,
        suggestions: List<Suggestion>,
        acceptedSuggestionIds: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        val project = dao.getProjectById(id) ?: return@withContext
        val updated = project.copy(
            suggestionsJson = gson.toJson(SuggestionsResponse(suggestions)),
            acceptedSuggestionIds = acceptedSuggestionIds.joinToString(","),
            status = "SUGGESTIONS_READY",
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProject(updated)
    }

    /**
     * MT-028: persist the user-selected color override (hex, e.g. "#B8860B")
     * so the next render reads it through RenderPromptBuilder.
     *
     * Pass `null` to clear the override and fall back to the template's
     * traditional palette.
     */
    suspend fun saveColorOverride(id: String, hex: String?) = withContext(Dispatchers.IO) {
        val project = dao.getProjectById(id) ?: return@withContext
        val updated = project.copy(
            colorOverride = hex,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProject(updated)
    }

    /**
     * MT-025: persist the comma-separated set of suggestion IDs the user
     * has accepted, so RenderPromptBuilder.build() reads them on the next
     * render and the renderer (MT-027) appends their preview hints to the
     * final prompt.
     */
    suspend fun saveAcceptedSuggestions(id: String, csvIds: String) = withContext(Dispatchers.IO) {
        val project = dao.getProjectById(id) ?: return@withContext
        val updated = project.copy(
            acceptedSuggestionIds = csvIds,
            status = "REFINEMENT_SELECTED",
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProject(updated)
    }

    /**
     * MT-029: persist a freshly produced render -- the absolute file path
     * (written by RenderFileStore) plus the exact prompt that produced it.
     * Sets renderedAt = now and bumps updatedAt so any Flow observer sees
     * the change.
     */
    suspend fun saveRender(id: String, renderedPath: String, renderPrompt: String) =
        withContext(Dispatchers.IO) {
            val project = dao.getProjectById(id) ?: return@withContext
            val now = System.currentTimeMillis()
            val updated = project.copy(
                renderedPath = renderedPath,
                renderPrompt = renderPrompt,
                renderedAt = now,
                status = "RENDERED",
                updatedAt = now,
            )
            dao.updateProject(updated)
        }

    suspend fun saveRenderAssessment(id: String, assessment: RenderAssessment) =
        withContext(Dispatchers.IO) {
            val project = dao.getProjectById(id) ?: return@withContext
            val updated = project.copy(
                renderQualityJson = gson.toJson(assessment),
                status = if (assessment.isProductionReady) "RENDER_READY" else "REFINEMENT_NEEDED",
                updatedAt = System.currentTimeMillis()
            )
            dao.updateProject(updated)
        }

    suspend fun saveSatisfactionFeedback(id: String, feedback: SatisfactionFeedback) =
        withContext(Dispatchers.IO) {
            val project = dao.getProjectById(id) ?: return@withContext
            val feedbackLine = "Refinement feedback: ${feedback.toSuggestionHint()}"
            val updatedNotes = listOfNotNull(project.notes, feedbackLine)
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n")
            val updated = project.copy(
                notes = updatedNotes,
                status = if (feedback.closenessToIntentPct >= 95) "USER_APPROVED" else "REFINEMENT_NEEDED",
                updatedAt = System.currentTimeMillis()
            )
            dao.updateProject(updated)
        }

    suspend fun saveProject(project: ProjectEntity) = dao.insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun deleteProject(id: String) = dao.deleteProjectById(id)
}

@Singleton
class ProductMockupRepository @Inject constructor(
    private val dao: ProductMockupDao
) {
    fun getAllMockups(): Flow<List<ProductMockupEntity>> = dao.getAllMockups()
    fun getMockupsByCategory(category: String): Flow<List<ProductMockupEntity>> = dao.getMockupsByCategory(category)
    suspend fun insertMockups(mockups: List<ProductMockupEntity>) = dao.insertMockups(mockups)
}