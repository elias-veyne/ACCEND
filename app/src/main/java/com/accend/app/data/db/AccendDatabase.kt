package com.accend.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        ExerciseEntity::class,
        ExerciseProgressionEntity::class,
        ExerciseLogEntity::class,
        NutritionLogEntity::class,
        MeditationLogEntity::class,
        ScreenTimeLogEntity::class,
        DeepWorkLogEntity::class,
        ReadingLogEntity::class,
        DayLogEntity::class,
        MoneyTransactionEntity::class,
        SavingsMoveEntity::class,
        SkillLogEntity::class,
        WeightLogEntity::class,
        HeightLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AccendConverters::class)
abstract class AccendDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun mindDao(): MindDao
    abstract fun dayLogDao(): DayLogDao
    abstract fun moneyDao(): MoneyDao
    abstract fun savingsDao(): SavingsDao
    abstract fun skillDao(): SkillDao
    abstract fun bodyDao(): BodyDao

    companion object {
        private const val DB_NAME = "accend_v2.db"

        fun create(context: Context): AccendDatabase =
            Room.databaseBuilder(context.applicationContext, AccendDatabase::class.java, DB_NAME)
                .build()
    }
}
