package com.oman.prayertimes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerHtmlParserTest {

    private fun sample(): String =
        javaClass.classLoader!!.getResourceAsStream("mara_month_sample.html")!!
            .bufferedReader().use { it.readText() }

    @Test
    fun parse_extractsEveryValidDayRow() {
        val days = PrayerHtmlParser.parse(sample())
        // 4 صفوف صالحة؛ الترويسة والصف الناقص (5 أوقات) يُتجاهلان
        assertEquals(4, days.size)
    }

    @Test
    fun parse_readsTimesInOrder_forTypicalRow() {
        val first = PrayerHtmlParser.parse(sample()).first()
        assertEquals("01/01/2026", first.date)
        assertEquals("05:12", first.fajr)
        assertEquals("06:34", first.sunrise)
        assertEquals("12:03", first.dhuhr)
        assertEquals("15:12", first.asr)
        assertEquals("17:28", first.maghrib)
        assertEquals("18:46", first.isha)
    }

    @Test
    fun parse_toleratesMissingFontAndExtraWhitespace() {
        val row = PrayerHtmlParser.parse(sample()).first { it.date == "03/01/2026" }
        assertEquals("05:13", row.fajr)
        assertEquals("18:48", row.isha)
    }

    @Test
    fun parse_skipsLeadingColumnsBeforeDate() {
        // الصف الذي يسبقه عمود اسم المدينة يجب أن يُقرأ بالتاريخ الصحيح
        val row = PrayerHtmlParser.parse(sample()).first { it.date == "04/01/2026" }
        assertEquals("05:13", row.fajr)
        assertEquals("18:49", row.isha)
    }

    @Test
    fun parse_ignoresRowWithFewerThanSixTimes() {
        val dates = PrayerHtmlParser.parse(sample()).map { it.date }
        assertTrue("الصف الناقص يجب ألا يظهر", "05/01/2026" !in dates)
    }

    @Test
    fun parseFirst_returnsFirstDay() {
        assertEquals("01/01/2026", PrayerHtmlParser.parseFirst(sample())?.date)
    }

    @Test
    fun parse_returnsEmpty_whenStructureHasNoTable() {
        // لو غيّرت الوزارة الهيكل جذريًا (بلا جداول)، نرجع قائمة فارغة
        // لتُترجَم لاحقًا إلى PARSE_FAILED بدل بيانات خاطئة.
        val changed = "<html><body><div>لا يوجد جدول مواقيت هنا</div></body></html>"
        assertTrue(PrayerHtmlParser.parse(changed).isEmpty())
    }

    @Test
    fun parse_sampleResourceExists() {
        assertNotNull(sample())
    }
}
