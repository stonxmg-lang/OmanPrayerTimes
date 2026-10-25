package com.oman.prayertimes.notif

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager

/**
 * تشغيل صوت قصير مرة واحدة على مسار الإشعارات (يتبع مستوى صوت الإشعارات
 * ويحترم الوضع الصامت). يُستخدم لتذكيرات الذكر فقط.
 * إن لم يوجد الملف في res/raw تُستخدم نغمة الإشعار الافتراضية للنظام.
 */
object AudioPlayer {
    private var player: MediaPlayer? = null

    fun play(context: Context, rawName: String) {
        stop()
        val ctx = context.applicationContext
        val resId = ctx.resources.getIdentifier(rawName, "raw", ctx.packageName)
        val mp = MediaPlayer()
        try {
            if (resId != 0) {
                ctx.resources.openRawResourceFd(resId).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            } else {
                mp.setDataSource(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setOnCompletionListener { stop() }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            try { mp.release() } catch (_: Exception) {}
            player = null
        }
    }

    fun stop() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
    }
}
