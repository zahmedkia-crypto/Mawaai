package com.mawaai.love.app.core.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View

object HapticUtils {
    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // App's minSdk is 26 (Build.VERSION_CODES.O), so the legacy
    // pre-O `vibrator.vibrate(Long)` / `(LongArray, Int)` branches are
    // dead — Phase 6 ObsoleteSdkInt cleanup. The S+ branch in
    // [getVibrator] is preserved because VibratorManager only exists on
    // API 31+.

    fun heartbeat(context: Context) {
        val vibrator = getVibrator(context)
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun success(context: Context) {
        val vibrator = getVibrator(context)
        val pattern = longArrayOf(0, 50, 100, 50)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun error(context: Context) {
        val vibrator = getVibrator(context)
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun click(view: View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
