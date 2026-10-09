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
                if (settings.dndEnabled) DndController.exit(context)
            }
            TYPE_COUNTDOWN -> {
                updateCountdown(context)
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

    /** تحديث إشعار الستار كل دقيقة أثناء الساعة الأخيرة قبل الصلاة */
    private fun updateCountdown(context: Context) {
        val repo = PrayerRepository(context)
        val today = LocalDate.now()
        val times = repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
            ?: return
        val now = java.util.Calendar.getInstance()
        val nowMin = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val idx = PrayerUtils.nextPrayerIndex(times, nowMin)
        val name = DayTimes.NAMES[idx]
        val t = DayTimes.get(times, DayTimes.KEYS[idx])
        val mins = PrayerUtils.toMinutes(t)
        val diff = if (mins > nowMin) mins - nowMin else (1440 - nowMin) // منتصف الليل

        if (diff in 1..60) {
            NotificationHelper.showPersistent(context,
                "⏳ الصلاة القادمة: $name بعد $diff دقيقة")
            // جدولة التحديث التالي بعد دقيقة
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", TYPE_COUNTDOWN)
            }
            val pi = PendingIntent.getBroadcast(context, AlarmScheduler.RQ_COUNTDOWN, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val next = System.currentTimeMillis() + 60_000
            try {
                if (am.canScheduleExactAlarms())
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
                else
                    am.setWindow(AlarmManager.RTC_WAKEUP, next, 60_000, pi)
            } catch (e: SecurityException) {
                am.setWindow(AlarmManager.RTC_WAKEUP, next, 60_000, pi)
            }
        } else {
            AlarmScheduler.updatePersistentNow(context, times)
        }
    }
}
