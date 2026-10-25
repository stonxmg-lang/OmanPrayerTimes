package com.oman.prayertimes.utils

import com.oman.prayertimes.model.DayTimes
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerUtilsTest {
    @Test
    fun format12_shouldReturnArabicSuffix() {
        assertEquals("12:00 م", PrayerUtils.format12("12:00"))
        assertEquals("6:30 ص", PrayerUtils.format12("06:30"))
    }

    @Test
    fun nextPrayerIndex_shouldReturnFirstUpcomingPrayer() {
        val times = DayTimes(
            date = "01/01/2025",
            fajr = "05:30",
            sunrise = "06:46",
            dhuhr = "12:15",
            asr = "15:30",
            maghrib = "18:15",
            isha = "19:45"
        )
        assertEquals(2, PrayerUtils.nextPrayerIndex(times, 12 * 60))   // 12:00 ← الظهر
    }

    @Test
    fun currentPrayerIndex_coversNightSunriseAndDay() {
        val times = DayTimes(
            date = "01/01/2025",
            fajr = "05:30", sunrise = "06:46", dhuhr = "12:15",
            asr = "15:30", maghrib = "18:15", isha = "19:45"
        )
        assertEquals(5, PrayerUtils.currentPrayerIndex(times, 2 * 60))        // قبل الفجر = العشاء
        assertEquals(0, PrayerUtils.currentPrayerIndex(times, 6 * 60))        // بعد الفجر
        assertEquals(-1, PrayerUtils.currentPrayerIndex(times, 9 * 60))       // بعد الشروق
        assertEquals(3, PrayerUtils.currentPrayerIndex(times, 16 * 60))       // العصر
        assertEquals(5, PrayerUtils.currentPrayerIndex(times, 23 * 60))       // بعد العشاء
    }
}
