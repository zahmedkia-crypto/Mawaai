package com.mawaai.love.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "love_letters")
data class LoveLetter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val backgroundId: Int = 0,   // 0-4 للخلفيات المدمجة
    val fontFamily: String = "Cairo",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
