package com.oman.prayertimes.data

import com.oman.prayertimes.model.DayTimes
import org.jsoup.Jsoup

/**
 * يقرأ مواقيت الصلاة من صفحة وزارة الأوقاف (mara.gov.om).
 *
 * بدل مطابقة شكل الـ HTML حرفيًا بـ regex، يحلّل الصفحة كجدول حقيقي عبر Jsoup
 * ثم يبحث في كل صف عن تاريخ يتبعه ستة أوقات مباشرة. هذا يتحمّل تغيّر خصائص
 * الوسوم، والمسافات، ووجود <font> أو عدمه، وإضافة أعمدة قبل عمود التاريخ.
 * يبقى يتكسّر فقط لو غيّرت الوزارة هيكل الجدول نفسه جذريًا.
 */
object PrayerHtmlParser {

    private val DATE = Regex("""\d{1,2}/\d{1,2}/\d{2,4}""")
    private val TIME = Regex("""\d{1,2}:\d{2}""")

    /** كل الأيام الموجودة في الصفحة (قد تكون شهرًا كاملًا أو يومًا واحدًا). */
    fun parse(html: String): List<DayTimes> {
        val doc = Jsoup.parse(html)
        val out = ArrayList<DayTimes>()
        for (row in doc.select("tr")) {
            val cells = row.select("td").map { it.text().trim() }
            extractRow(cells)?.let { out.add(it) }
        }
        return out
    }

    /** أول يوم في الصفحة، أو null إن لم يوجد. (لجلب يوم واحد) */
    fun parseFirst(html: String): DayTimes? = parse(html).firstOrNull()

    /** يبحث عن أول تاريخ في الصف يتبعه 6 أوقات متتالية. */
    private fun extractRow(cells: List<String>): DayTimes? {
        for (i in cells.indices) {
            if (!DATE.matches(cells[i])) continue
            val times = ArrayList<String>(6)
            var j = i + 1
            while (j < cells.size && times.size < 6 && TIME.matches(cells[j])) {
                times.add(cells[j]); j++
            }
            if (times.size == 6) {
                return DayTimes(cells[i], times[0], times[1], times[2], times[3], times[4], times[5])
            }
        }
        return null
    }
}
