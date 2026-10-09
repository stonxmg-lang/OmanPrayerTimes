package com.oman.prayertimes.model

data class DayTimes(
    val date: String = "",
    val fajr: String = "",
    val sunrise: String = "",
    val dhuhr: String = "",
    val asr: String = "",
    val maghrib: String = "",
    val isha: String = ""
) {
    companion object {
        val KEYS = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
        val NAMES = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        val ICONS = listOf("🌙", "🌅", "☀️", "🌤️", "🌇", "🌃")
        fun get(times: DayTimes, key: String): String = when (key) {
            "fajr" -> times.fajr; "sunrise" -> times.sunrise; "dhuhr" -> times.dhuhr
            "asr" -> times.asr; "maghrib" -> times.maghrib; "isha" -> times.isha
            else -> ""
        }
    }
}
