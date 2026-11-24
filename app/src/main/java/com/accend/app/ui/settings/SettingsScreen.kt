package com.accend.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accend.app.AppConfig
import com.accend.app.data.prefs.AccendPrefs
import com.accend.app.ui.components.AccendCard
import com.accend.app.ui.components.AccendInnerBox
import com.accend.app.ui.components.NumberStepper
import com.accend.app.ui.components.PfpAvatar
import com.accend.app.ui.components.PrimaryWhiteButton
import com.accend.app.ui.components.SecondaryDarkButton
import com.accend.app.ui.components.SectionHeaderRow
import com.accend.app.ui.components.TacticalBadge
import com.accend.app.ui.components.TacticalSwitch
import com.accend.app.ui.theme.AccendPalette
import com.accend.app.ui.theme.AccendThemeTokens
import com.accend.app.ui.theme.MonoFontFamily
import com.accend.app.util.Format
import com.accend.app.util.HapticLevel
import com.accend.app.util.Haptics
import com.accend.app.work.Notifications
import kotlin.math.roundToInt

data class SettingsUiState(
    val userName: String = "Operator",
    val pfpUri: String? = null,
    val dayNumber: Int = 1,
    val savedJar: Long = 0L,
    val prefs: AccendPrefs = AccendPrefs(),
    val pendingProgressionsCount: Int = 0
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onUpdateProfile: (name: String, newPhotoUri: Uri?, removePhoto: Boolean) -> Unit,
    onUpdateNotifications: (
        master: Boolean?,
        daily: Boolean?,
        dailyTime: String?,
        miss: Boolean?,
        missText: String?
    ) -> Unit,
    onUpdateTargets: (
        screenMin: Int?,
        readingTarget: Int?,
        deepWorkMin: Int?,
        savePct: Int?,
        weightKg: Float?,
        heightCm: Int?,
        runwayDate: String?
    ) -> Unit,
    onSetHapticLevel: (HapticLevel) -> Unit,
    onOpenProgression: () -> Unit,
    onOpenBodyLogs: () -> Unit,
    onOpenDataScreen: () -> Unit,
    onSaveProtocolConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val notifAllowed = remember { Notifications.hasPermission(context) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMissTextDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }
    var showTargetsEditDialog by remember { mutableStateOf(false) }

    var screenSliderMin by remember(state.prefs.screenTargetMinutes) {
        mutableFloatStateOf(state.prefs.screenTargetMinutes.toFloat())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Notification Blocked Banner (PRD §12)
        if (!notifAllowed) {
            AccendInnerBox(borderColor = c.warning, backgroundColor = c.warningBg) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications are blocked in system settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.warning,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        runCatching { context.startActivity(intent) }
                    }) {
                        Text("OPEN", color = AccendPalette.AccentWhite, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 1. Operator Profile Card (matches screen__4__89edfdfc.png)
        AccendCard(
            onClick = { showEditProfileDialog = true },
            modifier = Modifier.testTag("settings_profile_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AccendPalette.AccentWhite))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ENFORCEMENT ACTIVE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccendPalette.AccentWhite
                    )
                }
                TacticalBadge(text = AppConfig.APP_VERSION_DISPLAY, contentColor = c.textSecondary)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PfpAvatar(name = state.userName, pfpUri = state.pfpUri, size = 54.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.userName.uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AccendPalette.AccentWhite
                    )
                    Text(
                        text = "DAY ${state.dayNumber} · TAP TO EDIT PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("CLASSIFICATION", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    Text(
                        text = "OPERATOR",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = AccendPalette.AccentWhite
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            AccendInnerBox {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Shield, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("DISCIPLINE VAULT · SAVE RULE ${state.prefs.savePercent}%", style = MaterialTheme.typography.labelMedium, color = c.textPrimary)
                            Text("Runway target: ${state.prefs.runwayTargetDate}", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                        }
                    }
                    Text(
                        text = Format.npr(state.savedJar),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                }
            }
        }

        // 2. THRESHOLDS & ESCALATIONS (matches screen__4__89edfdfc.png)
        SectionHeaderRow(
            title = "THRESHOLDS & ESCALATIONS",
            trailing = "2 ALERT TYPES ONLY",
            leadingIcon = Icons.Outlined.NotificationsActive
        )

        // Screen Time Hard Ceiling Card
        AccendCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Screen Time Hard Ceiling", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                        TacticalBadge(
                            text = "STRIKE RULE",
                            containerColor = c.dangerDeep,
                            contentColor = AccendPalette.AccentWhite,
                            borderColor = c.danger
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Flags over-ceiling usage on Daily Work & Analysis charts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AccendInnerBox {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("THRESHOLD CEILING", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    Text(
                        text = Format.screenTimeTactical(screenSliderMin.roundToInt()),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                }
                Slider(
                    value = screenSliderMin,
                    onValueChange = { screenSliderMin = (it / 15f).roundToInt() * 15f },
                    onValueChangeFinished = {
                        onUpdateTargets(screenSliderMin.roundToInt(), null, null, null, null, null, null)
                    },
                    valueRange = 30f..480f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccendPalette.AccentWhite,
                        activeTrackColor = AccendPalette.AccentWhite,
                        inactiveTrackColor = c.surfaceHighlight
                    )
                )
            }
        }

        // Master Notifications + Daily Reminder + 2-Day Miss Alert
        AccendCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Master Notifications", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                    Text("Only two notification types exist in Accend.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
                TacticalSwitch(
                    checked = state.prefs.notificationsEnabled,
                    onCheckedChange = { onUpdateNotifications(it, null, null, null, null) }
                )
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = c.borderSubtle)
            Spacer(Modifier.height(12.dp))

            // Daily Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Protocol Reminder", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                    Text("Fires only when pending protocols remain.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
                TacticalSwitch(
                    checked = state.prefs.dailyReminderEnabled,
                    onCheckedChange = { onUpdateNotifications(null, it, null, null, null) }
                )
            }
            Spacer(Modifier.height(10.dp))
            AccendInnerBox(onClick = { showReminderTimeDialog = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Alarm, null, tint = c.textPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("TRIGGER CADENCE", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    }
                    TacticalBadge(
                        text = "${state.prefs.dailyReminderTime} SHARP",
                        contentColor = AccendPalette.AccentWhite
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = c.borderSubtle)
            Spacer(Modifier.height(12.dp))

            // 2-Day Miss Alert
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("2-Day Miss Alert", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                    Text("Fires once at 07:30 after 2 consecutive zero-completion days.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
                TacticalSwitch(
                    checked = state.prefs.missAlertEnabled,
                    onCheckedChange = { onUpdateNotifications(null, null, null, it, null) }
                )
            }
            Spacer(Modifier.height(10.dp))
            AccendInnerBox(onClick = { showMissTextDialog = true }) {
                Text("2-DAY MISS BROADCAST TEXT (TAP TO EDIT)", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.prefs.notifText2DayMiss,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textPrimary
                )
            }
        }

        // 3. TARGETS & GOVERNANCE (PRD §6.5)
        SectionHeaderRow(
            title = "TARGETS & GOVERNANCE",
            trailing = "ENFORCED",
            leadingIcon = Icons.Outlined.Gavel
        )
        AccendCard {
            // Deep work default duration chips
            Text("Deep Work Default Duration", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { mins ->
                    val selected = state.prefs.deepWorkDefaultMinutes == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AccendPalette.AccentWhite else c.surfaceInner)
                            .border(1.dp, if (selected) AccendPalette.AccentWhite else c.borderStrong, RoundedCornerShape(10.dp))
                            .clickable { onUpdateTargets(null, null, mins, null, null, null, null) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${mins}M",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) AccendPalette.OnAccentWhite else c.textSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = c.borderSubtle)
            Spacer(Modifier.height(12.dp))

            // Reading pages target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reading Target (Pages/Day)", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                    Text("1 page uses a single tap checkbox.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
                NumberStepper(
                    value = state.prefs.readingTarget,
                    onDecrement = {
                        if (state.prefs.readingTarget > 1) {
                            onUpdateTargets(null, state.prefs.readingTarget - 1, null, null, null, null, null)
                        }
                    },
                    onIncrement = {
                        if (state.prefs.readingTarget < 200) {
                            onUpdateTargets(null, state.prefs.readingTarget + 1, null, null, null, null, null)
                        }
                    },
                    label = "${state.prefs.readingTarget}"
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = c.borderSubtle)
            Spacer(Modifier.height(12.dp))

            // Save %, Weight Goal, Height Goal, Runway Date summary -> opens calibration dialog
            AccendInnerBox(onClick = { showTargetsEditDialog = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Save % · Body Goals · Runway Date", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                        Text(
                            text = "Save ${state.prefs.savePercent}% · Weight ${Format.decimal1(state.prefs.weightGoalKg.toDouble())}kg · Growth window ${Format.cmToFeetInches(state.prefs.heightGoalCm)} (${state.prefs.heightGoalCm}cm) · Runway ${state.prefs.runwayTargetDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted
                        )
                    }
                    TacticalBadge(text = "CONFIGURE", contentColor = AccendPalette.AccentWhite)
                }
            }
        }

        // 4. TRAINING, BODY LOGS & DATA NAVIGATION
        AccendCard(contentPadding = PaddingValues(vertical = 6.dp, horizontal = 14.dp)) {
            SettingsNavRow(
                icon = Icons.Outlined.FitnessCenter,
                title = "Progression Ladders",
                subtitle = "Compound & volume levels across all 9 muscle groups",
                badgeText = if (state.pendingProgressionsCount > 0) "${state.pendingProgressionsCount} READY" else null,
                onClick = onOpenProgression,
                testTag = "settings_nav_progression"
            )
            HorizontalDivider(color = c.borderSubtle)
            SettingsNavRow(
                icon = Icons.Outlined.MonitorWeight,
                title = "Body Logs",
                subtitle = "Weight (kg) and height (cm) telemetry archive",
                onClick = onOpenBodyLogs,
                testTag = "settings_nav_body"
            )
            HorizontalDivider(color = c.borderSubtle)
            SettingsNavRow(
                icon = Icons.Outlined.Storage,
                title = "Data Export, Import & Reset",
                subtitle = "Offline JSON backup, restore, and journey reset",
                onClick = onOpenDataScreen,
                testTag = "settings_nav_data"
            )
        }

        // 5. HARDWARE & SENSORY SINK (matches screen__4__89edfdfc.png)
        SectionHeaderRow(
            title = "HARDWARE & SENSORY SINK",
            trailing = "TACTICAL",
            leadingIcon = Icons.Outlined.Tune
        )
        AccendCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Haptic Feedback Intensity", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                Text("TACTICAL GRADE", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceInner)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    HapticLevel.LOW to "LOW",
                    HapticLevel.MEDIUM to "MEDIUM",
                    HapticLevel.HIGH to "HIGH TACTICAL"
                ).forEach { (lvl, label) ->
                    val selected = state.prefs.hapticLevel == lvl
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) AccendPalette.AccentWhite else c.surfaceInner)
                            .clickable {
                                Haptics.tick(view, lvl)
                                onSetHapticLevel(lvl)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) AccendPalette.OnAccentWhite else c.textMuted
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AccendInnerBox {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.DarkMode, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Dark Mode Lock", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                            Text("Non-negotiable high-contrast telemetry", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, tint = c.textMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ALWAYS ON", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
            }
        }

        // SAVE CTA & RESET LINK (matches screen__4__89edfdfc.png)
        PrimaryWhiteButton(
            text = "SAVE DISCIPLINE PROTOCOL",
            icon = Icons.Outlined.Save,
            onClick = {
                Haptics.heavy(view, state.prefs.hapticLevel)
                onSaveProtocolConfirmed()
            },
            testTag = "settings_save_protocol_button"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenDataScreen)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Restore, null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "RESET JOURNEY DATA (REQUIRES CONFIRMATION)",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )
            }
        }

        // About Footer
        AccendInnerBox {
            Text("ACCEND · LOCAL-FIRST DISCIPLINE ENGINE", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
            Text("v2.0.0 · Offline SQLite · Typography: Inter & JetBrains Mono (OFL)", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }

        Spacer(Modifier.height(12.dp))
    }

    // Profile Edit Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = state.userName,
            hasPhoto = !state.pfpUri.isNullOrBlank(),
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, uri, remove ->
                onUpdateProfile(name, uri, remove)
                showEditProfileDialog = false
            }
        )
    }

    // Daily Reminder Time Dialog
    if (showReminderTimeDialog) {
        val parts = state.prefs.dailyReminderTime.split(":")
        var hour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 20) }
        var minute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }
        AlertDialog(
            onDismissRequest = { showReminderTimeDialog = false },
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Daily Reminder Time", color = AccendPalette.AccentWhite) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberStepper(
                        value = hour,
                        onDecrement = { hour = (hour + 23) % 24 },
                        onIncrement = { hour = (hour + 1) % 24 },
                        label = String.format(java.util.Locale.US, "%02dH", hour)
                    )
                    NumberStepper(
                        value = minute,
                        onDecrement = { minute = (minute + 55) % 60 },
                        onIncrement = { minute = (minute + 5) % 60 },
                        label = String.format(java.util.Locale.US, "%02dM", minute)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                        onUpdateNotifications(null, null, formatted, null, null)
                        showReminderTimeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccendPalette.AccentWhite, contentColor = AccendPalette.OnAccentWhite)
                ) { Text("SET") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimeDialog = false }) { Text("CANCEL", color = c.textSecondary) }
            }
        )
    }

    // 2-Day Miss Custom Text Dialog
    if (showMissTextDialog) {
        var text by remember { mutableStateOf(state.prefs.notifText2DayMiss) }
        AlertDialog(
            onDismissRequest = { showMissTextDialog = false },
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("2-Day Miss Alert Text", color = AccendPalette.AccentWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { if (it.length <= 160) text = it },
                        label = { Text("Message (max 160 chars)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { text = AppConfig.DEFAULT_TWO_DAY_MISS_TEXT }) {
                        Text("RESET TO DEFAULT", color = c.textSecondary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateNotifications(null, null, null, null, text)
                        showMissTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccendPalette.AccentWhite, contentColor = AccendPalette.OnAccentWhite)
                ) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showMissTextDialog = false }) { Text("CANCEL", color = c.textSecondary) }
            }
        )
    }

    // Targets Calibration Dialog
    if (showTargetsEditDialog) {
        var savePctStr by remember { mutableStateOf(state.prefs.savePercent.toString()) }
        var weightStr by remember { mutableStateOf(Format.decimal1(state.prefs.weightGoalKg.toDouble())) }
        var heightStr by remember { mutableStateOf(state.prefs.heightGoalCm.toString()) }
        var runwayStr by remember { mutableStateOf(state.prefs.runwayTargetDate) }
        AlertDialog(
            onDismissRequest = { showTargetsEditDialog = false },
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Configure Targets", color = AccendPalette.AccentWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = savePctStr,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) savePctStr = it },
                        label = { Text("Auto-Save Jar % (0-100)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Weight Goal (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) heightStr = it },
                        label = { Text("Height Goal cm — Growth window") },
                        supportingText = { Text("A direction, not a deadline.", color = c.textMuted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = runwayStr,
                        onValueChange = { if (it.length <= 10) runwayStr = it },
                        label = { Text("Runway Target Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTargets(
                            null,
                            null,
                            null,
                            savePctStr.toIntOrNull()?.coerceIn(0, 100),
                            weightStr.toFloatOrNull()?.coerceIn(25f, 250f),
                            heightStr.toIntOrNull()?.coerceIn(100, 240),
                            runwayStr.takeIf { com.accend.app.util.AccendDate.parseOrNull(it) != null }
                        )
                        showTargetsEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccendPalette.AccentWhite, contentColor = AccendPalette.OnAccentWhite)
                ) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetsEditDialog = false }) { Text("CANCEL", color = c.textSecondary) }
            }
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AccendPalette.AccentWhite, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        if (badgeText != null) {
            TacticalBadge(
                text = badgeText,
                containerColor = c.successBg,
                contentColor = c.success,
                borderColor = c.successBorder
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = c.textMuted)
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, newPhotoUri: Uri?, removePhoto: Boolean) -> Unit
) {
    val c = AccendThemeTokens.colors
    var name by remember { mutableStateOf(currentName) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var removePhoto by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pickedUri = uri
            removePhoto = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceElevated,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Edit Operator Profile", color = AccendPalette.AccentWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    label = { Text("Name (1–24 chars)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryDarkButton(
                        text = if (pickedUri != null) "PHOTO PICKED" else "CHANGE PHOTO",
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (hasPhoto && !removePhoto) {
                        SecondaryDarkButton(
                            text = "REMOVE",
                            onClick = {
                                removePhoto = true
                                pickedUri = null
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().length in 1..24,
                onClick = { onSave(name.trim(), pickedUri, removePhoto) },
                colors = ButtonDefaults.buttonColors(containerColor = AccendPalette.AccentWhite, contentColor = AccendPalette.OnAccentWhite)
            ) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = c.textSecondary) }
        }
    )
}
