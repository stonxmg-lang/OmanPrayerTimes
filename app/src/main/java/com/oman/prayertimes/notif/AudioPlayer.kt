package com.oman.prayertimes.notif

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager

/**
 * يشغّل ملفات OGG/MP3 من res/raw بالاسم الديناميكي.
 * إذا غاب الملف يستخدم نغمة الإشعار الافتراضية للنظام.
 */
object AudioPlayer {
    private var player: MediaPlayer? = null
    private var remainingLoops = 1

    fun play(context: Context, rawName: String, repeatCount: Int = 1, alarmStream: Boolean = true) {
        stop()
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
                else { it.release(); if (player === it) player = null }
            }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            try { mp.release() } catch (_: Exception) {}
        }
    }

    fun stop() {
        try { player?.stop(); player?.release() } catch (_: Exception) {}
        player = null
        remainingLoops = 1
    }

    fun isPlaying(): Boolean = player?.isPlaying == true
}
