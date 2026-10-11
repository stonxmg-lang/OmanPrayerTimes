package com.oman.prayertimes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.oman.prayertimes.ui.MainScreen
import com.oman.prayertimes.ui.theme.OmanPrayerTheme

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Fallback remains safe: the app still runs, but users can enable the permission later.
            }
        }

        PermissionUtils.requestNotificationPermission(this, permissionLauncher)

        setContent {
            OmanPrayerTheme {
                MainScreen()
            }
        }
    }
}
