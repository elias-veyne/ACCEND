package com.accend.app.ui.analysis

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accend.app.data.repo.DayDetailSnapshot
import com.accend.app.ui.charts.ContributionGrid
import com.accend.app.ui.charts.GridCellData
import com.accend.app.ui.charts.LineChartPoint
import com.accend.app.ui.charts.MuscleRadarChart
import com.accend.app.ui.charts.RadarAxisPoint
import com.accend.app.ui.charts.TelemetryLineChart
import com.accend.app.ui.charts.WeeklyBarChart
import com.accend.app.ui.components.AccendCard
import com.accend.app.ui.components.AccendInnerBox
import com.accend.app.ui.components.PrimaryWhiteButton
import com.accend.app.ui.components.TacticalBadge
import com.accend.app.ui.theme.AccendPalette
import com.accend.app.ui.theme.AccendThemeTokens
import com.accend.app.ui.theme.MonoFontFamily
import com.accend.app.util.Format
import kotlin.math.roundToInt

enum class AnalysisRange(val label: String, val days: Int) {
    D7("7D", 7),
    D30("30D", 30),
    D90("90D", 90),
    ALL("ALL", 365)
}

data class AnalysisUiState(
    val selectedRange: AnalysisRange = AnalysisRange.D30,
    val neuroResilience: Int = 0,
    val neuroDeltaPercent: Double = 0.0,
    val executionIndex: Int = 0,
    val totalDataPoints: Int = 0,
    val radarAxes9: List<RadarAxisPoint> = emptyList(),
    val radarAxes4: List<RadarAxisPoint> = emptyList(),
    val totalVolume30d: Double = 0.0,
    val peakMuscleLabel: String = "—",
    val mindScoreSeries: List<LineChartPoint> = emptyList(),
    val mindScoreAvg: Double = 0.0,
    val screenTimeSeries: List<LineChartPoint> = emptyList(),
    val screenTargetHours: Float = 3f,
    val todayScreenMinutes: Int? = null,
    val meditationSeries: List<LineChartPoint> = emptyList(),
    val deepWorkWeeklySeries: List<LineChartPoint> = emptyList(),
    val readingSeries: List<LineChartPoint> = emptyList(),
    val weightWeeklySeries: List<LineChartPoint> = emptyList(),
    val weightGoalKg: Float = 75f,
    val heightMonthlySeries: List<LineChartPoint> = emptyList(),
    val heightGoalCm: Int = 183,
    val pagerBlocks: List<Pair<String, List<GridCellData>>> = emptyList(),
    val correlationSummary: String = "Log physical training and cognitive protocols to unlock cross-domain correlation telemetry.",
    val selectedDayDetail: DayDetailSnapshot? = null,
    val selectedDayNumber: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    state: AnalysisUiState,
    onSelectRange: (AnalysisRange) -> Unit,
    onDayCellClick: (GridCellData) -> Unit,
    onDismissDayDetail: () -> Unit,
    onExportTelemetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    var showFullNineAxes by remember { mutableStateOf(false) }
    var blockIndex by remember(state.pagerBlocks.size) {
        mutableIntStateOf((state.pagerBlocks.lastIndex).coerceAtLeast(0))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header (matches screen__2__17fc4a60.png)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PERFORMANCE\nANALYSIS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 30.sp
                    ),
                    color = AccendPalette.AccentWhite
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "EXECUTIVE TELEMETRY · BIO-COGNITIVE SYNC",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )
            }
            TacticalBadge(
                text = "LIVE MATRIX",
                leadingDotColor = AccendPalette.AccentWhite,
                contentColor = AccendPalette.AccentWhite
            )
        }

        // Range selector pill bar: 7D | 30D | 90D | ALL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surfaceCard)
                .border(1.dp, c.borderSubtle, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnalysisRange.entries.forEach { r ->
                val selected = state.selectedRange == r
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AccendPalette.AccentWhite else c.surfaceCard)
                        .clickable { onSelectRange(r) }
                        .padding(vertical = 10.dp)
                        .testTag("analysis_range_${r.label}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = r.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) AccendPalette.OnAccentWhite else c.textMuted
                    )
                }
            }
        }

        // 2 KPI Cards side-by-side (NEURO-RESILIENCE & EXECUTION INDEX)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccendCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NEURO-RESILIENCE", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    Icon(Icons.Outlined.Psychology, null, tint = c.textPrimary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.neuroResilience}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                    Spacer(Modifier.width(6.dp))
                    val sign = if (state.neuroDeltaPercent >= 0) "+" else ""
                    Text(
                        text = "$sign${Format.decimal1(state.neuroDeltaPercent)}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.neuroDeltaPercent >= 0) c.success else c.danger,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.neuroResilience / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = AccendPalette.AccentWhite,
                    trackColor = c.surfaceHighlight,
                    strokeCap = StrokeCap.Round
                )
            }

            AccendCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EXECUTION INDEX", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    Icon(Icons.Outlined.Bolt, null, tint = c.success, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.executionIndex}%",
                        style = MaterialTheme.typography.headlineLarge.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            state.executionIndex >= 85 -> "Optimal"
                            state.executionIndex >= 50 -> "Steady"
                            else -> "Build"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.executionIndex >= 85) c.success else c.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.executionIndex / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = c.success,
                    trackColor = c.surfaceHighlight,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // 1. MUSCLE VOLUME DISTRIBUTION (Radar Chart)
        AccendCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MUSCLE VOLUME DISTRIBUTION",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AccendPalette.AccentWhite
                    )
                    Text(
                        text = "30-Day Aggregated Volume Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textMuted
                    )
                }
                TacticalBadge(
                    text = if (showFullNineAxes) "9 AXES" else "4 AXES",
                    contentColor = AccendPalette.AccentWhite,
                    modifier = Modifier.clickable { showFullNineAxes = !showFullNineAxes }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (state.totalVolume30d <= 0.0) {
                ChartEmptyPlaceholder("Complete workouts to see your muscle volume balance.")
            } else {
                MuscleRadarChart(axes = if (showFullNineAxes) state.radarAxes9 else state.radarAxes4)
                Spacer(Modifier.height(8.dp))
                // Compact 9-group raw volume legend (PRD §6.3)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.radarAxes9.chunked(3).forEach { rowChunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowChunk.forEach { axis ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(c.surfaceInner)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(axis.label, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                                    Text(
                                        Format.number(axis.rawVolume.roundToInt()),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = c.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                AccendInnerBox(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CURRENT LOAD WAVE · PEAK ${state.peakMuscleLabel}", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                        Text(
                            text = "${Format.number(state.totalVolume30d.roundToInt())} PTS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccendPalette.AccentWhite
                        )
                    }
                }
            }
        }

        // 2. MIND SCORE TREND
        AccendCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MIND SCORE TREND",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AccendPalette.AccentWhite
                    )
                    Text("Cognitive Velocity & Coherence Index", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
                TacticalBadge(
                    text = "AVG ${Format.decimal1(state.mindScoreAvg)}",
                    containerColor = c.successBg,
                    contentColor = c.success,
                    borderColor = c.successBorder
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = Format.decimal1(state.mindScoreAvg),
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = MonoFontFamily),
                color = AccendPalette.AccentWhite
            )
            Spacer(Modifier.height(10.dp))
            if (state.mindScoreSeries.isEmpty()) {
                ChartEmptyPlaceholder("Complete cognitive protocols to track Mind Score.")
            } else {
                TelemetryLineChart(
                    points = state.mindScoreSeries,
                    minY = 0f,
                    maxY = 100f
                )
            }
        }

        // 3. SCREEN TIME DISCIPLINE
        AccendCard {
            val todayMins = state.todayScreenMinutes
            val isSafe = todayMins == null || (todayMins / 60f) <= state.screenTargetHours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SCREEN TIME DISCIPLINE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AccendPalette.AccentWhite
                    )
                    Text(
                        text = "Ceiling: ${Format.decimal1(state.screenTargetHours.toDouble())}h · Non-Negotiable Limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textMuted
                    )
                }
                TacticalBadge(
                    text = if (isSafe) "SAFE" else "EXCEED",
                    leadingDotColor = if (isSafe) c.success else c.danger,
                    containerColor = if (isSafe) c.successBg else c.dangerBg,
                    contentColor = if (isSafe) c.success else c.danger,
                    borderColor = if (isSafe) c.successBorder else c.dangerBorder
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (todayMins != null) Format.screenTime(todayMins) else "—",
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = MonoFontFamily),
                    color = AccendPalette.AccentWhite
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Logged Active Usage Today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            if (state.screenTimeSeries.isEmpty()) {
                ChartEmptyPlaceholder("Log screen time in Daily Work to see this trend.")
            } else {
                TelemetryLineChart(
                    points = state.screenTimeSeries,
                    minY = 0f,
                    targetLineValue = state.screenTargetHours,
                    targetLineLabel = "${Format.decimal1(state.screenTargetHours.toDouble())}H CEILING",
                    targetIsCeiling = true
                )
            }
        }

        // 4. 30-DAY JOURNEY PAGER GRID (with tappable Day Detail sheet per PRD §6.3 & §6.10)
        AccendCard {
            val currentBlock = state.pagerBlocks.getOrNull(blockIndex)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "30-DAY EXECUTION GRID",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AccendPalette.AccentWhite
                    )
                    Text(
                        text = (currentBlock?.first ?: "DAYS 1–30") + " · TAP ANY CELL FOR TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                }
                Row {
                    IconButton(
                        onClick = { if (blockIndex > 0) blockIndex-- },
                        enabled = blockIndex > 0
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous 30-day block",
                            tint = if (blockIndex > 0) AccendPalette.AccentWhite else c.textMuted
                        )
                    }
                    IconButton(
                        onClick = { if (blockIndex < state.pagerBlocks.lastIndex) blockIndex++ },
                        enabled = blockIndex < state.pagerBlocks.lastIndex
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Next 30-day block",
                            tint = if (blockIndex < state.pagerBlocks.lastIndex) AccendPalette.AccentWhite else c.textMuted
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (currentBlock != null) {
                ContributionGrid(
                    cells = currentBlock.second,
                    showDayNumber = true,
                    onCellClick = onDayCellClick
                )
            }
        }

        // 5. MEDITATION MINUTES
        SimpleLineChartCard(
            title = "MEDITATION MINUTES",
            subtitle = "Mindfulness Duration Trend",
            points = state.meditationSeries,
            emptyMessage = "Log meditation sessions to see this chart."
        )

        // 6. DEEP WORK WEEKLY BAR CHART (12 ISO weeks)
        AccendCard {
            Text(
                text = "DEEP WORK TOTAL (WEEKLY)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AccendPalette.AccentWhite
            )
            Text("Minutes per ISO Week (Mon–Sun)", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            Spacer(Modifier.height(12.dp))
            if (state.deepWorkWeeklySeries.all { it.value <= 0f }) {
                ChartEmptyPlaceholder("Complete Deep Work blocks to populate weekly bars.")
            } else {
                WeeklyBarChart(points = state.deepWorkWeeklySeries)
            }
        }

        // 7. READING PAGES
        SimpleLineChartCard(
            title = "READING PAGES",
            subtitle = "Daily Reading Output",
            points = state.readingSeries,
            emptyMessage = "Log reading pages to see this chart."
        )

        // 8. WEIGHT TREND
        AccendCard {
            Text(
                text = "WEIGHT TELEMETRY",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AccendPalette.AccentWhite
            )
            Text("Weekly latest entry · Goal ${Format.decimal1(state.weightGoalKg.toDouble())} kg", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            Spacer(Modifier.height(12.dp))
            if (state.weightWeeklySeries.isEmpty()) {
                ChartEmptyPlaceholder("Log body weight weekly to track progression.")
            } else {
                TelemetryLineChart(
                    points = state.weightWeeklySeries,
                    targetLineValue = state.weightGoalKg,
                    targetLineLabel = "GOAL ${Format.decimal1(state.weightGoalKg.toDouble())} KG"
                )
            }
        }

        // 9. HEIGHT TREND
        AccendCard {
            Text(
                text = "HEIGHT TELEMETRY",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AccendPalette.AccentWhite
            )
            Text(
                text = "Monthly log · Growth window (${Format.cmToFeetInches(state.heightGoalCm)} / ${state.heightGoalCm} cm)",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted
            )
            Spacer(Modifier.height(12.dp))
            if (state.heightMonthlySeries.isEmpty()) {
                ChartEmptyPlaceholder("Log height monthly to track your growth window.")
            } else {
                TelemetryLineChart(
                    points = state.heightMonthlySeries,
                    targetLineValue = state.heightGoalCm.toFloat(),
                    targetLineLabel = "GROWTH WINDOW (${state.heightGoalCm} CM)"
                )
            }
        }

        // DISCIPLINE CORRELATION MATRIX (matches screen__2__17fc4a60.png)
        AccendCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccendPalette.AccentWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Insights,
                        contentDescription = null,
                        tint = AccendPalette.OnAccentWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("DISCIPLINE CORRELATION MATRIX", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    Text(
                        text = "Kinetic Exhaustion & Neuro-Control",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccendPalette.AccentWhite
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AccendInnerBox {
                Text(
                    text = state.correlationSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = c.borderSubtle)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LOCAL TELEMETRY", style = MaterialTheme.typography.labelSmall, color = c.success)
                    Text("N=${state.totalDataPoints} DATA POINTS", style = MaterialTheme.typography.labelSmall, color = c.textPrimary)
                }
            }
        }

        // EXPORT TELEMETRY CTA (matches screen__2__17fc4a60.png)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryWhiteButton(
                text = "EXPORT TELEMETRY JSON",
                icon = Icons.Outlined.Download,
                onClick = onExportTelemetry,
                modifier = Modifier.weight(1f),
                testTag = "analysis_export_button"
            )
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceCard)
                    .border(1.dp, c.borderStrong, RoundedCornerShape(16.dp))
                    .clickable(onClick = onExportTelemetry),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share export",
                    tint = AccendPalette.AccentWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    // Read-only Day Detail Modal Bottom Sheet (§6.10)
    state.selectedDayDetail?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = onDismissDayDetail,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAY ${state.selectedDayNumber}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = AccendPalette.AccentWhite
                        )
                        Text(
                            text = detail.date.formatFull().uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.textMuted
                        )
                    }
                    TacticalBadge(
                        text = "${detail.completionPercent}% COMPLETE",
                        contentColor = if (detail.completionPercent >= 80) c.success else AccendPalette.AccentWhite
                    )
                }
                HorizontalDivider(color = c.borderSubtle)
                DetailRow("Physical", "${detail.physicalDone} / ${detail.physicalTotal}")
                DetailRow("Nutrition", "${detail.nutritionDone} / 5")
                DetailRow("Meditation", "${detail.meditationMinutes} min")
                DetailRow("Deep Work", "${detail.deepWorkMinutes} min")
                DetailRow("Reading", "${detail.readingPages} pages")
                DetailRow(
                    "Screen Time",
                    detail.screenTimeMinutes?.let { Format.screenTime(it) } ?: "Not logged"
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SimpleLineChartCard(
    title: String,
    subtitle: String,
    points: List<LineChartPoint>,
    emptyMessage: String
) {
    val c = AccendThemeTokens.colors
    AccendCard {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = AccendPalette.AccentWhite
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        Spacer(Modifier.height(12.dp))
        if (points.isEmpty()) {
            ChartEmptyPlaceholder(emptyMessage)
        } else {
            TelemetryLineChart(points = points, minY = 0f)
        }
    }
}

@Composable
private fun ChartEmptyPlaceholder(message: String) {
    val c = AccendThemeTokens.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceInner)
            .border(1.dp, c.borderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
            color = AccendPalette.AccentWhite
        )
    }
}
