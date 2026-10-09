package com.oman.prayertimes.data

import android.content.Context

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var cityId: Int
        get() = sp.getInt("city_id", Cities.defaultCityId)
        set(v) { sp.edit().putInt("city_id", v).apply() }

    var alertsEnabled: Boolean
        get() = sp.getBoolean("alerts_enabled", true)
        set(v) { sp.edit().putBoolean("alerts_enabled", v).apply() }

    var dndEnabled: Boolean
        get() = sp.getBoolean("dnd_enabled", true)
        set(v) { sp.edit().putBoolean("dnd_enabled", v).apply() }

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
