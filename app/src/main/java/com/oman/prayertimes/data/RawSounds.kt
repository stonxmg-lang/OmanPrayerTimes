package com.oman.prayertimes.data

import android.content.Context
import java.util.Locale

/**
 * اكتشاف ديناميكي لملفات الصوت في res/raw بالانعكاس على حقول R.raw.
 *
 * الاصطلاحات المتوقعة:
 *  - المنبهات:  alarm_01 ... (بلا حد أقصى، alarm_%02d بأرقام لاتينية دائمًا)
 *  - التنبيه:   alert_fajr ... alert_isha
 *  - التسبيح:   tasbih_01 ... tasbih_10
 *  - الأذان الافتراضي: adhan_fajr / adhan_dhuhr / adhan_asr / adhan_maghrib / adhan_isha
 *  - أصوات الأذان البديلة:
 *      صوت الفجر البديل:   adhan_fajr_<name>
 *      صوت باقي الصلوات:  adhan_<name>
 *    اسم الصوت المعروض = اسم الملف نفسه (بلا سابقة، والشرطات السفلية مسافات).
 *
 * أي ملف ينقص يُتجاوَز تلقائيًا ويُستخدم بديل النظام عند التشغيل.
 */
object RawSounds {

    data class Voice(val id: String, val display: String)

    private val BASE_ADHAN = setOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    private fun resId(context: Context, name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    fun exists(context: Context, name: String): Boolean = resId(context, name) != 0

    /** كل أسماء موارد raw (للانعكاس) */
    private fun rawNames(context: Context): List<String> = try {
        Class.forName("${context.packageName}.R\$raw")
            .fields
            .map { it.name }
    } catch (_: Exception) {
        emptyList()
    }

    // ── المنبهات ────────────────────────────────────────────────

    fun alarmNames(context: Context): List<String> =
        (1..99).map { String.format(Locale.US, "alarm_%02d", it) }.filter { exists(context, it) }

    fun alarmDisplay(rawName: String): String =
        rawName.removePrefix("alarm_").toIntOrNull()?.let { "منبه $it" } ?: rawName

    // ── أصوات الأذان (قائمتان مستقلتان، بلا حد أقصى) ─────────────

    /** أصوات الفجر البديلة: من adhan_fajr_<name> — id فارغ = الافتراضي */
    fun fajrVoices(context: Context): List<Voice> {
        val out = LinkedHashMap<String, String>()
        if (exists(context, "adhan_fajr")) out[""] = "الصوت الافتراضي"
        for (n in rawNames(context)) {
            if (n.startsWith("adhan_fajr_")) {
                val id = n.removePrefix("adhan_fajr_")
                out[id] = id.replace('_', ' ')
            }
        }
        if (out.isEmpty()) out[""] = "الصوت الافتراضي"
        return out.map { Voice(it.key, it.value) }
    }

    /** أصوات الظهر/العصر/المغرب/العشاء البديلة: من adhan_<name> — id فارغ = الافتراضي */
    fun regularVoices(context: Context): List<Voice> {
        val out = LinkedHashMap<String, String>()
        if (BASE_ADHAN.all { exists(context, "adhan_$it") }) out[""] = "الصوت الافتراضي"
        for (n in rawNames(context)) {
            if (!n.startsWith("adhan_")) continue
            val rest = n.removePrefix("adhan_")
            if (rest in BASE_ADHAN) continue          // adhan_fajr ... (الافتراضي)
            if (rest.startsWith("fajr_")) continue     // أصوات الفجر لها قائمتها
            if (rest.isEmpty()) continue
            out[rest] = rest.replace('_', ' ')
        }
        if (out.isEmpty()) out[""] = "الصوت الافتراضي"
        return out.map { Voice(it.key, it.value) }
    }

    /**
     * يحلّ اسم ملف الأذان الفعلي لصلاة معيّنة حسب الإعدادين المستقلين:
     * settings.fajrAdhanVoice للفجر، settings.adhanVoice لباقي الصلوات.
     * يعود null لو لا ملف متاحًا إطلاقًا (فيلغى جدولة الأذان لتلك الصلاة).
     */
    fun resolveAdhan(context: Context, prayerKey: String, settings: SettingsStore): String? {
        val voice = if (prayerKey == "fajr") settings.fajrAdhanVoice else settings.adhanVoice
        val chosen = if (voice.isEmpty()) "adhan_$prayerKey"
        else if (prayerKey == "fajr") "adhan_fajr_$voice" else "adhan_$voice"
        if (exists(context, chosen)) return chosen
        return if (exists(context, "adhan_$prayerKey")) "adhan_$prayerKey" else null
    }

    // ── تنبيهات وتسبيحات ────────────────────────────────────────

    fun tasbihCount(context: Context): Int =
        (1..10).count { exists(context, String.format(Locale.US, "tasbih_%02d", it)) }
}
