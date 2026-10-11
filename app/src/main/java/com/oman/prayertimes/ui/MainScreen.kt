package com.oman.prayertimes.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oman.prayertimes.ui.theme.OmanGreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("prayer", "المواقيت", Icons.Default.Mosque),
    Tab("settings", "الإعدادات", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val nav = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مواقيت الصلاة - عُمان") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OmanGreen,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
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
        // تبويبا القرآن والأذكار مخفيان حتى تجهيز محتواهما (ملفاهما القديمان يمكن حذفهما يدويًا)
        NavHost(nav, startDestination = "prayer", modifier = Modifier.padding(padding)) {
            composable("prayer") { PrayerScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
// END OF FILE