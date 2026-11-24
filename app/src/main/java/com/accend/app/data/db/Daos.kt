package com.accend.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = 1 LIMIT 1")
    fun observeUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE id = 1 LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE dayOfWeek = :dow ORDER BY orderInDay ASC")
    fun forDay(dow: Int): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE dayOfWeek = :dow ORDER BY orderInDay ASC")
    suspend fun forDayOnce(dow: Int): List<ExerciseEntity>

    @Query("SELECT * FROM exercise ORDER BY muscleGroup ASC, dayOfWeek ASC, orderInDay ASC")
    fun all(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise ORDER BY dayOfWeek ASC, orderInDay ASC")
    suspend fun allOnce(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRaw(exercise: ExerciseEntity): Long

    @Upsert
    suspend fun upsertProgression(progression: ExerciseProgressionEntity)

    @Query("SELECT * FROM exercise_progression WHERE exerciseId = :id LIMIT 1")
    suspend fun progression(id: Long): ExerciseProgressionEntity?

    @Query("SELECT * FROM exercise_progression")
    fun allProgressions(): Flow<List<ExerciseProgressionEntity>>

    @Query("SELECT * FROM exercise_progression")
    suspend fun allProgressionsOnce(): List<ExerciseProgressionEntity>

    @Query("DELETE FROM exercise")
    suspend fun deleteAll()
}

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM exercise_log WHERE date = :date")
    fun forDate(date: String): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_log WHERE date = :date")
    suspend fun forDateOnce(date: String): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_log WHERE date BETWEEN :from AND :to")
    fun rangeFlow(from: String, to: String): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_log WHERE date BETWEEN :from AND :to")
    suspend fun range(from: String, to: String): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_log")
    fun allFlow(): Flow<List<ExerciseLogEntity>>

    @Query("SELECT * FROM exercise_log")
    suspend fun allOnce(): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_log WHERE exerciseId = :id AND done = 1 ORDER BY date DESC")
    suspend fun doneFor(id: Long): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_log WHERE date = :date AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getByDateAndExercise(date: String, exerciseId: Long): ExerciseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: ExerciseLogEntity)

    @Query("DELETE FROM exercise_log")
    suspend fun deleteAll()
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_log WHERE date = :date LIMIT 1")
    fun forDate(date: String): Flow<NutritionLogEntity?>

    @Query("SELECT * FROM nutrition_log WHERE date = :date LIMIT 1")
    suspend fun forDateOnce(date: String): NutritionLogEntity?

    @Query("SELECT * FROM nutrition_log")
    suspend fun allOnce(): List<NutritionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: NutritionLogEntity)

    @Query("DELETE FROM nutrition_log")
    suspend fun deleteAll()
}

@Dao
interface MindDao {
    @Query("SELECT * FROM meditation_log WHERE date = :date ORDER BY id DESC")
    fun meditationForDate(date: String): Flow<List<MeditationLogEntity>>

    @Query("SELECT * FROM meditation_log WHERE date = :date")
    suspend fun meditationForDateOnce(date: String): List<MeditationLogEntity>

    @Query("SELECT * FROM meditation_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun meditationRange(from: String, to: String): Flow<List<MeditationLogEntity>>

    @Query("SELECT * FROM meditation_log")
    fun allMeditationsFlow(): Flow<List<MeditationLogEntity>>

    @Query("SELECT * FROM meditation_log")
    suspend fun allMeditationsOnce(): List<MeditationLogEntity>

    @Insert
    suspend fun insertMeditation(log: MeditationLogEntity): Long

    @Query("SELECT * FROM screen_time_log WHERE date = :date LIMIT 1")
    fun screenTimeForDate(date: String): Flow<ScreenTimeLogEntity?>

    @Query("SELECT * FROM screen_time_log WHERE date = :date LIMIT 1")
    suspend fun screenTimeForDateOnce(date: String): ScreenTimeLogEntity?

    @Query("SELECT * FROM screen_time_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun screenTimeRange(from: String, to: String): Flow<List<ScreenTimeLogEntity>>

    @Query("SELECT * FROM screen_time_log")
    fun allScreenTimeFlow(): Flow<List<ScreenTimeLogEntity>>

    @Query("SELECT * FROM screen_time_log")
    suspend fun allScreenTimeOnce(): List<ScreenTimeLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScreenTime(log: ScreenTimeLogEntity)

    @Query("SELECT * FROM deep_work_log WHERE date = :date ORDER BY id DESC")
    fun deepWorkForDate(date: String): Flow<List<DeepWorkLogEntity>>

    @Query("SELECT * FROM deep_work_log WHERE date = :date")
    suspend fun deepWorkForDateOnce(date: String): List<DeepWorkLogEntity>

    @Query("SELECT * FROM deep_work_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun deepWorkRange(from: String, to: String): Flow<List<DeepWorkLogEntity>>

    @Query("SELECT * FROM deep_work_log")
    fun allDeepWorkFlow(): Flow<List<DeepWorkLogEntity>>

    @Query("SELECT * FROM deep_work_log")
    suspend fun allDeepWorkOnce(): List<DeepWorkLogEntity>

    @Insert
    suspend fun insertDeepWork(log: DeepWorkLogEntity): Long

    @Query("SELECT * FROM reading_log WHERE date = :date LIMIT 1")
    fun readingForDate(date: String): Flow<ReadingLogEntity?>

    @Query("SELECT * FROM reading_log WHERE date = :date LIMIT 1")
    suspend fun readingForDateOnce(date: String): ReadingLogEntity?

    @Query("SELECT * FROM reading_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun readingRange(from: String, to: String): Flow<List<ReadingLogEntity>>

    @Query("SELECT * FROM reading_log")
    fun allReadingFlow(): Flow<List<ReadingLogEntity>>

    @Query("SELECT * FROM reading_log")
    suspend fun allReadingOnce(): List<ReadingLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReading(log: ReadingLogEntity)

    @Query("DELETE FROM meditation_log")
    suspend fun deleteAllMeditations()

    @Query("DELETE FROM screen_time_log")
    suspend fun deleteAllScreenTime()

    @Query("DELETE FROM deep_work_log")
    suspend fun deleteAllDeepWork()

    @Query("DELETE FROM reading_log")
    suspend fun deleteAllReading()
}

@Dao
interface DayLogDao {
    @Query("SELECT * FROM day_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun range(from: String, to: String): Flow<List<DayLogEntity>>

    @Query("SELECT * FROM day_log ORDER BY date ASC")
    fun allFlow(): Flow<List<DayLogEntity>>

    @Query("SELECT * FROM day_log ORDER BY date ASC")
    suspend fun allOnce(): List<DayLogEntity>

    @Query("SELECT * FROM day_log WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DayLogEntity?

    @Query("SELECT * FROM day_log WHERE date = :date LIMIT 1")
    fun observe(date: String): Flow<DayLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dayLog: DayLogEntity)

    @Query("DELETE FROM day_log")
    suspend fun deleteAll()
}

@Dao
interface MoneyDao {
    @Query("SELECT * FROM money_transaction ORDER BY date DESC, id DESC")
    fun ledger(): Flow<List<MoneyTransactionEntity>>

    @Query("SELECT * FROM money_transaction ORDER BY date DESC, id DESC")
    suspend fun allOnce(): List<MoneyTransactionEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM money_transaction WHERE type = 'IN'")
    fun totalIn(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM money_transaction WHERE type = 'OUT'")
    fun totalOut(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM money_transaction WHERE type = 'OUT' AND category = 'fun' AND date BETWEEN :from AND :to")
    fun funBetween(from: String, to: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: MoneyTransactionEntity): Long

    @Update
    suspend fun update(tx: MoneyTransactionEntity)

    @Delete
    suspend fun delete(tx: MoneyTransactionEntity)

    @Query("DELETE FROM money_transaction")
    suspend fun deleteAll()
}

@Dao
interface SavingsDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings_move WHERE direction = 'DEPOSIT'")
    fun totalDeposits(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings_move WHERE direction = 'WITHDRAW'")
    fun totalWithdrawals(): Flow<Long>

    @Query("SELECT * FROM savings_move ORDER BY date DESC, id DESC")
    fun allFlow(): Flow<List<SavingsMoveEntity>>

    @Query("SELECT * FROM savings_move ORDER BY date DESC, id DESC")
    suspend fun allOnce(): List<SavingsMoveEntity>

    @Query("SELECT * FROM savings_move WHERE linkedTransactionId = :txId")
    suspend fun linkedTo(txId: Long): List<SavingsMoveEntity>

    @Query("DELETE FROM savings_move WHERE linkedTransactionId = :txId")
    suspend fun deleteLinkedTo(txId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(move: SavingsMoveEntity): Long

    @Delete
    suspend fun delete(move: SavingsMoveEntity)

    @Query("DELETE FROM savings_move")
    suspend fun deleteAll()
}

@Dao
interface SkillDao {
    @Query("SELECT COALESCE(SUM(hours), 0.0) FROM skill_log")
    fun totalHours(): Flow<Double>

    @Query("SELECT * FROM skill_log ORDER BY date DESC, id DESC LIMIT :limit")
    fun recent(limit: Int = 50): Flow<List<SkillLogEntity>>

    @Query("SELECT * FROM skill_log ORDER BY date DESC, id DESC")
    suspend fun allOnce(): List<SkillLogEntity>

    @Query("SELECT DISTINCT tag FROM skill_log ORDER BY id DESC LIMIT 10")
    fun recentTags(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SkillLogEntity): Long

    @Delete
    suspend fun delete(log: SkillLogEntity)

    @Query("DELETE FROM skill_log")
    suspend fun deleteAll()
}

@Dao
interface BodyDao {
    @Query("SELECT * FROM weight_log ORDER BY date DESC")
    fun allWeights(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_log ORDER BY date ASC")
    suspend fun allWeightsOnce(): List<WeightLogEntity>

    @Query("SELECT MAX(date) FROM weight_log")
    fun latestWeightDate(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(log: WeightLogEntity)

    @Delete
    suspend fun deleteWeight(log: WeightLogEntity)

    @Query("SELECT * FROM height_log ORDER BY date DESC")
    fun allHeights(): Flow<List<HeightLogEntity>>

    @Query("SELECT * FROM height_log ORDER BY date ASC")
    suspend fun allHeightsOnce(): List<HeightLogEntity>

    @Query("SELECT MAX(date) FROM height_log")
    fun latestHeightDate(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHeight(log: HeightLogEntity)

    @Delete
    suspend fun deleteHeight(log: HeightLogEntity)

    @Query("DELETE FROM weight_log")
    suspend fun deleteAllWeights()

    @Query("DELETE FROM height_log")
    suspend fun deleteAllHeights()
}
