package com.accend.app.data.repo

import androidx.room.withTransaction
import com.accend.app.data.cloud.CloudRepository
import com.accend.app.data.db.AccendDatabase
import com.accend.app.data.db.DeepWorkLogEntity
import com.accend.app.data.db.ExerciseEntity
import com.accend.app.data.db.ExerciseLogEntity
import com.accend.app.data.db.ExerciseProgressionEntity
import com.accend.app.data.db.HeightLogEntity
import com.accend.app.data.db.MeditationLogEntity
import com.accend.app.data.db.MoneyTransactionEntity
import com.accend.app.data.db.NutritionLogEntity
import com.accend.app.data.db.ProgressionLevel
import com.accend.app.data.db.ReadingLogEntity
import com.accend.app.data.db.SavingsMoveEntity
import com.accend.app.data.db.ScreenTimeLogEntity
import com.accend.app.data.db.SkillLogEntity
import com.accend.app.data.db.UserEntity
import com.accend.app.data.db.WeightLogEntity
import com.accend.app.data.prefs.PrefsRepository
import com.accend.app.data.seed.SeedData
import com.accend.app.domain.DayEvaluator
import com.accend.app.domain.ProgressionRules
import com.accend.app.domain.VolumeCalculator
import com.accend.app.util.AccendDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import kotlin.math.max

class SeedRepository(private val db: AccendDatabase) {
    suspend fun seedIfEmpty(joinedDate: String = AccendDate.today().toIsoString()) {
        db.withTransaction {
            if (db.exerciseDao().count() > 0) return@withTransaction
            insertDefaultExercises(joinedDate)
        }
    }

    suspend fun insertDefaultExercises(joinedDate: String) {
        SeedData.weeklyProgram.forEach { spec ->
            val levels = SeedData.buildLevelsFor(spec)
            val firstLevel = levels.first()
            val exerciseId = db.exerciseDao().insertRaw(
                ExerciseEntity(
                    name = firstLevel.name,
                    muscleGroup = spec.muscleGroup,
                    currentLevelIndex = 0,
                    sets = firstLevel.sets,
                    targetReps = firstLevel.target,
                    unit = spec.unit,
                    orderInDay = spec.orderInDay,
                    dayOfWeek = spec.dayOfWeek,
                    levelSince = joinedDate,
                    suggestionSnoozedOn = null
                )
            )
            db.exerciseDao().upsertProgression(
                ExerciseProgressionEntity(
                    exerciseId = exerciseId,
                    levelsList = ProgressionLevel.toJson(levels)
                )
            )
        }
    }
}

class UserRepository(
    private val db: AccendDatabase,
    private val prefsRepo: PrefsRepository,
    private val seedRepo: SeedRepository
) {
    val userFlow: Flow<UserEntity?> = db.userDao().observeUser()

    suspend fun getUser(): UserEntity? = db.userDao().getUser()

    suspend fun completeOnboarding(name: String, pfpUri: String?) {
        val todayIso = AccendDate.today().toIsoString()
        db.withTransaction {
            db.userDao().upsert(
                UserEntity(
                    id = 1,
                    name = name.trim(),
                    pfpUri = pfpUri,
                    joinedDate = todayIso,
                    onboarded = true
                )
            )
            if (db.exerciseDao().count() == 0) {
                seedRepo.insertDefaultExercises(todayIso)
            }
        }
        prefsRepo.setOnboarded(true)
    }

    suspend fun updateProfile(name: String, pfpUri: String?) {
        val current = db.userDao().getUser() ?: return
        db.userDao().upsert(current.copy(name = name.trim(), pfpUri = pfpUri))
    }

    /** Restores the authoritative Firestore identity into the local DB after a reinstall. */
    suspend fun restoreCloudIdentity(name: String, pfpUri: String?, joinedDate: String?) {
        val iso = joinedDate?.takeIf { it.isNotBlank() } ?: AccendDate.today().toIsoString()
        val current = db.userDao().getUser()
        db.userDao().upsert(
            UserEntity(
                id = 1,
                name = name.trim().ifBlank { "Operator" },
                pfpUri = pfpUri,
                joinedDate = current?.joinedDate?.takeIf { it.isNotBlank() } ?: iso,
                onboarded = true
            )
        )
        if (db.exerciseDao().count() == 0) seedRepo.insertDefaultExercises(iso)
        prefsRepo.setOnboarded(true)
    }
}

class DisciplineRepository(
    private val db: AccendDatabase,
    private val evaluator: DayEvaluator,
    private val cloudRepo: CloudRepository
) {
    fun exercisesForDay(dow: Int) = db.exerciseDao().forDay(dow)
    fun allExercises() = db.exerciseDao().all()
    fun allProgressions() = db.exerciseDao().allProgressions()
    fun exerciseLogsForDate(date: String) = db.exerciseLogDao().forDate(date)
    fun allExerciseLogs() = db.exerciseLogDao().allFlow()
    fun nutritionForDate(date: String) = db.nutritionDao().forDate(date)
    fun meditationForDate(date: String) = db.mindDao().meditationForDate(date)
    fun allMeditations() = db.mindDao().allMeditationsFlow()
    fun screenTimeForDate(date: String) = db.mindDao().screenTimeForDate(date)
    fun allScreenTime() = db.mindDao().allScreenTimeFlow()
    fun deepWorkForDate(date: String) = db.mindDao().deepWorkForDate(date)
    fun allDeepWork() = db.mindDao().allDeepWorkFlow()
    fun readingForDate(date: String) = db.mindDao().readingForDate(date)
    fun allReading() = db.mindDao().allReadingFlow()
    fun dayLogsRange(from: String, to: String) = db.dayLogDao().range(from, to)
    fun allDayLogs() = db.dayLogDao().allFlow()

    suspend fun toggleExercise(date: AccendDate, exercise: ExerciseEntity) {
        val iso = date.toIsoString()
        val existing = db.exerciseLogDao().getByDateAndExercise(iso, exercise.id)
        val newDone = !(existing?.done ?: false)
        val volume = if (newDone) VolumeCalculator.computeVolume(exercise.sets, exercise.targetReps, exercise.unit) else 0.0
        db.exerciseLogDao().upsert(
            ExerciseLogEntity(
                id = existing?.id ?: 0,
                date = iso,
                exerciseId = exercise.id,
                done = newDone,
                levelNameSnapshot = exercise.name,
                volumeSnapshot = volume
            )
        )
        evaluator.recomputeDay(date, finalized = date < AccendDate.today())
        cloudRepo.pushDayIfSignedIn(iso)
        if (newDone) cloudRepo.awardExp(25, "ex-$iso-${exercise.id}")
    }

    suspend fun updateNutrition(date: AccendDate, transform: (NutritionLogEntity) -> NutritionLogEntity) {
        val iso = date.toIsoString()
        val current = db.nutritionDao().forDateOnce(iso) ?: NutritionLogEntity(date = iso)
        db.nutritionDao().upsert(transform(current))
        evaluator.recomputeDay(date, finalized = date < AccendDate.today())
        cloudRepo.pushDayIfSignedIn(iso)
        cloudRepo.awardExp(5, "nut-$iso")
    }

    suspend fun saveScreenTime(date: AccendDate, minutes: Int) {
        val iso = date.toIsoString()
        val existing = db.mindDao().screenTimeForDateOnce(iso)
        db.mindDao().upsertScreenTime(
            ScreenTimeLogEntity(
                id = existing?.id ?: 0,
                date = iso,
                minutes = minutes.coerceIn(0, 1440)
            )
        )
        evaluator.recomputeDay(date, finalized = date < AccendDate.today())
        cloudRepo.pushDayIfSignedIn(iso)
    }

    suspend fun saveReadingPages(date: AccendDate, pages: Int) {
        val iso = date.toIsoString()
        val existing = db.mindDao().readingForDateOnce(iso)
        db.mindDao().upsertReading(
            ReadingLogEntity(
                id = existing?.id ?: 0,
                date = iso,
                pages = pages.coerceIn(0, 1000)
            )
        )
        evaluator.recomputeDay(date, finalized = date < AccendDate.today())
        cloudRepo.pushDayIfSignedIn(iso)
    }

    suspend fun logMeditation(date: AccendDate, minutes: Int, restlessness: String) {
        db.mindDao().insertMeditation(
            MeditationLogEntity(
                date = date.toIsoString(),
                minutes = minutes.coerceAtLeast(1),
                restlessness = restlessness
            )
        )
        evaluator.recomputeDay(date, finalized = date < AccendDate.today())
        cloudRepo.pushDayIfSignedIn(date.toIsoString())
        cloudRepo.awardExp(40, "med-${System.currentTimeMillis()}")
    }

    suspend fun logDeepWork(date: AccendDate, minutes: Int, completed: Boolean) {
        if (minutes < 1) return
        db.mindDao().insertDeepWork(
            DeepWorkLogEntity(
                date = date.toIsoString(),
                minutes = minutes,
                completed = completed
            )
        )
        evaluator.recomputeDay(date, finalized = date < AccendDate.today())
        cloudRepo.pushDayIfSignedIn(date.toIsoString())
        if (completed) cloudRepo.awardExp(50, "deep-${System.currentTimeMillis()}")
    }

    suspend fun levelUpExercise(exerciseId: Long, today: AccendDate = AccendDate.today()) {
        val exercise = db.exerciseDao().getById(exerciseId) ?: return
        val prog = db.exerciseDao().progression(exerciseId) ?: return
        val levels = ProgressionLevel.parseList(prog.levelsList)
        val next = ProgressionRules.nextLevelPreview(exercise, levels) ?: return
        val newIndex = if (levels.size > 1) exercise.currentLevelIndex + 1 else exercise.currentLevelIndex
        db.exerciseDao().upsert(
            exercise.copy(
                name = next.name,
                currentLevelIndex = newIndex,
                sets = next.sets,
                targetReps = next.target,
                levelSince = today.toIsoString(),
                suggestionSnoozedOn = null
            )
        )
    }

    suspend fun levelDownExercise(exerciseId: Long, today: AccendDate = AccendDate.today()) {
        val exercise = db.exerciseDao().getById(exerciseId) ?: return
        val prog = db.exerciseDao().progression(exerciseId) ?: return
        val levels = ProgressionLevel.parseList(prog.levelsList)
        val prev = ProgressionRules.previousLevelPreview(exercise, levels) ?: return
        val newIndex = if (levels.size > 1) max(0, exercise.currentLevelIndex - 1) else exercise.currentLevelIndex
        db.exerciseDao().upsert(
            exercise.copy(
                name = prev.name,
                currentLevelIndex = newIndex,
                sets = prev.sets,
                targetReps = prev.target,
                levelSince = today.toIsoString(),
                suggestionSnoozedOn = null
            )
        )
    }

    suspend fun snoozeProgressionSuggestion(exerciseId: Long, today: AccendDate = AccendDate.today()) {
        val exercise = db.exerciseDao().getById(exerciseId) ?: return
        db.exerciseDao().upsert(exercise.copy(suggestionSnoozedOn = today.toIsoString()))
    }

    suspend fun getDayDetailSnapshot(date: AccendDate, readingTarget: Int): DayDetailSnapshot {
        val iso = date.toIsoString()
        val exercises = db.exerciseDao().forDayOnce(date.weekdaySundayOne)
        val exLogs = db.exerciseLogDao().forDateOnce(iso)
        val nutrition = db.nutritionDao().forDateOnce(iso)
        val meditations = db.mindDao().meditationForDateOnce(iso)
        val screenTime = db.mindDao().screenTimeForDateOnce(iso)
        val deepWorks = db.mindDao().deepWorkForDateOnce(iso)
        val reading = db.mindDao().readingForDateOnce(iso)
        val breakdown = com.accend.app.domain.CompletionCalculator.compute(
            exercises, exLogs, nutrition, meditations, screenTime, deepWorks, reading, readingTarget
        )
        return DayDetailSnapshot(
            date = date,
            completionPercent = breakdown.percent,
            physicalDone = breakdown.physicalDone,
            physicalTotal = breakdown.physicalTotal,
            nutritionDone = breakdown.nutritionDone,
            meditationMinutes = meditations.sumOf { it.minutes },
            deepWorkMinutes = deepWorks.sumOf { it.minutes },
            readingPages = reading?.pages ?: 0,
            screenTimeMinutes = screenTime?.minutes
        )
    }
}

data class DayDetailSnapshot(
    val date: AccendDate,
    val completionPercent: Int,
    val physicalDone: Int,
    val physicalTotal: Int,
    val nutritionDone: Int,
    val meditationMinutes: Int,
    val deepWorkMinutes: Int,
    val readingPages: Int,
    val screenTimeMinutes: Int?
)

data class MoneySummary(
    val balance: Long = 0L,
    val savedJar: Long = 0L,
    val totalIn: Long = 0L,
    val totalOut: Long = 0L
)

class MoneyRepository(
    private val db: AccendDatabase,
    private val cloudRepo: CloudRepository
) {
    val ledger: Flow<List<MoneyTransactionEntity>> = db.moneyDao().ledger()
    val totalSkillHours: Flow<Double> = db.skillDao().totalHours()
    val recentSkills: Flow<List<SkillLogEntity>> = db.skillDao().recent(50)
    val recentSkillTags: Flow<List<String>> = db.skillDao().recentTags()

    val summary: Flow<MoneySummary> = combine(
        db.moneyDao().totalIn(),
        db.moneyDao().totalOut(),
        db.savingsDao().totalDeposits(),
        db.savingsDao().totalWithdrawals()
    ) { totalIn, totalOut, deposits, withdrawals ->
        val jar = (deposits - withdrawals).coerceAtLeast(0L)
        val balance = totalIn - totalOut - deposits + withdrawals
        MoneySummary(balance = balance, savedJar = jar, totalIn = totalIn, totalOut = totalOut)
    }

    fun funSpendingBetween(from: AccendDate, to: AccendDate): Flow<Long> =
        db.moneyDao().funBetween(from.toIsoString(), to.toIsoString())

    suspend fun insertTransaction(tx: MoneyTransactionEntity): Long {
        val id = db.moneyDao().insert(tx)
        cloudRepo.pushDayIfSignedIn(tx.date)
        cloudRepo.awardExp(10, "tx-${UUID.randomUUID()}")
        return id
    }

    suspend fun updateTransaction(tx: MoneyTransactionEntity) {
        db.moneyDao().update(tx)
        cloudRepo.pushDayIfSignedIn(tx.date)
    }

    suspend fun updateImpulseAnswer(txId: Long, answer: String?) {
        val current = db.moneyDao().allOnce().firstOrNull { it.id == txId } ?: return
        db.moneyDao().update(current.copy(impulseAnswer = answer))
        cloudRepo.pushDayIfSignedIn(current.date)
    }

    suspend fun linkedSavingsFor(txId: Long): List<SavingsMoveEntity> = db.savingsDao().linkedTo(txId)

    suspend fun deleteTransaction(tx: MoneyTransactionEntity, deleteLinkedSavings: Boolean = false) {
        db.withTransaction {
            if (deleteLinkedSavings) {
                db.savingsDao().deleteLinkedTo(tx.id)
            }
            db.moneyDao().delete(tx)
        }
        cloudRepo.pushDayIfSignedIn(tx.date)
    }

    suspend fun moveSavings(date: AccendDate, amount: Long, direction: String, linkedTxId: Long? = null) {
        if (amount <= 0L) return
        db.savingsDao().insert(
            SavingsMoveEntity(
                date = date.toIsoString(),
                amount = amount,
                direction = direction,
                linkedTransactionId = linkedTxId
            )
        )
        cloudRepo.pushDayIfSignedIn(date.toIsoString())
    }

    suspend fun addSkillHours(date: AccendDate, hours: Double, tag: String) {
        if (hours <= 0.0 || hours > 24.0) return
        db.skillDao().insert(
            SkillLogEntity(
                date = date.toIsoString(),
                hours = hours,
                tag = tag.trim().ifEmpty { "General" }
            )
        )
        cloudRepo.pushDayIfSignedIn(date.toIsoString())
        cloudRepo.awardExp(20, "skill-${UUID.randomUUID()}")
    }

    suspend fun deleteSkillLog(log: SkillLogEntity) = db.skillDao().delete(log)
}

class BodyRepository(private val db: AccendDatabase) {
    val allWeights: Flow<List<WeightLogEntity>> = db.bodyDao().allWeights()
    val allHeights: Flow<List<HeightLogEntity>> = db.bodyDao().allHeights()
    val latestWeightDate: Flow<String?> = db.bodyDao().latestWeightDate()
    val latestHeightDate: Flow<String?> = db.bodyDao().latestHeightDate()

    suspend fun upsertWeight(date: AccendDate, kg: Double) {
        val iso = date.toIsoString()
        val existing = db.bodyDao().allWeightsOnce().firstOrNull { it.date == iso }
        db.bodyDao().upsertWeight(WeightLogEntity(id = existing?.id ?: 0, date = iso, kg = kg))
    }

    suspend fun deleteWeight(log: WeightLogEntity) = db.bodyDao().deleteWeight(log)

    suspend fun upsertHeight(date: AccendDate, cm: Double) {
        val iso = date.toIsoString()
        val existing = db.bodyDao().allHeightsOnce().firstOrNull { it.date == iso }
        db.bodyDao().upsertHeight(HeightLogEntity(id = existing?.id ?: 0, date = iso, cm = cm))
    }

    suspend fun deleteHeight(log: HeightLogEntity) = db.bodyDao().deleteHeight(log)
}
