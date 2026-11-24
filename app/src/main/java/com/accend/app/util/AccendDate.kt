package com.accend.app.util

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Pure local calendar date (API 24+ safe without java.time desugaring).
 * Always represents a local calendar date in ISO-8601 format yyyy-MM-dd.
 */
data class AccendDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<AccendDate> {

    fun toIsoString(): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    /** 1 = Sunday, 2 = Monday, ..., 7 = Saturday (matches PRD §8.1) */
    val weekdaySundayOne: Int
        get() = toCalendar().get(Calendar.DAY_OF_WEEK)

    val weekdayLabel: String
        get() = when (weekdaySundayOne) {
            1 -> "Sunday"
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            else -> "Saturday"
        }

    val weekdayShort: String
        get() = when (weekdaySundayOne) {
            1 -> "SUN"
            2 -> "MON"
            3 -> "TUE"
            4 -> "WED"
            5 -> "THU"
            6 -> "FRI"
            else -> "SAT"
        }

    val focusTitle: String
        get() = when (weekdaySundayOne) {
            1, 4 -> "UPPER BODY"
            2, 5 -> "CORE & GRIP"
            3, 6 -> "LEG DAY"
            else -> "ACTIVE RECOVERY"
        }

    fun plusDays(days: Int): AccendDate {
        val cal = toCalendar()
        cal.add(Calendar.DAY_OF_MONTH, days)
        return fromCalendar(cal)
    }

    fun minusDays(days: Int): AccendDate = plusDays(-days)

    /** Monday of the ISO week containing this date. */
    fun startOfIsoWeek(): AccendDate {
        val offset = when (weekdaySundayOne) {
            1 -> 6 // Sunday -> 6 days after Monday
            else -> weekdaySundayOne - 2 // Mon=0 .. Sat=5
        }
        return minusDays(offset)
    }

    fun endOfIsoWeek(): AccendDate = startOfIsoWeek().plusDays(6)

    fun yearMonthKey(): String = String.format(Locale.US, "%04d-%02d", year, month)

    fun formatShort(): String {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val m = months[(month - 1).coerceIn(0, 11)]
        return "$day $m"
    }

    fun formatFull(): String = "${formatShort()} $year"

    fun toEpochDay(): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, day, 12, 0, 0)
        return (cal.timeInMillis / 86_400_000.0).roundToLong()
    }

    private fun toCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, day, 12, 0, 0)
        return cal
    }

    override fun compareTo(other: AccendDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        fun today(): AccendDate = fromCalendar(Calendar.getInstance())

        fun fromCalendar(cal: Calendar): AccendDate = AccendDate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH)
        )

        fun parse(iso: String): AccendDate {
            val parts = iso.trim().split("-")
            if (parts.size != 3) return today()
            return AccendDate(
                year = parts[0].toIntOrNull() ?: today().year,
                month = (parts[1].toIntOrNull() ?: 1).coerceIn(1, 12),
                day = (parts[2].toIntOrNull() ?: 1).coerceIn(1, 31)
            )
        }

        fun parseOrNull(iso: String?): AccendDate? {
            if (iso.isNullOrBlank()) return null
            val parts = iso.trim().split("-")
            if (parts.size != 3) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (m !in 1..12 || d !in 1..31) return null
            return AccendDate(y, m, d)
        }

        fun daysBetween(start: AccendDate, end: AccendDate): Int {
            return (end.toEpochDay() - start.toEpochDay()).toInt()
        }

        fun dateRange(start: AccendDate, end: AccendDate): List<AccendDate> {
            if (start > end) return emptyList()
            val count = daysBetween(start, end)
            return (0..count).map { start.plusDays(it) }
        }
    }
}
