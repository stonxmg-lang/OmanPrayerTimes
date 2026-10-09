package com.oman.prayertimes.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.oman.prayertimes.MainActivity
import com.oman.prayertimes.R

object NotificationHelper {
    const val CH_ALERTS = "prayer_alerts"
    const val CH_TASBIH = "tasbih"
    const val CH_PERSIST = "persistent"
    const val CH_PLAYBACK = "playback"

    const val NTF_PERSIST = 1
    const val NTF_PLAYBACK = 2

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH_ALERTS, "تنبيهات الصلاة",
            NotificationManager.IMPORTANCE_HIGH).apply { description = "الأذان والتنبيه قبل الصلاة" })
        nm.createNotificationChannel(NotificationChannel(CH_TASBIH, "التذكير بالذكر",
            NotificationManager.IMPORTANCE_DEFAULT).apply { description = "تذكيرات الصلاة على النبي والأذكار" })
        nm.createNotificationChannel(NotificationChannel(CH_PERSIST, "الشريط الدائم",
            NotificationManager.IMPORTANCE_LOW).apply { description = "عرض الصلاة القادمة في شريط الإشعارات" })
        nm.createNotificationChannel(NotificationChannel(CH_PLAYBACK, "تشغيل الصوت",
            NotificationManager.IMPORTANCE_LOW).apply { description = "إشعار تشغيل الأذان والمنبه" })
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** إشعار ثابت في الستار: الصلاة القادمة أو العد التنازلي */
    fun showPersistent(context: Context, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(context, CH_PERSIST)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("مواقيت الصلاة - عُمان")
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context))
            .build()
        nm.notify(NTF_PERSIST, n)
    }

    fun showEvent(context: Context, channel: String, title: String, text: String, id: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        nm.notify(id, n)
    }

    fun playbackNotification(context: Context, title: String): android.app.Notification =
        NotificationCompat.Builder(context, CH_PLAYBACK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setOngoing(true)
            .setContentIntent(openAppIntent(context))
            .build()
}
