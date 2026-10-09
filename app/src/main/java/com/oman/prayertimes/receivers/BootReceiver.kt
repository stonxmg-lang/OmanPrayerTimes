package com.oman.prayertimes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.notif.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val repo = PrayerRepository(context)
                val times = repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
                if (times != null) {
                    AlarmScheduler.scheduleForDay(context, times, today)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
