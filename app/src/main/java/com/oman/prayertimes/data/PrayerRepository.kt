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
import javax.net.ssl.SSLException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class PrayerRepository(private val context: Context) {

    data class YearCache(val fetchedAt: String = "", val days: List<DayTimes> = emptyList())

    /** نتيجة محاولة التحديث: نفرّق بين عدم وجود إنترنت وبين تغيّر صفحة الوزارة. */
    enum class RefreshResult { UPDATED, UP_TO_DATE, NO_INTERNET, PARSE_FAILED }

    private val gson = Gson()
    private val settings = SettingsStore(context)
    private val cacheFile get() = File(context.filesDir, "prayer_cache.json")

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
     * وبعد بداية نوفمبر يحمّل السنة القادمة).
     * يرجع RefreshResult: UPDATED عند تغيّر الكاش، UP_TO_DATE إن لم يلزم تحديث،
     * NO_INTERNET إن تعذّر الوصول للموقع، PARSE_FAILED إن وصلت الصفحة لكن تعذّرت قراءتها.
     */
    suspend fun refreshIfNeeded(): RefreshResult {
        val cityId = settings.cityId // نثبّتها: لو تغيّرت أثناء التحميل لا تختلط البيانات
        val cache = loadCache()
        val today = LocalDate.now()
        val years = mutableListOf<Int>()
        if (shouldRefreshYear(cache, today.year)) years.add(today.year)
        if (today.monthValue >= 11 && shouldRefreshYear(cache, today.year + 1)) years.add(today.year + 1)

        var changed = false
        var sawParseFail = false
        var sawNetworkFail = false
        for (y in years) {
            val fetch = fetchYear(y, cityId)
            if (fetch.parseFailed) sawParseFail = true
            if (fetch.networkFailed) sawNetworkFail = true
            val hadOld = cache[keyFor(y, cityId)]?.days?.isNotEmpty() == true
            // لا نستبدل بيانات سليمة ببيانات ناقصة
            if (fetch.days.size >= 350 || (!hadOld && fetch.days.isNotEmpty())) {
                cache[keyFor(y, cityId)] = YearCache(LocalDateTime.now().toString(), fetch.days)
                changed = true
            }
        }
        if (cleanupOldYears(cache)) changed = true
        if (changed) saveCache(cache)

        return when {
            changed -> RefreshResult.UPDATED
            years.isEmpty() -> RefreshResult.UP_TO_DATE   // لم يلزم تحديث أصلًا
            sawParseFail -> RefreshResult.PARSE_FAILED     // الصفحة وصلت لكن تنسيقها تغيّر
            sawNetworkFail -> RefreshResult.NO_INTERNET
            else -> RefreshResult.UP_TO_DATE
        }
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
        fun probe(insecure: Boolean): Boolean {
            val conn = open("https://www.mara.gov.om", insecure)
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            return conn.responseCode in 200..399
        }
        try {
            probe(insecure = false)
        } catch (e: SSLException) {
            try { probe(insecure = true) } catch (e2: Exception) { false }
        } catch (e: Exception) { false }
    }

    suspend fun fetchDay(year: Int, month: Int, day: Int, cityId: Int): DayTimes? =
        withContext(Dispatchers.IO) {
            val html = httpPost(
                "https://www.mara.gov.om/arabic/calendar_page1.asp",
                "year=$year&month=$month&day=$day&CityID=$cityId"
            ) ?: return@withContext null
            PrayerHtmlParser.parseFirst(html)
        }

    /** نتيجة جلب شهر: نجاح ببيانات، أو فشل شبكة، أو صفحة وصلت لكن تعذّرت قراءتها. */
    private sealed interface MonthFetch {
        data class Ok(val days: List<DayTimes>) : MonthFetch
        object Network : MonthFetch
        object ParseFail : MonthFetch
    }

    private fun fetchMonth(year: Int, month: Int, cityId: Int): MonthFetch {
        var gotHtml = false
        repeat(2) { // محاولتان
            val html = httpPost(
                "https://www.mara.gov.om/arabic/calendar_page2.asp",
                "year=$year&month=$month&CityID=$cityId"
            )
            if (html != null) {
                gotHtml = true
                val list = PrayerHtmlParser.parse(html)
                if (list.isNotEmpty()) return MonthFetch.Ok(list)
            }
        }
        return if (gotHtml) MonthFetch.ParseFail else MonthFetch.Network
    }

    data class YearFetch(
        val days: List<DayTimes>,
        val parseFailed: Boolean,
        val networkFailed: Boolean
    )

    /** تحميل السنة: 4 أشهر بالتوازي في كل دفعة، ويتوقف مبكرًا لو لا يوجد اتصال. */
    suspend fun fetchYear(year: Int, cityId: Int = settings.cityId): YearFetch =
        withContext(Dispatchers.IO) {
            val all = mutableListOf<DayTimes>()
            var parseFailed = false
            var networkFailed = false
            for (chunk in (1..12).chunked(4)) {
                val parts = coroutineScope {
                    chunk.map { m -> async { fetchMonth(year, m, cityId) } }.awaitAll()
                }
                for (p in parts) when (p) {
                    is MonthFetch.Ok -> all.addAll(p.days)
                    is MonthFetch.ParseFail -> parseFailed = true
                    is MonthFetch.Network -> {}
                }
                // كل أشهر هذه الدفعة فشلت شبكيًا (لا HTML) ⇒ على الأرجح لا اتصال، نتوقف
                if (parts.all { it is MonthFetch.Network }) { networkFailed = true; break }
            }
            YearFetch(all, parseFailed, networkFailed && all.isEmpty())
        }

    /**
     * يفتح الاتصال. الوضع الافتراضي يفحص الشهادة بالكامل (آمن).
     * عند insecure=true فقط نتخطى الفحص لموقع mara.gov.om (نفس curl -k)،
     * ولا نلجأ له إلا لو فشل الاتصال الآمن بسبب شهادة الموقع الحكومي.
     */
    private fun open(url: String, insecure: Boolean = false): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        if (insecure && conn is HttpsURLConnection) {
            conn.sslSocketFactory = sslFactory
            conn.hostnameVerifier = HostnameVerifier { host, _ -> host == "mara.gov.om" || host == "www.mara.gov.om" }
        }
        return conn
    }

    private fun postOnce(url: String, form: String, insecure: Boolean): String? {
        val conn = open(url, insecure)
        conn.requestMethod = "POST"
        conn.connectTimeout = 8000
        conn.readTimeout = 15000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.outputStream.use { it.write(form.toByteArray()) }
        return if (conn.responseCode in 200..299)
            conn.inputStream.bufferedReader().use { it.readText() }
        else null
    }

    /** يجرّب الاتصال الآمن أولًا، ولا يتخطى فحص الشهادة إلا لو فشل بسببها تحديدًا. */
    private fun httpPost(url: String, form: String): String? {
        return try {
            postOnce(url, form, insecure = false)
        } catch (e: SSLException) {
            try { postOnce(url, form, insecure = true) } catch (e2: Exception) { null }
        } catch (e: Exception) { null }
    }
}

