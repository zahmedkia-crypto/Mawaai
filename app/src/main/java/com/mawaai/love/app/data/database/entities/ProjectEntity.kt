package com.mawaai.love.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("templateId"),
        Index("status"),
        Index("isPublic"),
        Index("createdAt")
    ]
)
data class ProjectEntity(
    @PrimaryKey val id: String,                    // UUID
    val templateId: String,
    val title: String?,
    val sketchPath: String?,                        // local file path
    val sketchCapturedAt: Long?,

    // Phase 3 output
    val analysisJson: String?,                      // serialized SketchAnalysis
    // Phase 4 output
    val suggestionsJson: String?,                   // serialized List<Suggestion>
    val acceptedSuggestionIds: String,              // CSV

    // Phase 7
    val colorOverride: String?,                     // hex
    val templateColor: String?,                     // legacy alias

    // Phase 5 + 6 output
    val renderedPath: String?,
    val renderPrompt: String?,
    val renderedAt: Long?,
    val renderQualityJson: String?,                 // serialized RenderQuality

    // Phase 8 output
    val exportPath: String?,
    val exportedAt: Long?,
    val exportMockupId: String?,

    // Sharing (deferred)
    val isPublic: Boolean = false,

    val notes: String?,
    val status: String = "DRAFT",                   // ProjectStatus.name
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
