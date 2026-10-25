package com.oman.prayertimes.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.receivers.AlarmReceiver
import com.oman.prayertimes.utils.PrayerUtils
import com.oman.prayertimes.work.PrayerWorker
import java.time.DayOfWeek
import java.time.LocalDate

object AlarmScheduler {

    const val RQ_ALERT_BASE      = 100
    const val RQ_DND_BASE        = 300
    const val RQ_COUNTDOWN       = 400
    const val RQ_TASBIH_BASE     = 500
    const val RQ_DND_ENTER_BASE  = 700
    const val RQ_DAY_ROLLOVER    = 800

    // رموز طلبات الإصدارات القديمة (الأذان ومنبه الفجر) — نلغيها فقط عند الترقية
    private const val LEGACY_RQ_ADHAN_BASE = 200
    private const val LEGACY_RQ_FAJR_ALARM = 600

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

    private fun intentOf(context: Context, type: String) =
        Intent(context, AlarmReceiver::class.java).putExtra("type", type)

    /** وقت بالمللي ثانية لدقيقة معيّنة من اليوم (قد تتجاوز 24 ساعة فتنتقل لليوم التالي) */
    private fun atMinute(date: LocalDate, minuteOfDay: Int): Long {
        val d = date.plusDays((minuteOfDay / 1440).toLong())
        val m = minuteOfDay % 1440
        return PrayerUtils.calendarForToday(
            String.format(java.util.Locale.US, "%02d:%02d", m / 60, m % 60), d)
    }

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
     * يجدول كل أحداث اليوم: التنبيه قبل الصلاة، دخول/خروج الصامت، العدّ التنازلي،
     * التذكيرات، ومنبّه منتصف الليل لجدولة اليوم التالي.
     * أي ميزة معطّلة تُلغى منبهاتها صراحة.
     */
    fun scheduleForDay(context: Context, times: DayTimes, date: LocalDate = LocalDate.now()) {
        val settings = SettingsStore(context)
        val keys = DayTimes.KEYS

        DndController.reconcile(context)

        val adhanMin = keys.map { PrayerUtils.toMinutes(DayTimes.get(times, it)) }

        for (i in keys.indices) {
            val key = keys[i]
            // تنظيف منبهات الأذان/المنبه من الإصدارات السابقة
            cancel(context, LEGACY_RQ_ADHAN_BASE + i)
            if (key == "sunrise") continue
            val adhan = adhanMin[i]
            if (adhan < 0) continue

            // ── التنبيه قبل الصلاة: إشعار + صوت ──
            val lead = settings.alertLead(key)
            val alertMin = adhan - lead
            if (settings.alertsEnabled && settings.alertEnabled(key) && alertMin >= 0) {
                val intent = intentOf(context, AlarmReceiver.TYPE_ALERT).apply {
                    putExtra("prayerName", DayTimes.NAMES[i])
                    putExtra("lead", lead)
                    putExtra("adhanTime", DayTimes.get(times, key))
                }
                exactAt(context, atMinute(date, alertMin), pending(context, RQ_ALERT_BASE + i, intent))
            } else {
                cancel(context, RQ_ALERT_BASE + i)
            }

            // ── الصامت: منبها دخول وخروج مستقلان لكل صلاة ──
            if (settings.dndEnabled) {
                exactAt(context, atMinute(date, adhan),
                    pending(context, RQ_DND_ENTER_BASE + i, intentOf(context, AlarmReceiver.TYPE_DND_ENTER)))
                exactAt(context, atMinute(date, adhan + dndExitMinutes(settings, key, date)),
                    pending(context, RQ_DND_BASE + i, intentOf(context, AlarmReceiver.TYPE_DND_EXIT)))
            } else {
                cancel(context, RQ_DND_ENTER_BASE + i)
                cancel(context, RQ_DND_BASE + i)
            }
        }
        cancel(context, LEGACY_RQ_FAJR_ALARM)

        refreshCountdown(context)
        scheduleTasbih(context, adhanMin, date, settings)
        scheduleRollover(context, date)
    }

    /** يعيد جدولة تنبيهات اليوم الحالي — يُستدعى من كل تعديل إعدادات */
    fun rescheduleToday(context: Context) {
        val repo = PrayerRepository(context)
        val today = LocalDate.now()
        val times = repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
        if (times == null) {
            // لا بيانات لليوم (مثلاً أول يناير قبل تحميل السنة الجديدة): حمّلها في الخلفية
            PrayerWorker.runOnce(context)
            return
        }
        scheduleForDay(context, times, today)
    }

    /** منبه بعد منتصف الليل بنصف دقيقة لجدولة اليوم الجديد (لا ننتظر WorkManager) */
    private fun scheduleRollover(context: Context, date: LocalDate) {
        val at = PrayerUtils.calendarForToday("00:00", date.plusDays(1)) + 30_000L
        exactAt(context, at, pending(context, RQ_DAY_ROLLOVER,
            intentOf(context, AlarmReceiver.TYPE_DAY_ROLLOVER)))
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
        exactAt(context, nextRefresh,
            pending(context, RQ_COUNTDOWN, intentOf(context, AlarmReceiver.TYPE_COUNTDOWN)))
    }

    private fun scheduleTasbih(context: Context, adhanMin: List<Int>,
                               date: LocalDate, settings: SettingsStore) {
        for (i in 0..9) cancel(context, RQ_TASBIH_BASE + i)
        if (!settings.tasbihEnabled) return

        // الصيغ التي اختارها المستخدم فقط (ولو ألغى الكل نستخدم الكل)
        val texts = TasbihTexts.list.indices.filter { settings.tasbihTextEnabled(it) }
            .ifEmpty { TasbihTexts.list.indices.toList() }

        val n = settings.tasbihCount.coerceIn(2, 10)
        val fajrMin = adhanMin[0]
        val ishaMin = adhanMin[5]
        if (fajrMin < 0 || ishaMin <= fajrMin) return

        val interval = (ishaMin - fajrMin).toDouble() / (n + 1)
        val windows = DayTimes.KEYS.indices
            .filter { DayTimes.KEYS[it] != "sunrise" }
            .map { adhanMin[it]..(adhanMin[it] + dndExitMinutes(settings, DayTimes.KEYS[it], date)) }

        var scheduled = 0
        for (k in 1..n) {
            val t = (fajrMin + interval * k).toInt()
            if (windows.any { t in it }) continue
            val intent = intentOf(context, AlarmReceiver.TYPE_TASBIH).apply {
                putExtra("tasbihIndex", texts[scheduled % texts.size])
            }
            exactAt(context, atMinute(date, t), pending(context, RQ_TASBIH_BASE + scheduled, intent))
            scheduled++
        }
    }

    fun cancel(context: Context, requestCode: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(context, requestCode,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    fun cancelAll(context: Context) {
        for (i in 0..5) {
            cancel(context, RQ_ALERT_BASE + i)
            cancel(context, LEGACY_RQ_ADHAN_BASE + i)
            cancel(context, RQ_DND_BASE + i)
            cancel(context, RQ_DND_ENTER_BASE + i)
        }
        for (i in 0..9) cancel(context, RQ_TASBIH_BASE + i)
        cancel(context, RQ_COUNTDOWN)
        cancel(context, LEGACY_RQ_FAJR_ALARM)
        cancel(context, RQ_DAY_ROLLOVER)
    }
}
