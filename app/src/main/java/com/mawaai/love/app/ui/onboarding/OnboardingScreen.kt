package com.mawaai.love.app.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.components.HeartButton
import com.mawaai.love.app.core.components.ParticleHeartSystem
import com.mawaai.love.app.core.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MawaaiColors.DeepNight)
    ) {
        // Background
        ParticleHeartSystem(particleCount = 6, modifier = Modifier.alpha(0.2f))

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPage(page)
            }

            // Bottom section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Keep the indicator + "Start" button above the gesture
                    // nav bar in edge-to-edge mode. Without this the primary
                    // CTA on the last onboarding page gets half-hidden behind
                    // the system gesture pill.
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "indicator")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) MawaaiColors.RoseGold else MawaaiColors.CardElevated)
                        )
                    }
                }

                // Button
                HeartButton(
                    text = if (pagerState.currentPage == 2) "ابدأ الرحلة 💕" else "التالي",
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OnboardingPage(page: Int) {
    val title = when (page) {
        0 -> "ذكرياتنا"
        1 -> "رسائلي لكِ"
        else -> "مواعيدنا"
    }
    val description = when (page) {
        0 -> "احفظي كل لحظة جميلة... لأن بعض اللحظات تستحق أن تخلد 💕"
        1 -> "كل ما يصعب قوله بالكلمات، يُكتب بالحروف 💌"
        else -> "كل لقاء يستحق احتفالاً... لأنكِ تستحقين العالم ⏳"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Custom Drawing
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> AlbumDrawing()
                1 -> EnvelopeDrawing()
                2 -> ClockDrawing()
            }
        }

        Text(
            text = title,
            fontFamily = CairoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MawaaiColors.RoseGold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            fontFamily = CairoFamily,
            fontSize = 18.sp,
            color = MawaaiColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

@Composable
fun AlbumDrawing() {
    val infiniteTransition = rememberInfiniteTransition(label = "album")
    val heartY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "heart"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2

        // Album Book
        val bookWidth = 120.dp.toPx()
        val bookHeight = 150.dp.toPx()
        drawRect(
            color = MawaaiColors.CardDark,
            topLeft = Offset(cx - bookWidth / 2, cy - bookHeight / 2),
            size = Size(bookWidth, bookHeight)
        )
        drawRect(
            color = MawaaiColors.RoseGold,
            topLeft = Offset(cx - bookWidth / 2, cy - bookHeight / 2),
            size = Size(bookWidth, bookHeight),
            style = Stroke(width = 4.dp.toPx())
        )

        // Photo Frame inside
        drawRect(
            color = MawaaiColors.SurfaceDark,
            topLeft = Offset(cx - bookWidth * 0.35f, cy - bookHeight * 0.35f),
            size = Size(bookWidth * 0.7f, bookHeight * 0.5f)
        )

        // Floating Hearts
        repeat(3) { i ->
            val ox = (i - 1) * 40.dp.toPx()
            val oy = -80.dp.toPx() + (i * 10.dp.toPx()) + heartY.dp.toPx()
            val s = 15.dp.toPx()
            val path = Path().apply {
                moveTo(cx + ox, cy + oy - s * 0.3f)
                cubicTo(cx + ox - s * 0.5f, cy + oy - s * 0.8f, cx + ox - s, cy + oy - s * 0.3f, cx + ox, cy + oy + s * 0.4f)
                cubicTo(cx + ox + s, cy + oy - s * 0.3f, cx + ox + s * 0.5f, cy + oy - s * 0.8f, cx + ox, cy + oy - s * 0.3f)
                close()
            }
            drawPath(path = path, color = MawaaiColors.SoftRose, alpha = 0.6f)
        }
    }
}

@Composable
fun EnvelopeDrawing() {
    val infiniteTransition = rememberInfiniteTransition(label = "envelope")
    val flapAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
        label = "flap"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val envW = 140.dp.toPx()
        val envH = 100.dp.toPx()

        // Envelope Body
        drawRect(
            color = MawaaiColors.CardDark,
            topLeft = Offset(cx - envW / 2, cy - envH / 2),
            size = Size(envW, envH)
        )
        
        // Envelope lines
        val path = Path().apply {
            moveTo(cx - envW / 2, cy - envH / 2)
            lineTo(cx, cy)
            lineTo(cx + envW / 2, cy - envH / 2)
        }
        drawPath(path = path, color = MawaaiColors.RoseGold, style = Stroke(width = 2.dp.toPx()))

        // Envelope border
        drawRect(
            color = MawaaiColors.RoseGold,
            topLeft = Offset(cx - envW / 2, cy - envH / 2),
            size = Size(envW, envH),
            style = Stroke(width = 2.dp.toPx())
        )

        // Animated Flap
        rotate(flapAngle, pivot = Offset(cx, cy - envH / 2)) {
            val flapPath = Path().apply {
                moveTo(cx - envW / 2, cy - envH / 2)
                lineTo(cx, cy - envH * 0.1f)
                lineTo(cx + envW / 2, cy - envH / 2)
            }
            drawPath(path = flapPath, color = MawaaiColors.RoseGold, style = Stroke(width = 2.dp.toPx()))
        }

        // Heart Seal
        val s = 10.dp.toPx()
        val sealPath = Path().apply {
            moveTo(cx, cy - s * 0.3f)
            cubicTo(cx - s * 0.5f, cy - s * 0.8f, cx - s, cy - s * 0.3f, cx, cy + s * 0.4f)
            cubicTo(cx + s, cy - s * 0.3f, cx + s * 0.5f, cy - s * 0.8f, cx, cy - s * 0.3f)
            close()
        }
        drawPath(path = sealPath, color = MawaaiColors.ChampagneGold)
    }
}

@Composable
fun ClockDrawing() {
    val infiniteTransition = rememberInfiniteTransition(label = "clock")
    val handRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing)),
        label = "hand"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val radius = 70.dp.toPx()

        // Clock Face
        drawCircle(
            color = MawaaiColors.CardDark,
            radius = radius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = MawaaiColors.RoseGold,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 4.dp.toPx())
        )

        // Hour marks
        repeat(12) { i ->
            rotate(i * 30f, pivot = Offset(cx, cy)) {
                drawLine(
                    color = MawaaiColors.ChampagneGold,
                    start = Offset(cx, cy - radius + 10.dp.toPx()),
                    end = Offset(cx, cy - radius + 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Hour Hand
        rotate(handRotation / 12f, pivot = Offset(cx, cy)) {
            drawLine(
                color = MawaaiColors.RoseGold,
                start = Offset(cx, cy),
                end = Offset(cx, cy - radius * 0.5f),
                strokeWidth = 4.dp.toPx()
            )
        }

        // Minute Hand
        rotate(handRotation, pivot = Offset(cx, cy)) {
            drawLine(
                color = MawaaiColors.ChampagneGold,
                start = Offset(cx, cy),
                end = Offset(cx, cy - radius * 0.8f),
                strokeWidth = 2.dp.toPx()
            )
        }
        
        // Center Dot
        drawCircle(color = MawaaiColors.ChampagneGold, radius = 4.dp.toPx(), center = Offset(cx, cy))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1209)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(onFinish = {})
}
