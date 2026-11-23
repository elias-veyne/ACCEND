package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DayCompletionBreakdown
import com.example.domain.StreakResult
import com.example.ui.charts.ContributionGrid
import com.example.ui.charts.GridCellData
import com.example.ui.components.AccendCard
import com.example.ui.components.AccendInnerBox
import com.example.ui.components.PrimaryWhiteButton
import com.example.ui.components.TacticalBadge
import com.example.ui.theme.AccendPalette
import com.example.ui.theme.AccendThemeTokens
import com.example.ui.theme.MonoFontFamily
import com.example.util.AccendDate
import com.example.util.Format
import kotlin.math.roundToInt

data class HomeUiState(
    val userName: String = "Operator",
    val pfpUri: String? = null,
    val dayNumber: Int = 1,
    val today: AccendDate = AccendDate.today(),
    val mindScore: Int = 0,
    val mindScoreDelta: Int = 0,
    val breakdown: DayCompletionBreakdown = DayCompletionBreakdown(0, 6, 0, false, false, false, false),
    val meditationTargetMin: Int = 1,
    val readingTargetPages: Int = 1,
    val gridCells: List<GridCellData> = emptyList(),
    val activeDays30: Int = 0,
    val avgPercent30: Int = 0,
    val perfectDays30: Int = 0,
    val physicalStreak: StreakResult = StreakResult(0, 0),
    val meditationStreak: StreakResult = StreakResult(0, 0),
    val deepWorkStreak: StreakResult = StreakResult(0, 0)
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenDaily: () -> Unit,
    onOpenAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    val summaryDoneCount = listOf(
        state.breakdown.physicalTotal > 0 && state.breakdown.physicalDone == state.breakdown.physicalTotal,
        state.breakdown.meditationDone,
        state.breakdown.deepWorkDone,
        state.breakdown.readingDone
    ).count { it }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Mind Score Ring Card (matches screen_54d495bc.png)
        AccendCard(
            modifier = Modifier.testTag("home_mind_score_card"),
            onClick = onOpenAnalysis,
            contentPadding = PaddingValues(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAY ${state.dayNumber} · ${state.userName.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c.textSecondary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "SYNCHRONIZED",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.charts.MindScoreRing(
                    score = state.mindScore,
                    ringSize = 212.dp
                )
            }

            Spacer(Modifier.height(14.dp))

            val deltaPositive = state.mindScoreDelta >= 0
            val deltaText = if (state.mindScoreDelta == 0) {
                "14-DAY COGNITIVE BASELINE"
            } else {
                val sign = if (deltaPositive) "+" else ""
                "$sign${state.mindScoreDelta} PTS VS YESTERDAY"
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (deltaPositive) c.successBg else c.dangerBg)
                        .border(
                            1.dp,
                            if (deltaPositive) c.successBorder else c.dangerBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = if (deltaPositive) c.success else c.danger,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (deltaPositive) c.success else c.danger
                    )
                }
            }
        }

        // Cycle complete milestone banner (>365) or Day-1 start hint
        if (state.dayNumber > 365) {
            AccendInnerBox(borderColor = c.successBorder, backgroundColor = c.successBg) {
                Text(
                    text = "365-DAY CYCLE COMPLETE · DAY ${state.dayNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.success
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "You forged a full year of discipline. Keep the chain unbroken.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textPrimary
                )
            }
        } else if (state.breakdown.percent == 0) {
            AccendInnerBox(onClick = onOpenDaily) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROTOCOL STANDING BY",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.textPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Start with today's checklist to raise your Mind Score.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TacticalBadge(text = "OPEN", contentColor = AccendPalette.AccentWhite)
                }
            }
        }

        // 2. Today's Protocol Summary Card (matches screen_54d495bc.png)
        AccendCard(
            modifier = Modifier.testTag("home_today_protocol_card"),
            onClick = onOpenDaily
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TODAY'S PROTOCOL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = c.textPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AccendPalette.AccentWhite)
                    )
                }
                TacticalBadge(
                    text = "$summaryDoneCount/4 COMPLETED",
                    contentColor = c.textPrimary
                )
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.breakdown.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccendPalette.AccentWhite,
                trackColor = c.surfaceHighlight,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val physComplete = state.breakdown.physicalTotal > 0 &&
                    state.breakdown.physicalDone == state.breakdown.physicalTotal
                ProtocolSummaryRow(
                    icon = Icons.Outlined.FitnessCenter,
                    iconTint = if (physComplete) c.success else c.textPrimary,
                    iconBg = if (physComplete) c.successBg else c.surfaceHighlight,
                    title = "Physical Training",
                    subtitle = "${state.today.focusTitle} Intensity Protocol",
                    statusText = "${state.breakdown.physicalDone}/${state.breakdown.physicalTotal}",
                    isDone = physComplete,
                    usePillStatus = true
                )
                ProtocolSummaryRow(
                    icon = Icons.Outlined.SelfImprovement,
                    iconTint = c.textPrimary,
                    iconBg = c.surfaceHighlight,
                    title = "Meditation",
                    subtitle = "Focus · ${state.meditationTargetMin} min target",
                    statusText = if (state.breakdown.meditationDone) "Done" else "PENDING",
                    isDone = state.breakdown.meditationDone
                )
                ProtocolSummaryRow(
                    icon = Icons.Outlined.Terminal,
                    iconTint = c.textPrimary,
                    iconBg = c.surfaceHighlight,
                    title = "Deep Work",
                    subtitle = "Focused Cognitive Blocks",
                    statusText = if (state.breakdown.deepWorkDone) "Done" else "PENDING",
                    isDone = state.breakdown.deepWorkDone
                )
                ProtocolSummaryRow(
                    icon = Icons.Outlined.AutoStories,
                    iconTint = c.textPrimary,
                    iconBg = c.surfaceHighlight,
                    title = "Reading",
                    subtitle = "Daily Reading · ${state.readingTargetPages} pg target",
                    statusText = if (state.breakdown.readingDone) "Done" else "PENDING",
                    isDone = state.breakdown.readingDone
                )
            }
        }

        // 3. Consistency Index (30-Day Grid Card — matches screen_54d495bc.png)
        AccendCard(
            modifier = Modifier.testTag("home_consistency_card"),
            onClick = onOpenAnalysis
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "CONSISTENCY INDEX",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = c.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "LAST 30 DAYS PERFORMANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.surfaceInner)
                        .border(1.dp, c.borderStrong, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = AccendPalette.AccentWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${maxOf(state.physicalStreak.current, state.meditationStreak.current, state.deepWorkStreak.current)} DAYS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccendPalette.AccentWhite
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            AccendInnerBox(contentPadding = PaddingValues(14.dp)) {
                ContributionGrid(cells = state.gridCells, showDayNumber = false)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Less", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                        listOf(
                            c.surfaceCard,
                            Color(0xFF474752),
                            Color(0xFF8A8A98),
                            Color(0xFFD0D0DC),
                            AccendPalette.AccentWhite
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                        Text("More", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    }
                    Text(
                        text = "${state.avgPercent30}% ADHERENCE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccendPalette.AccentWhite
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStatPill("ACTIVE", "${state.activeDays30}/30", Modifier.weight(1f))
                MiniStatPill("AVG", "${state.avgPercent30}%", Modifier.weight(1f))
                MiniStatPill("PERFECT", "${state.perfectDays30}", Modifier.weight(1f))
            }
        }

        // 4. Streak Chips Row (PRD §6.1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StreakChip("PHYSICAL", state.physicalStreak, onOpenDaily)
            StreakChip("MEDITATION", state.meditationStreak, onOpenDaily)
            StreakChip("DEEP WORK", state.deepWorkStreak, onOpenDaily)
        }

        // 5. Primary White CTA Button (matches screen_54d495bc.png)
        PrimaryWhiteButton(
            text = "EXECUTE NEXT PROTOCOL",
            icon = Icons.Outlined.Bolt,
            onClick = onOpenDaily,
            testTag = "home_execute_protocol_button"
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProtocolSummaryRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    statusText: String,
    isDone: Boolean,
    usePillStatus: Boolean = false
) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceInner)
            .border(1.dp, c.borderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        if (usePillStatus) {
            TacticalBadge(
                text = statusText,
                containerColor = if (isDone) c.successBg else c.surfaceHighlight,
                contentColor = if (isDone) c.success else c.textSecondary,
                borderColor = if (isDone) c.successBorder else c.borderStrong
            )
        } else {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDone) c.textSecondary else c.textMuted
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDone) AccendPalette.AccentWhite else c.surfaceHighlight)
                .border(
                    1.dp,
                    if (isDone) AccendPalette.AccentWhite else c.borderStrong,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDone) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = if (isDone) "Completed" else "Pending",
                tint = if (isDone) AccendPalette.OnAccentWhite else c.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val c = AccendThemeTokens.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceInner)
            .border(1.dp, c.borderSubtle, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
            color = c.textPrimary
        )
    }
}

@Composable
private fun StreakChip(label: String, streak: StreakResult, onClick: () -> Unit) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceCard)
            .border(1.dp, c.borderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = if (streak.current > 0) AccendPalette.AccentWhite else c.textMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "$label · ${streak.current}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = c.textPrimary
            )
            Text(
                text = "BEST ${streak.best}",
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted
            )
        }
    }
}
