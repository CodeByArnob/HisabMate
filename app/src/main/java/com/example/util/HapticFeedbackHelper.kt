package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralized haptic feedback manager for HisabMate.
 * Respects both user preferences ([hapticsEnabled]) and system haptics availability.
 */
object HisabHaptics {

    @Volatile
    var isHapticsEnabled: Boolean = true

    fun performClick(view: View?, enabled: Boolean = isHapticsEnabled) {
        if (!enabled || view == null) return
        runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun performSelection(view: View?, enabled: Boolean = isHapticsEnabled) {
        if (!enabled || view == null) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    fun performSuccess(context: Context?, view: View? = null, enabled: Boolean = isHapticsEnabled) {
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                return
            }
            vibratePattern(context, longArrayOf(0, 18, 45, 28), intArrayOf(0, 120, 0, 180))
        }
    }

    fun performWarning(context: Context?, view: View? = null, enabled: Boolean = isHapticsEnabled) {
        if (!enabled) return
        runCatching {
            vibratePattern(context, longArrayOf(0, 30, 50, 30), intArrayOf(0, 150, 0, 150))
        }
    }

    fun performError(context: Context?, view: View? = null, enabled: Boolean = isHapticsEnabled) {
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                return
            }
            vibratePattern(context, longArrayOf(0, 40, 40, 55), intArrayOf(0, 200, 0, 220))
        }
    }

    private fun vibratePattern(context: Context?, timings: LongArray, amplitudes: IntArray) {
        if (context == null) return
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35L)
            }
        }
    }
}
