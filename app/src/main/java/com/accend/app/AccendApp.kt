package com.accend.app

import android.app.Application
import android.content.Context
import com.accend.app.data.cloud.CloudRepository
import com.accend.app.data.db.AccendDatabase
import com.accend.app.data.export.ExportImportManager
import com.accend.app.data.prefs.PrefsRepository
import com.accend.app.data.repo.BodyRepository
import com.accend.app.data.repo.DisciplineRepository
import com.accend.app.data.repo.MoneyRepository
import com.accend.app.data.repo.SeedRepository
import com.accend.app.data.repo.UserRepository
import com.accend.app.domain.DayEvaluator
import com.accend.app.work.Notifications
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AccendDatabase by lazy { AccendDatabase.create(appContext) }
    val cloudRepo: CloudRepository by lazy { CloudRepository(database) }
    val prefsRepo: PrefsRepository by lazy { PrefsRepository(appContext) }
    val seedRepo: SeedRepository by lazy { SeedRepository(database) }
    val dayEvaluator: DayEvaluator by lazy { DayEvaluator(database, prefsRepo) }
    val userRepo: UserRepository by lazy { UserRepository(database, prefsRepo, seedRepo) }
    val disciplineRepo: DisciplineRepository by lazy { DisciplineRepository(database, dayEvaluator, cloudRepo) }
    val moneyRepo: MoneyRepository by lazy { MoneyRepository(database, cloudRepo) }
    val bodyRepo: BodyRepository by lazy { BodyRepository(database) }
    val exportImportManager: ExportImportManager by lazy {
        ExportImportManager(appContext, database, prefsRepo, seedRepo, dayEvaluator)
    }
}

class AccendApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this)
        container = AppContainer(this)
        Notifications.createChannels(this)
        // Keep the FCM token on the user document so server-triggered alerts keep working.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                container.cloudRepo.refreshFcmToken(token)
            }
        }
    }
}
