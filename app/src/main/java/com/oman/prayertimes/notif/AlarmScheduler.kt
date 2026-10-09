package com.oman.prayertimes.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.receivers.AlarmReceiver
import com.oman.prayertimes.utils.PrayerUtils
import java.time.LocalDate

object AlarmScheduler {

    const val RQ_ALERT_BASE   = 100  // + index (0..5)
    const val RQ_ADHAN_BASE   = 200
    const val RQ_DND_BASE     = 300
    const val RQ_COUNTDOWN    = 400
    const val RQ_TASBIH_BASE  = 500
    const val RQ_FAJR_ALARM   = 600

    const val ALERT_LEAD_FAJR = 5      // تنبيه الفجر قبل 5 دقائق
    const val ALERT_LEAD_OTHER = 10    // بقية الصلوات قبل 10 دقائق

    // مدة بقاء وضع عدم الإزعاج بعد كل صلاة (دقائق) - الشروق مستثنى
    val DND_EXIT_MINUTES = mapOf(
        "fajr" to 45, "dhuhr" to 35, "asr" to 35, "maghrib" to 25, "isha" to 35
    )

    private fun pending(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun exactAt(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000, pi)
            }
        } catch (e: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000, pi)
        }
    }

    /** يجدول كل أحداث اليوم: تنبيهات، أذان، DND، عد تنازلي، تسبيح، منبه الفجر */
    fun scheduleForDay(context: Context, times: DayTimes, date: LocalDate = LocalDate.now()) {
        val settings = SettingsStore(context)
        val keys = DayTimes.KEYS

        // أوقات الأذان بالدقائق
        val adhanMin = keys.map { PrayerUtils.toMinutes(DayTimes.get(times, it)) }

        // 1) تنبيه قبل الصلاة + 2) الأذان + 3) خروج DND
        for (i in keys.indices) {
            val key = keys[i]
            if (key == "sunrise") continue // الشروق: لا تنبيه ولا DND
            val adhan = adhanMin[i]

            // منبه الفجر (قبل الأذان بعدد دقائق محدد)
            var fajrAlarmMinute = -1
            if (key == "fajr" && settings.fajrAlarmEnabled) {
                fajrAlarmMinute = adhan - settings.fajrAlarmMinutes
                if (fajrAlarmMinute >= 0) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("type", AlarmReceiver.TYPE_FAJR_ALARM)
                    }
                    exactAt(context, PrayerUtils.calendarForToday(
                        "%02d:%02d".format(fajrAlarmMinute / 60, fajrAlarmMinute % 60), date),
                        pending(context, RQ_FAJR_ALARM, intent))
                }
            }

            // تنبيه قبل الصلاة — الأولوية للمنبه عند التصادم
            if (settings.alertsEnabled) {
                val lead = if (key == "fajr") ALERT_LEAD_FAJR else ALERT_LEAD_OTHER
                val alertMin = adhan - lead
                val collidesWithAlarm = key == "fajr" &&
                        settings.fajrAlarmEnabled && alertMin == fajrAlarmMinute
                if (alertMin >= 0 && !collidesWithAlarm) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("type", AlarmReceiver.TYPE_ALERT)
                        putExtra("prayerIndex", i)
                        putExtra("sound", "alert_$key")
                        putExtra("prayerName", DayTimes.NAMES[i])
                    }
                    exactAt(context, PrayerUtils.calendarForToday(
                        "%02d:%02d".format(alertMin / 60, alertMin % 60), date),
                        pending(context, RQ_ALERT_BASE + i, intent))
                } else {
                    cancel(context, RQ_ALERT_BASE + i)
                }
            } else cancel(context, RQ_ALERT_BASE + i)

            // الأذان
            val adhanIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", AlarmReceiver.TYPE_ADHAN)
                putExtra("prayerIndex", i)
                putExtra("sound", "adhan_$key")
                putExtra("prayerName", DayTimes.NAMES[i])
            }
            exactAt(context, PrayerUtils.calendarForToday(DayTimes.get(times, key), date),
                pending(context, RQ_ADHAN_BASE + i, adhanIntent))

            // خروج DND
            DND_EXIT_MINUTES[key]?.let { exitMin ->
                val exitAt = adhan + exitMin
                val exitIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", AlarmReceiver.TYPE_DND_EXIT)
                }
                exactAt(context, PrayerUtils.calendarForToday(
                    "%02d:%02d".format(exitAt / 60, exitAt % 60), date),
                    pending(context, RQ_DND_BASE + i, exitIntent))
            }
        }

        // 4) العد التنازلي في الستار (يبدأ قبل الصلاة بساعة)
        scheduleCountdownChain(context, times, date)

        // 5) تذكيرات الصلاة على النبي - موزعة بين الفجر والعشاء
        scheduleTasbih(context, times, adhanMin, date, settings)

        // إشعار الستار الأساسي
        updatePersistentNow(context, times)
    }

    fun scheduleCountdownChain(context: Context, times: DayTimes, date: LocalDate) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("type", AlarmReceiver.TYPE_COUNTDOWN)
        }
        // يُشغَّل أول مرة قبل الصلاة القادمة بساعة - الحساب الدقيق داخل المستقبِل
        exactAt(context, System.currentTimeMillis() + 30_000, pending(context, RQ_COUNTDOWN, intent))
        // البيانات تُقرأ من الكاش داخل المستقبِل في كل مرة
    }

    private fun scheduleTasbih(context: Context, times: DayTimes, adhanMin: List<Int>,
                               date: LocalDate, settings: SettingsStore) {
        for (i in 0..9) cancel(context, RQ_TASBIH_BASE + i)
        if (!settings.tasbihEnabled) return

        val n = settings.tasbihCount.coerceIn(2, 10)
        val fajrMin = adhanMin[0]
        val ishaMin = adhanMin[5]
        if (ishaMin <= fajrMin) return

        val interval = (ishaMin - fajrMin).toDouble() / (n + 1)
        val windows = DayTimes.KEYS.indices
            .filter { DayTimes.KEYS[it] != "sunrise" }
            .map { adhanMin[it]..(adhanMin[it] + (DND_EXIT_MINUTES[DayTimes.KEYS[it]] ?: 30)) }

        var scheduled = 0
        for (k in 1..n) {
            val t = (fajrMin + interval * k).toInt()
            if (windows.any { t in it }) continue // استثناء أوقات الصلاة
            val idx = scheduled % TasbihTexts.list.size
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", AlarmReceiver.TYPE_TASBIH)
                putExtra("tasbihIndex", idx)
            }
            exactAt(context, PrayerUtils.calendarForToday(
                "%02d:%02d".format(t / 60, t % 60), date),
                pending(context, RQ_TASBIH_BASE + scheduled, intent))
            scheduled++
        }
    }

    fun updatePersistentNow(context: Context, times: DayTimes) {
        val now = java.util.Calendar.getInstance()
        val nowMin = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                now.get(java.util.Calendar.MINUTE)
        val idx = PrayerUtils.nextPrayerIndex(times, nowMin)
        val name = DayTimes.NAMES[idx]
        val t = DayTimes.get(times, DayTimes.KEYS[idx])
        val mins = PrayerUtils.toMinutes(t)
        val diff = if (mins > nowMin) mins - nowMin else 0
        val text = if (diff in 1..60) {
            "الصلاة القادمة: $name بعد $diff دقيقة"
        } else {
            "الصلاة القادمة: $name - $t"
        }
        NotificationHelper.showPersistent(context, text)
    }

    fun cancel(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.cancel(pi)
    }

    fun cancelAll(context: Context) {
        for (i in 0..5) {
            cancel(context, RQ_ALERT_BASE + i); cancel(context, RQ_ADHAN_BASE + i)
            cancel(context, RQ_DND_BASE + i)
        }
        for (i in 0..9) cancel(context, RQ_TASBIH_BASE + i)
        cancel(context, RQ_COUNTDOWN); cancel(context, RQ_FAJR_ALARM)
    }
}
