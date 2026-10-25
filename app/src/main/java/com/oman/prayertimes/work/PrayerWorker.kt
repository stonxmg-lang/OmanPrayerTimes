package com.oman.prayertimes.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.notif.AlarmScheduler
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** يعمل دوريًا في الخلفية: يحدّث الكاش عند الحاجة ويعيد جدولة تنبيهات اليوم */
class PrayerWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = PrayerRepository(applicationContext)
            repo.refreshIfNeeded()

            val today = LocalDate.now()
            val times = repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
            if (times != null) AlarmScheduler.scheduleForDay(applicationContext, times, today)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PrayerWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "prayer_refresh", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** تشغيل فوري لمرة واحدة عند الحاجة لبيانات (يحتاج اتصالًا بالإنترنت) */
        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<PrayerWorker>()
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "prayer_refresh_once", ExistingWorkPolicy.KEEP, request)
        }
    }
}

