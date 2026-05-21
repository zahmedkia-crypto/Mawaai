package com.mawaai.love.app.core.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Spring Animations
val HeartSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

// Glow Modifier — used by romantic surfaces to halo important elements
// (HeartButton, AddCountdown FAB, hero typography). Kept here even when
// no other Motion utilities ship.
//
// `ShimmerBrush` was removed in the Phase 6 audit cleanup (zero callers
// + ComposableNaming lint warning). Re-derive inline via
// `rememberInfiniteTransition` if a future loading state needs it.
fun Modifier.goldGlow(radius: Dp = 12.dp): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = MawaaiColors.ChampagneGold.toArgb()
        frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
            radius.toPx(),
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
        canvas.drawCircle(
            center = center,
            radius = size.minDimension / 2,
            paint = paint
        )
    }
}
