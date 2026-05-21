package com.mawaai.love.app.design.showcase.domain

import androidx.compose.ui.graphics.Color

/**
 * A showcase scene — represented as a programmatically-rendered backdrop plus four
 * perspective points that define where the artwork should be placed (the "frame zone").
 * Top-left, top-right, bottom-right, bottom-left in NORMALIZED coordinates [0..1].
 */
data class ShowcaseScene(
    val id: String,
    val nameRes: Int,
    val backdrop: SceneBackdrop,
    val frameZone: FrameZone,
    val ambientColor: Color
)

data class FrameZone(
    val tlX: Float, val tlY: Float,
    val trX: Float, val trY: Float,
    val brX: Float, val brY: Float,
    val blX: Float, val blY: Float
)

enum class SceneBackdrop {
    GALLERY, LIVING_ROOM, MUSEUM, OUTDOOR, MODERN_HALL, MAJLIS
}

enum class ShowcaseFrame { NONE, GOLD, MODERN_BLACK, ARABIC_CARVED }

enum class ShowcaseLighting { NATURAL, WARM, COOL, DRAMATIC }
