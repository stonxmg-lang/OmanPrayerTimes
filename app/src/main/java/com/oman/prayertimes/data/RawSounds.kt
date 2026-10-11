package com.oman.prayertimes.data

import android.content.Context

/**
 * اكتشاف ديناميكي لملفات الصوت في res/raw بالأسماء المتفق عليها:
 *  - منبهات:  alarm_01 ... alarm_20
 *  - أذان:     adhan_fajr / adhan_dhuhr / ... (الصوت الافتراضي)
 *              أو مجموعات كاملة بصيغة adhan_fajr_makkah ... (صوت مؤذن إضافي)
 *  - تنبيه:    alert_fajr ... alert_isha
 *  - تسبيح:    tasbih_01 ... tasbih_10
 * أي ملف ينقص يُتجاوَز تلقائيًا ويُستخدم بديل النظام عند التشغيل.
 */
object RawSounds {

    data class Voice(val id: String, val display: String)

    private val ADHAN_PRAYERS = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    private fun resId(context: Context, name: String): Int =
        context.resources.getIdentifier(name, "raw", context.packageName)

    fun exists(context: Context, name: String): Boolean = resId(context, name) != 0

    /** أسماء ملفات المنبهات الموجودة فعليًا، مرتبة */
    fun alarmNames(context: Context): List<String> =
        (1..20).map { "alarm_%02d".format(it) }.filter { exists(context, it) }

    fun alarmDisplay(rawName: String): String {
        val n = rawName.removePrefix("alarm_").toIntOrNull()
        return if (n != null) "منبه $n" else rawName
    }

    /**
     * أصوات الأذان المتاحة. id فارغ = الصوت الافتراضي (adhan_fajr وغيرها).
     * يُقبل الصوت البديل فقط إذا وُجد ملف لكل الصلوات الخمس بنفس اللاحقة.
     */
    fun adhanVoices(context: Context): List<Voice> {
        val voices = LinkedHashMap<String, String>()
        if (ADHAN_PRAYERS.all { exists(context, "adhan_$it") })
            voices[""] = "الصوت الافتراضي"
        // اللواحق المعروفة شائعًا + أي لاحقة مخصصة يضيفها المستخدم بنفس النمط
        val candidates = listOf("makkah", "madinah", "aqsa", "egypt", "qatar", "nasser",
            "turki", "ali_ahmed", "abdulbasit") +
                (1..9).map { "v$it" }
        for (suffix in candidates) {
            if (suffix !in voices && ADHAN_PRAYERS.all { exists(context, "adhan_${'$'}it_${'$'}suffix") })
                voices[suffix] = friendlyVoice(suffix)
        }
        if (voices.isEmpty()) voices[""] = "الصوت الافتراضي"
        return voices.map { Voice(it.key, it.value) }
    }

    fun adhanSoundFor(prayerKey: String, voiceId: String): String =
        if (voiceId.isEmpty()) "adhan_$prayerKey" else "adhan_${'$'}{prayerKey}_$voiceId"

    fun alertExists(context: Context, prayerKey: String): Boolean = exists(context, "alert_$prayerKey")

    fun tasbihCount(context: Context): Int =
        (1..10).count { exists(context, "tasbih_%02d".format(it)) }

    private fun friendlyVoice(suffix: String): String = when (suffix.lowercase()) {
        "makkah" -> "الحرم المكي"
        "madinah" -> "الحرم النبوي"
        "aqsa" -> "المسجد الأقصى"
        "egypt" -> "الأذان المصري"
        "qatar" -> "أذان قطر"
        "nasser" -> "ناصر القطامي"
        "turki" -> "عبدالمحسن القاسم"
        "ali_ahmed" -> "علي أحمد ملا"
        "abdulbasit" -> "عبد الباسط عبد الصمد"
        else -> "صوت " + suffix.replace("_", " ")
    }
}
