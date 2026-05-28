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
