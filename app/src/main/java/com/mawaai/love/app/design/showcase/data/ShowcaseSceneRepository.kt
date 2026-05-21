package com.mawaai.love.app.design.showcase.data

import androidx.compose.ui.graphics.Color
import com.mawaai.love.app.R
import com.mawaai.love.app.design.showcase.domain.FrameZone
import com.mawaai.love.app.design.showcase.domain.SceneBackdrop
import com.mawaai.love.app.design.showcase.domain.ShowcaseScene
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Static, code-defined catalog of showcase scenes. Each scene declares where the
 * artwork should be placed (frame zone in normalized 0..1 coords) and a programmatic
 * backdrop for offline-first rendering.
 */
@Singleton
class ShowcaseSceneRepository @Inject constructor() {

    private val scenes: List<ShowcaseScene> = listOf(
        ShowcaseScene(
            id = "gallery",
            nameRes = R.string.showcase_scene_gallery,
            backdrop = SceneBackdrop.GALLERY,
            frameZone = FrameZone(0.25f, 0.18f, 0.75f, 0.18f, 0.75f, 0.72f, 0.25f, 0.72f),
            ambientColor = Color(0xFFEFE4D2)
        ),
        ShowcaseScene(
            id = "living_room",
            nameRes = R.string.showcase_scene_living_room,
            backdrop = SceneBackdrop.LIVING_ROOM,
            frameZone = FrameZone(0.30f, 0.15f, 0.70f, 0.18f, 0.68f, 0.55f, 0.32f, 0.52f),
            ambientColor = Color(0xFFE8D2B5)
        ),
        ShowcaseScene(
            id = "museum",
            nameRes = R.string.showcase_scene_museum,
            backdrop = SceneBackdrop.MUSEUM,
            frameZone = FrameZone(0.20f, 0.20f, 0.80f, 0.20f, 0.80f, 0.78f, 0.20f, 0.78f),
            ambientColor = Color(0xFFC9D6E0)
        ),
        ShowcaseScene(
            id = "outdoor",
            nameRes = R.string.showcase_scene_outdoor,
            backdrop = SceneBackdrop.OUTDOOR,
            frameZone = FrameZone(0.18f, 0.22f, 0.82f, 0.22f, 0.82f, 0.78f, 0.18f, 0.78f),
            ambientColor = Color(0xFFB8A78A)
        ),
        ShowcaseScene(
            id = "modern",
            nameRes = R.string.showcase_scene_modern,
            backdrop = SceneBackdrop.MODERN_HALL,
            frameZone = FrameZone(0.28f, 0.20f, 0.72f, 0.20f, 0.72f, 0.70f, 0.28f, 0.70f),
            ambientColor = Color(0xFFD9D9D9)
        ),
        ShowcaseScene(
            id = "majlis",
            nameRes = R.string.showcase_scene_majlis,
            backdrop = SceneBackdrop.MAJLIS,
            frameZone = FrameZone(0.32f, 0.22f, 0.68f, 0.20f, 0.66f, 0.58f, 0.34f, 0.56f),
            ambientColor = Color(0xFFD4884B)
        )
    )

    fun all(): List<ShowcaseScene> = scenes
    fun byId(id: String): ShowcaseScene? = scenes.firstOrNull { it.id == id }
}
