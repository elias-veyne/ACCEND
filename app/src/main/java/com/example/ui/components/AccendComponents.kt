package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AccendPalette
import com.example.ui.theme.AccendThemeTokens
import com.example.ui.theme.MonoFontFamily
import java.io.File

@Composable
fun AccendTopBar(
    subtitle: String,
    userName: String,
    pfpUri: String?,
    hasPendingAlert: Boolean = false,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square logo badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, c.borderStrong, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.accend_brand_mark),
                    contentDescription = "Accend emblem",
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACCEND",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    ),
                    color = c.textPrimary
                )
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onBellClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_bar_notifications")
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = c.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    if (hasPendingAlert) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(c.success)
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            PfpAvatar(
                name = userName,
                pfpUri = pfpUri,
                size = 38.dp,
                modifier = Modifier
                    .testTag("top_bar_avatar")
                    .clickable(onClick = onAvatarClick)
            )
        }
        HorizontalDivider(color = c.borderSubtle.copy(alpha = 0.6f), thickness = 1.dp)
    }
}

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val headerSubtitle: String
) {
    HOME("home", "Home", Icons.Outlined.GridView, "HOME"),
    DAILY("daily", "Daily Work", Icons.Outlined.Checklist, "DAILY WORK"),
    ANALYSIS("analysis", "Analysis", Icons.Outlined.Insights, "ANALYSIS"),
    MONEY("money", "Money", Icons.Outlined.AccountBalanceWallet, "MONEY"),
    SETTINGS("settings", "Settings", Icons.Outlined.Tune, "ACCEND SETTINGS")
}

@Composable
fun AccendBottomBar(
    currentRoute: String?,
    hasProgressionBadge: Boolean,
    onSelectTab: (MainTab) -> Unit
) {
    val c = AccendThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceElevated)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = c.borderSubtle, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 4.dp)
                        .testTag("nav_tab_${tab.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) AccendPalette.AccentWhite else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) AccendPalette.OnAccentWhite else c.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        if (tab == MainTab.SETTINGS && hasProgressionBadge) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 5.dp, end = 12.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) AccendPalette.OnAccentWhite else c.success)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 0.sp
                        ),
                        color = if (selected) AccendPalette.AccentWhite else c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AccendCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AccendThemeTokens.colors.surfaceCard,
    borderColor: Color = AccendThemeTokens.colors.borderSubtle,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun AccendInnerBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AccendThemeTokens.colors.surfaceInner,
    borderColor: Color = AccendThemeTokens.colors.borderSubtle,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AccendCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        cornerRadius = cornerRadius,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content
    )
}

@Composable
fun PrimaryWhiteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "primary_cta_button"
) {
    val glowAlpha = if (enabled) 0.16f else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = glowAlpha), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.65f
                        ),
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                }
            }
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccendPalette.AccentWhite,
                contentColor = AccendPalette.OnAccentWhite,
                disabledContainerColor = AccendPalette.SurfaceHighlight,
                disabledContentColor = AccendPalette.TextMuted
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
            )
        }
    }
}

@Composable
fun SecondaryDarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "secondary_button"
) {
    val c = AccendThemeTokens.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, c.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = c.surfaceInner,
            contentColor = c.textPrimary
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TacticalBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = AccendThemeTokens.colors.surfaceInner,
    contentColor: Color = AccendThemeTokens.colors.textSecondary,
    borderColor: Color = AccendThemeTokens.colors.borderStrong,
    leadingDotColor: Color? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(leadingDotColor)
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
fun TacticalCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Toggle protocol"
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.94f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "checkbox_scale"
    )
    val bg by animateColorAsState(
        targetValue = if (checked) AccendPalette.AccentWhite else Color.Transparent,
        animationSpec = tween(160),
        label = "checkbox_bg"
    )
    val border by animateColorAsState(
        targetValue = if (checked) AccendPalette.AccentWhite else AccendPalette.BorderStrong,
        animationSpec = tween(160),
        label = "checkbox_border"
    )
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(enabled = enabled, onClick = onCheckedChange),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .border(1.5.dp, border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = contentDescription,
                    tint = AccendPalette.OnAccentWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TacticalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "tactical_switch"
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) AccendPalette.AccentWhite else AccendPalette.SurfaceHighlight,
        animationSpec = tween(180),
        label = "switch_track"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) AccendPalette.OnAccentWhite else AccendPalette.TextSecondary,
        animationSpec = tween(180),
        label = "switch_thumb"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f),
        label = "switch_offset"
    )
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor)
                .border(
                    1.dp,
                    if (checked) AccendPalette.AccentWhite else AccendPalette.BorderStrong,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}

@Composable
fun PfpAvatar(
    name: String,
    pfpUri: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val fileExists = remember(pfpUri) {
        if (pfpUri.isNullOrBlank()) false
        else {
            val path = Uri.parse(pfpUri).path
            path != null && File(path).exists()
        }
    }
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2B2B34), Color(0xFF14141A))
                )
            )
            .border(1.5.dp, AccendPalette.BorderStrong, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (fileExists && pfpUri != null) {
            AsyncImage(
                model = pfpUri,
                contentDescription = "Operator avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                ),
                color = AccendPalette.AccentWhite
            )
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    trailing: String? = null,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = c.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 12.sp
                ),
                color = c.textPrimary
            )
        }
        if (trailing != null) {
            Text(
                text = trailing.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = c.textMuted
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    AccendCard(modifier = modifier, contentPadding = PaddingValues(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.textMuted,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                SecondaryDarkButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun NumberStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val c = AccendThemeTokens.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceInner)
            .border(1.dp, c.borderStrong, RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = "Decrease $label",
                tint = c.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = c.textPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Increase $label",
                tint = c.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    body: String,
    confirmText: String,
    isDanger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = AccendThemeTokens.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceElevated,
        titleContentColor = c.textPrimary,
        textContentColor = c.textSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDanger) c.danger else AccendPalette.AccentWhite,
                    contentColor = if (isDanger) AccendPalette.AccentWhite else AccendPalette.OnAccentWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText.uppercase(), style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = c.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}
