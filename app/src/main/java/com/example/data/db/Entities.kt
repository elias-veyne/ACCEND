package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

enum class MuscleGroup(val displayName: String, val shortLabel: String) {
    CHEST_PUSH("Chest / Push", "CHEST"),
    BACK_PULL("Back / Pull", "BACK"),
    BICEPS("Biceps", "BICEPS"),
    TRICEPS("Triceps", "TRICEPS"),
    SHOULDERS("Shoulders", "SHLDR"),
    CORE("Core", "CORE"),
    LEGS("Legs", "LEGS"),
    NECK("Neck", "NECK"),
    FOREARMS("Forearms", "GRIP"),
    RECOVERY("Recovery", "REC")
}

enum class TargetUnit {
    REPS, SECONDS, METERS, MINUTES, NONE
}

data class ProgressionLevel(
    val name: String,
    val sets: Int,
    val target: Int
) {
    companion object {
        fun parseList(json: String): List<ProgressionLevel> {
            val result = mutableListOf<ProgressionLevel>()
            runCatching {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    result += ProgressionLevel(
                        name = obj.getString("name"),
                        sets = obj.optInt("sets", 1),
                        target = obj.optInt("target", 1)
                    )
                }
            }
            return result
        }

        fun toJson(levels: List<ProgressionLevel>): String {
            val arr = JSONArray()
            levels.forEach { lvl ->
                arr.put(
                    JSONObject().apply {
                        put("name", lvl.name)
                        put("sets", lvl.sets)
                        put("target", lvl.target)
                    }
                )
            }
            return arr.toString()
        }
    }
}

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val pfpUri: String?,
    val joinedDate: String, // ISO yyyy-MM-dd
    val onboarded: Boolean = true
)

@Entity(
    tableName = "exercise",
    indices = [Index("dayOfWeek")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val currentLevelIndex: Int = 0,
    val sets: Int = 1,
    val targetReps: Int,
    val unit: TargetUnit,
    val orderInDay: Int,
    val dayOfWeek: Int, // 1=Sun .. 7=Sat
    val levelSince: String? = null,
    val suggestionSnoozedOn: String? = null
)

@Entity(
    tableName = "exercise_progression",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseProgressionEntity(
    @PrimaryKey val exerciseId: Long,
    val levelsList: String // JSON array of ProgressionLevel
)

@Entity(
    tableName = "exercise_log",
    indices = [
        Index(value = ["date", "exerciseId"], unique = true),
        Index(value = ["exerciseId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseId: Long,
    val done: Boolean,
    val levelNameSnapshot: String,
    val volumeSnapshot: Double
)

@Entity(
    tableName = "nutrition_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class NutritionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val protein: Boolean = false,
    val heavyMeals: Boolean = false,
    val breakfast: Boolean = false,
    val hydration: Boolean = false,
    val sleep9h: Boolean = false
)

@Entity(
    tableName = "meditation_log",
    indices = [Index("date")]
)
data class MeditationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val minutes: Int,
    val restlessness: String // "a lot" | "a little" | "still"
)

@Entity(
    tableName = "screen_time_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class ScreenTimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val minutes: Int
)

@Entity(
    tableName = "deep_work_log",
    indices = [Index("date")]
)
data class DeepWorkLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val minutes: Int,
    val completed: Boolean
)

@Entity(
    tableName = "reading_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class ReadingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val pages: Int
)

@Entity(
    tableName = "day_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class DayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val completionPercent: Int,
    val missedFlag: Boolean,
    val finalized: Boolean
)

@Entity(
    tableName = "money_transaction",
    indices = [Index("date")]
)
data class MoneyTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val type: String, // "IN" | "OUT"
    val amount: Long,
    val category: String?, // food|transport|data|books|fun|other
    val source: String?, // pocket_money|gift|freelance|other
    val note: String?,
    val impulseAnswer: String? // NEED | WANT | null
)

@Entity(
    tableName = "savings_move",
    indices = [Index("linkedTransactionId")],
    foreignKeys = [
        ForeignKey(
            entity = MoneyTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class SavingsMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val amount: Long,
    val direction: String, // "DEPOSIT" | "WITHDRAW"
    val linkedTransactionId: Long? = null
)

@Entity(
    tableName = "skill_log",
    indices = [Index("date")]
)
data class SkillLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val hours: Double,
    val tag: String
)

@Entity(
    tableName = "weight_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val kg: Double
)

@Entity(
    tableName = "height_log",
    indices = [Index(value = ["date"], unique = true)]
)
data class HeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val cm: Double
)
