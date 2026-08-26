package com.mashuktamim.inhale

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern Minimalist Typography System.
 * Clean, balanced geometric sans-serif hierarchy with refined tracking and line-heights.
 */
val InhaleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    ),
)

/**
 * Modern Minimal Shapes.
 */
val InhaleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Custom color palette tokens tailored for modern minimal interfaces.
 */
@Immutable
data class InhaleColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderSubtle: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentGlow: Color,
    val success: Color,
    val warning: Color,
    val isDark: Boolean,
)

// --- Color Palettes ---

val DarkPalette = InhaleColors(
    background = Color(0xFF0C0D14),
    surface = Color(0xFF141620),
    surfaceElevated = Color(0xFF1C1E2A),
    surfaceSubtle = Color(0xFF181A24),
    border = Color(0xFF262938),
    borderSubtle = Color(0xFF1E212E),
    primary = Color(0xFF818CF8),
    primaryContainer = Color(0xFF312E81),
    onPrimary = Color(0xFF0C0D14),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF64748B),
    accentGlow = Color(0x33818CF8),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    isDark = true,
)

val AmoledPalette = InhaleColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0C10),
    surfaceElevated = Color(0xFF14151B),
    surfaceSubtle = Color(0xFF101117),
    border = Color(0xFF20222B),
    borderSubtle = Color(0xFF181A22),
    primary = Color(0xFF818CF8),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimary = Color(0xFF000000),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF71717A),
    accentGlow = Color(0x40818CF8),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    isDark = true,
)

val LightPalette = InhaleColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    surfaceSubtle = Color(0xFFF8FAFC),
    border = Color(0xFFE2E8F0),
    borderSubtle = Color(0xFFEEF2F6),
    primary = Color(0xFF4F46E5),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    textTertiary = Color(0xFF94A3B8),
    accentGlow = Color(0x224F46E5),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    isDark = false,
)

val LocalInhaleColors = staticCompositionLocalOf { DarkPalette }

object InhaleTheme {
    val colors: InhaleColors
        @Composable
        get() = LocalInhaleColors.current
    val typography: Typography
        get() = InhaleTypography
    val shapes: Shapes
        get() = InhaleShapes
}

@Composable
fun InhaleAppTheme(
    themeMode: Prefs.ThemeMode = Prefs.ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        Prefs.ThemeMode.SYSTEM -> systemInDark
        Prefs.ThemeMode.DARK -> true
        Prefs.ThemeMode.AMOLED -> true
        Prefs.ThemeMode.LIGHT -> false
    }

    val inhaleColors = when (themeMode) {
        Prefs.ThemeMode.SYSTEM -> if (systemInDark) DarkPalette else LightPalette
        Prefs.ThemeMode.DARK -> DarkPalette
        Prefs.ThemeMode.AMOLED -> AmoledPalette
        Prefs.ThemeMode.LIGHT -> LightPalette
    }

    val materialColors = if (isDark) {
        darkColorScheme(
            background = inhaleColors.background,
            surface = inhaleColors.surface,
            surfaceVariant = inhaleColors.surfaceElevated,
            primary = inhaleColors.primary,
            onPrimary = inhaleColors.onPrimary,
            onBackground = inhaleColors.textPrimary,
            onSurface = inhaleColors.textPrimary,
            outline = inhaleColors.border,
            outlineVariant = inhaleColors.borderSubtle,
        )
    } else {
        lightColorScheme(
            background = inhaleColors.background,
            surface = inhaleColors.surface,
            surfaceVariant = inhaleColors.surfaceElevated,
            primary = inhaleColors.primary,
            onPrimary = inhaleColors.onPrimary,
            onBackground = inhaleColors.textPrimary,
            onSurface = inhaleColors.textPrimary,
            outline = inhaleColors.border,
            outlineVariant = inhaleColors.borderSubtle,
        )
    }

    CompositionLocalProvider(LocalInhaleColors provides inhaleColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = InhaleTypography,
            shapes = InhaleShapes,
            content = content
        )
    }
}
