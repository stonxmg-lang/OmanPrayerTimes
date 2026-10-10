package com.oman.prayertimes.notif

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.utils.PrayerUtils
import java.time.LocalDate

/**
 * الصامت التلقائي أثناء الصلاة.
 * - يستخدم وضع "المنبهات فقط" (يسكّت المكالمات والإشعارات ويبقي الأذان والمنبهات).
 * - يحفظ الوضع السابق مرة واحدة فقط، ولا يلمس الوضع إن كان الجهاز أصلاً في عدم إزعاج.
 * - reconcile() تصحّح الحالة (تدخل/تخرج) حسب الوقت الحالي حتى لو ضاع منبه الدخول أو الخروج.
 */
object DndController {
    private const val TAG = "DndController"
    private const val SP = "app_settings"
    private const val K_ACTIVE = "dnd_active"   // نحن من فعّل الصامت
    private const val K_PREV = "dnd_prev_filter"
    private const val K_UNTIL = "dnd_until"

    fun hasAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun requestAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /** نهاية نافذة الصامت للصلاة الجارية الآن، أو null إن لم نكن داخل نافذة */
    private fun windowEnd(context: Context, now: Long): Long? {
        val repo = PrayerRepository(context)
        val d = LocalDate.now()
        val times = repo.getDay(repo.loadCache(), d.year, d.monthValue, d.dayOfMonth) ?: return null
        for (key in DayTimes.KEYS) {
            val minutes = AlarmScheduler.DND_EXIT_MINUTES[key] ?: continue
            val start = PrayerUtils.calendarForToday(DayTimes.get(times, key), d)
            val end = start + minutes * 60_000L
            if (now in start until end) return end
        }
        return null
    }

    fun enter(context: Context) {
        val now = System.currentTimeMillis()
        val until = windowEnd(context, now) ?: (now + 30 * 60_000L)
        val sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE)

        if (!hasAccess(context)) {
            Log.w(TAG, "لم يدخل الصامت: إذن عدم الإزعاج غير ممنوح")
            return
        }
        // دخلنا من قبل: فقط مدّد الوقت ولا تحفظ الوضع السابق مرة ثانية
        if (sp.getBoolean(K_ACTIVE, false)) {
            sp.edit().putLong(K_UNTIL, until).apply()
            return
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val current = nm.currentInterruptionFilter
        // المستخدم أصلاً في عدم إزعاج: لا نغيّر شيئًا
        if (current != NotificationManager.INTERRUPTION_FILTER_ALL &&
            current != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
            Log.i(TAG, "لم يدخل الصامت: الجهاز أصلاً في وضع عدم إزعاج ($current)")
            return
        }
        try {
            sp.edit().putInt(K_PREV, current).putBoolean(K_ACTIVE, true)
                .putLong(K_UNTIL, until).apply()
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            Log.i(TAG, "دخل الصامت (المنبهات فقط) حتى $until")
        } catch (e: Exception) {
            sp.edit().putBoolean(K_ACTIVE, false).apply()
            Log.e(TAG, "فشل الدخول في الصامت", e)
        }
    }

    fun exit(context: Context) {
        val sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE)
        if (!sp.getBoolean(K_ACTIVE, false)) return
        try {
            if (hasAccess(context)) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // لو غيّرتَ الوضع بنفسك أثناء الصلاة فلا نلمسه
                if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS) {
                    val prev = sp.getInt(K_PREV, NotificationManager.INTERRUPTION_FILTER_ALL)
                    nm.setInterruptionFilter(prev)
                }
            }
            Log.i(TAG, "خرج من الصامت")
        } catch (e: Exception) {
            Log.e(TAG, "فشل الخروج من الصامت", e)
        } finally {
            sp.edit().remove(K_ACTIVE).remove(K_PREV).remove(K_UNTIL).apply()
        }
    }

    /** تصحيح الحالة حسب الوقت: تُستدعى عند فتح التطبيق والإقلاع وكل 6 ساعات */
    fun reconcile(context: Context) {
        val settings = SettingsStore(context)
        val sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (sp.getBoolean(K_ACTIVE, false)) {
            if (!settings.dndEnabled || now >= sp.getLong(K_UNTIL, 0L)) exit(context)
            return
        }
        if (settings.dndEnabled && windowEnd(context, now) != null) enter(context)
    }
}

