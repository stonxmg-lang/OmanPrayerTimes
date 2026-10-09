package com.oman.prayertimes.notif

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

object DndController {
    private const val PREV_FILTER = "dnd_prev_filter"

    fun hasAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun requestAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun enter(context: Context) {
        if (!hasAccess(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            val sp = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            // احفظ الحالة السابقة حرفيًا لإعادتها لاحقًا
            sp.edit().putInt(PREV_FILTER, nm.currentInterruptionFilter).apply()
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun exit(context: Context) {
        if (!hasAccess(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            val sp = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val prev = sp.getInt(PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
            nm.setInterruptionFilter(prev)
            sp.edit().remove(PREV_FILTER).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
