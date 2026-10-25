package com.oman.prayertimes

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.oman.prayertimes.ui.MainScreen
import com.oman.prayertimes.ui.theme.OmanPrayerTheme

class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        // الرأس أخضر في الوضعين، فأيقونات شريط الحالة بيضاء دائمًا
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)

        // الإشعارات هي كل وظيفة التنبيه الآن، فنطلبها عند أول فتح
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        PermissionUtils.requestNotificationPermission(this, permissionLauncher)

        setContent {
            OmanPrayerTheme {
                MainScreen()
            }
        }
    }
}
