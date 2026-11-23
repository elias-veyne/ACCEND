package com.example.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.db.ExerciseEntity
import com.example.data.db.HeightLogEntity
import com.example.data.db.MuscleGroup
import com.example.data.db.ProgressionLevel
import com.example.data.db.WeightLogEntity
import com.example.data.export.ExportImportManager
import com.example.data.export.ImportPreview
import com.example.domain.ProgressionRules
import com.example.ui.components.AccendCard
import com.example.ui.components.AccendInnerBox
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.PrimaryWhiteButton
import com.example.ui.components.SecondaryDarkButton
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.TacticalBadge
import com.example.ui.theme.AccendPalette
import com.example.ui.theme.AccendThemeTokens
import com.example.ui.theme.MonoFontFamily
import com.example.util.AccendDate
import com.example.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun SubScreenTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    val c = AccendThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bgBase)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = AccendPalette.AccentWhite
                )
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = AccendPalette.AccentWhite
                )
                Text(subtitle.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textMuted)
            }
        }
        HorizontalDivider(color = c.borderSubtle)
    }
}

data class ProgressionRowUi(
    val exercise: ExerciseEntity,
    val levels: List<ProgressionLevel>,
    val canLevelUp: Boolean,
    val canLevelDown: Boolean,
    val nextPreview: ProgressionLevel?,
    val isReady: Boolean
)

@Composable
fun ProgressionScreen(
    rows: List<ProgressionRowUi>,
    onLevelUp: (Long) -> Unit,
    onLevelDown: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onBack: () -> Unit
) {
    val c = AccendThemeTokens.colors
    var pendingConfirm by remember { mutableStateOf<ProgressionRowUi?>(null) }

    val grouped = remember(rows) {
        MuscleGroup.entries.mapNotNull { group ->
            val items = rows.filter { it.exercise.muscleGroup == group }
            if (items.isEmpty()) null else group to items
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bgBase)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar("Progression", "Compound & Volume Ladders", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            grouped.forEach { (group, items) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeaderRow(title = group.displayName, trailing = "${items.size} EXERCISES")
                    items.forEach { item ->
                        val ex = item.exercise
                        val weekday = when (ex.dayOfWeek) {
                            1 -> "SUN"; 2 -> "MON"; 3 -> "TUE"; 4 -> "WED"; 5 -> "THU"; 6 -> "FRI"; else -> "SAT"
                        }
                        val targetText = Format.exerciseTarget(ex.sets, ex.targetReps, ex.unit)
                        val levelSubtitle = if (item.levels.size > 1) {
                            "Level ${ex.currentLevelIndex + 1} of ${item.levels.size} · $weekday"
                        } else {
                            "Target ${targetText.ifEmpty { "Active" }} · $weekday"
                        }

                        AccendCard(
                            borderColor = if (item.isReady) c.successBorder else c.borderSubtle,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            if (item.isReady) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TacticalBadge(
                                        text = "READY TO LEVEL UP",
                                        containerColor = c.successBg,
                                        contentColor = c.success,
                                        borderColor = c.successBorder
                                    )
                                    Text(
                                        text = "NOT NOW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.textMuted,
                                        modifier = Modifier.clickable { onSnooze(ex.id) }.padding(4.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                                    Text(levelSubtitle, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                                    if (targetText.isNotEmpty()) {
                                        Text(targetText, style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (item.canLevelDown) {
                                        IconButton(
                                            onClick = { onLevelDown(ex.id) },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(c.surfaceInner)
                                        ) {
                                            Icon(Icons.Outlined.ArrowDownward, "Level down", tint = c.textSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (item.canLevelUp) AccendPalette.AccentWhite else c.surfaceHighlight)
                                            .clickable(enabled = item.canLevelUp) { pendingConfirm = item }
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.ArrowUpward,
                                                null,
                                                tint = if (item.canLevelUp) AccendPalette.OnAccentWhite else c.textMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (item.canLevelUp) "LEVEL UP" else "MAX",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (item.canLevelUp) AccendPalette.OnAccentWhite else c.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    pendingConfirm?.let { item ->
        val next = item.nextPreview
        if (next != null) {
            val nextTarget = Format.exerciseTarget(next.sets, next.target, item.exercise.unit)
            ConfirmActionDialog(
                title = "Level Up Exercise?",
                body = "${item.exercise.name} → ${next.name} · Target $nextTarget.\nApplies immediately.",
                confirmText = "Level Up",
                onConfirm = {
                    onLevelUp(item.exercise.id)
                    pendingConfirm = null
                },
                onDismiss = { pendingConfirm = null }
            )
        }
    }
}

@Composable
fun BodyLogsScreen(
    weights: List<WeightLogEntity>,
    heights: List<HeightLogEntity>,
    onAddWeight: (Double) -> Unit,
    onDeleteWeight: (WeightLogEntity) -> Unit,
    onAddHeight: (Double) -> Unit,
    onDeleteHeight: (HeightLogEntity) -> Unit,
    onBack: () -> Unit
) {
    val c = AccendThemeTokens.colors
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bgBase)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar("Body Logs", "Weight & Height Telemetry", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Log Today's Measurements
            AccendCard {
                Text("LOG TODAY (${AccendDate.today().formatShort().uppercase()})", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryDarkButton(
                        text = "SAVE",
                        onClick = {
                            val kg = weightInput.toDoubleOrNull()
                            if (kg != null && kg in 20.0..300.0) {
                                onAddWeight(kg)
                                weightInput = ""
                            }
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryDarkButton(
                        text = "SAVE",
                        onClick = {
                            val cm = heightInput.toDoubleOrNull()
                            if (cm != null && cm in 80.0..250.0) {
                                onAddHeight(cm)
                                heightInput = ""
                            }
                        }
                    )
                }
            }

            // Weight History
            AccendCard {
                SectionHeaderRow("Weight History", "${weights.size} LOGS")
                Spacer(Modifier.height(10.dp))
                if (weights.isEmpty()) {
                    Text("No weight entries logged yet.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                } else {
                    weights.forEach { w ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${Format.decimal1(w.kg)} kg", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily), color = AccendPalette.AccentWhite)
                                Text(w.date, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                            }
                            IconButton(onClick = { onDeleteWeight(w) }) {
                                Icon(Icons.Outlined.DeleteOutline, "Delete weight log", tint = c.textMuted)
                            }
                        }
                        HorizontalDivider(color = c.borderSubtle)
                    }
                }
            }

            // Height History
            AccendCard {
                SectionHeaderRow("Height History", "${heights.size} LOGS")
                Spacer(Modifier.height(10.dp))
                if (heights.isEmpty()) {
                    Text("No height entries logged yet.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                } else {
                    heights.forEach { h ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${Format.decimal1(h.cm)} cm", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily), color = AccendPalette.AccentWhite)
                                Text(h.date, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                            }
                            IconButton(onClick = { onDeleteHeight(h) }) {
                                Icon(Icons.Outlined.DeleteOutline, "Delete height log", tint = c.textMuted)
                            }
                        }
                        HorizontalDivider(color = c.borderSubtle)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DataScreen(
    exportImportManager: ExportImportManager,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val c = AccendThemeTokens.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingImport by remember { mutableStateOf<ImportPreview?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var resetStep by remember { mutableStateOf(0) } // 0=none, 1=first confirm, 2=type RESET
    var resetConfirmInput by remember { mutableStateOf("") }

    val saveDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { exportImportManager.writeExportToUri(uri) }.isSuccess
                }
                onShowMessage(if (ok) "Export saved to file." else "Failed to save export.")
            }
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (raw.isNullOrBlank()) {
                    importError = "Could not read the selected file."
                } else {
                    exportImportManager.validateImport(raw)
                        .onSuccess { pendingImport = it }
                        .onFailure { importError = it.message ?: "Invalid ACCEND JSON file." }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bgBase)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar("Data Governance", "Export, Import & Journey Reset", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Export
            AccendCard {
                Text("EXPORT LOCAL BACKUP (JSON)", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Exports profile, exercises, progression ladders, logs, money ledger, savings, skill hours, and preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted
                )
                Spacer(Modifier.height(14.dp))
                PrimaryWhiteButton(
                    text = "SHARE JSON EXPORT",
                    icon = Icons.Outlined.Share,
                    onClick = {
                        scope.launch {
                            runCatching {
                                val uri = withContext(Dispatchers.IO) { exportImportManager.exportToShareUri() }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share ACCEND Export"))
                            }.onFailure {
                                onShowMessage("Export failed: ${it.message}")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                SecondaryDarkButton(
                    text = "SAVE JSON TO DEVICE",
                    icon = Icons.Outlined.Download,
                    onClick = {
                        val stamp = AccendDate.today().toIsoString().replace("-", "")
                        saveDocLauncher.launch("accend_export_$stamp.json")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Import
            AccendCard {
                Text("IMPORT BACKUP (JSON)", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Restores an ACCEND JSON backup. Replaces all current records on this device in a single transaction.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted
                )
                Spacer(Modifier.height(14.dp))
                SecondaryDarkButton(
                    text = "SELECT JSON FILE",
                    icon = Icons.Outlined.Upload,
                    onClick = { openDocLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Danger Zone: Reset Journey
            AccendCard(borderColor = c.dangerBorder, backgroundColor = c.dangerBg.copy(alpha = 0.35f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = c.danger, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DANGER ZONE · RESET JOURNEY", style = MaterialTheme.typography.titleMedium, color = c.danger)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Wipes all workout logs, mind logs, money ledger, skills, and body logs. Re-seeds exercises at level 0 and resets to Day 1. Keeps your name, photo, and settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { resetStep = 1 },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.danger, contentColor = AccendPalette.AccentWhite)
                ) {
                    Text("RESET JOURNEY", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // Import confirmation dialog
    pendingImport?.let { preview ->
        ConfirmActionDialog(
            title = "Replace All Local Data?",
            body = "Operator: ${preview.userName}\nExercise logs: ${preview.exerciseLogsCount}\nTransactions: ${preview.transactionsCount}\nSkill logs: ${preview.skillLogsCount}\n\nThis replaces everything on this device.",
            confirmText = "Import",
            isDanger = true,
            onConfirm = {
                val raw = preview.rawJson
                pendingImport = null
                scope.launch {
                    val res = withContext(Dispatchers.IO) { exportImportManager.performImport(raw) }
                    res.onSuccess { onShowMessage("Backup imported successfully.") }
                        .onFailure { importError = it.message ?: "Import failed." }
                }
            },
            onDismiss = { pendingImport = null }
        )
    }

    // Import error dialog
    importError?.let { err ->
        AlertDialog(
            onDismissRequest = { importError = null },
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Import Rejected", color = c.danger) },
            text = { Text(err, color = c.textSecondary) },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text("OK", color = AccendPalette.AccentWhite) }
            }
        )
    }

    // Reset Step 1
    if (resetStep == 1) {
        ConfirmActionDialog(
            title = "Reset your journey?",
            body = "All logs, streaks, money entries, and progression levels will be wiped back to Day 1. Your name, photo, and preferences stay intact.",
            confirmText = "Continue",
            isDanger = true,
            onConfirm = {
                resetConfirmInput = ""
                resetStep = 2
            },
            onDismiss = { resetStep = 0 }
        )
    }

    // Reset Step 2 (Must type RESET)
    if (resetStep == 2) {
        AlertDialog(
            onDismissRequest = { resetStep = 0 },
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Final Confirmation", color = c.danger) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Type RESET in all caps to wipe your journey back to Day 1.", color = c.textSecondary)
                    OutlinedTextField(
                        value = resetConfirmInput,
                        onValueChange = { resetConfirmInput = it },
                        singleLine = true,
                        placeholder = { Text("RESET") }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = resetConfirmInput.trim() == "RESET",
                    onClick = {
                        resetStep = 0
                        scope.launch {
                            withContext(Dispatchers.IO) { exportImportManager.resetJourney() }
                            onShowMessage("Journey reset to Day 1.")
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.danger, contentColor = AccendPalette.AccentWhite)
                ) { Text("CONFIRM RESET") }
            },
            dismissButton = {
                TextButton(onClick = { resetStep = 0 }) { Text("CANCEL", color = c.textSecondary) }
            }
        )
    }
}
