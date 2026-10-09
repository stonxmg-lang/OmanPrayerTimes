package com.oman.prayertimes.utils

import com.oman.prayertimes.model.DayTimes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.floor

object PrayerUtils {

    private val fmt = DateTimeFormatter.ofPattern("HH:mm")

    fun toMinutes(hhmm: String): Int {
        return try {
            val p = hhmm.split(":")
            p[0].toInt() * 60 + p[1].toInt()
        } catch (e: Exception) { -1 }
    }

    /** يحوّل "15:27" إلى "3:27 م" للعرض فقط (الحساب الداخلي يبقى 24 ساعة) */
    fun format12(hhmm: String): String {
        val total = toMinutes(hhmm)
        if (total < 0) return hhmm
        val h24 = total / 60
        val mm = total % 60
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        val suffix = if (h24 < 12) "ص" else "م"
        return String.format(java.util.Locale.US, "%d:%02d %s", h12, mm, suffix)
    }

    /** index of next prayer, or null if all passed (next = tomorrow's fajr) */
    fun nextPrayerIndex(times: DayTimes, nowMinutes: Int): Int {
        for (i in DayTimes.KEYS.indices) {
            val m = toMinutes(DayTimes.get(times, DayTimes.KEYS[i]))
            if (m > nowMinutes) return i
        }
        return 0 // tomorrow fajr
    }

    fun timeUntilString(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "الآن"
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val l = java.util.Locale.US
        return if (h > 0) String.format(l, "%dس %02dد %02dث", h, m, s)
        else String.format(l, "%dد %02dث", m, s)
    }

    fun calendarForToday(hhmm: String, date: LocalDate = LocalDate.now()): Long {
        val m = toMinutes(hhmm)
        val cal = java.util.Calendar.getInstance()
        cal.set(date.year, date.monthValue - 1, date.dayOfMonth, m / 60, m % 60, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ── Hijri (approx, same algorithm as the original script) ──────
    private val HIJRI_MONTHS = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun hijriString(date: LocalDate): String {
        val (y, m, d) = toHijri(date.year, date.monthValue, date.dayOfMonth)
        return "$d ${HIJRI_MONTHS[m - 1]} $y هـ"
    }

    private fun toHijri(y: Int, m: Int, d: Int): Triple<Int, Int, Int> {
        val jd = 367.0 * y - floor(7.0 * (y + (m + 9) / 12) / 4) +
                floor(275.0 * m / 9) + d + 1721013.5
        val z = jd - 1948438.5 + 0.5
        val cyc = floor(z / 10631)
        var rem = z - cyc * 10631
        var yh = (cyc * 30).toInt()
        val y30 = doubleArrayOf(0.0, 354.0, 709.0, 1063.0, 1417.0, 1772.0, 2126.0,
            2480.0, 2835.0, 3189.0, 3543.0, 3898.0, 4252.0, 4606.0, 4961.0, 5315.0,
            5669.0, 6024.0, 6378.0, 6732.0, 7087.0, 7441.0, 7795.0, 8150.0, 8504.0,
            8858.0, 9213.0, 9567.0, 9921.0, 10276.0)
        for (i in 29 downTo 0) {
            if (rem >= y30[i]) { yh += i + 1; rem -= y30[i]; break }
        }
        val mh = minOf((rem * 30 / 885).toInt() + 1, 12)
        val dh = (rem - (mh - 1) * 29.5).toInt() + 1
        return Triple(yh, mh, dh)
    }
}

