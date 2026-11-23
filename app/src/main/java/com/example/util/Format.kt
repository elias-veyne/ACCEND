package com.example.util

import com.example.data.db.TargetUnit
import java.text.NumberFormat
import java.util.Locale

object Format {
    private val intFormat: NumberFormat
        get() = NumberFormat.getIntegerInstance(Locale.US)

    fun npr(amount: Long, showPlus: Boolean = false): String {
        val absVal = kotlin.math.abs(amount)
        val formatted = intFormat.format(absVal)
        return when {
            amount < 0 -> "-NPR $formatted"
            showPlus && amount > 0 -> "+NPR $formatted"
            else -> "NPR $formatted"
        }
    }

    fun number(value: Long): String = intFormat.format(value)

    fun number(value: Int): String = intFormat.format(value)

    fun decimal1(value: Double): String = String.format(Locale.US, "%.1f", value)

    fun screenTime(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val h = safe / 60
        val m = safe % 60
        return "${h}h ${m}m"
    }

    fun screenTimeTactical(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val h = safe / 60
        val m = safe % 60
        return String.format(Locale.US, "%02dH : %02dM", h, m)
    }

    fun timerMmSs(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val m = safe / 60
        val s = safe % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun exerciseTarget(sets: Int, target: Int, unit: TargetUnit): String {
        return when (unit) {
            TargetUnit.NONE -> ""
            TargetUnit.REPS -> if (sets > 1) "${sets}×$target reps" else "$target reps"
            TargetUnit.SECONDS -> if (sets > 1) "${sets}×$target s" else "$target s"
            TargetUnit.METERS -> if (sets > 1) "${sets}×$target m" else "$target m"
            TargetUnit.MINUTES -> if (sets > 1) "${sets}×$target min" else "$target min"
        }
    }

    fun cmToFeetInches(cm: Int): String {
        val totalInches = (cm / 2.54).toInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return "$feet'$inches\""
    }

    fun relativeDateLabel(date: AccendDate, today: AccendDate): String {
        return when (AccendDate.daysBetween(date, today)) {
            0 -> "TODAY"
            1 -> "YESTERDAY"
            else -> date.formatFull().uppercase(Locale.US)
        }
    }
}
