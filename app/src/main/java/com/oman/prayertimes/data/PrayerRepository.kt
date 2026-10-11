package com.oman.prayertimes.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.notif.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class PrayerRepository(private val context: Context) {

    data class YearCache(val fetchedAt: String = "", val days: List<DayTimes> = emptyList())

    private val gson = Gson()
    private val settings = SettingsStore(context)
    private val cacheFile get() = File(context.filesDir, "prayer_cache.json")

    private val monthPattern = Regex(
        "<td[^>]*>(?:<font[^>]*>)?(\\d+/\\d+/\\d+)(?:</font>)?</td>" +
        "\\s*<td[^>]*>(?:<font[^>]*>)?(\\d+:\\d+)(?:</font>)?</td>".repeat(6)
    )

    private val dayPattern = Regex(
        "<td[^>]*>(\\d+/\\d+/\\d+)</td>" +
        "\\s*<td[^>]*>(\\d+:\\d+)</td>".repeat(6)
    )

    companion object {
        private val lock = Any()
        private var mem: MutableMap<String, YearCache>? = null
        private var memStamp = -1L

        // يُنشأ مرة واحدة فقط (بدل إنشائه مع كل طلب)
        private val sslFactory: SSLSocketFactory by lazy {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>?, a: String?) {}
                override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>?, a: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, trustAll, java.security.SecureRandom())
            ctx.socketFactory
        }
    }

    // ── Cache ──────────────────────────────────────────────────────

    /** مفتاح الكاش: المدينة + السنة، حتى لا تختلط بيانات المدن */
    fun keyFor(year: Int, cityId: Int = settings.cityId): String = "${cityId}_$year"

    /** يقرأ الكاش من الذاكرة إن لم يتغيّر الملف (بدل تحليل JSON في كل مرة) */
    fun loadCache(): MutableMap<String, YearCache> = synchronized(lock) {
        val stamp = if (cacheFile.exists()) cacheFile.lastModified() else 0L
        val cached = mem
        if (cached != null && memStamp == stamp) return@synchronized cached
        val loaded: MutableMap<String, YearCache> = if (stamp == 0L) mutableMapOf() else try {
            val type = object : TypeToken<MutableMap<String, YearCache>>() {}.type
            gson.fromJson<MutableMap<String, YearCache>>(cacheFile.readText(), type) ?: mutableMapOf()
        } catch (e: Exception) { mutableMapOf() }
        mem = loaded
        memStamp = stamp
        loaded
    }

    fun saveCache(cache: Map<String, YearCache>) {
        synchronized(lock) {
            try {
                val tmp = File(context.filesDir, "prayer_cache.json.tmp")
                tmp.writeText(gson.toJson(cache))
                if (!tmp.renameTo(cacheFile)) {
                    cacheFile.writeText(tmp.readText())
                    tmp.delete()
                }
                @Suppress("UNCHECKED_CAST")
                val asMutable = (cache as? MutableMap<String, YearCache>) ?: cache.toMutableMap()
                mem = asMutable
                memStamp = cacheFile.lastModified()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun getDay(cache: Map<String, YearCache>, year: Int, month: Int, day: Int): DayTimes? {
        val dateStr = "$day/$month/$year"
        return cache[keyFor(year)]?.days?.firstOrNull { it.date == dateStr }
    }

    fun cacheAgeDays(cache: Map<String, YearCache>, year: Int): Int? {
        val fetched = cache[keyFor(year)]?.fetchedAt ?: return null
        return try {
            java.time.Duration.between(LocalDateTime.parse(fetched), LocalDateTime.now()).toDays().toInt()
        } catch (e: Exception) { null }
    }

    fun shouldRefreshYear(cache: Map<String, YearCache>, year: Int): Boolean {
        val entry = cache[keyFor(year)] ?: return true
        if (entry.days.isEmpty()) return true
        if (year == LocalDate.now().year) {
            val age = cacheAgeDays(cache, year) ?: return true
            return age >= 25
        }
        return false
    }

    /** يحذف السنوات القديمة وبيانات المدن الأخرى والمفاتيح القديمة */
    fun cleanupOldYears(cache: MutableMap<String, YearCache>): Boolean {
        val currentYear = LocalDate.now().year
        val city = settings.cityId.toString()
        val stale = cache.keys.filter { key ->
            val p = key.split("_")
            val keep = p.size == 2 && p[0] == city && (p[1].toIntOrNull() ?: 0) >= currentYear
            !keep
        }
        stale.forEach { cache.remove(it) }
        return stale.isNotEmpty()
    }

    /**
     * يحدّث الكاش للمدينة الحالية عند اللزوم (بيانات ناقصة أو أقدم من 25 يومًا،
     * وبعد بداية نوفمبر يحمّل السنة القادمة). يرجع true إذا تغيّر الكاش.
     */
    suspend fun refreshIfNeeded(): Boolean {
        val cityId = settings.cityId // نثبّتها: لو تغيّرت أثناء التحميل لا تختلط البيانات
        val cache = loadCache()
        val today = LocalDate.now()
        val years = mutableListOf<Int>()
        if (shouldRefreshYear(cache, today.year)) years.add(today.year)
        if (today.monthValue >= 11 && shouldRefreshYear(cache, today.year + 1)) years.add(today.year + 1)

        var changed = false
        for (y in years) {
            val days = fetchYear(y, cityId)
            val hadOld = cache[keyFor(y, cityId)]?.days?.isNotEmpty() == true
            // لا نستبدل بيانات سليمة ببيانات ناقصة
            if (days.size >= 350 || (!hadOld && days.isNotEmpty())) {
                cache[keyFor(y, cityId)] = YearCache(LocalDateTime.now().toString(), days)
                changed = true
            }
        }
        if (cleanupOldYears(cache)) changed = true
        if (changed) saveCache(cache)
        return changed
    }

    /** بعد تغيير المدينة: حمّل بياناتها ثم أعد جدولة التنبيهات. يرجع false إن لم تتوفر بيانات */
    suspend fun applyCityChange(): Boolean {
        refreshIfNeeded()
        val today = LocalDate.now()
        val times = getDay(loadCache(), today.year, today.monthValue, today.dayOfMonth) ?: return false
        withContext(Dispatchers.IO) { AlarmScheduler.scheduleForDay(context, times, today) }
        return true
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

    private fun fetchMonth(year: Int, month: Int, cityId: Int): List<DayTimes> {
        repeat(2) { // محاولتان
            val html = httpPost(
                "https://www.mara.gov.om/arabic/calendar_page2.asp",
                "year=$year&month=$month&CityID=$cityId"
            )
            if (html != null) {
                val list = monthPattern.findAll(html).map { m ->
                    DayTimes(m.groupValues[1], m.groupValues[2], m.groupValues[3],
                        m.groupValues[4], m.groupValues[5], m.groupValues[6], m.groupValues[7])
                }.toList()
                if (list.isNotEmpty()) return list
            }
        }
        return emptyList()
    }

    /** تحميل السنة: 4 أشهر بالتوازي في كل دفعة، ويتوقف مبكرًا لو لا يوجد اتصال */
    suspend fun fetchYear(year: Int, cityId: Int = settings.cityId): List<DayTimes> =
        withContext(Dispatchers.IO) {
            val all = mutableListOf<DayTimes>()
            for (chunk in (1..12).chunked(4)) {
                val parts = coroutineScope {
                    chunk.map { m -> async { fetchMonth(year, m, cityId) } }.awaitAll()
                }
                if (parts.all { it.isEmpty() }) break
                parts.forEach { all.addAll(it) }
            }
            all
        }

    // يتخطى فحص الشهادة لموقع mara.gov.om فقط (نفس curl -k)
    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        if (conn is HttpsURLConnection) {
            conn.sslSocketFactory = sslFactory
            conn.hostnameVerifier = HostnameVerifier { host, _ -> host.endsWith("mara.gov.om") }
        }
        return conn
    }

    private fun httpPost(url: String, form: String): String? {
        return try {
            val conn = open(url)
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(form.toByteArray()) }
            if (conn.responseCode in 200..299)
                conn.inputStream.bufferedReader().use { it.readText() }
            else null
        } catch (e: Exception) { null }
    }
}

