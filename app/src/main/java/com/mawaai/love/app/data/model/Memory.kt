package com.mawaai.love.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val imagePath: String?,        // مسار الصورة المحلي
    val date: Long,                // timestamp
    val category: MemoryCategory,
    val mood: MoodType,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedToCloud: Boolean = false
)

enum class MemoryCategory {
    ROMANTIC, TRAVEL, FOOD, SPECIAL_DAY, GENERAL
}
