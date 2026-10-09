package com.oman.prayertimes.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oman.prayertimes.services.PlaybackService
import com.oman.prayertimes.ui.theme.OmanPrayerTheme

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            OmanPrayerTheme {
                AlarmScreen(onStop = {
                    startService(android.content.Intent(this, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_STOP
                    })
                    finish()
                })
            }
        }
    }
}

@Composable
fun AlarmScreen(onStop: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🕌", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text("وقت المنبه", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("منبه الفجر - تقبل الله طاعتك",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            Button(onClick = onStop, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("إيقاف المنبه", fontSize = 18.sp)
            }
        }
    }
}
