package com.oman.prayertimes.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.notif.AudioPlayer
import com.oman.prayertimes.notif.NotificationHelper

/** خدمة تشغيل الأذان/المنبه - تبقى حية حتى مع قفل الشاشة، وتتوقف عند انتهاء الصوت أو الإيقاف */
class PlaybackService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }

        // نتجاهل أي intent بلا اسم صوت صريح (لا نغذي نغمة افتراضية)
        val sound = intent?.getStringExtra("sound")
        if (sound.isNullOrEmpty()) {
            stopPlayback()
            return START_NOT_STICKY
        }
        val title = intent.getStringExtra("title") ?: "الأذان"
        val repeat = intent.getIntExtra("repeat", 1)

        startForeground(NotificationHelper.NTF_PLAYBACK,
            NotificationHelper.playbackNotification(this, title))

        val volume = SettingsStore(this).adhanVolume / 100f

        // عند انتهاء الصوت أو ضغط الباور تتوقف الخدمة ويختفي إشعارا التشغيل وملء الشاشة
        AudioPlayer.play(this, sound, repeatCount = repeat, alarmStream = true,
            volume = volume, onDone = { stopPlayback() })

        // إيقاف تلقائي بعد مهلة أمان (60 دقيقة) حال بقاء الخدمة عالقة
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!AudioPlayer.isPlaying()) stopPlayback()
        }, 60 * 60 * 1000L)

        return START_NOT_STICKY
    }

    private fun stopPlayback() {
        AudioPlayer.stop()
        NotificationHelper.cancelFullScreen(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        AudioPlayer.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.oman.prayertimes.STOP_PLAYBACK"
    }
}
// END OF FILE