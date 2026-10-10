package com.oman.prayertimes.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * يشغّل ملفات OGG/MP3 من res/raw بالاسم الديناميكي.
 * إذا غاب الملف يستخدم نغمة الإشعار الافتراضية للنظام.
 * أثناء التشغيل: ضغطة زر الباور (إطفاء أو إشعال الشاشة) توقف الصوت.
 */
object AudioPlayer {
    private var player: MediaPlayer? = null
    private var remainingLoops = 1
    private var onDone: (() -> Unit)? = null

    private var screenReceiver: BroadcastReceiver? = null
    private var registeredOn: Context? = null
    private var startedAt = 0L
    // تجاهل أحداث الشاشة في أول ثواني (قد يكون الأذان نفسه هو من أشعل الشاشة)
    private const val POWER_GRACE_MS = 3000L

    fun play(
        context: Context,
        rawName: String,
        repeatCount: Int = 1,
        alarmStream: Boolean = true,
        onDone: (() -> Unit)? = null
    ) {
        stop()
        this.onDone = onDone
        remainingLoops = repeatCount.coerceAtLeast(1)
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        val mp = MediaPlayer()
        try {
            if (resId != 0) {
                val afd = context.resources.openRawResourceFd(resId)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mp.setDataSource(context, uri)
            }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (alarmStream) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setOnCompletionListener {
                remainingLoops--
                if (remainingLoops > 0) { it.seekTo(0); it.start() }
                else finish()
            }
            mp.prepare()
            mp.start()
            player = mp
            startedAt = SystemClock.elapsedRealtime()
            registerPowerStop(context.applicationContext)
        } catch (e: Exception) {
            try { mp.release() } catch (_: Exception) {}
            finish()
        }
    }

    /** إنهاء التشغيل ثم إبلاغ صاحب الطلب (مثلاً لإيقاف الخدمة) */
    private fun finish() {
        val cb = onDone
        stop()
        cb?.invoke()
    }

    fun stop() {
        try { player?.stop(); player?.release() } catch (_: Exception) {}
        player = null
        remainingLoops = 1
        onDone = null
        unregisterPowerStop()
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    private fun registerPowerStop(ctx: Context) {
        unregisterPowerStop()
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (SystemClock.elapsedRealtime() - startedAt > POWER_GRACE_MS) finish()
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(ctx, r, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        screenReceiver = r
        registeredOn = ctx
    }

    private fun unregisterPowerStop() {
        try { screenReceiver?.let { registeredOn?.unregisterReceiver(it) } } catch (_: Exception) {}
        screenReceiver = null
        registeredOn = null
    }
}

