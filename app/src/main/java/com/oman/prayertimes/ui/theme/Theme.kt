package com.oman.prayertimes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * الهوية اللونية: ورق كريمي دافئ (أسطح) + بنّي غامق (نصوص) + تركوازي هادئ (لمسة أساسية)
 * + ذهبي خفيف (لمسة ثانوية). ألوان العلامة (أحمر للمراجعة، أخضر للحفظ) ثابتة لأنها دلالية.
 */

// ── أسطح ونصوص ──
private val Cream = Color(0xFFF4ECDC)        // الخلفية العامة (ورقي دافئ)
private val CreamSurface = Color(0xFFFBF6EC) // البطاقات والأسطح
private val CreamHigh = Color(0xFFF1E7D3)    // أسطح أعلى قليلًا
private val InkBrown = Color(0xFF362F24)     // النص الأساسي (بنّي غامق)
private val InkMuted = Color(0xFF857A64)     // النص الثانوي
private val LineWarm = Color(0xFFE2D6BE)     // الفواصل والحدود

// ── لمسات ──
private val Teal = Color(0xFF0F766E)         // اللمسة الأساسية (تركوازي)
private val TealDeep = Color(0xFF0B5A54)
private val TealSoft = Color(0xFFCFE6E2)     // خلفية فاتحة للّمسة
private val Gold = Color(0xFF9A7B3F)         // لمسة ثانوية ذهبية
private val GoldSoft = Color(0xFFEFE3C8)
private val Crimson = Color(0xFFB23A2E)      // الأحمر (مراجعة/أخطاء)
private val CrimsonSoft = Color(0xFFF3DDD8)

private val CreamColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = TealSoft,
    onPrimaryContainer = TealDeep,
    secondary = Gold,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = GoldSoft,
    onSecondaryContainer = Color(0xFF5A4518),
    tertiary = Crimson,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = CrimsonSoft,
    onTertiaryContainer = Color(0xFF7A231A),
    error = Crimson,
    onError = Color(0xFFFFFFFF),
    background = Cream,
    onBackground = InkBrown,
    surface = CreamSurface,
    onSurface = InkBrown,
    surfaceVariant = CreamHigh,
    onSurfaceVariant = InkMuted,
    surfaceContainer = CreamSurface,
    surfaceContainerHigh = CreamHigh,
    surfaceContainerHighest = CreamHigh,
    outline = LineWarm,
    outlineVariant = LineWarm
)

/** ألوان خاصة بالتطبيق خارج نظام Material. */
@Immutable
data class BrandColors(
    val heroBrush: Brush,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroChip: Color,
    val accent: Color,        // لمسة التمييز (الصلاة التالية)
    val markReview: Color,    // علامة المراجعة (أحمر) — دلالية ثابتة
    val markMemorized: Color  // علامة الحفظ (أخضر) — دلالية ثابتة
)

private val CreamBrand = BrandColors(
    heroBrush = Brush.verticalGradient(listOf(Teal, TealDeep)),
    onHero = Color(0xFFFBF6EC),
    onHeroMuted = Color(0xFFFBF6EC).copy(alpha = 0.78f),
    heroChip = Color(0xFFFFFFFF).copy(alpha = 0.16f),
    accent = Color(0xFFE0A93B),      // ذهبي دافئ للمسة التمييز على الرأس
    markReview = Color(0xFFB23A2E),  // أحمر
    markMemorized = Color(0xFF2E7D32) // أخضر
)

val LocalBrand = staticCompositionLocalOf { CreamBrand }

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 52.sp),
    headlineMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 38.sp),
    headlineSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium,
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
    // هوية التطبيق ورقية فاتحة ثابتة (لا تتبع الوضع الداكن للنظام).
    MaterialTheme(
        colorScheme = CreamColors,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        // التطبيق عربي بالكامل: نثبّت الاتجاه من اليمين لليسار مهما كانت لغة الجهاز
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalBrand provides CreamBrand,
            content = content
        )
    }
}
