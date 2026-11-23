package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AccendExtendedColors(
    val bgBase: Color = AccendPalette.BgBase,
    val surfaceElevated: Color = AccendPalette.SurfaceElevated,
    val surfaceCard: Color = AccendPalette.SurfaceCard,
    val surfaceInner: Color = AccendPalette.SurfaceInner,
    val surfaceHighlight: Color = AccendPalette.SurfaceHighlight,
    val borderSubtle: Color = AccendPalette.BorderSubtle,
    val borderStrong: Color = AccendPalette.BorderStrong,
    val textPrimary: Color = AccendPalette.TextPrimary,
    val textSecondary: Color = AccendPalette.TextSecondary,
    val textMuted: Color = AccendPalette.TextMuted,
    val success: Color = AccendPalette.Success,
    val successBg: Color = AccendPalette.SuccessBg,
    val successBorder: Color = AccendPalette.SuccessBorder,
    val warning: Color = AccendPalette.Warning,
    val warningBg: Color = AccendPalette.WarningBg,
    val danger: Color = AccendPalette.Danger,
    val dangerDeep: Color = AccendPalette.DangerDeep,
    val dangerBg: Color = AccendPalette.DangerBg,
    val dangerBorder: Color = AccendPalette.DangerBorder
)

val LocalAccendColors = staticCompositionLocalOf { AccendExtendedColors() }

private val AccendDarkColorScheme = darkColorScheme(
    primary = AccendPalette.AccentWhite,
    onPrimary = AccendPalette.OnAccentWhite,
    primaryContainer = AccendPalette.SurfaceHighlight,
    onPrimaryContainer = AccendPalette.TextPrimary,
    secondary = AccendPalette.AccentSilver,
    onSecondary = AccendPalette.OnAccentWhite,
    secondaryContainer = AccendPalette.SurfaceInner,
    onSecondaryContainer = AccendPalette.TextPrimary,
    tertiary = AccendPalette.Success,
    onTertiary = AccendPalette.OnAccentWhite,
    background = AccendPalette.BgBase,
    onBackground = AccendPalette.TextPrimary,
    surface = AccendPalette.SurfaceCard,
    onSurface = AccendPalette.TextPrimary,
    surfaceVariant = AccendPalette.SurfaceInner,
    onSurfaceVariant = AccendPalette.TextSecondary,
    outline = AccendPalette.BorderSubtle,
    outlineVariant = AccendPalette.BorderStrong,
    error = AccendPalette.Danger,
    onError = AccendPalette.AccentWhite,
    errorContainer = AccendPalette.DangerBg,
    onErrorContainer = AccendPalette.Danger
)

private val AccendShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun AccendTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAccendColors provides AccendExtendedColors()) {
        MaterialTheme(
            colorScheme = AccendDarkColorScheme,
            typography = AccendTypography,
            shapes = AccendShapes,
            content = content
        )
    }
}

object AccendThemeTokens {
    val colors: AccendExtendedColors
        @Composable get() = LocalAccendColors.current
}
