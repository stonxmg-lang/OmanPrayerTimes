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
        fun get(times: DayTimes, key: String): String {
            val raw = when (key) {
                "fajr" -> times.fajr; "sunrise" -> times.sunrise; "dhuhr" -> times.dhuhr
                "asr" -> times.asr; "maghrib" -> times.maghrib; "isha" -> times.isha
                else -> ""
            }
            return to24h(key, raw)
        }

        private fun to24h(key: String, t: String): String {
            return try {
                val p = t.split(":")
                var h = p[0].toInt()
                val m = p[1].toInt()
                if (key in listOf("asr", "maghrib", "isha") && h < 12) h += 12
                else if (key == "dhuhr" && h < 6) h += 12
                "%02d:%02d".format(h, m)
            } catch (e: Exception) { t }
        }
    }
}

