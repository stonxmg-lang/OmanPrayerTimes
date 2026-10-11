package com.oman.prayertimes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.notif.AudioPlayer
import com.oman.prayertimes.notif.DndController
import com.oman.prayertimes.notif.NotificationHelper
import com.oman.prayertimes.services.PlaybackService

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
                    "اقترب وقت صلاة $name", "بقي ${'$'}{if (sound.contains("fajr")) "5" else "10"} دقائق على الأذان",
                    1000)
                // التشغيل عبر خدمة مقدمة (WakeLock آمن واستمرار رغم قفل الشاشة)
                context.startForegroundService(Intent(context, PlaybackService::class.java).apply {
                    putExtra("sound", sound)
                    putExtra("title", "تنبيه قبل صلاة $name")
                })
            }
            TYPE_ADHAN -> {
                val name = intent.getStringExtra("prayerName") ?: ""
                val sound = intent.getStringExtra("sound") ?: ""
                // 1) الأذان أولاً بدون أي تأخير (خدمة مقدمة تستمر رغم قفل الشاشة)
                context.startForegroundService(Intent(context, PlaybackService::class.java).apply {
                    putExtra("sound", sound)
                    putExtra("title", "الأذان - صلاة $name")
                })
                // 2) الإشعار
                NotificationHelper.showEvent(context, NotificationHelper.CH_ALERTS,
                    "حان الآن وقت صلاة $name", "تقبل الله طاعتكم", 1001)
                // 3) الصامت في خيط منفصل حتى لا يؤخر الأذان
                if (settings.dndEnabled) {
                    val pendingResult = goAsync()
                    Thread {
                        try { DndController.enter(context) } finally { pendingResult.finish() }
                    }.start()
                }
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
                if (settings.tasbihSound) AudioPlayer.play(context, TasbihTexts.soundFor(idx),
                    alarmStream = false, volume = settings.adhanVolume / 100f)
            }
            TYPE_FAJR_ALARM -> {
                // إشعار ملء الشاشة فوق شاشة القفل (أندرويد 10+ يمنع فتح Activity من الخلفية)
                NotificationHelper.showFullScreen(context, "منبه الفجر",
                    "حان وقت الاستيقاظ - تقبل الله طاعتك")
                context.startForegroundService(Intent(context, PlaybackService::class.java).apply {
                    putExtra("sound", settings.fajrAlarmSound)
                    putExtra("repeat", settings.fajrAlarmRepeat)
                    putExtra("title", "منبه الفجر")
                })
            }
        }
    }
}
