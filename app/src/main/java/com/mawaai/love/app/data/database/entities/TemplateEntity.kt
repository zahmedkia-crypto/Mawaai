package com.mawaai.love.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,                    // UUID
    val category: String,                           // TemplateCategory.name
    val name: String,
    val surfaceType: String,                        // e.g. "skin_palm"
    val description: String?,
    val referenceImageUrl: String?,
    val assetPath: String,                          // NEW for Android: assets path
    val zonesJson: String,                          // serialized List<TemplateZone>
    val culturalRulesJson: String,                  // serialized Map<String, String>
    val traditionalPaletteJson: String,             // serialized List<String> (hex colors)
    val primaryLight: String = "",
    val material: String = "",
    val surfaceReflectance: String = "",
    val recommendedComplexity: String = "medium",
    val maxCoveragePct: Int = 75,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class LightingProfile(
    val primaryLight: String = "",
    val material: String = "",
    val surfaceReflectance: String = ""
)

data class TemplateZone(
    val id: String,
    val name: String,
    val quad: List<List<Float>>,                    // 4 [x,y] pairs normalized [0..1]
    val blend: String = "FABRIC_REALISTIC",
    val alpha: Float = 0.8f
)
