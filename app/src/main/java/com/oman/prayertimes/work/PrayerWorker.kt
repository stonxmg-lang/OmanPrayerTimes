package com.oman.prayertimes.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.notif.AlarmScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** يعمل دوريًا في الخلفية: يحدّث الكاش ويعيد جدولة تنبيهات اليوم */
class PrayerWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            val repo = PrayerRepository(applicationContext)
            val settings = SettingsStore(applicationContext)
            var cache = repo.loadCache()

            // حدّث السنة الحالية إذا كانت قديمة (25 يومًا)
            if (repo.shouldRefreshYear(cache, today.year) && repo.hasInternet()) {
                val days = repo.fetchYear(today.year, settings.cityId)
                if (days.isNotEmpty()) {
                    cache[today.year.toString()] =
                        PrayerRepository.YearCache(LocalDateTime.now().toString(), days)
                    repo.saveCache(cache)
                }
            }

            // أعد جدولة تنبيهات اليوم من الكاش
            val times = repo.getDay(cache, today.year, today.monthValue, today.dayOfMonth)
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
    }
}
