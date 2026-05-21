package com.mawaai.love.app.design.showcase.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mawaai.love.app.R
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.design.showcase.domain.ShowcaseFrame
import com.mawaai.love.app.design.showcase.domain.ShowcaseLighting
import com.mawaai.love.app.design.showcase.domain.ShowcaseScene
import com.mawaai.love.app.design.showcase.render.FrameRenderer
import com.mawaai.love.app.design.showcase.render.LightingOverlay
import com.mawaai.love.app.design.showcase.render.PerspectiveCompositor
import com.mawaai.love.app.design.showcase.render.SceneBackdropView
import com.mawaai.love.app.design.showcase.render.VisitorSilhouettes
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun ShowcaseScreen(
    nav: NavController,
    artworkId: Long,
    viewModel: ShowcaseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(artworkId) { viewModel.load(artworkId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MawaaiColors.DesignBgDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MawaaiColors.DesignSurface)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MawaaiColors.DesignGold)
            }
            Text(
                text = stringResource(R.string.showcase_cinematic),
                fontFamily = CairoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MawaaiColors.DesignTextLight,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(onClick = { viewModel.toggleVisitors() }) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = stringResource(R.string.showcase_visitors_show),
                    tint = if (state.showVisitors) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = { viewModel.toggleTitleCard() }) {
                Icon(
                    Icons.Default.Title,
                    contentDescription = stringResource(R.string.showcase_title_card),
                    tint = if (state.showTitleCard) MawaaiColors.DesignGold else MawaaiColors.DesignTextLight.copy(alpha = 0.5f)
                )
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MawaaiColors.DesignGold)
            }
            return
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CinematicStage(
                scene = state.selectedScene,
                frame = state.selectedFrame,
                lighting = state.selectedLighting,
                artworkBitmap = state.artworkBitmap,
                showVisitors = state.showVisitors,
                showTitleCard = state.showTitleCard,
                title = state.artwork?.title.orEmpty()
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MawaaiColors.DesignSurface)
                .padding(12.dp)
        ) {
            ShowcaseSceneRow(
                scenes = state.scenes,
                selected = state.selectedScene,
                onSelect = { viewModel.selectScene(it) }
            )
            Spacer(Modifier.height(8.dp))
            ShowcaseFrameRow(
                selected = state.selectedFrame,
                onSelect = { viewModel.selectFrame(it) }
            )
            Spacer(Modifier.height(8.dp))
            ShowcaseLightingRow(
                selected = state.selectedLighting,
                onSelect = { viewModel.selectLighting(it) }
            )
        }
    }
}

@Composable
private fun CinematicStage(
    scene: ShowcaseScene?,
    frame: ShowcaseFrame,
    lighting: ShowcaseLighting,
    artworkBitmap: Bitmap?,
    showVisitors: Boolean,
    showTitleCard: Boolean,
    title: String
) {
    if (scene == null) return

    // Ken Burns effect: slow zoom + slight pan
    val transition = rememberInfiniteTransition(label = "kenburns")
    val zoom by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom"
    )
    val panX by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panX"
    )

    var stageSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { stageSize = it }
            .clip(RoundedCornerShape(0.dp))
    ) {
        // Animated container holding the entire scene
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(0.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
        ) {
                // Backdrop (programmatic, scaled by Ken Burns)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { composeCanvas ->
                        composeCanvas.nativeCanvas.save()
                        composeCanvas.nativeCanvas.translate(panX, 0f)
                        composeCanvas.nativeCanvas.scale(zoom, zoom, size.width / 2f, size.height / 2f)
                        composeCanvas.nativeCanvas.restore()
                    }
                }
                SceneBackdropView(backdrop = scene.backdrop, modifier = Modifier.fillMaxSize())
                // Artwork warped onto frame zone
                if (artworkBitmap != null && stageSize.width > 0 && stageSize.height > 0) {
                    ArtworkOverlay(
                        artwork = artworkBitmap,
                        scene = scene,
                        frame = frame,
                        stageSize = stageSize,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (showVisitors) {
                    VisitorSilhouettes(
                        visitorCount = 3,
                        color = scene.ambientColor.copy(alpha = 0.55f).overlayWithBlack(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                LightingOverlay(lighting = lighting, modifier = Modifier.fillMaxSize())
                if (showTitleCard && title.isNotBlank()) {
                    TitleCard(title = title, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun ArtworkOverlay(
    artwork: Bitmap,
    scene: ShowcaseScene,
    frame: ShowcaseFrame,
    stageSize: IntSize,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val tlX = scene.frameZone.tlX * w; val tlY = scene.frameZone.tlY * h
        val trX = scene.frameZone.trX * w; val trY = scene.frameZone.trY * h
        val brX = scene.frameZone.brX * w; val brY = scene.frameZone.brY * h
        val blX = scene.frameZone.blX * w; val blY = scene.frameZone.blY * h

        drawIntoCanvas { composeCanvas ->
            val nc = composeCanvas.nativeCanvas
            val src = floatArrayOf(
                0f, 0f,
                artwork.width.toFloat(), 0f,
                artwork.width.toFloat(), artwork.height.toFloat(),
                0f, artwork.height.toFloat()
            )
            val dst = floatArrayOf(tlX, tlY, trX, trY, brX, brY, blX, blY)
            val matrix = android.graphics.Matrix().apply { setPolyToPoly(src, 0, dst, 0, 4) }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
            }
            // Soft drop shadow under the artwork
            val shadowMatrix = android.graphics.Matrix().apply {
                setPolyToPoly(src, 0, floatArrayOf(tlX + 8, tlY + 8, trX + 8, trY + 8, brX + 8, brY + 8, blX + 8, blY + 8), 0, 4)
            }
            val shadowPaint = android.graphics.Paint().apply {
                colorFilter = android.graphics.PorterDuffColorFilter(android.graphics.Color.argb(120, 0, 0, 0), android.graphics.PorterDuff.Mode.SRC_IN)
                maskFilter = android.graphics.BlurMaskFilter(8f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            nc.drawBitmap(artwork, shadowMatrix, shadowPaint)
            nc.drawBitmap(artwork, matrix, paint)
        }

        FrameRenderer.draw(
            this,
            frame = frame,
            tl = Offset(tlX, tlY),
            tr = Offset(trX, trY),
            br = Offset(brX, brY),
            bl = Offset(blX, blY)
        )
    }
}

@Composable
private fun TitleCard(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000))
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ShowcaseSceneRow(
    scenes: List<ShowcaseScene>,
    selected: ShowcaseScene?,
    onSelect: (ShowcaseScene) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.showcase_pick_scene),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MawaaiColors.DesignTextLight
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(scenes, key = { it.id }) { scene ->
                val isSelected = scene.id == selected?.id
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MawaaiColors.DesignBgDark)
                        .border(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MawaaiColors.DesignGold else MawaaiColors.DesignGold.copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(scene) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        SceneBackdropView(backdrop = scene.backdrop)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(scene.nameRes),
                        fontFamily = CairoFamily,
                        fontSize = 11.sp,
                        color = MawaaiColors.DesignTextLight
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseFrameRow(selected: ShowcaseFrame, onSelect: (ShowcaseFrame) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.showcase_pick_frame),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MawaaiColors.DesignTextLight
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val frames = listOf(
                ShowcaseFrame.NONE to R.string.showcase_frame_none,
                ShowcaseFrame.GOLD to R.string.showcase_frame_gold,
                ShowcaseFrame.MODERN_BLACK to R.string.showcase_frame_modern,
                ShowcaseFrame.ARABIC_CARVED to R.string.showcase_frame_arabic
            )
            items(frames, key = { it.first.name }) { (frame, nameRes) ->
                Chip(
                    label = stringResource(nameRes),
                    selected = frame == selected,
                    onClick = { onSelect(frame) }
                )
            }
        }
    }
}

@Composable
private fun ShowcaseLightingRow(selected: ShowcaseLighting, onSelect: (ShowcaseLighting) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.showcase_pick_lighting),
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MawaaiColors.DesignTextLight
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val items = listOf(
                ShowcaseLighting.NATURAL to R.string.showcase_lighting_natural,
                ShowcaseLighting.WARM to R.string.showcase_lighting_warm,
                ShowcaseLighting.COOL to R.string.showcase_lighting_cool,
                ShowcaseLighting.DRAMATIC to R.string.showcase_lighting_dramatic
            )
            items(items, key = { it.first.name }) { (lighting, nameRes) ->
                Chip(
                    label = stringResource(nameRes),
                    selected = lighting == selected,
                    onClick = { onSelect(lighting) }
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MawaaiColors.DesignGold else MawaaiColors.DesignBgDark)
            .border(1.dp, MawaaiColors.DesignGold.copy(alpha = if (selected) 1f else 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontFamily = CairoFamily,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MawaaiColors.DesignBgDark else MawaaiColors.DesignTextLight
        )
    }
}

private fun Color.overlayWithBlack(): Color =
    Color(red * 0.4f, green * 0.4f, blue * 0.4f, alpha)

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun ShowcaseScreenPreview() {
    ShowcaseScreen(nav = rememberNavController(), artworkId = 0L)
}
