package com.mawaai.love.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mood: MoodType,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

enum class MoodType(val emoji: String, val label: String) {
    HAPPY("😊", "سعيد"),
    LOVING("💕", "محب"),
    AMAZED("😍", "مبهور"),
    GRATEFUL("🥰", "ممتنن"),
    EXCITED("💫", "متشوق")
}
