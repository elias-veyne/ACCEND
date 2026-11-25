package com.accend.app.ui.nav

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.accend.app.ui.AccendViewModel
import com.accend.app.ui.analysis.AnalysisScreen
import com.accend.app.ui.components.AccendBottomBar
import com.accend.app.ui.components.AccendTopBar
import com.accend.app.ui.components.ConfirmActionDialog
import com.accend.app.ui.components.MainTab
import com.accend.app.ui.daily.DailyWorkScreen
import com.accend.app.ui.home.HomeScreen
import com.accend.app.ui.leaderboard.LeaderboardScreen
import com.accend.app.ui.money.MoneyScreen
import com.accend.app.ui.onboarding.OnboardingScreen
import com.accend.app.ui.settings.BodyLogsScreen
import com.accend.app.ui.settings.DataScreen
import com.accend.app.ui.settings.ProgressionScreen
import com.accend.app.ui.settings.SettingsScreen
import com.accend.app.ui.signin.SignInScreen
import com.accend.app.ui.splash.SplashScreen
import com.accend.app.ui.theme.AccendThemeTokens
import com.accend.app.ui.timers.DeepWorkScreen
import com.accend.app.ui.timers.MeditationScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DAILY = "daily"
    const val ANALYSIS = "analysis"
    const val MONEY = "money"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"
    const val MEDITATION = "meditation"
    const val DEEP_WORK = "deepwork?minutes={minutes}"
    const val PROGRESSION = "progression"
    const val SETTINGS_BODY = "settings/body"
    const val SETTINGS_DATA = "settings/data"

    fun deepWork(minutes: Int) = "deepwork?minutes=$minutes"
}

@Composable
fun AccendNavHost(
    viewModel: AccendViewModel,
    openDailyDeepLink: Boolean,
    onConsumedDeepLink: () -> Unit
) {
    val c = AccendThemeTokens.colors
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val prefs by viewModel.prefsFlow.collectAsStateWithLifecycle()
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val dailyState by viewModel.dailyState.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val moneyState by viewModel.moneyState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val progressionRows by viewModel.progressionRows.collectAsStateWithLifecycle()
    val weights by viewModel.allWeights.collectAsStateWithLifecycle()
    val heights by viewModel.allHeights.collectAsStateWithLifecycle()
    val pendingDeleteTx by viewModel.pendingDeleteTx.collectAsStateWithLifecycle()

    val currentMainTab = MainTab.entries.firstOrNull { it.route == currentRoute }
    val showChrome = currentMainTab != null

    fun navigateToTab(tab: MainTab) {
        navController.navigate(tab.route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(openDailyDeepLink, prefs.onboarded, currentRoute) {
        if (openDailyDeepLink && prefs.onboarded && currentRoute != null && currentRoute != Routes.SPLASH) {
            onConsumedDeepLink()
            navigateToTab(MainTab.DAILY)
        }
    }

    Scaffold(
        containerColor = c.bgBase,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (showChrome && currentMainTab != null) {
                AccendTopBar(
                    subtitle = currentMainTab.headerSubtitle,
                    userName = homeState.userName,
                    pfpUri = homeState.pfpUri,
                    hasPendingAlert = settingsState.pendingProgressionsCount > 0,
                    onBellClick = {
                        if (currentMainTab == MainTab.SETTINGS) {
                            showMessage("Daily reminder: ${prefs.dailyReminderTime} · 2-day miss alert active")
                        } else {
                            navigateToTab(MainTab.SETTINGS)
                        }
                    },
                    onAvatarClick = { navigateToTab(MainTab.SETTINGS) }
                )
            }
        },
        bottomBar = {
            if (showChrome) {
                AccendBottomBar(
                    currentRoute = currentRoute,
                    hasProgressionBadge = settingsState.pendingProgressionsCount > 0,
                    onSelectTab = { navigateToTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.bgBase)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        onFinished = {
                            scope.launch {
                                val snap = withContext(Dispatchers.IO) { viewModel.container.prefsRepo.getSnapshot() }
                                // Auth state may still be restoring on a cold start.
                                val signedInUser = withContext(Dispatchers.IO) {
                                    viewModel.container.cloudRepo.awaitAuthUser()
                                }
                                if (signedInUser == null) {
                                    navController.navigate(Routes.SIGN_IN) {
                                        popUpTo(Routes.SPLASH) { inclusive = true }
                                    }
                                    return@launch
                                }
                                viewModel.syncCloudOnce()
                                if (!snap.onboarded) {
                                    // Returning user with wiped local data: restore identity from
                                    // Firestore and skip onboarding instead of overwriting it.
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { viewModel.restoreIdentityIfSignedIn() }
                                    }
                                    if (result.isFailure) {
                                        // Read failed (network/permissions) — never assume new user.
                                        showMessage("Network error. Please try again.")
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.SPLASH) { inclusive = true }
                                        }
                                        return@launch
                                    }
                                    if (result.getOrDefault(false)) {
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.SPLASH) { inclusive = true }
                                        }
                                        return@launch
                                    }
                                }
                                if (snap.onboarded) {
                                    withContext(Dispatchers.IO) {
                                        viewModel.container.seedRepo.seedIfEmpty()
                                        viewModel.container.dayEvaluator.catchUp()
                                    }
                                    val dest = if (openDailyDeepLink) {
                                        onConsumedDeepLink()
                                        Routes.DAILY
                                    } else Routes.HOME
                                    navController.navigate(dest) {
                                        popUpTo(Routes.SPLASH) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Routes.ONBOARDING) {
                                        popUpTo(Routes.SPLASH) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }

                composable(Routes.SIGN_IN) {
                    SignInScreen(
                        onSignIn = { idToken ->
                            viewModel.signIn(
                                idToken = idToken,
                                onSuccess = { restoredIdentity ->
                                    scope.launch {
                                        val snap = withContext(Dispatchers.IO) {
                                            viewModel.container.prefsRepo.getSnapshot()
                                        }
                                        val dest = if (restoredIdentity || snap.onboarded) {
                                            Routes.HOME
                                        } else Routes.ONBOARDING
                                        navController.navigate(dest) {
                                            popUpTo(Routes.SIGN_IN) { inclusive = true }
                                        }
                                    }
                                },
                                onError = { showMessage(it) }
                            )
                        },
                        onMessage = { showMessage(it) }
                    )
                }

                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        container = viewModel.container,
                        onCompleted = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        state = homeState,
                        onOpenDaily = { navigateToTab(MainTab.DAILY) },
                        onOpenAnalysis = { navigateToTab(MainTab.ANALYSIS) }
                    )
                }

                composable(Routes.DAILY) {
                    DailyWorkScreen(
                        state = dailyState,
                        onToggleExercise = { viewModel.toggleExercise(it) },
                        onUpdateNutrition = { viewModel.updateNutrition(it) },
                        onStartMeditation = { navController.navigate(Routes.MEDITATION) },
                        onSaveScreenTime = {
                            viewModel.saveScreenTime(it)
                            showMessage("Screen time logged.")
                        },
                        onStartDeepWork = { mins -> navController.navigate(Routes.deepWork(mins)) },
                        onSaveReadingPages = { viewModel.saveReadingPages(it) },
                        onSaveBody = { w, h ->
                            viewModel.saveBodyMeasurements(w, h)
                            showMessage("Body measurements saved.")
                        },
                        onLockInFeedback = {
                            val pct = dailyState.breakdown.percent
                            showMessage(
                                if (pct == 100) "All protocols locked for Day ${dailyState.dayNumber}."
                                else "Progress saved ($pct% complete · ${dailyState.breakdown.pendingCount} pending)."
                            )
                        }
                    )
                }

                composable(Routes.ANALYSIS) {
                    AnalysisScreen(
                        state = analysisState,
                        onSelectRange = { viewModel.selectAnalysisRange(it) },
                        onDayCellClick = { viewModel.openDayDetail(it) },
                        onDismissDayDetail = { viewModel.dismissDayDetail() },
                        onExportTelemetry = {
                            scope.launch {
                                runCatching {
                                    val uri = withContext(Dispatchers.IO) {
                                        viewModel.container.exportImportManager.exportToShareUri()
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share ACCEND Telemetry"))
                                }.onFailure {
                                    showMessage("Export failed: ${it.message}")
                                }
                            }
                        }
                    )
                }

                composable(Routes.MONEY) {
                    MoneyScreen(
                        state = moneyState,
                        onSaveTransaction = { tx, onSaved -> viewModel.saveTransaction(tx, onSaved) },
                        onDeleteTransaction = { tx ->
                            viewModel.requestDeleteTransaction(tx) { deleted ->
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = "Transaction deleted.",
                                        actionLabel = "UNDO",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreDeletedTransaction(deleted)
                                    }
                                }
                            }
                        },
                        onUpdateImpulseAnswer = { id, ans -> viewModel.updateImpulseAnswer(id, ans) },
                        onMoveSavings = { amt, dir, linkedId ->
                            viewModel.moveSavings(amt, dir, linkedId)
                            showMessage(if (dir == "DEPOSIT") "Moved to Save Jar." else "Withdrawn from Save Jar.")
                        },
                        onAddSkillHours = { hrs, tag ->
                            viewModel.addSkillHours(hrs, tag)
                            showMessage("Skill hours banked.")
                        },
                        onDeleteSkillLog = { viewModel.deleteSkillLog(it) },
                        onShowMessage = { showMessage(it) }
                    )
                }

                composable(Routes.LEADERBOARD) {
                    LeaderboardScreen(state = leaderboardState)
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        state = settingsState,
                        onUpdateProfile = { name, uri, remove ->
                            viewModel.updateProfile(name, uri, remove)
                            showMessage("Profile updated.")
                        },
                        onUpdateNotifications = { m, d, t, miss, txt ->
                            viewModel.updateNotifications(m, d, t, miss, txt)
                        },
                        onUpdateTargets = { sc, rd, dw, sp, wg, hg, rw ->
                            viewModel.updateTargets(sc, rd, dw, sp, wg, hg, rw)
                        },
                        onSetHapticLevel = { viewModel.setHapticLevel(it) },
                        onOpenProgression = { navController.navigate(Routes.PROGRESSION) },
                        onOpenBodyLogs = { navController.navigate(Routes.SETTINGS_BODY) },
                        onOpenDataScreen = { navController.navigate(Routes.SETTINGS_DATA) },
                        onSaveProtocolConfirmed = {
                            viewModel.syncSchedulesNow()
                            showMessage("Discipline protocol saved & schedules synced.")
                        },
                        onDeleteAccount = { idToken, onDone ->
                            viewModel.deleteAccount(idToken) { error ->
                                if (error == null) {
                                    navController.navigate(Routes.SIGN_IN) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    showMessage(error)
                                }
                            }
                        }
                    )
                }

                composable(Routes.MEDITATION) {
                    MeditationScreen(
                        weekNumber = dailyState.meditationWeek,
                        targetMinutes = dailyState.meditationTargetMin,
                        hapticLevel = prefs.hapticLevel,
                        onCompleted = { mins, restlessness ->
                            viewModel.logMeditation(mins, restlessness)
                            navController.popBackStack()
                            showMessage("Meditation logged ($mins min · $restlessness).")
                        },
                        onExit = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.DEEP_WORK,
                    arguments = listOf(navArgument("minutes") { type = NavType.IntType; defaultValue = 30 })
                ) { entry ->
                    val mins = entry.arguments?.getInt("minutes") ?: prefs.deepWorkDefaultMinutes
                    DeepWorkScreen(
                        initialMinutes = mins,
                        hapticLevel = prefs.hapticLevel,
                        onLogSession = { loggedMins, completed ->
                            viewModel.logDeepWork(loggedMins, completed)
                            navController.popBackStack()
                            showMessage(
                                if (completed) "Deep Work complete ($loggedMins min)."
                                else "Logged $loggedMins min partial session."
                            )
                        },
                        onExit = { navController.popBackStack() }
                    )
                }

                composable(Routes.PROGRESSION) {
                    ProgressionScreen(
                        rows = progressionRows,
                        onLevelUp = {
                            viewModel.levelUpExercise(it)
                            showMessage("Exercise leveled up.")
                        },
                        onLevelDown = {
                            viewModel.levelDownExercise(it)
                            showMessage("Exercise stepped down.")
                        },
                        onSnooze = {
                            viewModel.snoozeProgression(it)
                            showMessage("Suggestion snoozed until next completed session.")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS_BODY) {
                    BodyLogsScreen(
                        weights = weights,
                        heights = heights,
                        onAddWeight = {
                            viewModel.saveBodyMeasurements(it, null)
                            showMessage("Weight logged.")
                        },
                        onDeleteWeight = { viewModel.deleteWeight(it) },
                        onAddHeight = {
                            viewModel.saveBodyMeasurements(null, it)
                            showMessage("Height logged.")
                        },
                        onDeleteHeight = { viewModel.deleteHeight(it) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS_DATA) {
                    DataScreen(
                        exportImportManager = viewModel.container.exportImportManager,
                        onShowMessage = { showMessage(it) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    // Linked-Savings Deletion Confirmation Dialog (PRD §9.8)
    pendingDeleteTx?.let { (_, linkedCount) ->
        ConfirmActionDialog(
            title = "Delete Linked Savings Moves?",
            body = "This inflow has $linkedCount linked Save Jar deposit(s). Delete the linked jar deposit(s) as well?",
            confirmText = "Delete Both",
            isDanger = true,
            onConfirm = {
                viewModel.confirmDeleteTransactionWithLinked(deleteLinked = true) { showMessage(it) }
            },
            onDismiss = {
                viewModel.dismissPendingDeleteTx()
            }
        )
    }
}
