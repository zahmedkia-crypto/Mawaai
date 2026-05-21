@file:OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class)

package com.mawaai.love.app.core.responsive

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal that exposes the active [WindowSizeClass] to every
 * Composable in the tree. The activity wires the value once via
 * `calculateWindowSizeClass(this)` and provides it from
 * [com.mawaai.love.app.MainActivity.renderApp]; downstream screens
 * read it without having to plumb it through every navigation
 * argument.
 *
 * Default = `(WidthSizeClass.Compact, HeightSizeClass.Medium)` —
 * matches a normal portrait phone, so previews and any composable
 * that reads this without a provider above it still gets sane
 * behaviour. Production usage always has a provider.
 */
val LocalWindowSizeClass = compositionLocalOf { DEFAULT_WINDOW_SIZE_CLASS }

/**
 * Convenience getter for the width axis. The vast majority of layout
 * decisions (single-column vs. two-column, full-width nav vs. rail)
 * pivot on width alone — wrap `LocalWindowSizeClass.current.widthSizeClass`
 * so call sites stay tight.
 */
val isCompactWidth: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact

/**
 * True when the window is wide enough for a two-column or
 * preview-beside-controls layout. Matches Material's "Medium" /
 * "Expanded" buckets — phones in landscape, tablets, foldables
 * unfolded.
 */
val isWideWidth: Boolean
    @Composable
    @ReadOnlyComposable
    get() {
        val w = LocalWindowSizeClass.current.widthSizeClass
        return w == WindowWidthSizeClass.Medium || w == WindowWidthSizeClass.Expanded
    }

private val DEFAULT_WINDOW_SIZE_CLASS: WindowSizeClass =
    WindowSizeClass.calculateFromSize(DpSize(360.dp, 800.dp))

// Suppress unused-import lint for WindowHeightSizeClass: the height
// axis is exposed through `LocalWindowSizeClass.current` and consumed
// directly by call sites that care (currently none).
@Suppress("unused")
private val UNUSED_HEIGHT_HINT: WindowHeightSizeClass = WindowHeightSizeClass.Medium
