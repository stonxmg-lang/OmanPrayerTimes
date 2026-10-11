package com.oman.prayertimes

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

object PermissionUtils {
    fun isNotificationPermissionRequired(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasNotificationPermission(context: Context): Boolean =
        !isNotificationPermissionRequired() ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    fun requestNotificationPermission(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<String>,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (!isNotificationPermissionRequired()) {
            onResult(true)
            return
        }
        if (hasNotificationPermission(activity)) {
            onResult(true)
            return
        }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
