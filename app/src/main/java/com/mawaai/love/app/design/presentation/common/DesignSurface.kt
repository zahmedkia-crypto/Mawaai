package com.mawaai.love.app.design.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Transparent full-screen container for the design feature. The activity-level
 * [com.mawaai.love.app.core.theme.ThemedBackground] paints the morning/night
 * photo + readability scrim behind this surface, so the design hub inherits
 * the same warm backdrop as the rest of the app.
 */
@Composable
fun DesignSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
    }
}
