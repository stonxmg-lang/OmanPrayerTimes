package com.oman.prayertimes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OmanGreen   = Color(0xFF046A38)
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
    onSurface = Color(0xFF1A1C1A)
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
    onSurface = Color(0xFFE6E6E6)
)

@Composable
fun OmanPrayerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
