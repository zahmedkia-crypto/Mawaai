package com.mawaai.love.app.core.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mawaai.love.app.data.model.BackgroundTheme
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Resolves [BackgroundTheme.AUTO] against the device clock — morning runs
 * from 06:00 to 17:59 and night covers 18:00 to 05:59.
 */
fun BackgroundTheme.resolve(now: Long = System.currentTimeMillis()): BackgroundTheme {
    if (this != BackgroundTheme.AUTO) return this
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return if (hour in 6..17) BackgroundTheme.MORNING else BackgroundTheme.NIGHT
}

/**
 * Asset URI selector. The wide variant trades vertical safety for less
 * horizontal cropping on tablet / foldable displays; on portrait phones
 * we keep the original photo so the warm focal subject still anchors
 * the composition.
 *
 * Wide variants are optional — when missing, [ThemedBackground] falls
 * back to the portrait asset and Coil silently degrades. The runtime
 * picker uses the device aspect ratio rather than a width-class
 * threshold so foldable inner displays (~22:9 unfolded) get the
 * cleaner crop without us having to plumb [WindowSizeClass] through
 * every theme call site.
 */
private fun BackgroundTheme.assetUri(wide: Boolean): String = when {
    this == BackgroundTheme.NIGHT && wide -> "file:///android_asset/images/backgrounds/card_night_wide.jpg"
    this == BackgroundTheme.NIGHT -> "file:///android_asset/images/backgrounds/card_night.jpg"
    wide -> "file:///android_asset/images/backgrounds/card_morning_wide.jpg"
    else -> "file:///android_asset/images/backgrounds/card_morning.jpg"
}

/**
 * Full-screen image background driven by the user's [BackgroundTheme]
 * preference. A vertical dark scrim is drawn on top of the photograph so
 * text and translucent cards remain readable across the warm morning and
 * soft night artwork. The scrim is lighter at the top (where the status
 * bar / app bar tints already darken the surface) and stronger near the
 * bottom (where most body content sits).
 *
 * Auto-themed surfaces re-resolve once every 10 minutes so the morning/night
 * swap happens without restarting the app.
 *
 * Phase 18 (2026-05-17):
 *  - **Aspect-ratio variants**: tablets / foldables / very tall phones get
 *    a wide-cropped variant (`card_<theme>_wide.jpg`) when the screen
 *    aspect ratio is ≥ 1.85 (10:6) or ≤ 0.45 (≈ 21:9). The portrait
 *    variant stays for normal phones.
 *  - **Per-screen scrim**: callers can override [scrimAlphaTop] /
 *    [scrimAlphaBottom] when their content is dense enough to need
 *    higher contrast. Defaults match the pre-Phase-18 0.25 → 0.55
 *    gradient.
 */
@Composable
fun ThemedBackground(
    mode: BackgroundTheme,
    scrimAlphaTop: Float = DEFAULT_SCRIM_ALPHA_TOP,
    scrimAlphaBottom: Float = DEFAULT_SCRIM_ALPHA_BOTTOM,
    content: @Composable () -> Unit
) {
    // Long-state to avoid autoboxing the wall-clock timestamp each tick.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(mode) {
        while (mode == BackgroundTheme.AUTO) {
            delay(10 * 60_000L)
            now = System.currentTimeMillis()
        }
    }

    val resolved = mode.resolve(now)
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // Aspect ratio in (width / height) terms. Anything outside [0.45..1.85]
    // is "unusual" and gets the wide variant; the threshold is empirical —
    // wider than this the portrait crop loses key visual content.
    val aspect = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    val wideRequested = aspect >= WIDE_VARIANT_AR_HIGH || aspect <= WIDE_VARIANT_AR_LOW
    // Phase 18: only opt into the wide asset when the file actually ships
    // in `assets/images/backgrounds/`. The wide variants are authored
    // content and may be added incrementally; falling back to the portrait
    // photo (slightly cropped) is far better than rendering a flat
    // [MawaaiColors.DeepNight] color when the asset is missing.
    val wideAssets = remember { wideAssetsAvailable(context) }
    val useWideVariant = wideRequested && wideAssets
    Box(modifier = Modifier.fillMaxSize().background(MawaaiColors.DeepNight)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(resolved.assetUri(useWideVariant))
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = scrimAlphaTop),
                            Color.Black.copy(alpha = scrimAlphaBottom)
                        )
                    )
                )
        )
        content()
    }
}

private const val DEFAULT_SCRIM_ALPHA_TOP: Float = 0.25f
private const val DEFAULT_SCRIM_ALPHA_BOTTOM: Float = 0.55f

// Aspect ratio thresholds for swapping to the wide-cropped photo variant.
// Above [WIDE_VARIANT_AR_HIGH] the screen is wider than ~1.85:1
// (most tablets in landscape, foldable inner displays). Below
// [WIDE_VARIANT_AR_LOW] the screen is taller than ~1:2.2 (foldable
// portrait, ultra-narrow tall phones — care more about top/bottom
// preservation than left/right). Both buckets benefit from a photo
// authored with the wider crop region in mind.
private const val WIDE_VARIANT_AR_HIGH: Float = 1.85f
private const val WIDE_VARIANT_AR_LOW: Float = 0.45f

/**
 * Probes the asset bundle for the wide-aspect variants. Both variants
 * must exist for the runtime picker to opt into them — having only one
 * (e.g. shipping morning_wide but not night_wide) would produce an
 * inconsistent night experience on tablets.
 */
private fun wideAssetsAvailable(context: android.content.Context): Boolean {
    val list = runCatching {
        context.assets.list("images/backgrounds")?.toSet().orEmpty()
    }.getOrDefault(emptySet())
    return "card_morning_wide.jpg" in list && "card_night_wide.jpg" in list
}
