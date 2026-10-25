package com.oman.prayertimes.data

import android.content.Context

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        /** مدد الصامت الافتراضية بعد كل صلاة (دقائق) */
        val DND_DEFAULTS = mapOf(
            "fajr" to 45, "dhuhr" to 35, "asr" to 35, "maghrib" to 25, "isha" to 35
        )

        /** خيارات مدة التنبيه قبل الأذان (دقائق) */
        val ALERT_LEAD_OPTIONS = listOf(5, 10, 15, 20, 30)
    }

    var cityId: Int
        get() = sp.getInt("city_id", Cities.defaultCityId)
        set(v) { sp.edit().putInt("city_id", v).apply() }

    // ── التنبيه قبل الصلاة (إشعار + صوت) ──

    var alertsEnabled: Boolean
        get() = sp.getBoolean("alerts_enabled", true)
        set(v) { sp.edit().putBoolean("alerts_enabled", v).apply() }

    /** تفعيل التنبيه لكل صلاة على حدة */
    fun alertEnabled(key: String): Boolean = sp.getBoolean("alert_on_$key", true)
    fun setAlertEnabled(key: String, enabled: Boolean) {
        sp.edit().putBoolean("alert_on_$key", enabled).apply()
    }

    /** كم دقيقة قبل أذان الفجر */
    var alertLeadFajr: Int
        get() = sp.getInt("alert_lead_fajr", 5)
        set(v) { sp.edit().putInt("alert_lead_fajr", v).apply() }

    /** كم دقيقة قبل أذان باقي الصلوات */
    var alertLeadOther: Int
        get() = sp.getInt("alert_lead_other", 10)
        set(v) { sp.edit().putInt("alert_lead_other", v).apply() }

    fun alertLead(key: String): Int = if (key == "fajr") alertLeadFajr else alertLeadOther

    // ── الصامت أثناء الصلاة ──

    var dndEnabled: Boolean
        get() = sp.getBoolean("dnd_enabled", true)
        set(v) { sp.edit().putBoolean("dnd_enabled", v).apply() }

    fun dndMinutes(key: String): Int = sp.getInt("dnd_min_$key", DND_DEFAULTS[key] ?: 30)
    fun setDndMinutes(key: String, minutes: Int) { sp.edit().putInt("dnd_min_$key", minutes).apply() }

    var dndFridayMinutes: Int
        get() = sp.getInt("dnd_friday_min", 60)
        set(v) { sp.edit().putInt("dnd_friday_min", v).apply() }

    // ── التذكير بالذكر ──

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
