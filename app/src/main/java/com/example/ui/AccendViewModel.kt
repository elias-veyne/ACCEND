package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AccendApp
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
import com.example.data.db.ProgressionLevel
import com.example.data.db.ReadingLogEntity
import com.example.data.db.ScreenTimeLogEntity
import com.example.data.db.SkillLogEntity
import com.example.data.db.UserEntity
import com.example.data.db.WeightLogEntity
import com.example.data.prefs.AccendPrefs
import com.example.data.repo.DayDetailSnapshot
import com.example.data.repo.MoneySummary
import com.example.domain.CompletionCalculator
import com.example.domain.MeditationLadder
import com.example.domain.MindScoreCalculator
import com.example.domain.ProgressionRules
import com.example.domain.StreakCalculator
import com.example.domain.VolumeCalculator
import com.example.ui.analysis.AnalysisRange
import com.example.ui.analysis.AnalysisUiState
import com.example.ui.charts.GridCellData
import com.example.ui.charts.LineChartPoint
import com.example.ui.charts.RadarAxisPoint
import com.example.ui.daily.DailyExerciseRowUi
import com.example.ui.daily.DailyWorkUiState
import com.example.ui.home.HomeUiState
import com.example.ui.money.MoneyUiState
import com.example.ui.settings.ProgressionRowUi
import com.example.ui.settings.SettingsUiState
import com.example.util.AccendDate
import com.example.util.Format
import com.example.util.HapticLevel
import com.example.util.ImageUtils
import com.example.work.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private data class CoreBundle(
    val user: UserEntity?,
    val prefs: AccendPrefs,
    val today: AccendDate,
    val exercises: List<ExerciseEntity>,
    val progressions: List<ExerciseProgressionEntity>,
    val exLogs: List<ExerciseLogEntity>,
    val dayLogs: List<DayLogEntity>,
    val meditations: List<MeditationLogEntity>,
    val deepWorks: List<DeepWorkLogEntity>,
    val readings: List<ReadingLogEntity>,
    val screenTimes: List<ScreenTimeLogEntity>,
    val weights: List<WeightLogEntity>,
    val heights: List<HeightLogEntity>
)

class AccendViewModel(application: Application) : AndroidViewModel(application) {
    val container = (application as AccendApp).container

    private val _today = MutableStateFlow(AccendDate.today())
    val today: StateFlow<AccendDate> = _today

    private val _selectedRange = MutableStateFlow(AnalysisRange.D30)
    private val _selectedDayDetail = MutableStateFlow<Pair<Int, DayDetailSnapshot>?>(null)

    private val _pendingDeleteTx = MutableStateFlow<Pair<MoneyTransactionEntity, Int>?>(null)
    val pendingDeleteTx: StateFlow<Pair<MoneyTransactionEntity, Int>?> = _pendingDeleteTx

    init {
        // Midnight rollover ticker (re-checks every 30s)
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                val now = AccendDate.today()
                if (now != _today.value) {
                    _today.value = now
                    withContext(Dispatchers.IO) { container.dayEvaluator.catchUp(now) }
                }
            }
        }
    }

    fun onResumeApp() {
        val now = AccendDate.today()
        if (now != _today.value) _today.value = now
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = container.prefsRepo.getSnapshot()
            if (prefs.onboarded) {
                container.seedRepo.seedIfEmpty()
                container.dayEvaluator.catchUp(now)
                NotificationScheduler.sync(getApplication(), prefs)
            }
        }
    }

    val prefsFlow: StateFlow<AccendPrefs> = container.prefsRepo.prefsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), AccendPrefs()
    )

    private val corePart1 = combine(
        container.userRepo.userFlow,
        container.prefsRepo.prefsFlow,
        _today,
        container.disciplineRepo.allExercises(),
        container.disciplineRepo.allProgressions()
    ) { user, prefs, today, exercises, progs ->
        listOf(user, prefs, today, exercises, progs)
    }

    private val corePart2 = combine(
        container.disciplineRepo.allExerciseLogs(),
        container.disciplineRepo.allDayLogs(),
        container.disciplineRepo.allMeditations(),
        container.disciplineRepo.allDeepWork(),
        container.disciplineRepo.allReading()
    ) { exLogs, dayLogs, meds, deeps, reads ->
        listOf(exLogs, dayLogs, meds, deeps, reads)
    }

    private val corePart3 = combine(
        container.disciplineRepo.allScreenTime(),
        container.bodyRepo.allWeights,
        container.bodyRepo.allHeights
    ) { screens, weights, heights ->
        Triple(screens, weights, heights)
    }

    @Suppress("UNCHECKED_CAST")
    private val coreBundleFlow = combine(corePart1, corePart2, corePart3) { p1, p2, p3 ->
        CoreBundle(
            user = p1[0] as UserEntity?,
            prefs = p1[1] as AccendPrefs,
            today = p1[2] as AccendDate,
            exercises = p1[3] as List<ExerciseEntity>,
            progressions = p1[4] as List<ExerciseProgressionEntity>,
            exLogs = p2[0] as List<ExerciseLogEntity>,
            dayLogs = p2[1] as List<DayLogEntity>,
            meditations = p2[2] as List<MeditationLogEntity>,
            deepWorks = p2[3] as List<DeepWorkLogEntity>,
            readings = p2[4] as List<ReadingLogEntity>,
            screenTimes = p3.first,
            weights = p3.second,
            heights = p3.third
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CoreBundle(null, AccendPrefs(), AccendDate.today(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val todayNutritionFlow = _today.flatMapLatest { d ->
        container.disciplineRepo.nutritionForDate(d.toIsoString())
    }

    val homeState: StateFlow<HomeUiState> = combine(coreBundleFlow, todayNutritionFlow) { b, todayNutr ->
        val today = b.today
        val todayIso = today.toIsoString()
        val joined = AccendDate.parseOrNull(b.user?.joinedDate) ?: today
        val dayNum = (AccendDate.daysBetween(joined, today) + 1).coerceAtLeast(1)

        val todayExercises = b.exercises.filter { it.dayOfWeek == today.weekdaySundayOne }.sortedBy { it.orderInDay }
        val todayExLogs = b.exLogs.filter { it.date == todayIso }
        val todayMeds = b.meditations.filter { it.date == todayIso }
        val todayDeeps = b.deepWorks.filter { it.date == todayIso }
        val todayRead = b.readings.firstOrNull { it.date == todayIso }
        val todayScreen = b.screenTimes.firstOrNull { it.date == todayIso }

        val breakdown = CompletionCalculator.compute(
            todayExercises, todayExLogs, todayNutr, todayMeds, todayScreen, todayDeeps, todayRead, b.prefs.readingTarget
        )

        val medByDate = b.meditations.groupBy { it.date }.mapValues { it.value.sumOf { m -> m.minutes } }
        val deepCompByDate = b.deepWorks.filter { it.completed }.groupBy { it.date }.mapValues { it.value.sumOf { d -> d.minutes } }
        val readByDate = b.readings.associate { it.date to it.pages }
        val screenByDate = b.screenTimes.associate { it.date to it.minutes }

        val mindToday = MindScoreCalculator.rollingMindScore(today, joined, medByDate, deepCompByDate, readByDate, screenByDate, b.prefs)
        val mindYesterday = if (today > joined) {
            MindScoreCalculator.rollingMindScore(today.minusDays(1), joined, medByDate, deepCompByDate, readByDate, screenByDate, b.prefs)
        } else mindToday

        val dayLogMap = b.dayLogs.associateBy { it.date }
        val start30 = today.minusDays(29)
        val gridCells = (0 until 30).map { offset ->
            val d = start30.plusDays(offset)
            val iso = d.toIsoString()
            val pct = if (d == today) breakdown.percent else (dayLogMap[iso]?.completionPercent ?: 0)
            GridCellData(
                date = d,
                completionPercent = pct,
                isBeforeJoin = d < joined,
                isFuture = d > today,
                isToday = d == today
            )
        }
        val validCells = gridCells.filter { !it.isBeforeJoin && !it.isFuture }
        val activeDays = validCells.count { it.completionPercent > 0 }
        val avgPct = if (validCells.isEmpty()) 0 else validCells.map { it.completionPercent }.average().roundToInt()
        val perfectDays = validCells.count { it.completionPercent == 100 }

        // Streaks
        val exByDow = b.exercises.groupBy { it.dayOfWeek }
        val exLogsByDate = b.exLogs.filter { it.done }.groupBy { it.date }.mapValues { it.value.map { l -> l.exerciseId }.toSet() }
        val physDoneDates = exLogsByDate.filter { (iso, doneIds) ->
            val dow = AccendDate.parse(iso).weekdaySundayOne
            val req = exByDow[dow].orEmpty().map { it.id }.toSet()
            req.isNotEmpty() && req.all { it in doneIds }
        }.keys
        val medDoneDates = b.meditations.map { it.date }.toSet()
        val deepDoneDates = b.deepWorks.filter { it.completed }.map { it.date }.toSet()

        HomeUiState(
            userName = b.user?.name ?: "Operator",
            pfpUri = b.user?.pfpUri,
            dayNumber = dayNum,
            today = today,
            mindScore = mindToday,
            mindScoreDelta = mindToday - mindYesterday,
            breakdown = breakdown,
            meditationTargetMin = MeditationLadder.targetMinutesForDay(dayNum),
            readingTargetPages = b.prefs.readingTarget,
            gridCells = gridCells,
            activeDays30 = activeDays,
            avgPercent30 = avgPct,
            perfectDays30 = perfectDays,
            physicalStreak = StreakCalculator.calculate(joined, today, physDoneDates),
            meditationStreak = StreakCalculator.calculate(joined, today, medDoneDates),
            deepWorkStreak = StreakCalculator.calculate(joined, today, deepDoneDates)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val dailyState: StateFlow<DailyWorkUiState> = combine(coreBundleFlow, todayNutritionFlow, homeState) { b, todayNutr, home ->
        val today = b.today
        val todayIso = today.toIsoString()
        val progMap = b.progressions.associateBy { it.exerciseId }
        val todayExercises = b.exercises.filter { it.dayOfWeek == today.weekdaySundayOne }.sortedBy { it.orderInDay }
        val todayExLogs = b.exLogs.filter { it.date == todayIso }.associateBy { it.exerciseId }

        val rows = todayExercises.map { ex ->
            val levels = progMap[ex.id]?.let { ProgressionLevel.parseList(it.levelsList) }.orEmpty()
            DailyExerciseRowUi(
                exercise = ex,
                done = todayExLogs[ex.id]?.done == true,
                levelsCount = levels.size.coerceAtLeast(1),
                isCompound = levels.size > 1
            )
        }

        val latestWeight = b.weights.firstOrNull()
        val latestHeight = b.heights.firstOrNull()
        val weightDue = latestWeight == null || AccendDate.daysBetween(AccendDate.parse(latestWeight.date), today) >= 7
        val heightDue = latestHeight == null || AccendDate.daysBetween(AccendDate.parse(latestHeight.date), today) >= 30

        val todayMeds = b.meditations.filter { it.date == todayIso }
        val todayDeeps = b.deepWorks.filter { it.date == todayIso }
        val todayRead = b.readings.firstOrNull { it.date == todayIso }
        val todayScreen = b.screenTimes.firstOrNull { it.date == todayIso }

        DailyWorkUiState(
            dayNumber = home.dayNumber,
            today = today,
            exercises = rows,
            nutrition = todayNutr ?: NutritionLogEntity(date = todayIso),
            meditationTargetMin = MeditationLadder.targetMinutesForDay(home.dayNumber),
            meditationWeek = MeditationLadder.weekNumber(home.dayNumber),
            meditationLoggedMin = todayMeds.sumOf { it.minutes },
            screenTimeLoggedMin = todayScreen?.minutes,
            screenTimeTargetMin = b.prefs.screenTargetMinutes,
            deepWorkDefaultMin = b.prefs.deepWorkDefaultMinutes,
            deepWorkTotalMin = todayDeeps.sumOf { it.minutes },
            deepWorkCompletedToday = todayDeeps.any { it.completed },
            readingPages = todayRead?.pages ?: 0,
            readingTarget = b.prefs.readingTarget,
            todayVolume = todayExLogs.values.filter { it.done }.sumOf { it.volumeSnapshot },
            currentStreak = maxOf(home.physicalStreak.current, home.meditationStreak.current, home.deepWorkStreak.current),
            breakdown = home.breakdown,
            bodyReminderDue = weightDue || heightDue,
            latestWeightKg = latestWeight?.kg,
            latestHeightCm = latestHeight?.cm,
            hapticLevel = b.prefs.hapticLevel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyWorkUiState())

    val analysisState: StateFlow<AnalysisUiState> = combine(
        coreBundleFlow,
        homeState,
        _selectedRange,
        _selectedDayDetail
    ) { b, home, range, dayDetailPair ->
        val today = b.today
        val joined = AccendDate.parseOrNull(b.user?.joinedDate) ?: today
        val daysWindow = range.days
        val startWindow = today.minusDays(daysWindow - 1).let { if (it < joined) joined else it }
        val datesWindow = AccendDate.dateRange(startWindow, today)

        // 1. Muscle Radar (last 30 days per PRD §6.3)
        val radarStart = today.minusDays(29).toIsoString()
        val exGroupById = b.exercises.associate { it.id to it.muscleGroup }
        val volByGroup = mutableMapOf<MuscleGroup, Double>()
        b.exLogs.filter { it.done && it.date >= radarStart }.forEach { log ->
            val grp = exGroupById[log.exerciseId] ?: return@forEach
            if (grp != MuscleGroup.RECOVERY) {
                volByGroup[grp] = (volByGroup[grp] ?: 0.0) + log.volumeSnapshot
            }
        }
        val maxVol9 = (VolumeCalculator.radarGroups.maxOfOrNull { volByGroup[it] ?: 0.0 } ?: 0.0).coerceAtLeast(1.0)
        val radar9 = VolumeCalculator.radarGroups.map { g ->
            val v = volByGroup[g] ?: 0.0
            RadarAxisPoint(g.shortLabel, (v / maxVol9).toFloat(), v)
        }
        // Executive 4-cardinal radar (CHEST/PUSH, BACK/PULL, LEGS, CORE) matching screen__2
        val chestVol = (volByGroup[MuscleGroup.CHEST_PUSH] ?: 0.0) + (volByGroup[MuscleGroup.TRICEPS] ?: 0.0) + (volByGroup[MuscleGroup.SHOULDERS] ?: 0.0)
        val backVol = (volByGroup[MuscleGroup.BACK_PULL] ?: 0.0) + (volByGroup[MuscleGroup.BICEPS] ?: 0.0) + (volByGroup[MuscleGroup.NECK] ?: 0.0)
        val legsVol = volByGroup[MuscleGroup.LEGS] ?: 0.0
        val coreVol = (volByGroup[MuscleGroup.CORE] ?: 0.0) + (volByGroup[MuscleGroup.FOREARMS] ?: 0.0)
        val maxVol4 = maxOf(chestVol, backVol, legsVol, coreVol, 1.0)
        val radar4 = listOf(
            RadarAxisPoint("CHEST", (chestVol / maxVol4).toFloat(), chestVol),
            RadarAxisPoint("BACK", (backVol / maxVol4).toFloat(), backVol),
            RadarAxisPoint("LEGS", (legsVol / maxVol4).toFloat(), legsVol),
            RadarAxisPoint("CORE", (coreVol / maxVol4).toFloat(), coreVol)
        )
        val totalVol30 = volByGroup.values.sum()
        val peakGroup = radar9.maxByOrNull { it.rawVolume }?.takeIf { it.rawVolume > 0.0 }?.label ?: "—"

        // 2. Mind Score series
        val medByDate = b.meditations.groupBy { it.date }.mapValues { it.value.sumOf { m -> m.minutes } }
        val deepCompByDate = b.deepWorks.filter { it.completed }.groupBy { it.date }.mapValues { it.value.sumOf { d -> d.minutes } }
        val readByDate = b.readings.associate { it.date to it.pages }
        val screenByDate = b.screenTimes.associate { it.date to it.minutes }

        val mindPoints = datesWindow.map { d ->
            val score = MindScoreCalculator.rollingMindScore(d, joined, medByDate, deepCompByDate, readByDate, screenByDate, b.prefs)
            LineChartPoint(d.formatShort().uppercase(), score.toFloat(), "${d.formatShort()}: $score pts")
        }
        val mindAvg = if (mindPoints.isEmpty()) 0.0 else mindPoints.map { it.value.toDouble() }.average()

        // 3. Screen time series (hours)
        val screenPoints = datesWindow.mapNotNull { d ->
            val mins = screenByDate[d.toIsoString()] ?: return@mapNotNull null
            val hrs = mins / 60f
            LineChartPoint(d.formatShort().uppercase(), hrs, "${d.formatShort()}: ${Format.screenTime(mins)}")
        }

        // 4. Meditation series
        val medPoints = datesWindow.mapNotNull { d ->
            val mins = medByDate[d.toIsoString()] ?: return@mapNotNull null
            LineChartPoint(d.formatShort().uppercase(), mins.toFloat(), "${d.formatShort()}: $mins min")
        }

        // 5. Deep Work weekly bar chart (last 12 ISO weeks)
        val currentWeekStart = today.startOfIsoWeek()
        val deepAllByDate = b.deepWorks.groupBy { it.date }.mapValues { it.value.sumOf { d -> d.minutes } }
        val deepWeekly = (11 downTo 0).map { weeksAgo ->
            val wkStart = currentWeekStart.minusDays(weeksAgo * 7)
            val wkEnd = wkStart.plusDays(6)
            val sum = AccendDate.dateRange(wkStart, wkEnd).sumOf { deepAllByDate[it.toIsoString()] ?: 0 }
            LineChartPoint(wkStart.formatShort().uppercase(), sum.toFloat(), "Wk ${wkStart.formatShort()}: $sum min")
        }

        // 6. Reading series
        val readPoints = datesWindow.mapNotNull { d ->
            val pgs = readByDate[d.toIsoString()] ?: return@mapNotNull null
            LineChartPoint(d.formatShort().uppercase(), pgs.toFloat(), "${d.formatShort()}: $pgs pg")
        }

        // 7. Weight weekly series (last 12 weeks, latest entry per week)
        val weightsAsc = b.weights.sortedBy { it.date }
        val weightWeekly = (11 downTo 0).mapNotNull { weeksAgo ->
            val wkStart = currentWeekStart.minusDays(weeksAgo * 7)
            val wkEnd = wkStart.plusDays(6)
            val latest = weightsAsc.lastOrNull {
                val parsed = AccendDate.parse(it.date)
                parsed >= wkStart && parsed <= wkEnd
            } ?: return@mapNotNull null
            LineChartPoint(wkStart.formatShort().uppercase(), latest.kg.toFloat(), "${latest.date}: ${Format.decimal1(latest.kg)} kg")
        }

        // 8. Height monthly series (last 12 months)
        val heightMonthly = b.heights.sortedBy { it.date }
            .groupBy { AccendDate.parse(it.date).yearMonthKey() }
            .entries.toList().takeLast(12)
            .map { (ym, list) ->
                val latest = list.last()
                LineChartPoint(ym, latest.cm.toFloat(), "${latest.date}: ${Format.decimal1(latest.cm)} cm")
            }

        // 9. Journey-anchored 30-day pager blocks (PRD §6.3)
        val dayLogMap = b.dayLogs.associateBy { it.date }
        val totalJourneyDays = max(1, AccendDate.daysBetween(joined, today) + 1)
        val numBlocks = ((totalJourneyDays - 1) / 30) + 1
        val pagerBlocks = (0 until numBlocks).map { blk ->
            val startDayNum = blk * 30 + 1
            val endDayNum = startDayNum + 29
            val title = "DAYS $startDayNum–$endDayNum"
            val cells = (0 until 30).map { offset ->
                val d = joined.plusDays(blk * 30 + offset)
                val iso = d.toIsoString()
                val pct = if (d == today) home.breakdown.percent else (dayLogMap[iso]?.completionPercent ?: 0)
                GridCellData(
                    date = d,
                    completionPercent = pct,
                    isBeforeJoin = false,
                    isFuture = d > today,
                    isToday = d == today
                )
            }
            title to cells
        }

        val totalPoints = b.exLogs.size + b.meditations.size + b.deepWorks.size + b.readings.size + b.screenTimes.size
        val correlationText = if (totalPoints < 5) {
            "Complete at least 5 physical & cognitive protocols to calibrate your kinetic vs. neuro-control telemetry."
        } else {
            val activeWorkoutDays = b.exLogs.filter { it.done }.map { it.date }.toSet().size
            "Across $activeWorkoutDays active training days, your Execution Index holds at ${home.avgPercent30}% with a Mind Score baseline of ${Format.decimal1(mindAvg)}."
        }

        val neuroDelta = if (mindPoints.size >= 2) {
            val first = mindPoints.first().value.coerceAtLeast(1f)
            val last = mindPoints.last().value
            ((last - first) / first) * 100.0
        } else 0.0

        AnalysisUiState(
            selectedRange = range,
            neuroResilience = home.mindScore,
            neuroDeltaPercent = neuroDelta,
            executionIndex = home.avgPercent30,
            totalDataPoints = totalPoints,
            radarAxes9 = radar9,
            radarAxes4 = radar4,
            totalVolume30d = totalVol30,
            peakMuscleLabel = peakGroup,
            mindScoreSeries = mindPoints,
            mindScoreAvg = mindAvg,
            screenTimeSeries = screenPoints,
            screenTargetHours = b.prefs.screenTargetMinutes / 60f,
            todayScreenMinutes = screenByDate[today.toIsoString()],
            meditationSeries = medPoints,
            deepWorkWeeklySeries = deepWeekly,
            readingSeries = readPoints,
            weightWeeklySeries = weightWeekly,
            weightGoalKg = b.prefs.weightGoalKg,
            heightMonthlySeries = heightMonthly,
            heightGoalCm = b.prefs.heightGoalCm,
            pagerBlocks = pagerBlocks,
            correlationSummary = correlationText,
            selectedDayDetail = dayDetailPair?.second,
            selectedDayNumber = dayDetailPair?.first ?: 1
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val funWeekFlow = _today.flatMapLatest { d ->
        val thisStart = d.startOfIsoWeek()
        val thisEnd = d.endOfIsoWeek()
        val prevStart = thisStart.minusDays(7)
        val prevEnd = thisStart.minusDays(1)
        combine(
            container.moneyRepo.funSpendingBetween(thisStart, thisEnd),
            container.moneyRepo.funSpendingBetween(prevStart, prevEnd)
        ) { cur, prev -> cur to prev }
    }

    val moneyState: StateFlow<MoneyUiState> = combine(
        container.moneyRepo.summary,
        container.moneyRepo.ledger,
        funWeekFlow,
        combine(
            container.moneyRepo.totalSkillHours,
            container.moneyRepo.recentSkills,
            container.moneyRepo.recentSkillTags
        ) { total, recent, tags -> Triple(total, recent, tags) },
        combine(container.prefsRepo.prefsFlow, _today) { p, t -> p to t }
    ) { sum: MoneySummary, ledger, funPair, skillTriple, prefsToday ->
        val (prefs, today) = prefsToday
        val targetDate = AccendDate.parseOrNull(prefs.runwayTargetDate) ?: AccendDate.parse("2027-06-01")
        val runwayDays = AccendDate.daysBetween(today, targetDate).coerceAtLeast(0)
        MoneyUiState(
            balance = sum.balance,
            savedJar = sum.savedJar,
            savePercent = prefs.savePercent,
            funThisWeek = funPair.first,
            funLastWeek = funPair.second,
            totalSkillHours = skillTriple.first,
            recentSkills = skillTriple.second,
            recentSkillTags = skillTriple.third,
            runwayDays = runwayDays,
            runwayTargetDate = prefs.runwayTargetDate,
            transactions = ledger,
            today = today
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoneyUiState())

    val progressionRows: StateFlow<List<ProgressionRowUi>> = coreBundleFlow.combine(_today) { b, today ->
        val joined = AccendDate.parseOrNull(b.user?.joinedDate) ?: today
        val progMap = b.progressions.associateBy { it.exerciseId }
        val logsByEx = b.exLogs.groupBy { it.exerciseId }
        b.exercises.map { ex ->
            val levels = progMap[ex.id]?.let { ProgressionLevel.parseList(it.levelsList) }.orEmpty()
            val doneLogs = logsByEx[ex.id].orEmpty().filter { it.done }
            ProgressionRowUi(
                exercise = ex,
                levels = levels,
                canLevelUp = ProgressionRules.canLevelUp(ex, levels),
                canLevelDown = ProgressionRules.canLevelDown(ex, levels),
                nextPreview = ProgressionRules.nextLevelPreview(ex, levels),
                isReady = ProgressionRules.isReadyToLevelUp(ex, levels, doneLogs, joined, today)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsState: StateFlow<SettingsUiState> = combine(
        coreBundleFlow,
        moneyState,
        progressionRows
    ) { b, money, progs ->
        val joined = AccendDate.parseOrNull(b.user?.joinedDate) ?: b.today
        val dayNum = (AccendDate.daysBetween(joined, b.today) + 1).coerceAtLeast(1)
        SettingsUiState(
            userName = b.user?.name ?: "Operator",
            pfpUri = b.user?.pfpUri,
            dayNumber = dayNum,
            savedJar = money.savedJar,
            prefs = b.prefs,
            pendingProgressionsCount = progs.count { it.isReady }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val allWeights: StateFlow<List<WeightLogEntity>> = container.bodyRepo.allWeights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHeights: StateFlow<List<HeightLogEntity>> = container.bodyRepo.allHeights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Actions ---
    fun selectAnalysisRange(range: AnalysisRange) {
        _selectedRange.value = range
    }

    fun openDayDetail(cell: GridCellData) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = container.prefsRepo.getSnapshot()
            val user = container.userRepo.getUser()
            val joined = AccendDate.parseOrNull(user?.joinedDate) ?: _today.value
            val dayNum = (AccendDate.daysBetween(joined, cell.date) + 1).coerceAtLeast(1)
            val snap = container.disciplineRepo.getDayDetailSnapshot(cell.date, prefs.readingTarget)
            _selectedDayDetail.value = dayNum to snap
        }
    }

    fun dismissDayDetail() {
        _selectedDayDetail.value = null
    }

    fun toggleExercise(ex: ExerciseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            container.disciplineRepo.toggleExercise(_today.value, ex)
        }
    }

    fun updateNutrition(transform: (NutritionLogEntity) -> NutritionLogEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            container.disciplineRepo.updateNutrition(_today.value, transform)
        }
    }

    fun saveScreenTime(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            container.disciplineRepo.saveScreenTime(_today.value, minutes)
        }
    }

    fun saveReadingPages(pages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            container.disciplineRepo.saveReadingPages(_today.value, pages)
        }
    }

    fun logMeditation(minutes: Int, restlessness: String) {
        viewModelScope.launch(Dispatchers.IO) {
            container.disciplineRepo.logMeditation(AccendDate.today(), minutes, restlessness)
        }
    }

    fun logDeepWork(minutes: Int, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            container.disciplineRepo.logDeepWork(AccendDate.today(), minutes, completed)
        }
    }

    fun saveBodyMeasurements(weightKg: Double?, heightCm: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            val d = _today.value
            weightKg?.let { container.bodyRepo.upsertWeight(d, it) }
            heightCm?.let { container.bodyRepo.upsertHeight(d, it) }
        }
    }

    fun deleteWeight(log: WeightLogEntity) {
        viewModelScope.launch(Dispatchers.IO) { container.bodyRepo.deleteWeight(log) }
    }

    fun deleteHeight(log: HeightLogEntity) {
        viewModelScope.launch(Dispatchers.IO) { container.bodyRepo.deleteHeight(log) }
    }

    fun levelUpExercise(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { container.disciplineRepo.levelUpExercise(id, _today.value) }
    }

    fun levelDownExercise(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { container.disciplineRepo.levelDownExercise(id, _today.value) }
    }

    fun snoozeProgression(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { container.disciplineRepo.snoozeProgressionSuggestion(id, _today.value) }
    }

    fun saveTransaction(tx: MoneyTransactionEntity, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val savedId = withContext(Dispatchers.IO) {
                if (tx.id == 0L) container.moneyRepo.insertTransaction(tx)
                else {
                    container.moneyRepo.updateTransaction(tx)
                    tx.id
                }
            }
            onSaved(savedId)
        }
    }

    fun requestDeleteTransaction(tx: MoneyTransactionEntity, onDeletedImmediate: (MoneyTransactionEntity) -> Unit) {
        viewModelScope.launch {
            val linked = withContext(Dispatchers.IO) { container.moneyRepo.linkedSavingsFor(tx.id) }
            if (linked.isNotEmpty()) {
                _pendingDeleteTx.value = tx to linked.size
            } else {
                withContext(Dispatchers.IO) { container.moneyRepo.deleteTransaction(tx, false) }
                onDeletedImmediate(tx)
            }
        }
    }

    fun confirmDeleteTransactionWithLinked(deleteLinked: Boolean, onDone: (String) -> Unit) {
        val pair = _pendingDeleteTx.value ?: return
        _pendingDeleteTx.value = null
        viewModelScope.launch {
            val tx = pair.first
            val currentBal = moneyState.value.balance
            if (!deleteLinked && tx.type == "IN" && currentBal - tx.amount < 0L) {
                onDone("Blocked: deleting without linked jar deposits would make balance negative.")
                return@launch
            }
            withContext(Dispatchers.IO) { container.moneyRepo.deleteTransaction(tx, deleteLinked) }
            onDone("Entry deleted.")
        }
    }

    fun dismissPendingDeleteTx() {
        _pendingDeleteTx.value = null
    }

    fun restoreDeletedTransaction(tx: MoneyTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            container.moneyRepo.insertTransaction(tx.copy(id = 0L))
        }
    }

    fun updateImpulseAnswer(txId: Long, answer: String?) {
        viewModelScope.launch(Dispatchers.IO) { container.moneyRepo.updateImpulseAnswer(txId, answer) }
    }

    fun moveSavings(amount: Long, direction: String, linkedTxId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            container.moneyRepo.moveSavings(_today.value, amount, direction, linkedTxId)
        }
    }

    fun addSkillHours(hours: Double, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            container.moneyRepo.addSkillHours(_today.value, hours, tag)
        }
    }

    fun deleteSkillLog(log: SkillLogEntity) {
        viewModelScope.launch(Dispatchers.IO) { container.moneyRepo.deleteSkillLog(log) }
    }

    fun updateProfile(name: String, newPhotoUri: Uri?, removePhoto: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = container.userRepo.getUser() ?: return@launch
            val finalUri = when {
                removePhoto -> {
                    ImageUtils.deleteProfilePhotos(getApplication())
                    null
                }
                newPhotoUri != null -> ImageUtils.saveProfilePhoto(getApplication(), newPhotoUri) ?: current.pfpUri
                else -> current.pfpUri
            }
            container.userRepo.updateProfile(name, finalUri)
        }
    }

    fun updateNotifications(
        master: Boolean?,
        daily: Boolean?,
        dailyTime: String?,
        miss: Boolean?,
        missText: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            container.prefsRepo.updateNotifications(master, daily, dailyTime, miss, missText)
            val updated = container.prefsRepo.getSnapshot()
            NotificationScheduler.sync(getApplication(), updated)
        }
    }

    fun updateTargets(
        screenMin: Int?,
        readingTarget: Int?,
        deepWorkMin: Int?,
        savePct: Int?,
        weightKg: Float?,
        heightCm: Int?,
        runwayDate: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            container.prefsRepo.updateTargets(screenMin, readingTarget, deepWorkMin, savePct, weightKg, heightCm, runwayDate)
            container.dayEvaluator.recomputeDay(_today.value, finalized = false)
        }
    }

    fun setHapticLevel(level: HapticLevel) {
        viewModelScope.launch(Dispatchers.IO) { container.prefsRepo.setHapticLevel(level) }
    }

    fun syncSchedulesNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = container.prefsRepo.getSnapshot()
            NotificationScheduler.sync(getApplication(), prefs)
        }
    }
}
