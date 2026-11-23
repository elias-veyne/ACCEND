package com.example.domain

import com.example.AppConfig
import com.example.data.db.AccendDatabase
import com.example.data.db.DayLogEntity
import com.example.data.db.DeepWorkLogEntity
import com.example.data.db.ExerciseEntity
import com.example.data.db.ExerciseLogEntity
import com.example.data.db.MeditationLogEntity
import com.example.data.db.MuscleGroup
import com.example.data.db.NutritionLogEntity
import com.example.data.db.ProgressionLevel
import com.example.data.db.ReadingLogEntity
import com.example.data.db.ScreenTimeLogEntity
import com.example.data.db.TargetUnit
import com.example.data.prefs.AccendPrefs
import com.example.data.prefs.PrefsRepository
import com.example.util.AccendDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MeditationLadder {
    private val minutesByWeek = intArrayOf(1, 2, 3, 5, 7, 10, 12, 15, 17, 20)

    fun weekNumber(dayNumber: Int): Int = ((dayNumber - 1).coerceAtLeast(0) / 7) + 1

    fun targetMinutesForWeek(week: Int): Int {
        val idx = (week - 1).coerceIn(0, minutesByWeek.lastIndex)
        return minutesByWeek[idx]
    }

    fun targetMinutesForDay(dayNumber: Int): Int = targetMinutesForWeek(weekNumber(dayNumber))
}

object VolumeCalculator {
    fun unitWeight(unit: TargetUnit): Double = when (unit) {
        TargetUnit.REPS -> 1.0
        TargetUnit.SECONDS -> 0.5
        TargetUnit.METERS -> 0.1
        TargetUnit.MINUTES -> 3.0
        TargetUnit.NONE -> 0.0
    }

    fun computeVolume(sets: Int, targetReps: Int, unit: TargetUnit): Double {
        return sets * targetReps * unitWeight(unit)
    }

    val radarGroups: List<MuscleGroup> = listOf(
        MuscleGroup.CHEST_PUSH,
        MuscleGroup.BACK_PULL,
        MuscleGroup.BICEPS,
        MuscleGroup.TRICEPS,
        MuscleGroup.SHOULDERS,
        MuscleGroup.CORE,
        MuscleGroup.LEGS,
        MuscleGroup.NECK,
        MuscleGroup.FOREARMS
    )
}

data class DayCompletionBreakdown(
    val physicalDone: Int,
    val physicalTotal: Int,
    val nutritionDone: Int,
    val meditationDone: Boolean,
    val screenTimeLogged: Boolean,
    val deepWorkDone: Boolean,
    val readingDone: Boolean
) {
    val mindDone: Int
        get() = listOf(meditationDone, screenTimeLogged, deepWorkDone, readingDone).count { it }

    val totalDone: Int
        get() = physicalDone + nutritionDone + mindDone

    val totalItems: Int
        get() = physicalTotal + 5 + 4

    val pendingCount: Int
        get() = (totalItems - totalDone).coerceAtLeast(0)

    val percent: Int
        get() = if (totalItems <= 0) 0 else ((100.0 * totalDone) / totalItems).roundToInt().coerceIn(0, 100)
}

object CompletionCalculator {
    fun compute(
        exercisesForDay: List<ExerciseEntity>,
        exerciseLogs: List<ExerciseLogEntity>,
        nutrition: NutritionLogEntity?,
        meditations: List<MeditationLogEntity>,
        screenTime: ScreenTimeLogEntity?,
        deepWorks: List<DeepWorkLogEntity>,
        reading: ReadingLogEntity?,
        readingTarget: Int
    ): DayCompletionBreakdown {
        val scheduledIds = exercisesForDay.map { it.id }.toSet()
        val physDone = exerciseLogs.count { it.done && it.exerciseId in scheduledIds }
        val nutrDone = listOfNotNull(
            nutrition?.protein,
            nutrition?.heavyMeals,
            nutrition?.breakfast,
            nutrition?.hydration,
            nutrition?.sleep9h
        ).count { it }
        val medDone = meditations.isNotEmpty()
        val screenDone = screenTime != null
        val deepDone = deepWorks.any { it.completed }
        val readDone = (reading?.pages ?: 0) >= readingTarget.coerceAtLeast(1)
        return DayCompletionBreakdown(
            physicalDone = physDone,
            physicalTotal = exercisesForDay.size,
            nutritionDone = nutrDone,
            meditationDone = medDone,
            screenTimeLogged = screenDone,
            deepWorkDone = deepDone,
            readingDone = readDone
        )
    }
}

data class StreakResult(
    val current: Int,
    val best: Int
)

object StreakCalculator {
    fun calculate(
        joinedDate: AccendDate,
        today: AccendDate,
        doneDates: Set<String>,
        graceDays: Int = AppConfig.STREAK_GRACE_DAYS
    ): StreakResult {
        if (today < joinedDate) return StreakResult(0, 0)

        var current = if (today.toIsoString() in doneDates) 1 else 0
        var missesInRow = 0
        var cursor = today.minusDays(1)
        while (cursor >= joinedDate) {
            if (cursor.toIsoString() in doneDates) {
                current++
                missesInRow = 0
            } else {
                missesInRow++
                if (missesInRow > graceDays) break
            }
            cursor = cursor.minusDays(1)
        }

        // Best streak across full history
        var best = 0
        var run = 0
        var runMisses = 0
        val allDates = AccendDate.dateRange(joinedDate, today)
        for (d in allDates) {
            if (d.toIsoString() in doneDates) {
                run++
                runMisses = 0
                if (run > best) best = run
            } else if (d < today) {
                runMisses++
                if (runMisses > graceDays) {
                    run = 0
                    runMisses = 0
                }
            }
        }
        return StreakResult(current = current, best = max(best, current))
    }
}

object MindScoreCalculator {
    fun dailyRawScore(
        date: AccendDate,
        joinedDate: AccendDate,
        meditationMinutes: Int,
        completedDeepMinutes: Int,
        readingPages: Int,
        screenMinutes: Int?,
        prefs: AccendPrefs
    ): Double {
        val dayNum = AccendDate.daysBetween(joinedDate, date) + 1
        val medTarget = MeditationLadder.targetMinutesForDay(dayNum).coerceAtLeast(1).toDouble()
        val med = min(1.0, meditationMinutes / medTarget)

        val deepTarget = prefs.deepWorkDefaultMinutes.coerceAtLeast(1).toDouble()
        val deep = min(1.0, completedDeepMinutes / deepTarget)

        val readTarget = prefs.readingTarget.coerceAtLeast(1).toDouble()
        val read = min(1.0, readingPages / readTarget)

        val screenTarget = prefs.screenTargetMinutes.coerceAtLeast(1).toDouble()
        val screen = when {
            screenMinutes == null -> 0.0
            screenMinutes <= screenTarget -> 1.0
            else -> max(0.0, 1.0 - (screenMinutes - screenTarget) / screenTarget)
        }

        return 100.0 * (0.30 * med + 0.30 * deep + 0.20 * read + 0.20 * screen)
    }

    fun rollingMindScore(
        targetDate: AccendDate,
        joinedDate: AccendDate,
        meditationByDate: Map<String, Int>,
        deepWorkCompletedByDate: Map<String, Int>,
        readingByDate: Map<String, Int>,
        screenByDate: Map<String, Int>,
        prefs: AccendPrefs
    ): Int {
        if (targetDate < joinedDate) return 0
        val windowStart = targetDate.minusDays(AppConfig.MIND_WINDOW_DAYS - 1)
        val effectiveStart = if (windowStart < joinedDate) joinedDate else windowStart
        val dates = AccendDate.dateRange(effectiveStart, targetDate)
        if (dates.isEmpty()) return 0
        val avg = dates.map { d ->
            val iso = d.toIsoString()
            dailyRawScore(
                date = d,
                joinedDate = joinedDate,
                meditationMinutes = meditationByDate[iso] ?: 0,
                completedDeepMinutes = deepWorkCompletedByDate[iso] ?: 0,
                readingPages = readingByDate[iso] ?: 0,
                screenMinutes = screenByDate[iso],
                prefs = prefs
            )
        }.average()
        return avg.roundToInt().coerceIn(0, 100)
    }
}

object ProgressionRules {
    fun volumeStep(startTarget: Int, unit: TargetUnit): Int = when (unit) {
        TargetUnit.REPS -> if (startTarget >= 20) 5 else 2
        TargetUnit.SECONDS -> 5
        TargetUnit.METERS -> 20
        TargetUnit.MINUTES -> 1
        TargetUnit.NONE -> 0
    }

    fun canLevelUp(exercise: ExerciseEntity, levels: List<ProgressionLevel>): Boolean {
        if (exercise.unit == TargetUnit.NONE) return false
        return if (levels.size > 1) {
            exercise.currentLevelIndex < levels.lastIndex
        } else {
            val start = levels.firstOrNull()?.target ?: exercise.targetReps
            val cap = start * 2
            val step = volumeStep(start, exercise.unit)
            step > 0 && exercise.targetReps + step <= cap
        }
    }

    fun canLevelDown(exercise: ExerciseEntity, levels: List<ProgressionLevel>): Boolean {
        if (exercise.unit == TargetUnit.NONE) return false
        return if (levels.size > 1) {
            exercise.currentLevelIndex > 0
        } else {
            val start = levels.firstOrNull()?.target ?: exercise.targetReps
            val step = volumeStep(start, exercise.unit)
            step > 0 && exercise.targetReps - step >= start
        }
    }

    fun nextLevelPreview(exercise: ExerciseEntity, levels: List<ProgressionLevel>): ProgressionLevel? {
        if (!canLevelUp(exercise, levels)) return null
        return if (levels.size > 1) {
            levels.getOrNull(exercise.currentLevelIndex + 1)
        } else {
            val start = levels.firstOrNull()?.target ?: exercise.targetReps
            val step = volumeStep(start, exercise.unit)
            ProgressionLevel(exercise.name, exercise.sets, exercise.targetReps + step)
        }
    }

    fun previousLevelPreview(exercise: ExerciseEntity, levels: List<ProgressionLevel>): ProgressionLevel? {
        if (!canLevelDown(exercise, levels)) return null
        return if (levels.size > 1) {
            levels.getOrNull(exercise.currentLevelIndex - 1)
        } else {
            val start = levels.firstOrNull()?.target ?: exercise.targetReps
            val step = volumeStep(start, exercise.unit)
            ProgressionLevel(exercise.name, exercise.sets, (exercise.targetReps - step).coerceAtLeast(start))
        }
    }

    fun isReadyToLevelUp(
        exercise: ExerciseEntity,
        levels: List<ProgressionLevel>,
        doneLogs: List<ExerciseLogEntity>,
        joinedDate: AccendDate,
        today: AccendDate
    ): Boolean {
        if (!canLevelUp(exercise, levels)) return false
        val levelSince = AccendDate.parseOrNull(exercise.levelSince) ?: joinedDate
        val effectiveStart = if (levelSince > joinedDate) levelSince else joinedDate
        if (effectiveStart > today) return false

        val doneDates = doneLogs.filter { it.done }.map { it.date }.toSet()
        val snoozed = AccendDate.parseOrNull(exercise.suggestionSnoozedOn)

        // Check if snoozed and no newer done session exists after snooze
        if (snoozed != null) {
            val hasNewerDone = doneLogs.any { it.done && AccendDate.parse(it.date) > snoozed }
            if (!hasNewerDone) return false
        }

        // Gather scheduled occurrences for exercise.dayOfWeek
        val occurrences = AccendDate.dateRange(effectiveStart, today)
            .filter { it.weekdaySundayOne == exercise.dayOfWeek }
            .reversed()

        var consecutive = 0
        for (occ in occurrences) {
            val iso = occ.toIsoString()
            if (iso in doneDates) {
                consecutive++
            } else if (occ == today) {
                // Today not done yet is skipped
                continue
            } else {
                break
            }
        }
        return consecutive >= 3
    }
}

class DayEvaluator(
    private val db: AccendDatabase,
    private val prefsRepo: PrefsRepository
) {
    suspend fun recomputeDay(date: AccendDate, finalized: Boolean): DayLogEntity {
        val iso = date.toIsoString()
        val prefs = prefsRepo.getSnapshot()
        val exercises = db.exerciseDao().forDayOnce(date.weekdaySundayOne)
        val exLogs = db.exerciseLogDao().forDateOnce(iso)
        val nutrition = db.nutritionDao().forDateOnce(iso)
        val meditations = db.mindDao().meditationForDateOnce(iso)
        val screenTime = db.mindDao().screenTimeForDateOnce(iso)
        val deepWorks = db.mindDao().deepWorkForDateOnce(iso)
        val reading = db.mindDao().readingForDateOnce(iso)

        val breakdown = CompletionCalculator.compute(
            exercisesForDay = exercises,
            exerciseLogs = exLogs,
            nutrition = nutrition,
            meditations = meditations,
            screenTime = screenTime,
            deepWorks = deepWorks,
            reading = reading,
            readingTarget = prefs.readingTarget
        )

        val existing = db.dayLogDao().get(iso)
        val entity = DayLogEntity(
            id = existing?.id ?: 0,
            date = iso,
            completionPercent = breakdown.percent,
            missedFlag = finalized && breakdown.percent == 0,
            finalized = finalized
        )
        db.dayLogDao().upsert(entity)
        if (breakdown.percent > 0) {
            prefsRepo.updateEvaluatorState(miss2Armed = true)
        }
        return entity
    }

    suspend fun catchUp(today: AccendDate = AccendDate.today()) {
        val user = db.userDao().getUser() ?: return
        val joined = AccendDate.parse(user.joinedDate)
        if (today < joined) return

        val prefs = prefsRepo.getSnapshot()
        val lastEval = AccendDate.parseOrNull(prefs.lastEvaluatedDate)
        val yesterday = today.minusDays(1)

        // Guard against backward clock jumps
        if (lastEval != null && today < lastEval) {
            recomputeDay(today, finalized = false)
            return
        }

        val start = if (lastEval != null) lastEval.plusDays(1) else joined
        var anyActive = false
        if (start <= yesterday) {
            val range = AccendDate.dateRange(start, yesterday)
            for (d in range) {
                if (d >= joined) {
                    val dayLog = recomputeDay(d, finalized = true)
                    if (dayLog.completionPercent > 0) anyActive = true
                }
            }
            prefsRepo.updateEvaluatorState(
                lastEvaluatedDate = yesterday.toIsoString(),
                miss2Armed = if (anyActive) true else null
            )
        }
        recomputeDay(today, finalized = false)
    }
}
