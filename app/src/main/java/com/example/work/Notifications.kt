package com.example.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.AccendApp
import com.example.AppConfig
import com.example.R
import com.example.data.prefs.AccendPrefs
import com.example.domain.CompletionCalculator
import com.example.util.AccendDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Notifications {
    const val CHANNEL_DAILY = "daily_reminder"
    const val CHANNEL_MISS = "miss_alert"
    private const val ID_DAILY = 1001
    private const val ID_MISS = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val daily = NotificationChannel(
                CHANNEL_DAILY,
                context.getString(R.string.notif_channel_daily_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_daily_desc)
            }
            val miss = NotificationChannel(
                CHANNEL_MISS,
                context.getString(R.string.notif_channel_miss_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_miss_desc)
            }
            nm.createNotificationChannel(daily)
            nm.createNotificationChannel(miss)
        }
    }

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun dailyDeepLinkPendingIntent(context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("accend://daily")).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postDailyReminder(context: Context, dayNumber: Int, pending: Int) {
        if (!hasPermission(context) || pending <= 0) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.accend_brand_mark)
            .setContentTitle("ACCEND · Day $dayNumber")
            .setContentText("Day $dayNumber · $pending left")
            .setAutoCancel(true)
            .setContentIntent(dailyDeepLinkPendingIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_DAILY, notification) }
    }

    fun postTwoDayMissAlert(context: Context, message: String) {
        if (!hasPermission(context)) return
        val text = message.ifBlank { AppConfig.DEFAULT_TWO_DAY_MISS_TEXT }
        val notification = NotificationCompat.Builder(context, CHANNEL_MISS)
            .setSmallIcon(R.drawable.accend_brand_mark)
            .setContentTitle("ACCEND · 2-Day Miss Alert")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(dailyDeepLinkPendingIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_MISS, notification) }
    }
}

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? AccendApp)?.container ?: return Result.success()
        val prefs = container.prefsRepo.getSnapshot()
        if (!prefs.onboarded || !prefs.notificationsEnabled || !prefs.dailyReminderEnabled) {
            return Result.success()
        }
        val user = container.database.userDao().getUser() ?: return Result.success()
        val today = AccendDate.today()
        val joined = AccendDate.parse(user.joinedDate)
        val dayNum = (AccendDate.daysBetween(joined, today) + 1).coerceAtLeast(1)
        val iso = today.toIsoString()

        val exercises = container.database.exerciseDao().forDayOnce(today.weekdaySundayOne)
        val exLogs = container.database.exerciseLogDao().forDateOnce(iso)
        val nutrition = container.database.nutritionDao().forDateOnce(iso)
        val meditations = container.database.mindDao().meditationForDateOnce(iso)
        val screenTime = container.database.mindDao().screenTimeForDateOnce(iso)
        val deepWorks = container.database.mindDao().deepWorkForDateOnce(iso)
        val reading = container.database.mindDao().readingForDateOnce(iso)

        val breakdown = CompletionCalculator.compute(
            exercises, exLogs, nutrition, meditations, screenTime, deepWorks, reading, prefs.readingTarget
        )
        if (breakdown.pendingCount > 0) {
            Notifications.postDailyReminder(applicationContext, dayNum, breakdown.pendingCount)
        }
        return Result.success()
    }
}

class MissCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? AccendApp)?.container ?: return Result.success()
        val prefsBefore = container.prefsRepo.getSnapshot()
        if (!prefsBefore.onboarded) return Result.success()

        val today = AccendDate.today()
        container.dayEvaluator.catchUp(today)

        val prefs = container.prefsRepo.getSnapshot()
        if (!prefs.notificationsEnabled || !prefs.missAlertEnabled || !prefs.miss2Armed) {
            return Result.success()
        }

        val user = container.database.userDao().getUser() ?: return Result.success()
        val joined = AccendDate.parse(user.joinedDate)
        val d1 = today.minusDays(1)
        val d2 = today.minusDays(2)
        if (d2 < joined) return Result.success()

        val log1 = container.database.dayLogDao().get(d1.toIsoString())
        val log2 = container.database.dayLogDao().get(d2.toIsoString())
        if (log1?.missedFlag == true && log2?.missedFlag == true) {
            Notifications.postTwoDayMissAlert(applicationContext, prefs.notifText2DayMiss)
            container.prefsRepo.updateEvaluatorState(
                miss2Armed = false,
                lastMissAlertDate = today.toIsoString()
            )
        }
        return Result.success()
    }
}

object NotificationScheduler {
    private const val WORK_DAILY = "daily_reminder"
    private const val WORK_MISS = "miss_check"

    fun sync(context: Context, prefs: AccendPrefs) {
        val wm = WorkManager.getInstance(context)
        if (!prefs.onboarded || !prefs.notificationsEnabled) {
            wm.cancelUniqueWork(WORK_DAILY)
            wm.cancelUniqueWork(WORK_MISS)
            return
        }

        if (prefs.dailyReminderEnabled) {
            val parts = prefs.dailyReminderTime.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 20
            val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            val delayMs = millisUntilNext(hour, minute)
            val req = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_DAILY, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK_DAILY)
        }

        if (prefs.missAlertEnabled) {
            val delayMs = millisUntilNext(AppConfig.MISS_ALERT_HOUR, AppConfig.MISS_ALERT_MINUTE)
            val req = PeriodicWorkRequestBuilder<MissCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_MISS, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK_MISS)
        }
    }

    private fun millisUntilNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? AccendApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = app.container.prefsRepo.getSnapshot()
                NotificationScheduler.sync(context, prefs)
            } finally {
                pending.finish()
            }
        }
    }
}
