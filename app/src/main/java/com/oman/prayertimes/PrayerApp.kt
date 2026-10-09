package com.oman.prayertimes

import android.app.Application
import com.oman.prayertimes.notif.NotificationHelper
import com.oman.prayertimes.work.PrayerWorker

class PrayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        PrayerWorker.schedule(this)
    }
}
