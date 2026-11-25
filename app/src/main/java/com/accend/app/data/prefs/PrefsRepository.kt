package com.accend.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.accend.app.AppConfig
import com.accend.app.util.HapticLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.accendDataStore: DataStore<Preferences> by preferencesDataStore(name = "accend_prefs")

data class AccendPrefs(
    val onboarded: Boolean = false,
    val screenTargetMinutes: Int = 180,
    val readingTarget: Int = 1,
    val deepWorkDefaultMinutes: Int = 30,
    val savePercent: Int = 40,
    val notifText2DayMiss: String = AppConfig.DEFAULT_TWO_DAY_MISS_TEXT,
    val dailyReminderTime: String = "20:00",
    val weightGoalKg: Float = 75f,
    val heightGoalCm: Int = 183,
    val runwayTargetDate: String = AppConfig.DEFAULT_RUNWAY_DATE,
    val notificationsEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val missAlertEnabled: Boolean = true,
    val hapticLevel: HapticLevel = HapticLevel.HIGH,
    // Internal evaluator state (non-exported)
    val lastEvaluatedDate: String? = null,
    val miss2Armed: Boolean = true,
    val lastMissAlertDate: String? = null
)

class PrefsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SCREEN_TARGET = intPreferencesKey("screenTarget")
        val READING_TARGET = intPreferencesKey("readingTarget")
        val DEEP_WORK_DEFAULT = intPreferencesKey("deepWorkDefault")
        val SAVE_PERCENT = intPreferencesKey("savePercent")
        val NOTIF_TEXT_2DAY_MISS = stringPreferencesKey("notifText2DayMiss")
        val DAILY_REMINDER_TIME = stringPreferencesKey("dailyReminderTime")
        val WEIGHT_GOAL = floatPreferencesKey("weightGoal")
        val HEIGHT_GOAL_CM = intPreferencesKey("heightGoalCm")
        val RUNWAY_TARGET_DATE = stringPreferencesKey("runwayTargetDate")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notificationsEnabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("dailyReminderEnabled")
        val MISS_ALERT_ENABLED = booleanPreferencesKey("missAlertEnabled")
        val HAPTIC_LEVEL = stringPreferencesKey("hapticLevel")
        val LAST_EVALUATED_DATE = stringPreferencesKey("lastEvaluatedDate")
        val MISS2_ARMED = booleanPreferencesKey("miss2Armed")
        val LAST_MISS_ALERT_DATE = stringPreferencesKey("lastMissAlertDate")
    }

    val prefsFlow: Flow<AccendPrefs> = context.accendDataStore.data.map { p ->
        AccendPrefs(
            onboarded = p[Keys.ONBOARDED] ?: false,
            screenTargetMinutes = p[Keys.SCREEN_TARGET] ?: 180,
            readingTarget = (p[Keys.READING_TARGET] ?: 1).coerceAtLeast(1),
            deepWorkDefaultMinutes = p[Keys.DEEP_WORK_DEFAULT] ?: 30,
            savePercent = (p[Keys.SAVE_PERCENT] ?: 40).coerceIn(0, 100),
            notifText2DayMiss = p[Keys.NOTIF_TEXT_2DAY_MISS]?.ifBlank { AppConfig.DEFAULT_TWO_DAY_MISS_TEXT }
                ?: AppConfig.DEFAULT_TWO_DAY_MISS_TEXT,
            dailyReminderTime = p[Keys.DAILY_REMINDER_TIME] ?: "20:00",
            weightGoalKg = p[Keys.WEIGHT_GOAL] ?: 75f,
            heightGoalCm = p[Keys.HEIGHT_GOAL_CM] ?: 183,
            runwayTargetDate = p[Keys.RUNWAY_TARGET_DATE] ?: AppConfig.DEFAULT_RUNWAY_DATE,
            notificationsEnabled = p[Keys.NOTIFICATIONS_ENABLED] ?: true,
            dailyReminderEnabled = p[Keys.DAILY_REMINDER_ENABLED] ?: true,
            missAlertEnabled = p[Keys.MISS_ALERT_ENABLED] ?: true,
            hapticLevel = runCatching { HapticLevel.valueOf(p[Keys.HAPTIC_LEVEL] ?: "HIGH") }
                .getOrDefault(HapticLevel.HIGH),
            lastEvaluatedDate = p[Keys.LAST_EVALUATED_DATE],
            miss2Armed = p[Keys.MISS2_ARMED] ?: true,
            lastMissAlertDate = p[Keys.LAST_MISS_ALERT_DATE]
        )
    }

    suspend fun getSnapshot(): AccendPrefs = prefsFlow.first()

    suspend fun setOnboarded(value: Boolean) {
        context.accendDataStore.edit { it[Keys.ONBOARDED] = value }
    }

    suspend fun clearAll() {
        context.accendDataStore.edit { it.clear() }
    }

    suspend fun updateTargets(
        screenTargetMinutes: Int? = null,
        readingTarget: Int? = null,
        deepWorkDefaultMinutes: Int? = null,
        savePercent: Int? = null,
        weightGoalKg: Float? = null,
        heightGoalCm: Int? = null,
        runwayTargetDate: String? = null
    ) {
        context.accendDataStore.edit { p ->
            screenTargetMinutes?.let { p[Keys.SCREEN_TARGET] = it.coerceIn(15, 1440) }
            readingTarget?.let { p[Keys.READING_TARGET] = it.coerceIn(1, 500) }
            deepWorkDefaultMinutes?.let { p[Keys.DEEP_WORK_DEFAULT] = it }
            savePercent?.let { p[Keys.SAVE_PERCENT] = it.coerceIn(0, 100) }
            weightGoalKg?.let { p[Keys.WEIGHT_GOAL] = it.coerceIn(25f, 250f) }
            heightGoalCm?.let { p[Keys.HEIGHT_GOAL_CM] = it.coerceIn(100, 240) }
            runwayTargetDate?.let { p[Keys.RUNWAY_TARGET_DATE] = it }
        }
    }

    suspend fun updateNotifications(
        masterEnabled: Boolean? = null,
        dailyEnabled: Boolean? = null,
        dailyTime: String? = null,
        missEnabled: Boolean? = null,
        missText: String? = null
    ) {
        context.accendDataStore.edit { p ->
            masterEnabled?.let { p[Keys.NOTIFICATIONS_ENABLED] = it }
            dailyEnabled?.let { p[Keys.DAILY_REMINDER_ENABLED] = it }
            dailyTime?.let { p[Keys.DAILY_REMINDER_TIME] = it }
            missEnabled?.let { p[Keys.MISS_ALERT_ENABLED] = it }
            missText?.let {
                val trimmed = it.trim().take(160)
                p[Keys.NOTIF_TEXT_2DAY_MISS] = trimmed.ifEmpty { AppConfig.DEFAULT_TWO_DAY_MISS_TEXT }
            }
        }
    }

    suspend fun setHapticLevel(level: HapticLevel) {
        context.accendDataStore.edit { it[Keys.HAPTIC_LEVEL] = level.name }
    }

    suspend fun updateEvaluatorState(
        lastEvaluatedDate: String? = null,
        miss2Armed: Boolean? = null,
        lastMissAlertDate: String? = null
    ) {
        context.accendDataStore.edit { p ->
            if (lastEvaluatedDate != null) p[Keys.LAST_EVALUATED_DATE] = lastEvaluatedDate
            if (miss2Armed != null) p[Keys.MISS2_ARMED] = miss2Armed
            if (lastMissAlertDate != null) p[Keys.LAST_MISS_ALERT_DATE] = lastMissAlertDate
        }
    }

    suspend fun clearEvaluatorState() {
        context.accendDataStore.edit { p ->
            p.remove(Keys.LAST_EVALUATED_DATE)
            p[Keys.MISS2_ARMED] = true
            p.remove(Keys.LAST_MISS_ALERT_DATE)
        }
    }

    suspend fun importPreferences(prefs: AccendPrefs) {
        context.accendDataStore.edit { p ->
            p[Keys.ONBOARDED] = true
            p[Keys.SCREEN_TARGET] = prefs.screenTargetMinutes
            p[Keys.READING_TARGET] = prefs.readingTarget
            p[Keys.DEEP_WORK_DEFAULT] = prefs.deepWorkDefaultMinutes
            p[Keys.SAVE_PERCENT] = prefs.savePercent
            p[Keys.NOTIF_TEXT_2DAY_MISS] = prefs.notifText2DayMiss
            p[Keys.DAILY_REMINDER_TIME] = prefs.dailyReminderTime
            p[Keys.WEIGHT_GOAL] = prefs.weightGoalKg
            p[Keys.HEIGHT_GOAL_CM] = prefs.heightGoalCm
            p[Keys.RUNWAY_TARGET_DATE] = prefs.runwayTargetDate
            p[Keys.NOTIFICATIONS_ENABLED] = prefs.notificationsEnabled
            p[Keys.DAILY_REMINDER_ENABLED] = prefs.dailyReminderEnabled
            p[Keys.MISS_ALERT_ENABLED] = prefs.missAlertEnabled
            p[Keys.HAPTIC_LEVEL] = prefs.hapticLevel.name
            p.remove(Keys.LAST_EVALUATED_DATE)
            p[Keys.MISS2_ARMED] = true
            p.remove(Keys.LAST_MISS_ALERT_DATE)
        }
    }
}
