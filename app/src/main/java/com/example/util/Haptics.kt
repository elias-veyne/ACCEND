package com.example.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

enum class HapticLevel {
    LOW, MEDIUM, HIGH
}

object Haptics {
    fun tick(view: View, level: HapticLevel = HapticLevel.HIGH) {
        val constant = when (level) {
            HapticLevel.LOW -> HapticFeedbackConstants.CLOCK_TICK
            HapticLevel.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
            HapticLevel.HIGH -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
        }
        view.performHapticFeedback(constant)
    }

    fun heavy(view: View, level: HapticLevel = HapticLevel.HIGH) {
        val constant = when (level) {
            HapticLevel.LOW -> HapticFeedbackConstants.VIRTUAL_KEY
            HapticLevel.MEDIUM, HapticLevel.HIGH -> HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}
