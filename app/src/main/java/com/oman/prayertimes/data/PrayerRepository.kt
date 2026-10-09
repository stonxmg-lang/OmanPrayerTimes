package com.oman.prayertimes.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oman.prayertimes.model.DayTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerRepository(private val context: Context) {

    data class YearCache(val fetchedAt: String = "", val days: List<DayTimes> = emptyList())

    private val gson = Gson()
    private val cacheFile get() = File(context.filesDir, "prayer_cache.json")

    private val monthPattern = Regex(
        "<td[^>]*>(?:<font[^>]*>)?(\\d+/\\d+/\\d+)(?:</font>)?</td>" +
        "\\s*<td[^>]*>(?:<font[^>]*>)?(\\d+:\\d+)(?:</font>)?</td>".repeat(6)
    )

    private val dayPattern = Regex(
        "<td[^>]*>(\\d+/\\d+/\\d+)</td>" +
        "\\s*<td[^>]*>(\\d+:\\d+)</td>".repeat(6)
    )

    // ── Cache ──────────────────────────────────────────────────────
    fun loadCache(): MutableMap<String, YearCache> {
        if (!cacheFile.exists()) return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, YearCache>>() {}.type
            gson.fromJson(cacheFile.readText(), type) ?: mutableMapOf()
        } catch (e: Exception) { mutableMapOf() }
    }

    fun saveCache(cache: Map<String, YearCache>) {
        try {
            val tmp = File(context.filesDir, "prayer_cache.json.tmp")
            tmp.writeText(gson.toJson(cache))
            tmp.renameTo(cacheFile)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getDay(cache: Map<String, YearCache>, year: Int, month: Int, day: Int): DayTimes? {
        val dateStr = "$day/$month/$year"
        return cache[year.toString()]?.days?.firstOrNull { it.date == dateStr }
    }

    fun cacheAgeDays(cache: Map<String, YearCache>, year: Int): Int? {
        val fetched = cache[year.toString()]?.fetchedAt ?: return null
        return try {
            java.time.Duration.between(LocalDateTime.parse(fetched), LocalDateTime.now()).toDays().toInt()
        } catch (e: Exception) { null }
    }

    fun shouldRefreshYear(cache: Map<String, YearCache>, year: Int): Boolean {
        val entry = cache[year.toString()] ?: return true
        if (entry.days.isEmpty()) return true
        if (year == LocalDate.now().year) {
            val age = cacheAgeDays(cache, year) ?: return true
            return age >= 25
        }
        return false
    }

    fun cleanupOldYears(cache: MutableMap<String, YearCache>): Boolean {
        val current = LocalDate.now().year
        val keys = cache.keys.filter { it.toIntOrNull() != null && it.toInt() < current }
        keys.forEach { cache.remove(it) }
        return keys.isNotEmpty()
    }

    // ── Network ────────────────────────────────────────────────────
    suspend fun hasInternet(): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = open("https://www.mara.gov.om")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.responseCode in 200..399
        } catch (e: Exception) { false }
    }

    suspend fun fetchDay(year: Int, month: Int, day: Int, cityId: Int): DayTimes? =
        withContext(Dispatchers.IO) {
            val html = httpPost(
                "https://www.mara.gov.om/arabic/calendar_page1.asp",
                "year=$year&month=$month&day=$day&CityID=$cityId"
            ) ?: return@withContext null
            dayPattern.find(html)?.let { m ->
                DayTimes(m.groupValues[1], m.groupValues[2], m.groupValues[3],
                    m.groupValues[4], m.groupValues[5], m.groupValues[6], m.groupValues[7])
            }
        }

    suspend fun fetchYear(year: Int, cityId: Int): List<DayTimes> = withContext(Dispatchers.IO) {
        val all = mutableListOf<DayTimes>()
        for (month in 1..12) {
            val html = httpPost(
                "https://www.mara.gov.om/arabic/calendar_page2.asp",
                "year=$year&month=$month&CityID=$cityId"
            )
            if (html != null) {
                monthPattern.findAll(html).forEach { m ->
                    all.add(DayTimes(m.groupValues[1], m.groupValues[2], m.groupValues[3],
                        m.groupValues[4], m.groupValues[5], m.groupValues[6], m.groupValues[7]))
                }
            }
        }
        all
    }

    // يتخطى فحص الشهادة لموقع mara.gov.om فقط (نفس curl -k)
    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        if (conn is javax.net.ssl.HttpsURLConnection) {
            val trustAll = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>?, a: String?) {}
                override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>?, a: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val ctx = javax.net.ssl.SSLContext.getInstance("TLS")
            ctx.init(null, trustAll, java.security.SecureRandom())
            conn.sslSocketFactory = ctx.socketFactory
            conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { host, _ -> host.endsWith("mara.gov.om") }
        }
        return conn
    }

    private fun httpPost(url: String, form: String): String? {
        return try {
            val conn = open(url)
            conn.requestMethod = "POST"
            conn.connectTimeout = 12000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(form.toByteArray()) }
            if (conn.responseCode in 200..299)
                conn.inputStream.bufferedReader().use { it.readText() }
            else null
        } catch (e: Exception) { null }
    }
}

