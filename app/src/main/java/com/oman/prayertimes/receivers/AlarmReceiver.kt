package com.oman.prayertimes.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.notif.AudioPlayer
import com.oman.prayertimes.notif.DndController
import com.oman.prayertimes.notif.NotificationHelper
import com.oman.prayertimes.services.PlaybackService
import com.oman.prayertimes.ui.AlarmActivity
import com.oman.prayertimes.utils.PrayerUtils
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TYPE_ALERT = "alert"
        const val TYPE_ADHAN = "adhan"
        const val TYPE_DND_ENTER = "dnd_enter"
        const val TYPE_DND_EXIT = "dnd_exit"
        const val TYPE_COUNTDOWN = "countdown"
        const val TYPE_TASBIH = "tasbih"
        const val TYPE_FAJR_ALARM = "fajr_alarm"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsStore(context)
        when (intent.getStringExtra("type")) {
            TYPE_ALERT -> {
                val name = intent.getStringExtra("prayerName") ?: ""
                val sound = intent.getStringExtra("sound") ?: ""
                NotificationHelper.showEvent(context, NotificationHelper.CH_ALERTS,
                    "اقترب وقت صلاة $name", "بقي ${if (sound.contains("fajr")) "5" else "10"} دقائق على الأذان",
                    1000)
                AudioPlayer.play(context, sound)
            }
            TYPE_ADHAN -> {
                val name = intent.getStringExtra("prayerName") ?: ""
                val sound = intent.getStringExtra("sound") ?: ""
                NotificationHelper.showEvent(context, NotificationHelper.CH_ALERTS,
                    "حان الآن وقت صلاة $name", "تقبل الله طاعتكم", 1001)
                // دخول وضع عدم الإزعاج
                if (settings.dndEnabled) DndController.enter(context)
                // تشغيل الأذان عبر خدمة مقدمة في المقدمة (يستمر رغم قفل الشاشة)
                context.startForegroundService(Intent(context, PlaybackService::class.java).apply {
                    putExtra("sound", sound)
                    putExtra("title", "الأذان - صلاة $name")
                })
            }
            TYPE_DND_EXIT -> {
                DndController.exit(context)
            }
            TYPE_COUNTDOWN -> {
                AlarmScheduler.refreshCountdown(context)
            }
            TYPE_TASBIH -> {
                val idx = intent.getIntExtra("tasbihIndex", 0)
                val text = TasbihTexts.list.getOrElse(idx) { TasbihTexts.list[0] }
                NotificationHelper.showEvent(context, NotificationHelper.CH_TASBIH,
                    "تذكير", text, 2000 + idx)
                if (settings.tasbihSound) AudioPlayer.play(context, TasbihTexts.soundFor(idx))
            }
            TYPE_FAJR_ALARM -> {
                // شاشة المنبه الكاملة + صوت عبر الخدمة
                val fullScreen = Intent(context, AlarmActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fullScreen)
                context.startForegroundService(Intent(context, PlaybackService::class.java).apply {
                    putExtra("sound", settings.fajrAlarmSound)
                    putExtra("repeat", settings.fajrAlarmRepeat)
                    putExtra("title", "منبه الفجر")
                })
            }
        }
    }
}

