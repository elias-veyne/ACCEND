package com.example

import android.app.Application
import android.content.Context
import com.example.data.db.AccendDatabase
import com.example.data.export.ExportImportManager
import com.example.data.prefs.PrefsRepository
import com.example.data.repo.BodyRepository
import com.example.data.repo.DisciplineRepository
import com.example.data.repo.MoneyRepository
import com.example.data.repo.SeedRepository
import com.example.data.repo.UserRepository
import com.example.domain.DayEvaluator
import com.example.work.Notifications

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AccendDatabase by lazy { AccendDatabase.create(appContext) }
    val prefsRepo: PrefsRepository by lazy { PrefsRepository(appContext) }
    val seedRepo: SeedRepository by lazy { SeedRepository(database) }
    val dayEvaluator: DayEvaluator by lazy { DayEvaluator(database, prefsRepo) }
    val userRepo: UserRepository by lazy { UserRepository(database, prefsRepo, seedRepo) }
    val disciplineRepo: DisciplineRepository by lazy { DisciplineRepository(database, dayEvaluator) }
    val moneyRepo: MoneyRepository by lazy { MoneyRepository(database) }
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
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}
