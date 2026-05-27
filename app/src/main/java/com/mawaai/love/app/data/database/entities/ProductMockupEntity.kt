package com.mawaai.love.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_mockups")
data class ProductMockupEntity(
    @PrimaryKey val id: String,                    // UUID
    val name: String,
    val category: String,                          // TemplateCategory.name
    val surfaceMatchCsv: String,                   // CSV of surface ids
    val scene: String,                             // descriptive scene prompt
    val lighting: String,
    val perspective: String,
    val accentColor: String,                       // hex
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
