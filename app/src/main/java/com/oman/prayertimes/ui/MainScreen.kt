package com.oman.prayertimes.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.*

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("prayer", "المواقيت", Icons.Default.DateRange),
    Tab("quran", "القرآن", Icons.Default.Star),
    Tab("athkar", "الأذكار", Icons.Default.Notifications),
    Tab("settings", "الإعدادات", Icons.Default.Settings)
)

@Composable
fun MainScreen() {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { nav.navigate(tab.route) { popUpTo(0) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "prayer", modifier = Modifier.padding(padding)) {
            composable("prayer") { PrayerScreen() }
            composable("quran") { QuranScreen() }
            composable("athkar") { AthkarScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
