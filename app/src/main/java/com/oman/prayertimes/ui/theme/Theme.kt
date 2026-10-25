package com.oman.prayertimes.ui.theme

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * الهوية اللونية: أخضر (أساسي) + أحمر (لمسات وتمييز) + أبيض (أسطح) + رمادي (نصوص ثانوية وفواصل).
 */

// ── الألوان الأساسية ──
val OmanGreen = Color(0xFF0B6E4F)
val OmanGreenDeep = Color(0xFF064A35)
val OmanRed = Color(0xFFC8102E)
val White = Color(0xFFFFFFFF)

// ── الرمادي ──
private val Gray50 = Color(0xFFF4F5F4)
private val Gray100 = Color(0xFFEDEFEE)
private val Gray200 = Color(0xFFE2E5E3)
private val Gray500 = Color(0xFF6B716E)
private val Gray900 = Color(0xFF1B1F1D)

private val LightColors = lightColorScheme(
    primary = OmanGreen,
    onPrimary = White,
    primaryContainer = Color(0xFFE3F1EA),
    onPrimaryContainer = OmanGreenDeep,
    secondary = Gray500,
    onSecondary = White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray900,
    tertiary = OmanRed,
    onTertiary = White,
    tertiaryContainer = Color(0xFFFBE7EA),
    onTertiaryContainer = Color(0xFF8A0B20),
    error = OmanRed,
    onError = White,
    background = Gray50,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Gray100,
    outline = Gray200,
    outlineVariant = Gray200
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FBF94),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF123D2F),
    onPrimaryContainer = Color(0xFFBFE8D6),
    secondary = Color(0xFF9AA19E),
    onSecondary = Color(0xFF1B1F1D),
    secondaryContainer = Color(0xFF262B29),
    onSecondaryContainer = Color(0xFFE3E6E4),
    tertiary = Color(0xFFF0646C),
    onTertiary = Color(0xFF3B0009),
    tertiaryContainer = Color(0xFF4A1219),
    onTertiaryContainer = Color(0xFFFFD9DC),
    error = Color(0xFFF0646C),
    background = Color(0xFF0F1211),
    onBackground = Color(0xFFE3E6E4),
    surface = Color(0xFF181C1A),
    onSurface = Color(0xFFE3E6E4),
    surfaceVariant = Color(0xFF222725),
    onSurfaceVariant = Color(0xFF9AA19E),
    surfaceContainer = Color(0xFF181C1A),
    surfaceContainerHigh = Color(0xFF1E2220),
    surfaceContainerHighest = Color(0xFF262B29),
    outline = Color(0xFF2C3230),
    outlineVariant = Color(0xFF2C3230)
)

/** ألوان خاصة بالتطبيق خارج نظام Material (الرأس الأخضر ولمسته الحمراء) */
@Immutable
data class BrandColors(
    val heroBrush: Brush,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroChip: Color,
    val accent: Color
)

private val LightBrand = BrandColors(
    heroBrush = Brush.verticalGradient(listOf(Color(0xFF0D7A58), OmanGreenDeep)),
    onHero = White,
    onHeroMuted = White.copy(alpha = 0.75f),
    heroChip = White.copy(alpha = 0.16f),
    accent = OmanRed
)

private val DarkBrand = BrandColors(
    heroBrush = Brush.verticalGradient(listOf(Color(0xFF0A4F3B), Color(0xFF06302A))),
    onHero = White,
    onHeroMuted = White.copy(alpha = 0.7f),
    heroChip = White.copy(alpha = 0.12f),
    accent = Color(0xFFF0646C)
)

val LocalBrand = staticCompositionLocalOf { LightBrand }

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 52.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OmanPrayerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        // التطبيق عربي بالكامل: نثبّت الاتجاه من اليمين لليسار مهما كانت لغة الجهاز
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalBrand provides if (dark) DarkBrand else LightBrand,
            content = content
        )
    }
}
