package com.mawaai.love.app.ui.intro

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.OptIn as AndroidxOptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mawaai.love.app.core.components.HeartButton
import androidx.compose.ui.tooling.preview.Preview

/**
 * Plays the bundled intro video every app launch. When the video ends (or
 * the user taps "تخطي") the screen reports back the first-launch flag so the
 * caller can route to Onboarding (first time) or Home (every other time).
 *
 * Video sizing uses [AspectRatioFrameLayout.RESIZE_MODE_FIT] so the entire
 * frame is always visible — letterboxed against the deep black background on
 * displays whose aspect ratio doesn't match the source. The skip button
 * respects the system's safe-drawing insets so it never collides with status
 * bar or display cutout regardless of device.
 */
@AndroidxOptIn(UnstableApi::class)
@Composable
fun IntroScreen(
    onFinish: (isFirstLaunch: Boolean) -> Unit,
    viewModel: IntroViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsStateWithLifecycle()
    val firstLaunchValue = isFirstLaunch
    val finish by rememberUpdatedState(newValue = onFinish)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("file:///android_asset/templates/into.mp4"))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) finish(firstLaunchValue ?: true)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    finish(firstLaunchValue ?: true)
                }
            })
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val isWide = maxWidth > maxHeight

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    // FIT keeps the entire video visible on every screen size; on
                    // landscape devices we switch to FILL since the intro is
                    // authored portrait-ish.
                    resizeMode = if (isWide) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = if (isWide) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            HeartButton(
                text = "تخطي",
                onClick = { finish(firstLaunchValue ?: true) },
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun IntroScreenPreview() {
    IntroScreen(onFinish = {})
}
