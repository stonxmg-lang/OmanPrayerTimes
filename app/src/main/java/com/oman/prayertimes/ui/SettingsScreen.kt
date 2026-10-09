package com.oman.prayertimes.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oman.prayertimes.data.Cities
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.notif.DndController
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {

        Text("المدينة", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        CityPicker(settings)

        Spacer(Modifier.height(24.dp))

        Text("الصلاحيات", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        PermissionsSection(context)

        Spacer(Modifier.height(24.dp))

        Text("تنبيهات الصلاة", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        var alerts by remember { mutableStateOf(settings.alertsEnabled) }
        SwitchRow("تنبيه قبل الصلاة (الفجر 5د / باقي الصلوات 10د)", alerts) {
            alerts = it; settings.alertsEnabled = it
        }
        var dnd by remember { mutableStateOf(settings.dndEnabled) }
        SwitchRow("الصامت تلقائيًا أثناء الصلاة (45/35/35/25/35 دقيقة)", dnd) {
            dnd = it; settings.dndEnabled = it
        }

        Spacer(Modifier.height(24.dp))

        Text("منبه الفجر", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        FajrAlarmSection(settings)

        Spacer(Modifier.height(24.dp))

        Text("التذكير بالصلاة على النبي ﷺ", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        TasbihSection(settings)

        Spacer(Modifier.height(32.dp))
        Text("التطبيق يعمل دون صلاحيات اختيارية، لكنها تفعّل الميزات الكاملة",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPicker(settings: SettingsStore) {
    var expanded by remember { mutableStateOf(false) }
    val cityName = Cities.nameOf(settings.cityId)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = cityName,
            onValueChange = {},
            readOnly = true,
            label = { Text("اختر المدينة") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Cities.list.forEach { (name, id) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { settings.cityId = id; expanded = false }
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    AutoLocationButton(settings)
}

@Composable
private fun AutoLocationButton(settings: SettingsStore) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = try {
                @Suppress("MissingPermission")
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (e: Exception) { null }
            if (loc != null) {
                // تحويل الإحداثيات إلى أقرب مدينة بالاسم
                try {
                    val geocoder = Geocoder(context, Locale("ar"))
                    val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    val locality = addresses?.firstOrNull()?.locality
                        ?: addresses?.firstOrNull()?.subAdminArea ?: ""
                    val match = Cities.list.firstOrNull { (name, _) ->
                        locality.contains(name) || name.contains(locality)
                    }
                    if (match != null) {
                        settings.cityId = match.second
                        status = "تم التحديد تلقائيًا: ${match.first}"
                    } else status = "لم نتعرف على المدينة - اختر يدويًا"
                } catch (e: Exception) { status = "تعذر التحديد - اختر يدويًا" }
            } else status = "الموقع غير متاح حاليًا - اختر يدويًا"
        } else status = "تم رفض الإذن - الاختيار اليدوي متاح"
    }
    OutlinedButton(
        onClick = { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("📍 تحديد تلقائي بالموقع الجغرافي") }
    if (status.isNotEmpty()) {
        Text(status, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PermissionsSection(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // إذن الإشعارات
    val notifGranted = if (Build.VERSION.SDK_INT >= 33)
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    else true
    PermissionRow("الإشعارات", notifGranted) {
        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        })
    }

    // إذن المنبه الدقيق
    val exactGranted = if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
    PermissionRow("المنبه الدقيق (لتنبيهات في وقتها)", exactGranted) {
        if (Build.VERSION.SDK_INT >= 31)
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }

    // إذن DND
    val dndGranted = nm.isNotificationPolicyAccessGranted
    PermissionRow("وضع عدم الإزعاج (الصامت أثناء الصلاة)", dndGranted) {
        context.startActivity(DndController.requestAccessIntent())
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (granted) "✅" else "⚠️", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(if (granted) "مفعّل" else "اضغط للتفعيل",
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FajrAlarmSection(settings: SettingsStore) {
    var enabled by remember { mutableStateOf(settings.fajrAlarmEnabled) }
    var minutes by remember { mutableStateOf(settings.fajrAlarmMinutes) }
    var sound by remember { mutableStateOf(settings.fajrAlarmSound) }
    var repeat by remember { mutableStateOf(settings.fajrAlarmRepeat) }

    SwitchRow("تفعيل منبه الفجر", enabled) { enabled = it; settings.fajrAlarmEnabled = it }
    if (enabled) {
        Text("قبل الأذان بـ $minutes دقيقة", modifier = Modifier.padding(vertical = 4.dp))
        Slider(
            value = minutes.toFloat(), onValueChange = { minutes = it.toInt(); settings.fajrAlarmMinutes = minutes },
            valueRange = 5f..60f, steps = 10
        )
        Text("ملف الصوت: $sound", modifier = Modifier.padding(vertical = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..5) {
                val name = "alarm_%02d".format(i)
                FilterChip(
                    selected = sound == name,
                    onClick = { sound = name; settings.fajrAlarmSound = name },
                    label = { Text("منبه $i") }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("عدد التكرار: $repeat", modifier = Modifier.padding(vertical = 4.dp))
        Slider(
            value = repeat.toFloat(), onValueChange = { repeat = it.toInt(); settings.fajrAlarmRepeat = repeat },
            valueRange = 1f..5f, steps = 3
        )
        Text("بدون غفوة - مدة الرنين = مدة الملف الصوتي",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TasbihSection(settings: SettingsStore) {
    var enabled by remember { mutableStateOf(settings.tasbihEnabled) }
    var count by remember { mutableStateOf(settings.tasbihCount) }
    var sound by remember { mutableStateOf(settings.tasbihSound) }

    SwitchRow("تفعيل التذكيرات (موزعة بين الفجر والعشاء)", enabled) {
        enabled = it; settings.tasbihEnabled = it
    }
    if (enabled) {
        Text("عدد التذكيرات اليومية: $count (بحد أقصى 10)",
            modifier = Modifier.padding(vertical = 4.dp))
        Slider(
            value = count.toFloat(), onValueChange = { count = it.toInt(); settings.tasbihCount = count },
            valueRange = 2f..10f, steps = 7
        )
        SwitchRow("تشغيل صوت مع التذكير", sound) { sound = it; settings.tasbihSound = it }
        Spacer(Modifier.height(8.dp))
        Text("الصيغ المتاحة:", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold)
        TasbihTexts.list.forEachIndexed { i, text ->
            var itemEnabled by remember { mutableStateOf(settings.tasbihTextEnabled(i)) }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = itemEnabled, onCheckedChange = {
                    itemEnabled = it; settings.setTasbihTextEnabled(i, it)
                })
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
