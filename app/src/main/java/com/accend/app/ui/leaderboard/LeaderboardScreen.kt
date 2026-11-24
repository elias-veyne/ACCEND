package com.accend.app.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.accend.app.data.cloud.LeaderboardEntry
import com.accend.app.data.cloud.LevelCalculator
import com.accend.app.ui.theme.AccendPalette
import com.accend.app.ui.theme.AccendThemeTokens
import com.accend.app.ui.theme.MonoFontFamily

data class LeaderboardUiState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val currentUid: String? = null,
    val isSignedIn: Boolean = true
)

@Composable
fun LeaderboardScreen(
    state: LeaderboardUiState,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    val top = state.entries.take(3)
    val rest = state.entries.drop(3)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GLOBAL LEADERBOARD",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = c.textPrimary
                )
                Text(
                    text = "LIVE SYNCHRONIZED · TOP 50 OPERATORS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = c.textMuted
                )
            }
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = "Leaderboard",
                tint = c.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        if (!state.isSignedIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceCard)
                    .border(1.dp, c.borderSubtle, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SIGN IN TO ENTER THE ARENA",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textMuted
                )
            }
            return@Column
        }

        if (state.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceCard)
                    .border(1.dp, c.borderSubtle, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AWAITING FIRST TELEMETRY…",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textMuted
                )
            }
        }

        if (top.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val order = if (top.size == 3) listOf(top[1], top[0], top[2]) else top
                order.forEach { entry ->
                    PodiumCard(
                        entry = entry,
                        isCurrent = entry.uid == state.currentUid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (rest.isNotEmpty()) {
            Text(
                text = "ACTIVE COHORT RANKS",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                color = c.textMuted
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surfaceCard)
                    .border(1.dp, c.borderSubtle, RoundedCornerShape(16.dp))
            ) {
                rest.forEachIndexed { index, entry ->
                    LeaderboardRow(
                        entry = entry,
                        isCurrent = entry.uid == state.currentUid,
                        divider = index != rest.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun OperatorAvatar(entry: LeaderboardEntry, size: androidx.compose.ui.unit.Dp) {
    if (entry.photoUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AccendPalette.SurfaceHighlight)
                .border(1.dp, AccendPalette.BorderStrong, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.name.take(1).uppercase(),
                color = AccendPalette.TextPrimary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    } else {
        AsyncImage(
            model = entry.photoUrl,
            contentDescription = entry.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AccendPalette.SurfaceHighlight)
        )
    }
}

@Composable
private fun PodiumCard(
    entry: LeaderboardEntry,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    val isFirst = entry.rank == 1
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFirst) AccendPalette.SurfaceHighlight else c.surfaceCard)
            .border(
                1.dp,
                if (isCurrent || isFirst) AccendPalette.BorderStrong else c.borderSubtle,
                RoundedCornerShape(14.dp)
            )
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "#${entry.rank}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isFirst) AccendPalette.AccentWhite else c.textMuted
        )
        Spacer(Modifier.height(8.dp))
        OperatorAvatar(entry, if (isFirst) 54.dp else 44.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "LVL ${LevelCalculator.level(entry.exp)}",
            style = MaterialTheme.typography.labelSmall,
            color = c.textMuted
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${entry.exp}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = c.textPrimary
        )
        Text(text = "EXP", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    isCurrent: Boolean,
    divider: Boolean
) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) AccendPalette.SurfaceInner else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.rank.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = c.textMuted,
            modifier = Modifier.width(30.dp)
        )
        OperatorAvatar(entry, 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccendPalette.OnAccentWhite,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccendPalette.AccentWhite)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "LVL ${LevelCalculator.level(entry.exp)}",
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.exp}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = c.textPrimary
            )
            Text(text = "EXP", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
    }
    if (divider) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 14.dp)
                .background(c.borderSubtle)
        )
    }
}
