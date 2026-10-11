package com.oman.prayertimes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val OmanGreen   = Color(0xFF046A38)
val OmanGreenDark = Color(0xFF0B3D22)
val OmanRed     = Color(0xFFC8102E)
val OmanGold    = Color(0xFFD4AF37)
val SoftGreen   = Color(0xFFE8F5E9)
val DarkSurface = Color(0xFF121212)
val DarkCard    = Color(0xFF1E1E1E)

private val LightColors = lightColorScheme(
    primary = OmanGreen,
    onPrimary = Color.White,
    secondary = OmanGold,
    onSecondary = Color.Black,
    tertiary = OmanRed,
    background = Color(0xFFF6F8F6),
    surface = Color.White,
    surfaceVariant = SoftGreen,
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A),
    onSurfaceVariant = Color(0xFF5A615C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD8A8),
    onPrimary = Color(0xFF00391C),
    secondary = OmanGold,
    onSecondary = Color.Black,
    tertiary = Color(0xFFFF8A80),
    background = DarkSurface,
    surface = DarkCard,
    surfaceVariant = Color(0xFF252525),
    onBackground = Color(0xFFE6E6E6),
    onSurface = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF9AA39D)
)

/** خطوط مضبوطة الأوزان لشاشات عربية واضحة (بالخط الافتراضي للنظام) */
private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun OmanPrayerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
// END OF FILE