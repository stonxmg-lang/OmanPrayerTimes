package com.oman.prayertimes.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DayTimesTest {
    @Test
    fun get_shouldNormalizePrayerTimes() {
        val times = DayTimes(
            date = "01/01/2025",
            fajr = "05:30",
            sunrise = "06:45",
            dhuhr = "12:10",
            asr = "15:20",
            maghrib = "18:05",
            isha = "19:40"
        )

        assertEquals("05:30", DayTimes.get(times, "fajr"))
        assertEquals("12:10", DayTimes.get(times, "dhuhr"))
        assertEquals("15:20", DayTimes.get(times, "asr"))
        assertEquals("18:05", DayTimes.get(times, "maghrib"))
        assertEquals("19:40", DayTimes.get(times, "isha"))
    }
}
