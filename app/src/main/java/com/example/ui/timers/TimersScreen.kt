package com.example.ui.timers

import android.app.Activity
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimaryWhiteButton
import com.example.ui.components.SecondaryDarkButton
import com.example.ui.theme.AccendPalette
import com.example.ui.theme.AccendThemeTokens
import com.example.ui.theme.MonoFontFamily
import com.example.util.Format
import com.example.util.HapticLevel
import com.example.util.Haptics
import kotlinx.coroutines.delay

@Composable
private fun KeepScreenOnEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
private fun CountdownRing(
    remainingSeconds: Long,
    totalSeconds: Long,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds <= 0L) 0f else (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    Box(modifier = modifier.size(256.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f + 12.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = AccendPalette.SurfaceHighlight,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke + 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = AccendPalette.AccentWhite,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = Format.timerMmSs(remainingSeconds),
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = MonoFontFamily,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold
            ),
            color = AccendPalette.AccentWhite
        )
    }
}

/**
 * Pure Meditation Timer (PRD §6.6):
 * Timestamp-based (survives backgrounding), keeps screen on, restlessness check on finish.
 */
@Composable
fun MeditationScreen(
    weekNumber: Int,
    targetMinutes: Int,
    hapticLevel: HapticLevel,
    onCompleted: (minutes: Int, restlessness: String) -> Unit,
    onExit: () -> Unit
) {
    KeepScreenOnEffect()
    val c = AccendThemeTokens.colors
    val view = LocalView.current

    val totalSeconds = (targetMinutes.coerceAtLeast(1) * 60L)
    var phase by rememberSaveable { mutableIntStateOf(0) } // 0=pre, 1=running, 2=restlessness
    var endElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    var remainingSeconds by rememberSaveable { mutableLongStateOf(totalSeconds) }
    var selectedRestlessness by rememberSaveable { mutableStateOf<String?>(null) }
    var showEndEarlyDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = phase == 1) {
        showEndEarlyDialog = true
    }

    LaunchedEffect(phase, endElapsedMs) {
        if (phase == 1) {
            while (true) {
                val remMs = endElapsedMs - SystemClock.elapsedRealtime()
                if (remMs <= 0L) {
                    remainingSeconds = 0L
                    Haptics.heavy(view, hapticLevel)
                    phase = 2
                    break
                } else {
                    remainingSeconds = (remMs + 999L) / 1000L
                }
                delay(250L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AccendPalette.BgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MEDITATION · WEEK $weekNumber",
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted
            )
            if (phase == 0) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Outlined.Close, "Close", tint = c.textSecondary)
                }
            }
        }

        when (phase) {
            0 -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$targetMinutes MIN",
                        style = MaterialTheme.typography.displayLarge.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Week $weekNumber · Stillness Protocol",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.textSecondary
                    )
                }
                PrimaryWhiteButton(
                    text = "BEGIN SESSION",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {
                        endElapsedMs = SystemClock.elapsedRealtime() + totalSeconds * 1000L
                        remainingSeconds = totalSeconds
                        phase = 1
                    }
                )
            }

            1 -> {
                CountdownRing(remainingSeconds = remainingSeconds, totalSeconds = totalSeconds)
                TextButton(onClick = { showEndEarlyDialog = true }) {
                    Text("END EARLY", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                }
            }

            2 -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("SESSION COMPLETE", style = MaterialTheme.typography.labelMedium, color = c.success)
                    Text("How restless was your mind?", style = MaterialTheme.typography.headlineMedium, color = AccendPalette.AccentWhite)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("a lot", "a little", "still").forEach { option ->
                            val selected = selectedRestlessness == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) AccendPalette.AccentWhite else c.surfaceCard)
                                    .border(1.dp, if (selected) AccendPalette.AccentWhite else c.borderStrong, RoundedCornerShape(14.dp))
                                    .clickable { selectedRestlessness = option }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) AccendPalette.OnAccentWhite else c.textPrimary
                                )
                            }
                        }
                    }
                }
                PrimaryWhiteButton(
                    text = "DONE",
                    enabled = selectedRestlessness != null,
                    onClick = {
                        selectedRestlessness?.let { onCompleted(targetMinutes, it) }
                    }
                )
            }
        }
    }

    if (showEndEarlyDialog) {
        ConfirmActionDialog(
            title = "End session?",
            body = "Ending early won't be logged.",
            confirmText = "End Session",
            isDanger = true,
            onConfirm = {
                showEndEarlyDialog = false
                onExit()
            },
            onDismiss = { showEndEarlyDialog = false }
        )
    }
}

/**
 * Full-Screen Deep Work Timer (PRD §6.7):
 * Presets 15/30/45/60, Pause/Resume/Stop, logs partial on Stop if >=1 min (completed=false),
 * auto-ticks only when full session completes.
 */
@Composable
fun DeepWorkScreen(
    initialMinutes: Int,
    hapticLevel: HapticLevel,
    onLogSession: (minutes: Int, completed: Boolean) -> Unit,
    onExit: () -> Unit
) {
    KeepScreenOnEffect()
    val c = AccendThemeTokens.colors
    val view = LocalView.current

    var targetMinutes by rememberSaveable { mutableIntStateOf(initialMinutes.coerceIn(15, 60)) }
    var phase by rememberSaveable { mutableIntStateOf(0) } // 0=pre, 1=running, 2=paused, 3=completed
    var endElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    var remainingMs by rememberSaveable { mutableLongStateOf(targetMinutes * 60_000L) }
    var showStopDialog by remember { mutableStateOf(false) }

    val totalMs = targetMinutes * 60_000L

    BackHandler(enabled = phase == 1 || phase == 2) {
        showStopDialog = true
    }

    LaunchedEffect(phase, endElapsedMs) {
        if (phase == 1) {
            while (true) {
                val rem = endElapsedMs - SystemClock.elapsedRealtime()
                if (rem <= 0L) {
                    remainingMs = 0L
                    Haptics.heavy(view, hapticLevel)
                    phase = 3
                    break
                } else {
                    remainingMs = rem
                }
                delay(250L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AccendPalette.BgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DEEP WORK PROTOCOL", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            if (phase == 0) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Outlined.Close, "Close", tint = c.textSecondary)
                }
            }
        }

        when (phase) {
            0 -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$targetMinutes MIN",
                        style = MaterialTheme.typography.displayLarge.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            val active = targetMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) AccendPalette.AccentWhite else c.surfaceCard)
                                    .border(1.dp, if (active) AccendPalette.AccentWhite else c.borderStrong, RoundedCornerShape(12.dp))
                                    .clickable {
                                        targetMinutes = mins
                                        remainingMs = mins * 60_000L
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "${mins}M",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) AccendPalette.OnAccentWhite else c.textSecondary
                                )
                            }
                        }
                    }
                }
                PrimaryWhiteButton(
                    text = "START DEEP WORK",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {
                        remainingMs = targetMinutes * 60_000L
                        endElapsedMs = SystemClock.elapsedRealtime() + remainingMs
                        phase = 1
                    }
                )
            }

            1, 2 -> {
                CountdownRing(
                    remainingSeconds = (remainingMs + 999L) / 1000L,
                    totalSeconds = totalMs / 1000L
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryDarkButton(
                        text = if (phase == 1) "PAUSE" else "RESUME",
                        icon = if (phase == 1) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        onClick = {
                            if (phase == 1) {
                                remainingMs = (endElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                                phase = 2
                            } else {
                                endElapsedMs = SystemClock.elapsedRealtime() + remainingMs
                                phase = 1
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryDarkButton(
                        text = "STOP",
                        icon = Icons.Outlined.Stop,
                        onClick = { showStopDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            3 -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SESSION COMPLETE", style = MaterialTheme.typography.labelMedium, color = c.success)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$targetMinutes MIN LOCKED",
                        style = MaterialTheme.typography.displayMedium.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                }
                PrimaryWhiteButton(
                    text = "DONE",
                    onClick = { onLogSession(targetMinutes, true) }
                )
            }
        }
    }

    if (showStopDialog) {
        val elapsedMin = ((totalMs - remainingMs).coerceAtLeast(0L) / 60_000L).toInt()
        if (elapsedMin >= 1) {
            ConfirmActionDialog(
                title = "Log $elapsedMin min as partial?",
                body = "Partial sessions add to your weekly Deep Work chart but do not tick today's Deep Work protocol.",
                confirmText = "Log Partial",
                onConfirm = {
                    showStopDialog = false
                    onLogSession(elapsedMin, false)
                },
                onDismiss = {
                    showStopDialog = false
                    onExit()
                }
            )
        } else {
            ConfirmActionDialog(
                title = "Discard session?",
                body = "Less than 1 minute elapsed. Stopping now discards this session.",
                confirmText = "Discard",
                isDanger = true,
                onConfirm = {
                    showStopDialog = false
                    onExit()
                },
                onDismiss = { showStopDialog = false }
            )
        }
    }
}
