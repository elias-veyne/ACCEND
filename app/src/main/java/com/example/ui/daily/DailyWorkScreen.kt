package com.example.ui.daily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ExerciseEntity
import com.example.data.db.NutritionLogEntity
import com.example.data.db.ProgressionLevel
import com.example.domain.DayCompletionBreakdown
import com.example.ui.components.AccendCard
import com.example.ui.components.AccendInnerBox
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.NumberStepper
import com.example.ui.components.PrimaryWhiteButton
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TacticalBadge
import com.example.ui.components.TacticalCheckbox
import com.example.ui.components.TacticalSwitch
import com.example.ui.theme.AccendPalette
import com.example.ui.theme.AccendThemeTokens
import com.example.ui.theme.MonoFontFamily
import com.example.util.AccendDate
import com.example.util.Format
import com.example.util.HapticLevel
import com.example.util.Haptics
import kotlin.math.roundToInt

data class DailyExerciseRowUi(
    val exercise: ExerciseEntity,
    val done: Boolean,
    val levelsCount: Int,
    val isCompound: Boolean
)

data class DailyWorkUiState(
    val dayNumber: Int = 1,
    val today: AccendDate = AccendDate.today(),
    val exercises: List<DailyExerciseRowUi> = emptyList(),
    val nutrition: NutritionLogEntity = NutritionLogEntity(date = AccendDate.today().toIsoString()),
    val meditationTargetMin: Int = 1,
    val meditationWeek: Int = 1,
    val meditationLoggedMin: Int = 0,
    val screenTimeLoggedMin: Int? = null,
    val screenTimeTargetMin: Int = 180,
    val deepWorkDefaultMin: Int = 30,
    val deepWorkTotalMin: Int = 0,
    val deepWorkCompletedToday: Boolean = false,
    val readingPages: Int = 0,
    val readingTarget: Int = 1,
    val todayVolume: Double = 0.0,
    val currentStreak: Int = 0,
    val breakdown: DayCompletionBreakdown = DayCompletionBreakdown(0, 6, 0, false, false, false, false),
    val bodyReminderDue: Boolean = false,
    val latestWeightKg: Double? = null,
    val latestHeightCm: Double? = null,
    val hapticLevel: HapticLevel = HapticLevel.HIGH
)

@Composable
fun DailyWorkScreen(
    state: DailyWorkUiState,
    onToggleExercise: (ExerciseEntity) -> Unit,
    onUpdateNutrition: ((NutritionLogEntity) -> NutritionLogEntity) -> Unit,
    onStartMeditation: () -> Unit,
    onSaveScreenTime: (Int) -> Unit,
    onStartDeepWork: (Int) -> Unit,
    onSaveReadingPages: (Int) -> Unit,
    onSaveBody: (Double?, Double?) -> Unit,
    onLockInFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    val view = LocalView.current

    var expandedExerciseId by remember { mutableStateOf<Long?>(null) }
    var selectedDeepWorkPreset by remember(state.deepWorkDefaultMin) {
        mutableIntStateOf(state.deepWorkDefaultMin)
    }
    var screenHours by remember(state.screenTimeLoggedMin) {
        mutableIntStateOf((state.screenTimeLoggedMin ?: 0) / 60)
    }
    var screenMinutes by remember(state.screenTimeLoggedMin) {
        mutableIntStateOf((state.screenTimeLoggedMin ?: 0) % 60)
    }
    var showScreenEditor by remember { mutableStateOf(false) }

    var bodyExpanded by remember(state.bodyReminderDue) { mutableStateOf(state.bodyReminderDue) }
    var weightInput by remember(state.latestWeightKg) {
        mutableStateOf(state.latestWeightKg?.let { Format.decimal1(it) } ?: "")
    }
    var heightInput by remember(state.latestHeightCm) {
        mutableStateOf(state.latestHeightCm?.roundToInt()?.toString() ?: "")
    }

    var celebrated100 by remember(state.today) { mutableStateOf(state.breakdown.percent == 100) }
    LaunchedEffect(state.breakdown.percent) {
        if (state.breakdown.percent == 100 && !celebrated100) {
            celebrated100 = true
            Haptics.heavy(view, state.hapticLevel)
        }
    }

    val isSaturday = state.today.weekdaySundayOne == 7

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // DAILY DIRECTIVE Header Card (matches screen__1__e573701c.png)
        AccendCard(
            borderColor = if (state.breakdown.percent == 100) AccendPalette.AccentWhite else c.borderSubtle
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY DIRECTIVE · ${state.today.formatShort().uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (state.breakdown.percent == 100) c.success else AccendPalette.AccentWhite)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.breakdown.percent == 100) "PROTOCOL LOCKED" else "CYCLE ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.breakdown.percent == 100) c.success else c.textPrimary
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "DAY ${state.dayNumber} · ${state.today.focusTitle}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = AccendPalette.AccentWhite
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EXECUTION OVER EXCUSES",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )
                Text(
                    text = "${state.breakdown.totalDone} / ${state.breakdown.totalItems} PROTOCOLS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccendPalette.AccentWhite
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.breakdown.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccendPalette.AccentWhite,
                trackColor = c.surfaceHighlight,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DirectiveMetricTile("VOLUME", "${Format.number(state.todayVolume.roundToInt())}", "PTS", Modifier.weight(1f))
                DirectiveMetricTile("FOCUS RATE", "${state.breakdown.percent}%", "", Modifier.weight(1f))
                DirectiveMetricTile("STREAK", "${state.currentStreak}", "DAYS", Modifier.weight(1f))
            }
        }

        // 01 / PHYSICAL DISCIPLINE
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val physicalRemaining = (state.breakdown.physicalTotal - state.breakdown.physicalDone).coerceAtLeast(0)
            SectionHeaderRow(
                title = if (isSaturday) "01 / REST & ACTIVE RECOVERY" else "01 / PHYSICAL DISCIPLINE",
                trailing = if (physicalRemaining == 0) "COMPLETE" else "$physicalRemaining REMAINING"
            )

            if (state.exercises.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Outlined.Bolt,
                    message = "No exercises scheduled today."
                )
            } else {
                state.exercises.forEach { row ->
                    val expanded = expandedExerciseId == row.exercise.id
                    val targetText = Format.exerciseTarget(row.exercise.sets, row.exercise.targetReps, row.exercise.unit)
                    AccendCard(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        borderColor = if (row.done) c.borderSubtle else c.borderStrong,
                        modifier = Modifier.testTag("exercise_row_${row.exercise.id}"),
                        onClick = {
                            Haptics.tick(view, state.hapticLevel)
                            onToggleExercise(row.exercise)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TacticalCheckbox(
                                checked = row.done,
                                onCheckedChange = {
                                    Haptics.tick(view, state.hapticLevel)
                                    onToggleExercise(row.exercise)
                                },
                                contentDescription = "Toggle ${row.exercise.name}"
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        expandedExerciseId = if (expanded) null else row.exercise.id
                                    }
                            ) {
                                Text(
                                    text = row.exercise.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = if (row.done) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (row.done) c.textMuted else c.textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = row.exercise.muscleGroup.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textMuted
                                )
                            }
                            if (row.done || targetText.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                TacticalBadge(
                                    text = if (row.done) "DONE" else targetText.uppercase(),
                                    contentColor = if (row.done) c.textMuted else c.textPrimary
                                )
                            }
                        }
                        AnimatedVisibility(visible = expanded) {
                            val levelNote = if (row.isCompound) {
                                "Level ${row.exercise.currentLevelIndex + 1} of ${row.levelsCount} · ${row.exercise.muscleGroup.displayName}"
                            } else {
                                "Volume target: ${targetText.ifEmpty { "Completion" }} · ${row.exercise.muscleGroup.displayName}"
                            }
                            Text(
                                text = levelNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textSecondary,
                                modifier = Modifier.padding(start = 52.dp, top = 6.dp, bottom = 2.dp)
                            )
                        }
                    }
                }

                // PRD A5: Saturday mirrored Sleep 9h control counted once
                if (isSaturday) {
                    AccendCard(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        onClick = {
                            Haptics.tick(view, state.hapticLevel)
                            onUpdateNutrition { it.copy(sleep9h = !it.sleep9h) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TacticalCheckbox(
                                checked = state.nutrition.sleep9h,
                                onCheckedChange = {
                                    Haptics.tick(view, state.hapticLevel)
                                    onUpdateNutrition { it.copy(sleep9h = !it.sleep9h) }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sleep 9h (Synced with Recovery)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = if (state.nutrition.sleep9h) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (state.nutrition.sleep9h) c.textMuted else c.textPrimary
                                )
                                Text(
                                    text = "MIRRORED NUTRITION PROTOCOL · COUNTED ONCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textMuted
                                )
                            }
                            TacticalBadge(text = if (state.nutrition.sleep9h) "DONE" else "9H")
                        }
                    }
                }
            }
        }

        // 02 / FUEL & REGENERATION (5 Switches — matches screen__1__e573701c.png)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeaderRow(
                title = "02 / FUEL & REGENERATION",
                trailing = "${state.breakdown.nutritionDone}/5 LOCKED"
            )
            AccendCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                FuelSwitchRow(
                    title = "Ate Protein",
                    subtitle = "HIGH-QUALITY DAILY PROTEIN TARGET",
                    checked = state.nutrition.protein,
                    testTag = "nutr_protein",
                    onToggle = {
                        Haptics.tick(view, state.hapticLevel)
                        onUpdateNutrition { it.copy(protein = !it.protein) }
                    }
                )
                HorizontalDivider(color = c.borderSubtle)
                FuelSwitchRow(
                    title = "2 Heavy Meals",
                    subtitle = "ZERO MICRO-SNACKING WINDOW",
                    checked = state.nutrition.heavyMeals,
                    testTag = "nutr_heavy_meals",
                    onToggle = {
                        Haptics.tick(view, state.hapticLevel)
                        onUpdateNutrition { it.copy(heavyMeals = !it.heavyMeals) }
                    }
                )
                HorizontalDivider(color = c.borderSubtle)
                FuelSwitchRow(
                    title = "Breakfast",
                    subtitle = "DISCIPLINED MORNING FUEL WINDOW",
                    checked = state.nutrition.breakfast,
                    testTag = "nutr_breakfast",
                    onToggle = {
                        Haptics.tick(view, state.hapticLevel)
                        onUpdateNutrition { it.copy(breakfast = !it.breakfast) }
                    }
                )
                HorizontalDivider(color = c.borderSubtle)
                FuelSwitchRow(
                    title = "Hydration 3L",
                    subtitle = "ELECTROLYTES + CLEAN WATER INTAKE",
                    checked = state.nutrition.hydration,
                    testTag = "nutr_hydration",
                    onToggle = {
                        Haptics.tick(view, state.hapticLevel)
                        onUpdateNutrition { it.copy(hydration = !it.hydration) }
                    }
                )
                HorizontalDivider(color = c.borderSubtle)
                FuelSwitchRow(
                    title = "Sleep 9h",
                    subtitle = "FULL NEURAL & MUSCLE REGENERATION",
                    checked = state.nutrition.sleep9h,
                    testTag = "nutr_sleep",
                    onToggle = {
                        Haptics.tick(view, state.hapticLevel)
                        onUpdateNutrition { it.copy(sleep9h = !it.sleep9h) }
                    }
                )
            }
        }

        // 03 / COGNITIVE FORTITUDE (4 Mind Rows — matches screen__1__e573701c.png & PRD §6.2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeaderRow(
                title = "03 / COGNITIVE FORTITUDE",
                trailing = "${state.breakdown.mindDone}/4 EXECUTED"
            )

            // 1. Meditation (auto-ticked by timer)
            AccendCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TacticalCheckbox(
                        checked = state.breakdown.meditationDone,
                        enabled = false,
                        onCheckedChange = {},
                        contentDescription = "Meditation status"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Meditation (Week ${state.meditationWeek} · ${state.meditationTargetMin}m)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (state.breakdown.meditationDone) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (state.breakdown.meditationDone) c.textMuted else c.textPrimary
                        )
                        Text(
                            text = if (state.meditationLoggedMin > 0) {
                                "${state.meditationLoggedMin} MIN LOGGED TODAY · AUTO-TICKED"
                            } else {
                                "BREATH AWARENESS · COMPLETES AUTOMATICALLY"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textMuted
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccendPalette.AccentWhite)
                            .clickable(onClick = onStartMeditation)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("daily_start_meditation")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Start meditation",
                                tint = AccendPalette.OnAccentWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "START",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccendPalette.OnAccentWhite
                            )
                        }
                    }
                }
            }

            // 2. Screen Time Limit (inline editor)
            val screenLogged = state.screenTimeLoggedMin != null
            val screenOver = (state.screenTimeLoggedMin ?: 0) > state.screenTimeTargetMin
            AccendCard(
                contentPadding = PaddingValues(14.dp),
                onClick = { showScreenEditor = !showScreenEditor }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TacticalCheckbox(
                        checked = screenLogged,
                        enabled = false,
                        onCheckedChange = {},
                        contentDescription = "Screen time status"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Screen Time Limit (< ${Format.screenTime(state.screenTimeTargetMin)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.textPrimary
                        )
                        Text(
                            text = if (screenLogged) {
                                "LOGGED: ${Format.screenTime(state.screenTimeLoggedMin ?: 0)} · TAP TO EDIT"
                            } else {
                                "TAP TO LOG TODAY'S SCREEN USAGE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textMuted
                        )
                    }
                    TacticalBadge(
                        text = when {
                            !screenLogged -> "LOG"
                            screenOver -> "OVER"
                            else -> Format.screenTime(state.screenTimeLoggedMin ?: 0).uppercase()
                        },
                        containerColor = when {
                            !screenLogged -> c.surfaceInner
                            screenOver -> c.warningBg
                            else -> c.successBg
                        },
                        contentColor = when {
                            !screenLogged -> c.textPrimary
                            screenOver -> c.warning
                            else -> c.success
                        },
                        borderColor = when {
                            !screenLogged -> c.borderStrong
                            screenOver -> c.warning
                            else -> c.successBorder
                        }
                    )
                }
                AnimatedVisibility(visible = showScreenEditor) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberStepper(
                                value = screenHours,
                                onDecrement = { if (screenHours > 0) screenHours-- },
                                onIncrement = { if (screenHours < 23) screenHours++ },
                                label = "${screenHours}h"
                            )
                            NumberStepper(
                                value = screenMinutes,
                                onDecrement = { screenMinutes = if (screenMinutes >= 5) screenMinutes - 5 else 55 },
                                onIncrement = { screenMinutes = (screenMinutes + 5) % 60 },
                                label = "${screenMinutes}m"
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccendPalette.AccentWhite)
                                    .clickable {
                                        Haptics.tick(view, state.hapticLevel)
                                        onSaveScreenTime(screenHours * 60 + screenMinutes)
                                        showScreenEditor = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("daily_save_screen_time")
                            ) {
                                Text(
                                    text = "SAVE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AccendPalette.OnAccentWhite
                                )
                            }
                        }
                    }
                }
            }

            // 3. Deep Work (preset chips + timer)
            AccendCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TacticalCheckbox(
                        checked = state.breakdown.deepWorkDone,
                        enabled = false,
                        onCheckedChange = {},
                        contentDescription = "Deep Work status"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Deep Work",
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (state.breakdown.deepWorkDone) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (state.breakdown.deepWorkDone) c.textMuted else c.textPrimary
                        )
                        Text(
                            text = if (state.deepWorkTotalMin > 0) {
                                "${state.deepWorkTotalMin} MIN LOGGED TODAY"
                            } else {
                                "UNINTERRUPTED COGNITIVE OUTPUT"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccendPalette.AccentWhite)
                            .clickable { onStartDeepWork(selectedDeepWorkPreset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("daily_start_deepwork")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Start Deep Work",
                                tint = AccendPalette.OnAccentWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "START",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccendPalette.OnAccentWhite
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        val active = selectedDeepWorkPreset == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AccendPalette.AccentWhite else c.surfaceInner)
                                .border(1.dp, if (active) AccendPalette.AccentWhite else c.borderStrong, RoundedCornerShape(8.dp))
                                .clickable { selectedDeepWorkPreset = mins }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${mins}M",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) AccendPalette.OnAccentWhite else c.textSecondary
                            )
                        }
                    }
                }
            }

            // 4. Reading (plain checkbox if target==1, stepper otherwise)
            AccendCard(
                contentPadding = PaddingValues(14.dp),
                onClick = if (state.readingTarget == 1) {
                    {
                        Haptics.tick(view, state.hapticLevel)
                        onSaveReadingPages(if (state.readingPages >= 1) 0 else 1)
                    }
                } else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TacticalCheckbox(
                        checked = state.breakdown.readingDone,
                        onCheckedChange = {
                            Haptics.tick(view, state.hapticLevel)
                            if (state.readingTarget == 1) {
                                onSaveReadingPages(if (state.readingPages >= 1) 0 else 1)
                            } else {
                                onSaveReadingPages(if (state.breakdown.readingDone) 0 else state.readingTarget)
                            }
                        },
                        contentDescription = "Reading completion"
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reading (${state.readingTarget} ${if (state.readingTarget == 1) "Page" else "Pages"} Min)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (state.breakdown.readingDone) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (state.breakdown.readingDone) c.textMuted else c.textPrimary
                        )
                        Text(
                            text = "DELIBERATE DAILY STUDY",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textMuted
                        )
                    }
                    if (state.readingTarget > 1) {
                        NumberStepper(
                            value = state.readingPages,
                            onDecrement = {
                                if (state.readingPages > 0) {
                                    Haptics.tick(view, state.hapticLevel)
                                    onSaveReadingPages(state.readingPages - 1)
                                }
                            },
                            onIncrement = {
                                Haptics.tick(view, state.hapticLevel)
                                onSaveReadingPages(state.readingPages + 1)
                            },
                            label = "${state.readingPages}/${state.readingTarget}"
                        )
                    } else {
                        TacticalBadge(text = if (state.breakdown.readingDone) "DONE" else "1 PG")
                    }
                }
            }
        }

        // 04 / BODY TELEMETRY (Collapsible per PRD A13)
        AccendCard(contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bodyExpanded = !bodyExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "04 / BODY TELEMETRY",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                        color = c.textPrimary
                    )
                    if (state.bodyReminderDue) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(c.warning)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WEEKLY / MONTHLY",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (bodyExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Expand body log",
                        tint = c.textSecondary
                    )
                }
            }
            AnimatedVisibility(visible = bodyExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccendPalette.AccentWhite,
                                unfocusedBorderColor = c.borderStrong
                            )
                        )
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("Height (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccendPalette.AccentWhite,
                                unfocusedBorderColor = c.borderStrong
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccendPalette.AccentWhite)
                            .clickable {
                                val w = weightInput.toDoubleOrNull()?.takeIf { it in 20.0..300.0 }
                                val h = heightInput.toDoubleOrNull()?.takeIf { it in 80.0..250.0 }
                                if (w != null || h != null) {
                                    Haptics.tick(view, state.hapticLevel)
                                    onSaveBody(w, h)
                                    bodyExpanded = false
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "LOG MEASUREMENTS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccendPalette.OnAccentWhite
                        )
                    }
                }
            }
        }

        // Primary White CTA (matches screen__1__e573701c.png)
        PrimaryWhiteButton(
            text = if (state.breakdown.percent == 100) "PROTOCOL LOCKED (100%)" else "LOCK IN PROTOCOL",
            icon = Icons.Outlined.Bolt,
            testTag = "daily_lock_in_button",
            onClick = {
                Haptics.heavy(view, state.hapticLevel)
                onLockInFeedback()
            }
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DirectiveMetricTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    AccendInnerBox(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = AccendPalette.AccentWhite,
                maxLines = 1
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FuelSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onToggle: () -> Unit
) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        TacticalSwitch(checked = checked, onCheckedChange = { onToggle() }, testTag = testTag)
    }
}
