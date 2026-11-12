package com.oman.prayertimes.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * بيانات الأذكار (حصن المسلم): أبواب وأذكار محمّلة من assets/hisn_athkar.json.
 * النصوص أدعية من القرآن والسنّة، تُعرض بتصميم التطبيق الخاص.
 */
data class AthkarCategory(
    val id: Int,
    val name: String,
    @SerializedName("nameShort") val nameShort: String,
    val weight: Int
)

data class Zekr(
    val id: Int,
    @SerializedName("cat") val categoryId: Int,
    val text: String,
    val short: String,
    val ref: String,
    val count: Int
)

private data class AthkarFile(
    val categories: List<AthkarCategory> = emptyList(),
    val athkar: List<Zekr> = emptyList()
)

object AthkarData {
    @Volatile private var categoriesCache: List<AthkarCategory>? = null
    @Volatile private var byCategory: Map<Int, List<Zekr>>? = null

    private fun ensureLoaded(context: Context) {
        if (categoriesCache != null) return
        synchronized(this) {
            if (categoriesCache != null) return
            val json = context.assets.open("hisn_athkar.json")
                .bufferedReader().use { it.readText() }
            val file = Gson().fromJson(json, AthkarFile::class.java)
            byCategory = file.athkar.groupBy { it.categoryId }
            categoriesCache = file.categories
        }
    }

    fun categories(context: Context): List<AthkarCategory> {
        ensureLoaded(context)
        return categoriesCache.orEmpty()
    }

    fun athkarOf(context: Context, categoryId: Int): List<Zekr> {
        ensureLoaded(context)
        return byCategory?.get(categoryId).orEmpty()
    }
}
