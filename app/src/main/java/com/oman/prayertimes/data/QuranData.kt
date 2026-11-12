package com.oman.prayertimes.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** سورة: المعرّف، الاسم، النقحرة، النوع (مكية/مدنية)، عدد الآيات، وهل لها بسملة. */
data class Surah(
    val id: Int,
    val name: String,
    val tr: String,
    val type: String,
    val verses: Int,
    val bism: Boolean
) {
    val typeArabic: String get() = if (type == "meccan") "مكية" else "مدنية"
}

private data class QuranFile(
    val basmala: String = "",
    val surahs: List<Surah> = emptyList(),
    val verses: Map<String, List<String>> = emptyMap(),
    // صفحات المصحف المدني (604): كل صفحة قائمة [رقم السورة، رقم الآية]
    val pages: List<List<List<Int>>> = emptyList()
)

/** نص القرآن العثماني (Tanzil، رواية حفص) محمّلًا من assets/quran.json. */
object QuranData {
    @Volatile private var file: QuranFile? = null

    private fun load(context: Context): QuranFile {
        file?.let { return it }
        synchronized(this) {
            file?.let { return it }
            val json = context.assets.open("quran.json")
                .bufferedReader().use { it.readText() }
            val parsed = Gson().fromJson(json, QuranFile::class.java)
            file = parsed
            return parsed
        }
    }

    fun surahs(context: Context): List<Surah> = load(context).surahs
    fun versesOf(context: Context, surahId: Int): List<String> =
        load(context).verses[surahId.toString()].orEmpty()
    fun basmala(context: Context): String = load(context).basmala

    /** صفحات المصحف: pages[i] = قائمة [سورة، آية] المرتبة لتلك الصفحة. */
    fun pages(context: Context): List<List<List<Int>>> = load(context).pages

    /** نص آية بعينها. */
    fun verseText(context: Context, surahId: Int, ayah: Int): String =
        load(context).verses[surahId.toString()]?.getOrNull(ayah - 1).orEmpty()

    /** رقم صفحة (الفهرس من 0) التي تبدأ عندها السورة. */
    fun firstPageOf(context: Context, surahId: Int): Int {
        val p = load(context).pages
        for (i in p.indices) {
            if (p[i].any { it[0] == surahId && it[1] == 1 }) return i
        }
        return 0
    }
}

/** التفسير الميسّر: يُحمّل ملف السورة عند الحاجة فقط (assets/tafsir/<سورة>.json). */
object TafsirData {
    private val cache = HashMap<Int, List<String>>()

    fun of(context: Context, surahId: Int, ayah: Int): String {
        val list = synchronized(cache) {
            cache.getOrPut(surahId) {
                try {
                    val json = context.assets.open("tafsir/$surahId.json")
                        .bufferedReader().use { it.readText() }
                    Gson().fromJson(json, Array<String>::class.java).toList()
                } catch (e: Exception) { emptyList() }
            }
        }
        return list.getOrNull(ayah - 1).orEmpty()
    }
}

/**
 * علامات الصفحات (شريط على حافة الصفحة)، محفوظة بين الجلسات.
 * الحالة: 0 بلا علامة، 1 مراجعة (أحمر)، 2 حفظ (أخضر).
 * الصفحة برقمها من 1 إلى 604.
 */
class QuranMarks(context: Context) {
    companion object {
        const val NONE = 0
        const val REVIEW = 1    // أحمر — مراجعة
        const val MEMORIZED = 2 // أخضر — حفظ
    }

    private val prefs = context.getSharedPreferences("quran_page_marks", Context.MODE_PRIVATE)
    private fun key(page: Int) = "p$page"

    fun get(page: Int): Int = prefs.getInt(key(page), NONE)

    fun set(page: Int, state: Int) {
        if (state == NONE) prefs.edit().remove(key(page)).apply()
        else prefs.edit().putInt(key(page), state).apply()
    }

    /** كل الصفحات المعلَّمة مرتبة: (رقم الصفحة، الحالة). */
    fun bookmarks(): List<Pair<Int, Int>> =
        prefs.all.mapNotNull { (k, v) ->
            if (k.startsWith("p") && v is Int) k.substring(1).toIntOrNull()?.let { it to v } else null
        }.sortedBy { it.first }
}
