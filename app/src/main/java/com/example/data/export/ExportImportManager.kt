package com.example.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.AppConfig
import com.example.data.db.AccendDatabase
import com.example.data.db.DayLogEntity
import com.example.data.db.DeepWorkLogEntity
import com.example.data.db.ExerciseEntity
import com.example.data.db.ExerciseLogEntity
import com.example.data.db.ExerciseProgressionEntity
import com.example.data.db.HeightLogEntity
import com.example.data.db.MeditationLogEntity
import com.example.data.db.MoneyTransactionEntity
import com.example.data.db.MuscleGroup
import com.example.data.db.NutritionLogEntity
import com.example.data.db.ReadingLogEntity
import com.example.data.db.SavingsMoveEntity
import com.example.data.db.ScreenTimeLogEntity
import com.example.data.db.SkillLogEntity
import com.example.data.db.TargetUnit
import com.example.data.db.UserEntity
import com.example.data.db.WeightLogEntity
import com.example.data.prefs.AccendPrefs
import com.example.data.prefs.PrefsRepository
import com.example.data.repo.SeedRepository
import com.example.domain.DayEvaluator
import com.example.util.AccendDate
import com.example.util.HapticLevel
import com.example.util.ImageUtils
import com.example.work.NotificationScheduler
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ImportPreview(
    val userName: String,
    val exerciseLogsCount: Int,
    val transactionsCount: Int,
    val skillLogsCount: Int,
    val rawJson: String
)

class ExportImportManager(
    private val context: Context,
    private val db: AccendDatabase,
    private val prefsRepo: PrefsRepository,
    private val seedRepo: SeedRepository,
    private val evaluator: DayEvaluator
) {
    suspend fun buildExportJson(): String {
        val user = db.userDao().getUser()
        val prefs = prefsRepo.getSnapshot()
        val exercises = db.exerciseDao().allOnce()
        val progressions = db.exerciseDao().allProgressionsOnce()
        val exLogs = db.exerciseLogDao().allOnce()
        val nutrLogs = db.nutritionDao().allOnce()
        val medLogs = db.mindDao().allMeditationsOnce()
        val screenLogs = db.mindDao().allScreenTimeOnce()
        val deepLogs = db.mindDao().allDeepWorkOnce()
        val readLogs = db.mindDao().allReadingOnce()
        val dayLogs = db.dayLogDao().allOnce()
        val weights = db.bodyDao().allWeightsOnce()
        val heights = db.bodyDao().allHeightsOnce()
        val txs = db.moneyDao().allOnce()
        val savings = db.savingsDao().allOnce()
        val skills = db.skillDao().allOnce()

        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val root = JSONObject().apply {
            put("app", "ACCEND")
            put("schemaVersion", AppConfig.SCHEMA_VERSION)
            put("exportedAt", isoNow)
            user?.let { u ->
                put("user", JSONObject().apply {
                    put("name", u.name)
                    put("joinedDate", u.joinedDate)
                    put("onboarded", u.onboarded)
                })
                ImageUtils.encodeProfileThumbBase64(u.pfpUri)?.let { b64 ->
                    put("pfpThumbBase64", b64)
                }
            }
            put("preferences", JSONObject().apply {
                put("screenTarget", prefs.screenTargetMinutes)
                put("readingTarget", prefs.readingTarget)
                put("deepWorkDefault", prefs.deepWorkDefaultMinutes)
                put("savePercent", prefs.savePercent)
                put("notifText2DayMiss", prefs.notifText2DayMiss)
                put("dailyReminderTime", prefs.dailyReminderTime)
                put("weightGoal", prefs.weightGoalKg.toDouble())
                put("heightGoalCm", prefs.heightGoalCm)
                put("runwayTargetDate", prefs.runwayTargetDate)
                put("notificationsEnabled", prefs.notificationsEnabled)
                put("dailyReminderEnabled", prefs.dailyReminderEnabled)
                put("missAlertEnabled", prefs.missAlertEnabled)
                put("hapticLevel", prefs.hapticLevel.name)
            })
            put("exercises", JSONArray().apply {
                exercises.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("name", e.name)
                        put("muscleGroup", e.muscleGroup.name)
                        put("currentLevelIndex", e.currentLevelIndex)
                        put("sets", e.sets)
                        put("targetReps", e.targetReps)
                        put("unit", e.unit.name)
                        put("orderInDay", e.orderInDay)
                        put("dayOfWeek", e.dayOfWeek)
                        put("levelSince", e.levelSince)
                        put("suggestionSnoozedOn", e.suggestionSnoozedOn)
                    })
                }
            })
            put("progressions", JSONArray().apply {
                progressions.forEach { p ->
                    put(JSONObject().apply {
                        put("exerciseId", p.exerciseId)
                        put("levelsList", p.levelsList)
                    })
                }
            })
            put("logs", JSONObject().apply {
                put("exercise", JSONArray().apply {
                    exLogs.forEach { l ->
                        put(JSONObject().apply {
                            put("date", l.date)
                            put("exerciseId", l.exerciseId)
                            put("done", l.done)
                            put("levelNameSnapshot", l.levelNameSnapshot)
                            put("volumeSnapshot", l.volumeSnapshot)
                        })
                    }
                })
                put("nutrition", JSONArray().apply {
                    nutrLogs.forEach { l ->
                        put(JSONObject().apply {
                            put("date", l.date)
                            put("protein", l.protein)
                            put("heavyMeals", l.heavyMeals)
                            put("breakfast", l.breakfast)
                            put("hydration", l.hydration)
                            put("sleep9h", l.sleep9h)
                        })
                    }
                })
                put("meditation", JSONArray().apply {
                    medLogs.forEach { l ->
                        put(JSONObject().apply {
                            put("date", l.date)
                            put("minutes", l.minutes)
                            put("restlessness", l.restlessness)
                        })
                    }
                })
                put("screenTime", JSONArray().apply {
                    screenLogs.forEach { l ->
                        put(JSONObject().apply {
                            put("date", l.date)
                            put("minutes", l.minutes)
                        })
                    }
                })
                put("deepWork", JSONArray().apply {
                    deepLogs.forEach { l ->
                        put(JSONObject().apply {
                            put("date", l.date)
                            put("minutes", l.minutes)
                            put("completed", l.completed)
                        })
                    }
                })
                put("reading", JSONArray().apply {
                    readLogs.forEach { l ->
                        put(JSONObject().apply {
                            put("date", l.date)
                            put("pages", l.pages)
                        })
                    }
                })
                put("day", JSONArray().apply {
                    dayLogs.forEach { l ->
                        put(JSONObject().apply {
                            put("date", l.date)
                            put("completionPercent", l.completionPercent)
                            put("missedFlag", l.missedFlag)
                            put("finalized", l.finalized)
                        })
                    }
                })
                put("weight", JSONArray().apply {
                    weights.forEach { w ->
                        put(JSONObject().apply {
                            put("date", w.date)
                            put("kg", w.kg)
                        })
                    }
                })
                put("height", JSONArray().apply {
                    heights.forEach { h ->
                        put(JSONObject().apply {
                            put("date", h.date)
                            put("cm", h.cm)
                        })
                    }
                })
            })
            put("money", JSONObject().apply {
                put("transactions", JSONArray().apply {
                    txs.forEach { t ->
                        put(JSONObject().apply {
                            put("id", t.id)
                            put("date", t.date)
                            put("type", t.type)
                            put("amount", t.amount)
                            put("category", t.category)
                            put("source", t.source)
                            put("note", t.note)
                            put("impulseAnswer", t.impulseAnswer)
                        })
                    }
                })
                put("savingsMoves", JSONArray().apply {
                    savings.forEach { s ->
                        put(JSONObject().apply {
                            put("date", s.date)
                            put("amount", s.amount)
                            put("direction", s.direction)
                            put("linkedTransactionId", s.linkedTransactionId)
                        })
                    }
                })
            })
            put("skills", JSONArray().apply {
                skills.forEach { s ->
                    put(JSONObject().apply {
                        put("date", s.date)
                        put("hours", s.hours)
                        put("tag", s.tag)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun exportToShareUri(): Uri {
        val json = buildExportJson()
        val stamp = AccendDate.today().toIsoString().replace("-", "")
        val file = File(context.cacheDir, "accend_export_$stamp.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun writeExportToUri(uri: Uri) {
        val json = buildExportJson()
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }
    }

    fun validateImport(rawJson: String): Result<ImportPreview> = runCatching {
        val root = JSONObject(rawJson)
        val app = root.optString("app")
        if (app != "ACCEND") error("Invalid export file: app identifier mismatch.")
        val version = root.optInt("schemaVersion", Int.MAX_VALUE)
        if (version > AppConfig.SCHEMA_VERSION) error("Export file requires a newer version of Accend.")
        val userObj = root.optJSONObject("user") ?: error("Export file is missing user profile.")
        val name = userObj.optString("name").ifBlank { "Operator" }
        val logs = root.optJSONObject("logs")
        val exCount = logs?.optJSONArray("exercise")?.length() ?: 0
        val txCount = root.optJSONObject("money")?.optJSONArray("transactions")?.length() ?: 0
        val skillCount = root.optJSONArray("skills")?.length() ?: 0
        ImportPreview(
            userName = name,
            exerciseLogsCount = exCount,
            transactionsCount = txCount,
            skillLogsCount = skillCount,
            rawJson = rawJson
        )
    }

    suspend fun performImport(rawJson: String): Result<Unit> = runCatching {
        val root = JSONObject(rawJson)
        if (root.optString("app") != "ACCEND") error("Invalid export file.")
        if (root.optInt("schemaVersion", Int.MAX_VALUE) > AppConfig.SCHEMA_VERSION) {
            error("Unsupported schema version.")
        }

        val userObj = root.getJSONObject("user")
        val pfpBase64 = root.optString("pfpThumbBase64", "")
        val restoredPfpUri = if (pfpBase64.isNotBlank()) {
            ImageUtils.decodeBase64ToProfilePhoto(context, pfpBase64)
        } else null

        db.withTransaction {
            // Clear all data tables
            db.exerciseLogDao().deleteAll()
            db.exerciseDao().deleteAll()
            db.nutritionDao().deleteAll()
            db.mindDao().deleteAllMeditations()
            db.mindDao().deleteAllScreenTime()
            db.mindDao().deleteAllDeepWork()
            db.mindDao().deleteAllReading()
            db.dayLogDao().deleteAll()
            db.savingsDao().deleteAll()
            db.moneyDao().deleteAll()
            db.skillDao().deleteAll()
            db.bodyDao().deleteAllWeights()
            db.bodyDao().deleteAllHeights()

            // Restore user
            db.userDao().upsert(
                UserEntity(
                    id = 1,
                    name = userObj.optString("name", "Operator"),
                    pfpUri = restoredPfpUri,
                    joinedDate = userObj.optString("joinedDate", AccendDate.today().toIsoString()),
                    onboarded = true
                )
            )

            // Restore exercises & remap IDs
            val exerciseIdMap = mutableMapOf<Long, Long>()
            val exArr = root.optJSONArray("exercises") ?: JSONArray()
            for (i in 0 until exArr.length()) {
                val o = exArr.getJSONObject(i)
                val oldId = o.optLong("id", 0L)
                val newId = db.exerciseDao().insertRaw(
                    ExerciseEntity(
                        name = o.getString("name"),
                        muscleGroup = runCatching { MuscleGroup.valueOf(o.getString("muscleGroup")) }
                            .getOrDefault(MuscleGroup.RECOVERY),
                        currentLevelIndex = o.optInt("currentLevelIndex", 0),
                        sets = o.optInt("sets", 1),
                        targetReps = o.optInt("targetReps", 0),
                        unit = runCatching { TargetUnit.valueOf(o.getString("unit")) }
                            .getOrDefault(TargetUnit.REPS),
                        orderInDay = o.optInt("orderInDay", i + 1),
                        dayOfWeek = o.optInt("dayOfWeek", 1),
                        levelSince = o.optString("levelSince").takeIf { it.isNotBlank() && it != "null" },
                        suggestionSnoozedOn = o.optString("suggestionSnoozedOn").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
                exerciseIdMap[oldId] = newId
            }

            if (exArr.length() == 0) {
                seedRepo.insertDefaultExercises(userObj.optString("joinedDate", AccendDate.today().toIsoString()))
            } else {
                val progArr = root.optJSONArray("progressions") ?: JSONArray()
                for (i in 0 until progArr.length()) {
                    val o = progArr.getJSONObject(i)
                    val mappedId = exerciseIdMap[o.optLong("exerciseId")] ?: continue
                    db.exerciseDao().upsertProgression(
                        ExerciseProgressionEntity(
                            exerciseId = mappedId,
                            levelsList = o.getString("levelsList")
                        )
                    )
                }
            }

            val logs = root.optJSONObject("logs") ?: JSONObject()
            logs.optJSONArray("exercise")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val mappedId = exerciseIdMap[o.optLong("exerciseId")] ?: continue
                    db.exerciseLogDao().upsert(
                        ExerciseLogEntity(
                            date = o.getString("date"),
                            exerciseId = mappedId,
                            done = o.optBoolean("done", false),
                            levelNameSnapshot = o.optString("levelNameSnapshot", ""),
                            volumeSnapshot = o.optDouble("volumeSnapshot", 0.0)
                        )
                    )
                }
            }
            logs.optJSONArray("nutrition")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.nutritionDao().upsert(
                        NutritionLogEntity(
                            date = o.getString("date"),
                            protein = o.optBoolean("protein", false),
                            heavyMeals = o.optBoolean("heavyMeals", false),
                            breakfast = o.optBoolean("breakfast", false),
                            hydration = o.optBoolean("hydration", false),
                            sleep9h = o.optBoolean("sleep9h", false)
                        )
                    )
                }
            }
            logs.optJSONArray("meditation")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.mindDao().insertMeditation(
                        MeditationLogEntity(
                            date = o.getString("date"),
                            minutes = o.optInt("minutes", 1),
                            restlessness = o.optString("restlessness", "still")
                        )
                    )
                }
            }
            logs.optJSONArray("screenTime")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.mindDao().upsertScreenTime(
                        ScreenTimeLogEntity(
                            date = o.getString("date"),
                            minutes = o.optInt("minutes", 0)
                        )
                    )
                }
            }
            logs.optJSONArray("deepWork")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.mindDao().insertDeepWork(
                        DeepWorkLogEntity(
                            date = o.getString("date"),
                            minutes = o.optInt("minutes", 15),
                            completed = o.optBoolean("completed", true)
                        )
                    )
                }
            }
            logs.optJSONArray("reading")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.mindDao().upsertReading(
                        ReadingLogEntity(
                            date = o.getString("date"),
                            pages = o.optInt("pages", 0)
                        )
                    )
                }
            }
            logs.optJSONArray("day")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.dayLogDao().upsert(
                        DayLogEntity(
                            date = o.getString("date"),
                            completionPercent = o.optInt("completionPercent", 0),
                            missedFlag = o.optBoolean("missedFlag", false),
                            finalized = o.optBoolean("finalized", false)
                        )
                    )
                }
            }
            logs.optJSONArray("weight")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.bodyDao().upsertWeight(
                        WeightLogEntity(date = o.getString("date"), kg = o.getDouble("kg"))
                    )
                }
            }
            logs.optJSONArray("height")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.bodyDao().upsertHeight(
                        HeightLogEntity(date = o.getString("date"), cm = o.getDouble("cm"))
                    )
                }
            }

            val txIdMap = mutableMapOf<Long, Long>()
            val moneyObj = root.optJSONObject("money") ?: JSONObject()
            moneyObj.optJSONArray("transactions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val oldId = o.optLong("id", 0L)
                    val newId = db.moneyDao().insert(
                        MoneyTransactionEntity(
                            date = o.getString("date"),
                            type = o.getString("type"),
                            amount = o.getLong("amount"),
                            category = o.optString("category").takeIf { it.isNotBlank() && it != "null" },
                            source = o.optString("source").takeIf { it.isNotBlank() && it != "null" },
                            note = o.optString("note").takeIf { it.isNotBlank() && it != "null" },
                            impulseAnswer = o.optString("impulseAnswer").takeIf { it.isNotBlank() && it != "null" }
                        )
                    )
                    txIdMap[oldId] = newId
                }
            }
            moneyObj.optJSONArray("savingsMoves")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val oldLinked = if (o.isNull("linkedTransactionId")) null else o.optLong("linkedTransactionId")
                    db.savingsDao().insert(
                        SavingsMoveEntity(
                            date = o.getString("date"),
                            amount = o.getLong("amount"),
                            direction = o.getString("direction"),
                            linkedTransactionId = oldLinked?.let { txIdMap[it] }
                        )
                    )
                }
            }
            root.optJSONArray("skills")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.skillDao().insert(
                        SkillLogEntity(
                            date = o.getString("date"),
                            hours = o.getDouble("hours"),
                            tag = o.optString("tag", "Skill")
                        )
                    )
                }
            }
        }

        // Restore preferences
        root.optJSONObject("preferences")?.let { p ->
            prefsRepo.importPreferences(
                AccendPrefs(
                    onboarded = true,
                    screenTargetMinutes = p.optInt("screenTarget", 180),
                    readingTarget = p.optInt("readingTarget", 1),
                    deepWorkDefaultMinutes = p.optInt("deepWorkDefault", 30),
                    savePercent = p.optInt("savePercent", 40),
                    notifText2DayMiss = p.optString("notifText2DayMiss", AppConfig.DEFAULT_TWO_DAY_MISS_TEXT),
                    dailyReminderTime = p.optString("dailyReminderTime", "20:00"),
                    weightGoalKg = p.optDouble("weightGoal", 75.0).toFloat(),
                    heightGoalCm = p.optInt("heightGoalCm", 183),
                    runwayTargetDate = p.optString("runwayTargetDate", AppConfig.DEFAULT_RUNWAY_DATE),
                    notificationsEnabled = p.optBoolean("notificationsEnabled", true),
                    dailyReminderEnabled = p.optBoolean("dailyReminderEnabled", true),
                    missAlertEnabled = p.optBoolean("missAlertEnabled", true),
                    hapticLevel = runCatching { HapticLevel.valueOf(p.optString("hapticLevel", "HIGH")) }
                        .getOrDefault(HapticLevel.HIGH)
                )
            )
        }

        evaluator.catchUp()
        NotificationScheduler.sync(context, prefsRepo.getSnapshot())
    }

    /**
     * Resets all journey logs, money, skills, body logs, and re-seeds exercises at level 0
     * with joinedDate = today. Keeps user name, PFP, and all preferences.
     */
    suspend fun resetJourney() {
        val todayIso = AccendDate.today().toIsoString()
        db.withTransaction {
            val currentUser = db.userDao().getUser()
            db.exerciseLogDao().deleteAll()
            db.exerciseDao().deleteAll()
            db.nutritionDao().deleteAll()
            db.mindDao().deleteAllMeditations()
            db.mindDao().deleteAllScreenTime()
            db.mindDao().deleteAllDeepWork()
            db.mindDao().deleteAllReading()
            db.dayLogDao().deleteAll()
            db.savingsDao().deleteAll()
            db.moneyDao().deleteAll()
            db.skillDao().deleteAll()
            db.bodyDao().deleteAllWeights()
            db.bodyDao().deleteAllHeights()

            if (currentUser != null) {
                db.userDao().upsert(currentUser.copy(joinedDate = todayIso))
            }
            seedRepo.insertDefaultExercises(todayIso)
        }
        prefsRepo.clearEvaluatorState()
        evaluator.catchUp()
        NotificationScheduler.sync(context, prefsRepo.getSnapshot())
    }
}
