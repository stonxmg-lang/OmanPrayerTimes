package com.oman.prayertimes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oman.prayertimes.notif.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * يعيد جدولة تنبيهات اليوم بعد: إعادة تشغيل الجهاز، تحديث التطبيق
 * (وهذا يلغي أيضًا منبهات الأذان القديمة من الإصدارات السابقة)، أو تغيير الوقت/المنطقة.
 */
class BootReceiver : BroadcastReceiver() {

    private val handled = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in handled) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler.rescheduleToday(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
