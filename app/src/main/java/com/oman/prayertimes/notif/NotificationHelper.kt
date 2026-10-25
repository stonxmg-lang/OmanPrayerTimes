package com.oman.prayertimes.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.oman.prayertimes.MainActivity
import com.oman.prayertimes.R
import com.oman.prayertimes.utils.PrayerUtils

object NotificationHelper {
    /** قناة التنبيه قبل الصلاة — صوتها ملف res/raw/prealert */
    const val CH_PREALERT = "prayer_prealert_v2"
    /** قناة تذكيرات الذكر — صامتة، والصوت (إن فُعّل) يشغّله AudioPlayer */
    const val CH_TASBIH = "tasbih_v2"
    const val CH_PERSIST = "persistent"

    /** قنوات الإصدارات السابقة (الأذان والمنبه) — تُحذف عند التشغيل */
    private val LEGACY_CHANNELS = listOf("prayer_alerts", "playback", "tasbih")

    const val NTF_PERSIST = 1
    private const val NTF_PREALERT = 1000
    private const val NTF_TASBIH_BASE = 2000

    private val BRAND_GREEN = 0xFF0B6E4F.toInt()

    private fun nm(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun prealertSoundUri(context: Context): Uri =
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.prealert}")

    fun createChannels(context: Context) {
        val nm = nm(context)
        LEGACY_CHANNELS.forEach { id -> try { nm.deleteNotificationChannel(id) } catch (_: Exception) {} }

        nm.createNotificationChannel(NotificationChannel(CH_PREALERT, "التنبيه قبل الصلاة",
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = "إشعار بصوت قبل موعد الأذان"
            setSound(prealertSoundUri(context), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            enableVibration(true)
            enableLights(true)
            lightColor = BRAND_GREEN
        })
        nm.createNotificationChannel(NotificationChannel(CH_TASBIH, "التذكير بالذكر",
            NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "تذكيرات الصلاة على النبي ﷺ والأذكار"
            setSound(null, null)
        })
        nm.createNotificationChannel(NotificationChannel(CH_PERSIST, "الشريط الدائم",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "العدّ التنازلي للصلاة القادمة في شريط الإشعارات"
            setShowBadge(false)
        })
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** «5 دقائق» / «15 دقيقة» … بالصيغة العربية الصحيحة */
    fun minutesText(n: Int): String = when {
        n == 1 -> "دقيقة واحدة"
        n == 2 -> "دقيقتان"
        n in 3..10 -> "$n دقائق"
        else -> "$n دقيقة"
    }

    /** إشعار التنبيه قبل الصلاة (الصوت يأتي من القناة نفسها) */
    fun showPreAlert(context: Context, prayerName: String, leadMinutes: Int, adhanTime: String) {
        val n = NotificationCompat.Builder(context, CH_PREALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_GREEN)
            .setContentTitle("اقترب وقت صلاة $prayerName")
            .setContentText("بقي ${minutesText(leadMinutes)} • الأذان ${PrayerUtils.format12(adhanTime)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        nm(context).notify(NTF_PREALERT, n)
    }

    fun showTasbih(context: Context, text: String, index: Int) {
        val n = NotificationCompat.Builder(context, CH_TASBIH)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_GREEN)
            .setContentTitle("تذكير")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        nm(context).notify(NTF_TASBIH_BASE + index, n)
    }

    /** عدّاد تنازلي حيّ في الستار (يعدّ بالثانية بواسطة النظام نفسه) */
    fun showCountdown(context: Context, label: String, targetMillis: Long) {
        val remaining = targetMillis - System.currentTimeMillis()
        if (remaining <= 0) return
        val hours = remaining / 3_600_000L
        // النظام يرسم أرقام العدّاد بلغة الجهاز، فنستخدم نفس نوع الصفر ليتناسق الشكل
        val zero = String.format(java.util.Locale.getDefault(), "%d", 0)
        val format = when {
            hours >= 10 -> "%s"
            hours >= 1 -> "$zero%s"
            else -> "$zero$zero:%s"
        }
        val rv = RemoteViews(context.packageName, R.layout.notif_countdown)
        rv.setTextViewText(R.id.notif_label, label)
        rv.setChronometer(R.id.notif_chrono, SystemClock.elapsedRealtime() + remaining, format, true)
        rv.setChronometerCountDown(R.id.notif_chrono, true)
        val n = NotificationCompat.Builder(context, CH_PERSIST)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_GREEN)
            .setContentTitle(label)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(rv)
            .setShowWhen(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context))
            .build()
        nm(context).notify(NTF_PERSIST, n)
    }
}
