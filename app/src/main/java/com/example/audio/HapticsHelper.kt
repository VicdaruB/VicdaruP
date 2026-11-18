package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticsHelper(context: Context) {

    companion object {
        val MORNING_NOTIFICATION_TIMINGS = longArrayOf(0L, 30L, 70L, 48L)
        val MORNING_NOTIFICATION_AMPLITUDES = intArrayOf(0, 75, 0, 130)

        val DAYTIME_NOTIFICATION_TIMINGS = longArrayOf(0L, 18L, 42L, 18L, 42L, 30L)
        val DAYTIME_NOTIFICATION_AMPLITUDES = intArrayOf(0, 70, 0, 85, 0, 130)
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // --- Climber Minigame ---

    fun subtleBounce() {
        vibrateOneShot(durationMs = 16L, amplitude = 48)
    }

    fun climberSteer() {
        vibrateOneShot(durationMs = 10L, amplitude = 34)
    }

    fun speedBoost() {
        vibrateWaveform(longArrayOf(0L, 22L, 28L, 34L), intArrayOf(0, 95, 0, 150))
    }

    fun trapFreeze() {
        vibrateOneShot(durationMs = 65L, amplitude = 140)
    }

    fun stunEffect() {
        vibrateWaveform(longArrayOf(0L, 20L, 25L, 20L, 25L, 24L), intArrayOf(0, 110, 0, 130, 0, 90))
    }

    fun climberGameOver() {
        vibrateWaveform(longArrayOf(0L, 28L, 45L, 55L), intArrayOf(0, 115, 0, 65))
    }

    fun climberUnlock() {
        vibrateWaveform(longArrayOf(0L, 18L, 28L, 22L, 28L, 42L), intArrayOf(0, 75, 0, 115, 0, 185))
    }

    // --- Navigation & General UI ---

    fun navTabSwitch() {
        vibrateOneShot(durationMs = 12L, amplitude = 44)
    }

    fun subtleTap() {
        vibrateOneShot(durationMs = 10L, amplitude = 36)
    }

    fun toggleSwitch(enabled: Boolean) {
        if (enabled) {
            vibrateWaveform(longArrayOf(0L, 12L, 24L, 20L), intArrayOf(0, 50, 0, 95))
        } else {
            vibrateOneShot(durationMs = 14L, amplitude = 38)
        }
    }

    // --- Habits ---

    fun habitComplete() {
        vibrateWaveform(longArrayOf(0L, 18L, 34L, 34L), intArrayOf(0, 75, 0, 150))
    }

    fun habitUndo() {
        vibrateOneShot(durationMs = 14L, amplitude = 36)
    }

    fun habitCreated() {
        vibrateWaveform(longArrayOf(0L, 15L, 25L, 24L), intArrayOf(0, 65, 0, 120))
    }

    // --- Quotes, Archive & Sharing ---

    fun quoteDelivered() {
        vibrateWaveform(longArrayOf(0L, 16L, 28L, 22L, 28L, 36L), intArrayOf(0, 60, 0, 95, 0, 145))
    }

    fun favoriteToggle(favorited: Boolean) {
        if (favorited) {
            vibrateWaveform(longArrayOf(0L, 16L, 42L, 26L), intArrayOf(0, 75, 0, 125))
        } else {
            vibrateOneShot(durationMs = 12L, amplitude = 34)
        }
    }

    fun shareOrCopy() {
        vibrateOneShot(durationMs = 18L, amplitude = 88)
    }

    fun downloadStart() {
        vibrateWaveform(longArrayOf(0L, 20L, 25L, 32L), intArrayOf(0, 100, 0, 60))
    }

    // --- Notifications ---

    fun notificationMorning() {
        vibrateWaveform(MORNING_NOTIFICATION_TIMINGS, MORNING_NOTIFICATION_AMPLITUDES)
    }

    fun notificationDaytime() {
        vibrateWaveform(DAYTIME_NOTIFICATION_TIMINGS, DAYTIME_NOTIFICATION_AMPLITUDES)
    }

    // --- Music Focus & Live Chat ---

    fun focusTimerToggle(started: Boolean) {
        if (started) {
            vibrateWaveform(longArrayOf(0L, 24L, 30L, 26L), intArrayOf(0, 90, 0, 115))
        } else {
            vibrateOneShot(durationMs = 16L, amplitude = 44)
        }
    }

    fun focusPhaseTransition() {
        vibrateWaveform(longArrayOf(0L, 22L, 48L, 28L, 48L, 40L), intArrayOf(0, 80, 0, 110, 0, 155))
    }

    fun chatMessageSent() {
        vibrateOneShot(durationMs = 14L, amplitude = 72)
    }

    fun chatMessageReceived() {
        vibrateOneShot(durationMs = 12L, amplitude = 46)
    }

    fun vibeLevelUp() {
        vibrateWaveform(longArrayOf(0L, 18L, 35L, 30L, 35L, 45L), intArrayOf(0, 80, 0, 120, 0, 175))
    }

    // --- Social, Multiplayer & Alerts ---

    fun socialAction() {
        vibrateWaveform(longArrayOf(0L, 18L, 30L, 28L), intArrayOf(0, 65, 0, 115))
    }

    fun softDelete() {
        vibrateWaveform(longArrayOf(0L, 22L, 35L, 30L), intArrayOf(0, 110, 0, 85))
    }

    fun errorNotice() {
        vibrateWaveform(longArrayOf(0L, 28L, 32L, 32L), intArrayOf(0, 135, 0, 115))
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val safeAmp = if (vib.hasAmplitudeControl()) amplitude.coerceIn(1, 255)
                else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(durationMs, safeAmp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vib.hasAmplitudeControl()) {
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vib.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        } catch (_: Exception) {
        }
    }
}
