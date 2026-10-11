package com.oman.prayertimes.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.RawSounds
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.receivers.AlarmReceiver
import com.oman.prayertimes.utils.PrayerUtils
import java.time.DayOfWeek
import java.time.LocalDate

object AlarmScheduler {

    const val RQ_ALERT_BASE      = 100
    const val RQ_ADHAN_BASE      = 200
    const val RQ_DND_BASE        = 300
    const val RQ_COUNTDOWN       = 400
    const val RQ_TASBIH_BASE     = 500
    const val RQ_FAJR_ALARM      = 600
    const val RQ_DND_ENTER_BASE  = 700

    const val ALERT_LEAD_FAJR = 5
    const val ALERT_LEAD_OTHER = 10

    /**
     * مدة الصامت بعد كل صلاة بالدقائق: من إعدادات المستخدم،
     * ويوم الجمعة مدة خاصة أطول لصلاة الظهر.
     */
    fun dndExitMinutes(settings: SettingsStore, key: String, date: LocalDate): Int =
        if (key == "dhuhr" && date.dayOfWeek == DayOfWeek.FRIDAY)
            settings.dndFridayMinutes
        else
            settings.dndMinutes(key)

    private fun pending(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun exactAt(context: Context, triggerAt: Long, pi: PendingIntent) {
        if (triggerAt <= System.currentTimeMillis()) return
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

    /**
     * يجدول كل أحداث اليوم: تنبيهات، أذان، دخول/خروج الصامت (مستقلان عن الأذان)،
     * عدّ تنازلي، تسبيحات، ومنبه الفجر.
     * أي ميزة معطّلة تُلغى منبهاتها صراحة.
     */
    fun scheduleForDay(context: Context, times: DayTimes, date: LocalDate = LocalDate.now()) {
        val settings = SettingsStore(context)
        val keys = DayTimes.KEYS

        DndController.reconcile(context)

        val adhanMin = keys.map { PrayerUtils.toMinutes(DayTimes.get(times, it)) }

        for (i in keys.indices) {
            val key = keys[i]
            if (key == "sunrise") continue
            val adhan = adhanMin[i]

            // ── منبه الفجر ──
            if (key == "fajr") {
                if (settings.fajrAlarmEnabled) {
                    val fajrAlarmMinute = adhan - settings.fajrAlarmMinutes
                    if (fajrAlarmMinute >= 0) {
                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra("type", AlarmReceiver.TYPE_FAJR_ALARM)
                        }
                        exactAt(context, PrayerUtils.calendarForToday(
                            String.format(java.util.Locale.US, "%02d:%02d",
                                fajrAlarmMinute / 60, fajrAlarmMinute % 60), date),
                            pending(context, RQ_FAJR_ALARM, intent))
                    } else {
                        cancel(context, RQ_FAJR_ALARM)
                    }
                } else {
                    cancel(context, RQ_FAJR_ALARM)
                }
            }

            // ── تنبيه قبل الصلاة ──
            if (settings.alertsEnabled) {
                val lead = if (key == "fajr") ALERT_LEAD_FAJR else ALERT_LEAD_OTHER
                val alertMin = adhan - lead
                val fajrAlarmMinute = if (key == "fajr" && settings.fajrAlarmEnabled)
                    adhan - settings.fajrAlarmMinutes else -1
                val collidesWithAlarm = key == "fajr" && alertMin == fajrAlarmMinute
                if (alertMin >= 0 && !collidesWithAlarm) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("type", AlarmReceiver.TYPE_ALERT)
                        putExtra("prayerIndex", i)
                        putExtra("sound", "alert_$key")
                        putExtra("prayerName", DayTimes.NAMES[i])
                    }
                    exactAt(context, PrayerUtils.calendarForToday(
                        String.format(java.util.Locale.US, "%02d:%02d",
                            alertMin / 60, alertMin % 60), date),
                        pending(context, RQ_ALERT_BASE + i, intent))
                } else {
                    cancel(context, RQ_ALERT_BASE + i)
                }
            } else {
                cancel(context, RQ_ALERT_BASE + i)
            }

            // ── الأذان (يُلغى عند التعطيل أو غياب أي ملف صوت) ──
            val adhanSound = RawSounds.resolveAdhan(context, key, settings)
            if (settings.adhanEnabled(key) && adhanSound != null) {
                val adhanIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", AlarmReceiver.TYPE_ADHAN)
                    putExtra("prayerIndex", i)
                    putExtra("sound", adhanSound)
                    putExtra("prayerName", DayTimes.NAMES[i])
                }
                exactAt(context, PrayerUtils.calendarForToday(DayTimes.get(times, key), date),
                    pending(context, RQ_ADHAN_BASE + i, adhanIntent))
            } else {
                cancel(context, RQ_ADHAN_BASE + i)
            }

            // ── الصامت: منبها دخول وخروج مستقلان لكل صلاة ──
            if (settings.dndEnabled) {
                val enterIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", AlarmReceiver.TYPE_DND_ENTER)
                }
                exactAt(context, PrayerUtils.calendarForToday(DayTimes.get(times, key), date),
                    pending(context, RQ_DND_ENTER_BASE + i, enterIntent))

                val exitMin = dndExitMinutes(settings, key, date)
                val exitAt = adhan + exitMin
                val exitIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("type", AlarmReceiver.TYPE_DND_EXIT)
                }
                exactAt(context, PrayerUtils.calendarForToday(
                    String.format(java.util.Locale.US, "%02d:%02d", exitAt / 60, exitAt % 60), date),
                    pending(context, RQ_DND_BASE + i, exitIntent))
            } else {
                cancel(context, RQ_DND_ENTER_BASE + i)
                cancel(context, RQ_DND_BASE + i)
            }
        }

        refreshCountdown(context)
        scheduleTasbih(context, times, adhanMin, date, settings)
    }

    /** يعيد جدولة تنبيهات اليوم الحالي — يُستدعى من كل تعديل إعدادات */
    fun rescheduleToday(context: Context) {
        val repo = PrayerRepository(context)
        val today = LocalDate.now()
        val times = repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
            ?: return
        scheduleForDay(context, times, today)
    }

    fun refreshCountdown(context: Context) {
        val repo = PrayerRepository(context)
        val cache = repo.loadCache()
        val today = LocalDate.now()
        val times = repo.getDay(cache, today.year, today.monthValue, today.dayOfMonth) ?: return
        val now = System.currentTimeMillis()

        var idx = -1
        var target = 0L
        for (i in DayTimes.KEYS.indices) {
            val t = PrayerUtils.calendarForToday(DayTimes.get(times, DayTimes.KEYS[i]), today)
            if (t > now + 1000) { idx = i; target = t; break }
        }
        if (idx < 0) {
            idx = 0
            val tomorrow = today.plusDays(1)
            val tTimes = repo.getDay(cache, tomorrow.year, tomorrow.monthValue, tomorrow.dayOfMonth) ?: times
            target = PrayerUtils.calendarForToday(DayTimes.get(tTimes, "fajr"), tomorrow)
        }

        val name = DayTimes.NAMES[idx]
        val label = if (DayTimes.KEYS[idx] == "sunrise") "باقي على $name" else "باقي على صلاة $name"
        NotificationHelper.showCountdown(context, label, target)

        val remaining = target - now
        val nextRefresh = if (remaining > 3_600_000L) target - 3_600_000L else target
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("type", AlarmReceiver.TYPE_COUNTDOWN)
        }
        exactAt(context, nextRefresh, pending(context, RQ_COUNTDOWN, intent))
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
            .map { adhanMin[it]..(adhanMin[it] + dndExitMinutes(settings, DayTimes.KEYS[it], date)) }

        var scheduled = 0
        for (k in 1..n) {
            val t = (fajrMin + interval * k).toInt()
            if (windows.any { t in it }) continue
            val idx = scheduled % TasbihTexts.list.size
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("type", AlarmReceiver.TYPE_TASBIH)
                putExtra("tasbihIndex", idx)
            }
            exactAt(context, PrayerUtils.calendarForToday(
                String.format(java.util.Locale.US, "%02d:%02d", t / 60, t % 60), date),
                pending(context, RQ_TASBIH_BASE + scheduled, intent))
            scheduled++
        }
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
            cancel(context, RQ_ALERT_BASE + i)
            cancel(context, RQ_ADHAN_BASE + i)
            cancel(context, RQ_DND_BASE + i)
            cancel(context, RQ_DND_ENTER_BASE + i)
        }
        for (i in 0..9) cancel(context, RQ_TASBIH_BASE + i)
        cancel(context, RQ_COUNTDOWN)
        cancel(context, RQ_FAJR_ALARM)
    }
}
// END OF FILE