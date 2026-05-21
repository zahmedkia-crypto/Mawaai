package com.mawaai.love.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artworks")
data class Artwork(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val categoryId: String,         // henna / abaya / walls
    val subTypeId: String?,         // hand / palm / mihrab / etc.
    val fullImagePath: String,      // PNG (flattened)
    val thumbnailPath: String,      // 256x256 thumbnail
    val width: Int,
    val height: Int,
    val tags: String = "",          // comma-separated
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
