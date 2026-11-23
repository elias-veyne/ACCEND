package com.example.ui.money

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MoneyTransactionEntity
import com.example.data.db.SkillLogEntity
import com.example.ui.components.AccendCard
import com.example.ui.components.AccendInnerBox
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PrimaryWhiteButton
import com.example.ui.components.SecondaryDarkButton
import com.example.ui.components.TacticalBadge
import com.example.ui.theme.AccendPalette
import com.example.ui.theme.AccendThemeTokens
import com.example.ui.theme.MonoFontFamily
import com.example.util.AccendDate
import com.example.util.Format
import kotlin.math.roundToLong

data class MoneyUiState(
    val balance: Long = 0L,
    val savedJar: Long = 0L,
    val savePercent: Int = 40,
    val funThisWeek: Long = 0L,
    val funLastWeek: Long = 0L,
    val totalSkillHours: Double = 0.0,
    val recentSkills: List<SkillLogEntity> = emptyList(),
    val recentSkillTags: List<String> = emptyList(),
    val runwayDays: Int = 0,
    val runwayTargetDate: String = "2027-06-01",
    val transactions: List<MoneyTransactionEntity> = emptyList(),
    val today: AccendDate = AccendDate.today()
)

private val IN_SOURCES = listOf(
    "pocket_money" to "Pocket money",
    "gift" to "Gift",
    "freelance" to "Freelance",
    "other" to "Other"
)

private val OUT_CATEGORIES = listOf(
    "food" to "Food",
    "transport" to "Transport",
    "data" to "Data",
    "books" to "Books",
    "fun" to "Fun",
    "other" to "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    state: MoneyUiState,
    onSaveTransaction: (MoneyTransactionEntity, onSaved: (Long) -> Unit) -> Unit,
    onDeleteTransaction: (MoneyTransactionEntity) -> Unit,
    onUpdateImpulseAnswer: (Long, String?) -> Unit,
    onMoveSavings: (amount: Long, direction: String, linkedTxId: Long?) -> Unit,
    onAddSkillHours: (hours: Double, tag: String) -> Unit,
    onDeleteSkillLog: (SkillLogEntity) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors

    var editingTx by remember { mutableStateOf<MoneyTransactionEntity?>(null) }
    var showAddTxSheet by remember { mutableStateOf(false) }
    var addTxInitialType by remember { mutableStateOf("IN") }

    var showJarSheet by remember { mutableStateOf<String?>(null) } // "DEPOSIT" or "WITHDRAW"
    var autoSuggestJar by remember { mutableStateOf<Pair<Long, Long>?>(null) } // (suggestedAmount, linkedTxId)
    var impulseCheckTxId by remember { mutableStateOf<Long?>(null) }

    var showAddSkillSheet by remember { mutableStateOf(false) }
    var showSkillListSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Balance Capital Card (matches screen__3__5037bd4c.png)
            AccendCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCIPLINE CAPITAL · BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                    TacticalBadge(
                        text = "NPR LEDGER",
                        contentColor = AccendPalette.AccentWhite
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "NPR ",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                        color = c.textMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = Format.number(state.balance),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp
                        ),
                        color = if (state.balance < 0) c.danger else AccendPalette.AccentWhite
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = c.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Active Spendable Cash · Jar Separate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryDarkButton(
                        text = "ADD INFLOW",
                        icon = Icons.Outlined.AddCircleOutline,
                        onClick = {
                            editingTx = null
                            addTxInitialType = "IN"
                            showAddTxSheet = true
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "money_add_in_button"
                    )
                    SecondaryDarkButton(
                        text = "LOG EXPENSE",
                        icon = Icons.Outlined.RemoveCircleOutline,
                        onClick = {
                            editingTx = null
                            addTxInitialType = "OUT"
                            showAddTxSheet = true
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "money_add_out_button"
                    )
                }
            }

            // 2. Discipline Vault — Save Jar Compound + Fun Leak Card (matches screen__3__5037bd4c.png)
            AccendCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.surfaceInner)
                                .border(1.dp, c.borderStrong, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Savings, null, tint = AccendPalette.AccentWhite, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("DISCIPLINE VAULT", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                            Text("Save Jar Compound", style = MaterialTheme.typography.titleMedium, color = AccendPalette.AccentWhite)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccendPalette.AccentWhite)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COMPOUNDING",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = AccendPalette.OnAccentWhite
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = Format.npr(state.savedJar),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = AccendPalette.AccentWhite
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SAVED · ${state.savePercent}% AUTO-RULE",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryDarkButton(
                        text = "ADD TO JAR",
                        onClick = { showJarSheet = "DEPOSIT" },
                        modifier = Modifier.weight(1f),
                        testTag = "jar_add_button"
                    )
                    SecondaryDarkButton(
                        text = "WITHDRAW",
                        onClick = { showJarSheet = "WITHDRAW" },
                        modifier = Modifier.weight(1f),
                        testTag = "jar_withdraw_button"
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Leak Card (Fun Spending This Week — PRD §6.4, factual, no lecture)
                AccendInnerBox(
                    backgroundColor = if (state.funThisWeek > 0) c.dangerBg.copy(alpha = 0.45f) else c.surfaceInner,
                    borderColor = if (state.funThisWeek > 0) c.dangerBorder else c.borderSubtle
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = if (state.funThisWeek > 0) c.danger else c.textSecondary,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FUN SPENDING THIS WEEK",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (state.funThisWeek > 0) c.danger else c.textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = Format.npr(state.funThisWeek),
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                                color = AccendPalette.AccentWhite
                            )
                            Text(
                                text = "Previous week: ${Format.npr(state.funLastWeek)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textMuted
                            )
                        }
                    }
                }
            }

            // 3. Skill Hours & Runway to 18 Card (PRD §6.4)
            AccendCard(onClick = { showSkillListSheet = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Flag, null, tint = AccendPalette.AccentWhite, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "RUNWAY TO 18 & SKILL HOURS",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                            color = AccendPalette.AccentWhite
                        )
                    }
                    Text(
                        text = state.runwayTargetDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccendInnerBox(modifier = Modifier.weight(1f)) {
                        Text("RUNWAY", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (state.runwayDays > 0) "${state.runwayDays} DAYS" else "REACHED",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                            color = AccendPalette.AccentWhite
                        )
                    }
                    AccendInnerBox(modifier = Modifier.weight(1f)) {
                        Text("SKILL HOURS", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${Format.decimal1(state.totalSkillHours)} H",
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                            color = AccendPalette.AccentWhite
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap card to view skill history (${state.recentSkills.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textMuted
                    )
                    SecondaryDarkButton(
                        text = "ADD HOURS",
                        icon = Icons.Outlined.School,
                        onClick = { showAddSkillSheet = true },
                        testTag = "money_add_skill_button"
                    )
                }
            }

            // 4. Transaction Ledger (grouped by date — matches screen__3__5037bd4c.png)
            AccendCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ReceiptLong, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "TRANSACTION LEDGER",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                            color = c.textSecondary
                        )
                    }
                    Text(
                        text = "${state.transactions.size} ENTRIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textPrimary
                    )
                }
                Spacer(Modifier.height(14.dp))

                if (state.transactions.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Outlined.ReceiptLong,
                        message = "No entries yet. Tap + to log money in or out."
                    )
                } else {
                    val grouped = remember(state.transactions) {
                        state.transactions.groupBy { it.date }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        grouped.forEach { (isoDate, txs) ->
                            val parsed = AccendDate.parse(isoDate)
                            Text(
                                text = Format.relativeDateLabel(parsed, state.today),
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textMuted
                            )
                            txs.forEach { tx ->
                                val isIn = tx.type == "IN"
                                val isFun = !isIn && tx.category == "fun"
                                val labelTitle = tx.note?.takeIf { it.isNotBlank() }
                                    ?: formatSourceOrCategory(if (isIn) tx.source else tx.category)
                                val subCaption = formatSourceOrCategory(if (isIn) tx.source else tx.category)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(c.surfaceInner)
                                        .border(1.dp, c.borderSubtle, RoundedCornerShape(12.dp))
                                        .clickable {
                                            editingTx = tx
                                            addTxInitialType = tx.type
                                            showAddTxSheet = true
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isFun) c.dangerBg else c.surfaceHighlight)
                                            .border(
                                                1.dp,
                                                if (isFun) c.dangerBorder else c.borderStrong,
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isIn) Icons.Outlined.SouthWest else Icons.Outlined.NorthEast,
                                            contentDescription = if (isIn) "Money In" else "Money Out",
                                            tint = if (isIn) c.success else if (isFun) c.danger else AccendPalette.AccentWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = labelTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = c.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = subCaption,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = c.textMuted
                                            )
                                            if (tx.impulseAnswer != null) {
                                                TacticalBadge(
                                                    text = tx.impulseAnswer,
                                                    containerColor = if (tx.impulseAnswer == "WANT") c.dangerBg else c.surfaceHighlight,
                                                    contentColor = if (tx.impulseAnswer == "WANT") c.danger else c.textSecondary,
                                                    borderColor = if (tx.impulseAnswer == "WANT") c.dangerBorder else c.borderStrong
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isIn) Format.npr(tx.amount, showPlus = true) else Format.npr(-tx.amount),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = MonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isIn) c.success else if (isFun) c.danger else AccendPalette.AccentWhite
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteTransaction(tx) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete transaction",
                                            tint = c.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stoic discipline quote footer (matches screen__3__5037bd4c.png)
            AccendCard(contentPadding = PaddingValues(16.dp)) {
                Text(
                    text = "\"True discipline is quantified by what you are willing to forfeit when resolve wavers.\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = c.textSecondary
                )
            }

            Spacer(Modifier.height(74.dp))
        }

        // Bright White Circular FAB (matches screen__3__5037bd4c.png)
        FloatingActionButton(
            onClick = {
                editingTx = null
                addTxInitialType = "IN"
                showAddTxSheet = true
            },
            shape = CircleShape,
            containerColor = AccendPalette.AccentWhite,
            contentColor = AccendPalette.OnAccentWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .testTag("money_fab_add")
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add transaction", modifier = Modifier.size(26.dp))
        }
    }

    // Add / Edit Transaction Modal Sheet
    if (showAddTxSheet) {
        AddTransactionSheet(
            initial = editingTx,
            initialType = addTxInitialType,
            today = state.today,
            onDismiss = { showAddTxSheet = false },
            onSave = { tx ->
                val isNew = editingTx == null
                onSaveTransaction(tx) { savedId ->
                    showAddTxSheet = false
                    if (isNew && tx.type == "IN" && state.savePercent > 0) {
                        val suggested = (tx.amount * state.savePercent / 100.0).roundToLong()
                        if (suggested > 0) {
                            autoSuggestJar = suggested to savedId
                        }
                    } else if (isNew && tx.type == "OUT" && tx.category == "fun") {
                        impulseCheckTxId = savedId
                    }
                }
            }
        )
    }

    // Auto-suggest Save Jar Bottom Sheet (PRD §6.4)
    autoSuggestJar?.let { (suggestedAmount, linkedTxId) ->
        ModalBottomSheet(
            onDismissRequest = { autoSuggestJar = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("DISCIPLINE VAULT RULE", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                Text(
                    text = "Move ${Format.npr(suggestedAmount)} to your jar?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccendPalette.AccentWhite
                )
                Text(
                    text = "Based on your ${state.savePercent}% auto-save rule.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryDarkButton(
                        text = "SKIP",
                        onClick = { autoSuggestJar = null },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onMoveSavings(suggestedAmount, "DEPOSIT", linkedTxId)
                            autoSuggestJar = null
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccendPalette.AccentWhite,
                            contentColor = AccendPalette.OnAccentWhite
                        )
                    ) {
                        Text("MOVE", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // Non-blocking Fun Impulse Check Modal (PRD §6.4)
    impulseCheckTxId?.let { txId ->
        AlertDialog(
            onDismissRequest = { impulseCheckTxId = null },
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Need or want right now?", style = MaterialTheme.typography.titleLarge, color = AccendPalette.AccentWhite)
            },
            text = {
                Text(
                    text = "Your transaction is already saved. Tagging impulse decisions sharpens awareness.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateImpulseAnswer(txId, "NEED")
                        impulseCheckTxId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccendPalette.AccentWhite,
                        contentColor = AccendPalette.OnAccentWhite
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("NEED") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { impulseCheckTxId = null }) {
                        Text("SKIP", color = c.textMuted)
                    }
                    Button(
                        onClick = {
                            onUpdateImpulseAnswer(txId, "WANT")
                            impulseCheckTxId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = c.danger, contentColor = AccendPalette.AccentWhite),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("WANT") }
                }
            }
        )
    }

    // Save Jar Deposit / Withdraw Sheet
    showJarSheet?.let { direction ->
        var amountInput by remember { mutableStateOf("") }
        val maxAllowed = if (direction == "DEPOSIT") state.balance.coerceAtLeast(0L) else state.savedJar
        ModalBottomSheet(
            onDismissRequest = { showJarSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (direction == "DEPOSIT") "ADD TO SAVE JAR" else "WITHDRAW FROM JAR",
                    style = MaterialTheme.typography.titleLarge,
                    color = AccendPalette.AccentWhite
                )
                Text(
                    text = "Available: ${Format.npr(maxAllowed)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) amountInput = it },
                    label = { Text("Amount (NPR)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                PrimaryWhiteButton(
                    text = if (direction == "DEPOSIT") "DEPOSIT" else "WITHDRAW",
                    onClick = {
                        val amt = amountInput.toLongOrNull() ?: 0L
                        when {
                            amt <= 0L -> onShowMessage("Enter an amount greater than 0.")
                            amt > maxAllowed -> onShowMessage("Amount exceeds available ${Format.npr(maxAllowed)}.")
                            else -> {
                                onMoveSavings(amt, direction, null)
                                showJarSheet = null
                            }
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // Add Skill Hours Sheet
    if (showAddSkillSheet) {
        var hours by remember { mutableStateOf(1.0) }
        var tag by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showAddSkillSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("LOG SKILL HOURS", style = MaterialTheme.typography.titleLarge, color = AccendPalette.AccentWhite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryDarkButton(text = "-0.25 H", onClick = { if (hours > 0.25) hours -= 0.25 })
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.2f", hours)} H",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFontFamily),
                        color = AccendPalette.AccentWhite
                    )
                    SecondaryDarkButton(text = "+0.25 H", onClick = { if (hours < 24.0) hours += 0.25 })
                }
                OutlinedTextField(
                    value = tag,
                    onValueChange = { if (it.length <= 32) tag = it },
                    label = { Text("Skill Tag (e.g. Coding, Design, Sales)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.recentSkillTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.recentSkillTags.forEach { t ->
                            TacticalBadge(
                                text = t,
                                contentColor = AccendPalette.AccentWhite,
                                modifier = Modifier.clickable { tag = t }
                            )
                        }
                    }
                }
                PrimaryWhiteButton(
                    text = "BANK SKILL HOURS",
                    onClick = {
                        onAddSkillHours(hours, tag)
                        showAddSkillSheet = false
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // Recent Skill Logs Sheet
    if (showSkillListSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSkillListSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surfaceElevated,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("SKILL HOURS LOG", style = MaterialTheme.typography.titleLarge, color = AccendPalette.AccentWhite)
                if (state.recentSkills.isEmpty()) {
                    Text("No skill hours banked yet.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                } else {
                    state.recentSkills.take(15).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.surfaceInner)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(log.tag, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                                Text(log.date, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${Format.decimal1(log.hours)} h",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                    color = AccendPalette.AccentWhite
                                )
                                IconButton(onClick = { onDeleteSkillLog(log) }) {
                                    Icon(Icons.Outlined.DeleteOutline, "Delete skill log", tint = c.textMuted)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    initial: MoneyTransactionEntity?,
    initialType: String,
    today: AccendDate,
    onDismiss: () -> Unit,
    onSave: (MoneyTransactionEntity) -> Unit
) {
    val c = AccendThemeTokens.colors
    var type by remember { mutableStateOf(initial?.type ?: initialType) }
    var amountStr by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var source by remember { mutableStateOf(initial?.source ?: "pocket_money") }
    var category by remember { mutableStateOf(initial?.category ?: "food") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dateIso by remember { mutableStateOf(initial?.date ?: today.toIsoString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initial == null) "NEW LEDGER ENTRY" else "EDIT LEDGER ENTRY",
                style = MaterialTheme.typography.titleLarge,
                color = AccendPalette.AccentWhite
            )

            // Segmented IN | OUT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceInner)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("IN" to "MONEY IN", "OUT" to "MONEY OUT").forEach { (key, label) ->
                    val selected = type == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AccendPalette.AccentWhite else c.surfaceInner)
                            .clickable { type = key }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) AccendPalette.OnAccentWhite else c.textMuted
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountStr,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) amountStr = it },
                label = { Text("Amount (Whole NPR)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccendPalette.AccentWhite,
                    unfocusedBorderColor = c.borderStrong
                )
            )

            Text(
                text = if (type == "IN") "SOURCE" else "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted
            )
            val chips = if (type == "IN") IN_SOURCES else OUT_CATEGORIES
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { (key, label) ->
                    val active = if (type == "IN") source == key else category == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) AccendPalette.AccentWhite else c.surfaceInner)
                            .border(1.dp, if (active) AccendPalette.AccentWhite else c.borderStrong, RoundedCornerShape(10.dp))
                            .clickable {
                                if (type == "IN") source = key else category = key
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) AccendPalette.OnAccentWhite else c.textSecondary
                        )
                    }
                }
            }

            // Date selector quick pills (Today, Yesterday, -2d)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    today.toIsoString() to "Today",
                    today.minusDays(1).toIsoString() to "Yesterday",
                    today.minusDays(2).toIsoString() to today.minusDays(2).formatShort()
                ).forEach { (iso, lbl) ->
                    val active = dateIso == iso
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) AccendPalette.AccentWhite else c.surfaceInner)
                            .clickable { dateIso = iso }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) AccendPalette.OnAccentWhite else c.textSecondary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 80) note = it },
                label = { Text("Note (optional, max 80 chars)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMsg != null) {
                Text(errorMsg!!, style = MaterialTheme.typography.labelSmall, color = c.danger)
            }

            PrimaryWhiteButton(
                text = "SAVE ENTRY",
                onClick = {
                    val amount = amountStr.toLongOrNull() ?: 0L
                    val parsedDate = AccendDate.parseOrNull(dateIso)
                    when {
                        amount <= 0L || amount > 99_999_999L -> errorMsg = "Enter a valid NPR amount (1 to 99,999,999)."
                        parsedDate == null || parsedDate > today -> errorMsg = "Date cannot be in the future."
                        else -> {
                            onSave(
                                MoneyTransactionEntity(
                                    id = initial?.id ?: 0L,
                                    date = dateIso,
                                    type = type,
                                    amount = amount,
                                    category = if (type == "OUT") category else null,
                                    source = if (type == "IN") source else null,
                                    note = note.trim().ifEmpty { null },
                                    impulseAnswer = initial?.impulseAnswer
                                )
                            )
                        }
                    }
                }
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

private fun formatSourceOrCategory(raw: String?): String = when (raw) {
    "pocket_money" -> "Pocket Money"
    "gift" -> "Gift"
    "freelance" -> "Freelance"
    "food" -> "Food"
    "transport" -> "Transport"
    "data" -> "Data"
    "books" -> "Books"
    "fun" -> "Fun"
    "other" -> "Other"
    else -> "Entry"
}
