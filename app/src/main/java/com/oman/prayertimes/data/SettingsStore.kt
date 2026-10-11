package com.oman.prayertimes.data

import android.content.Context

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        /** مدد الصامت الافتراضية بعد كل صلاة (دقائق) */
        val DND_DEFAULTS = mapOf(
            "fajr" to 45, "dhuhr" to 35, "asr" to 35, "maghrib" to 25, "isha" to 35
        )
    }

    var cityId: Int
        get() = sp.getInt("city_id", Cities.defaultCityId)
        set(v) { sp.edit().putInt("city_id", v).apply() }

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(v) { sp.edit().putBoolean("onboarding_done", v).apply() }

    var alertsEnabled: Boolean
        get() = sp.getBoolean("alerts_enabled", true)
        set(v) { sp.edit().putBoolean("alerts_enabled", v).apply() }

    var dndEnabled: Boolean
        get() = sp.getBoolean("dnd_enabled", true)
        set(v) { sp.edit().putBoolean("dnd_enabled", v).apply() }

    /** مدة بقاء الصامت بعد صلاة معيّنة بالدقائق */
    fun dndMinutes(key: String): Int = sp.getInt("dnd_min_$key", DND_DEFAULTS[key] ?: 30)
    fun setDndMinutes(key: String, minutes: Int) { sp.edit().putInt("dnd_min_$key", minutes).apply() }

    /** مدة الصامت بعد صلاة الظهر يوم الجمعة */
    var dndFridayMinutes: Int
        get() = sp.getInt("dnd_friday_min", 60)
        set(v) { sp.edit().putInt("dnd_friday_min", v).apply() }

    // ── منبه الفجر ──────────────────────────────────────────────
    var fajrAlarmEnabled: Boolean
        get() = sp.getBoolean("fajr_alarm_enabled", false)
        set(v) { sp.edit().putBoolean("fajr_alarm_enabled", v).apply() }

    var fajrAlarmMinutes: Int
        get() = sp.getInt("fajr_alarm_minutes", 30)
        set(v) { sp.edit().putInt("fajr_alarm_minutes", v).apply() }

    var fajrAlarmSound: String
        get() = sp.getString("fajr_alarm_sound", "alarm_01") ?: "alarm_01"
        set(v) { sp.edit().putString("fajr_alarm_sound", v).apply() }

    var fajrAlarmRepeat: Int
        get() = sp.getInt("fajr_alarm_repeat", 1)
        set(v) { sp.edit().putInt("fajr_alarm_repeat", v).apply() }

    // ── الأذان ──────────────────────────────────────────────────
    fun adhanEnabled(key: String): Boolean = sp.getBoolean("adhan_on_$key", true)
    fun setAdhanEnabled(key: String, enabled: Boolean) {
        sp.edit().putBoolean("adhan_on_$key", enabled).apply()
    }

    /** لاحقة صوت المؤذن المختار (فارغ = الصوت الافتراضي) */
    var adhanVoice: String
        get() = sp.getString("adhan_voice", "") ?: ""
        set(v) { sp.edit().putString("adhan_voice", v).apply() }

    /** مستوى صوت الأذان والتنبيهات والتسبيحات (0..100) */
    var adhanVolume: Int
        get() = sp.getInt("adhan_volume", 100)
        set(v) { sp.edit().putInt("adhan_volume", v).apply() }

    // ── التسبيحات ───────────────────────────────────────────────
    var tasbihEnabled: Boolean
        get() = sp.getBoolean("tasbih_enabled", false)
        set(v) { sp.edit().putBoolean("tasbih_enabled", v).apply() }

    var tasbihCount: Int
        get() = sp.getInt("tasbih_count", 6)
        set(v) { sp.edit().putInt("tasbih_count", v).apply() }

    var tasbihSound: Boolean
        get() = sp.getBoolean("tasbih_sound", true)
        set(v) { sp.edit().putBoolean("tasbih_sound", v).apply() }

    fun tasbihTextEnabled(index: Int): Boolean =
        sp.getBoolean("tasbih_text_$index", true)

    fun setTasbihTextEnabled(index: Int, enabled: Boolean) {
        sp.edit().putBoolean("tasbih_text_$index", enabled).apply()
    }
}
