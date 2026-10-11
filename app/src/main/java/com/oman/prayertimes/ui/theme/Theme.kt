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

val OmanGreen = Color(0xFF0E6B4F)
val OmanGreenDark = Color(0xFF0A4A37)
val OmanRed = Color(0xFFB3372B)
val OmanGold = Color(0xFFC9A227)
val Ivory = Color(0xFFF5F3EE)
val NightBg = Color(0xFF101412)
val NightCard = Color(0xFF1A201D)

private val LightColors = lightColorScheme(
    primary = OmanGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EBE1),
    onPrimaryContainer = Color(0xFF07382A),
    secondary = OmanGold,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFFF3E7C3),
    onSecondaryContainer = Color(0xFF4A3800),
    tertiary = OmanRed,
    background = Ivory,
    onBackground = Color(0xFF1C1B1A),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFEDEBE4),
    onSurfaceVariant = Color(0xFF5F625C),
    outline = Color(0xFFD8D5CC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD3B4),
    onPrimary = Color(0xFF06382A),
    primaryContainer = Color(0xFF12493A),
    onPrimaryContainer = Color(0xFFB9E9D4),
    secondary = Color(0xFFD8BC5E),
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = Color(0xFF4A3A10),
    onSecondaryContainer = Color(0xFFEEDFAC),
    tertiary = Color(0xFFE5988E),
    background = NightBg,
    onBackground = Color(0xFFE3E6E1),
    surface = NightCard,
    onSurface = Color(0xFFE3E6E1),
    surfaceVariant = Color(0xFF242B27),
    onSurfaceVariant = Color(0xFF9BA39C),
    outline = Color(0xFF3A423D)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
