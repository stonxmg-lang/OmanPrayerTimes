package com.oman.prayertimes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.notif.AudioPlayer
import com.oman.prayertimes.notif.DndController
import com.oman.prayertimes.notif.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TYPE_ALERT = "alert"
        const val TYPE_DND_ENTER = "dnd_enter"
        const val TYPE_DND_EXIT = "dnd_exit"
        const val TYPE_COUNTDOWN = "countdown"
        const val TYPE_TASBIH = "tasbih"
        /** بعد منتصف الليل: جدولة تنبيهات اليوم الجديد */
        const val TYPE_DAY_ROLLOVER = "day_rollover"
        // أنواع الإصدارات القديمة ("adhan" و "fajr_alarm") تُتجاهل عمدًا
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra("type")) {
            TYPE_ALERT -> {
                val name = intent.getStringExtra("prayerName") ?: ""
                val lead = intent.getIntExtra("lead", 10)
                val time = intent.getStringExtra("adhanTime") ?: ""
                NotificationHelper.showPreAlert(context, name, lead, time)
            }
            TYPE_DND_ENTER -> DndController.enter(context)
            TYPE_DND_EXIT -> DndController.exit(context)
            TYPE_COUNTDOWN -> AlarmScheduler.refreshCountdown(context)
            TYPE_TASBIH -> {
                val idx = intent.getIntExtra("tasbihIndex", 0)
                val text = TasbihTexts.list.getOrElse(idx) { TasbihTexts.list[0] }
                NotificationHelper.showTasbih(context, text, idx)
                if (SettingsStore(context).tasbihSound) AudioPlayer.play(context, TasbihTexts.soundFor(idx))
            }
            TYPE_DAY_ROLLOVER -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { AlarmScheduler.rescheduleToday(context.applicationContext) }
                    finally { pending.finish() }
                }
            }
        }
    }
}
