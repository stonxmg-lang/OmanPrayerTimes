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
    val verses: Map<String, List<String>> = emptyMap()
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
}

/**
 * علامات الحفظ والمراجعة لكل آية، محفوظة بين الجلسات.
 * الحالة: 0 بلا علامة، 1 مراجعة (أحمر)، 2 حفظ (أخضر).
 */
class QuranMarks(context: Context) {
    companion object {
        const val NONE = 0
        const val REVIEW = 1   // أحمر
        const val MEMORIZED = 2 // أخضر
    }

    private val prefs = context.getSharedPreferences("quran_marks", Context.MODE_PRIVATE)
    private fun key(surah: Int, ayah: Int) = "$surah:$ayah"

    fun get(surah: Int, ayah: Int): Int = prefs.getInt(key(surah, ayah), NONE)

    /** يدوّر العلامة: بلا ← مراجعة ← حفظ ← بلا. يرجع الحالة الجديدة. */
    fun cycle(surah: Int, ayah: Int): Int {
        val next = (get(surah, ayah) + 1) % 3
        val k = key(surah, ayah)
        if (next == NONE) prefs.edit().remove(k).apply()
        else prefs.edit().putInt(k, next).apply()
        return next
    }

    /** عدد آيات السورة المحفوظة (علامة خضراء). */
    fun memorizedCount(surahId: Int, verseCount: Int): Int {
        var n = 0
        for (a in 1..verseCount) if (get(surahId, a) == MEMORIZED) n++
        return n
    }
}
