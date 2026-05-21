package com.mawaai.love.app.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks when the app process moves to background, so callers can ask
 * whether a returning foreground event has crossed the [REPLAY_INTRO_AFTER_MS]
 * threshold and the intro video should be replayed.
 *
 * Wired against [androidx.lifecycle.ProcessLifecycleOwner] in
 * `MawaaiApp.onCreate` so the timer counts true process-level
 * background time, not Activity rotation / configuration changes.
 */
@Singleton
class ForegroundResumeTracker @Inject constructor() : DefaultLifecycleObserver {

    @Volatile private var lastBackgroundElapsed: Long = -1L

    override fun onStop(owner: LifecycleOwner) {
        lastBackgroundElapsed = elapsedNow()
    }

    /**
     * Returns true exactly once per background → foreground transition when
     * the gap exceeded [REPLAY_INTRO_AFTER_MS]. Consumes the timestamp so
     * the next call returns false until the next background event.
     */
    fun consumeShouldReplayIntro(): Boolean {
        val last = lastBackgroundElapsed
        if (last < 0L) return false
        lastBackgroundElapsed = -1L
        return (elapsedNow() - last) >= REPLAY_INTRO_AFTER_MS
    }

    private fun elapsedNow(): Long = android.os.SystemClock.elapsedRealtime()

    companion object {
        const val REPLAY_INTRO_AFTER_MS = 30_000L
    }
}
