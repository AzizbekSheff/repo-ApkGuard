package com.apkguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand colors ──────────────────────────────────────────────────────────────
val PrimaryBlue    = Color(0xFF4A9EF5)
val BackgroundDark = Color(0xFF0D0D1A)
val CardBackground = Color(0xFF1A1A2E)
val SurfaceColor   = Color(0xFF16213E)

val SuccessGreen   = Color(0xFF00C853)
val WarningOrange  = Color(0xFFFFB300)
val DangerRed      = Color(0xFFFF4444)

val RiskLow      = SuccessGreen
val RiskMedium   = WarningOrange
val RiskHigh     = Color(0xFFFF7043)
val RiskCritical = DangerRed

val TextPrimary    = Color(0xFFFFFFFF)
val TextSecondary  = Color(0xFF8A8A9A)

private val DarkColors = darkColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = TextPrimary,
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = CardBackground,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceColor,
    onSurfaceVariant = TextSecondary,
    error            = DangerRed,
    secondary        = SuccessGreen
)

@Composable
fun ApkGuardTheme(
    darkTheme: Boolean = true, // Force dark theme as per spec
    content: @Composable () -> Unit
) {
    val colors = DarkColors // Dark theme only as per spec
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
