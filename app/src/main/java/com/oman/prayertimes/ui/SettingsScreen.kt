package com.oman.prayertimes.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.oman.prayertimes.data.Cities
import com.oman.prayertimes.data.CityCoords
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.RawSounds
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.notif.AudioPlayer
import com.oman.prayertimes.notif.DndController
import com.oman.prayertimes.notif.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SettingsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CityCard()
        PermissionsCard()
        AlertsCard()
        AdhanCard()
        FajrAlarmCard()
        DndCard()
        TasbihCard()
        Text(
            "يعمل التطبيق دون الصلاحيات الاختيارية، لكنها تفعّل الميزات كاملة",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
}

// ── أدوات مشتركة ─────────────────────────────────────────────

@Composable
private fun SectionCard(
    icon: ImageVector, title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun OnResumeEffect(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) onResume()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

private fun reschedule(context: Context, scope: CoroutineScope) {
    scope.launch(Dispatchers.IO) { AlarmScheduler.rescheduleToday(context) }
}

@Composable
private fun DropdownSelector(value: Int, options: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("$value دقيقة", color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text("$o دقيقة") },
                    onClick = { onSelect(o); expanded = false })
            }
        }
    }
}

// ── المدينة ──────────────────────────────────────────────────

@Composable
private fun CityCard() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var cityName by remember { mutableStateOf(Cities.nameOf(settings.cityId)) }
    var syncMsg by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    OnResumeEffect { cityName = Cities.nameOf(settings.cityId) }

    fun applyCity(name: String) {
        syncMsg = "جاري تحديث مواقيت $name…"
        scope.launch(Dispatchers.IO) {
            val ok = try { PrayerRepository(context).applyCityChange() } catch (e: Exception) { false }
            syncMsg = if (ok) "تم تحديث المواقيت والتنبيهات لمدينة $name ✅"
            else "تعذّر تحميل مواقيت $name - تحقق من الإنترنت"
        }
    }

    SectionCard(Icons.Default.LocationOn, "المدينة") {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showPicker = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("المدينة الحالية", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(cityName, style = MaterialTheme.typography.titleMedium)
            }
            Text("تغيير", color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(8.dp))
        AutoLocateRow(onLocated = { id, name ->
            settings.cityId = id
            cityName = name
            applyCity(name)
        })
        if (syncMsg.isNotEmpty()) {
            Text(syncMsg, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp))
        }
    }

    if (showPicker) {
        CityPickerDialog(
            currentId = settings.cityId,
            onDismiss = { showPicker = false },
            onSelect = { id, name ->
                settings.cityId = id
                cityName = name
                showPicker = false
                applyCity(name)
            })
    }
}

@Composable
private fun CityPickerDialog(currentId: Int, onDismiss: () -> Unit,
                             onSelect: (Int, String) -> Unit) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر المدينة") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                val q = query.trim()
                val filtered = Cities.list.filter { q.isEmpty() || it.first.contains(q) }
                LazyColumn(Modifier.fillMaxWidth().height(340.dp)) {
                    items(filtered) { (name, id) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(id, name) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = id == currentId, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

/** أقرب مدينة من جدول الإحداثيات الموثوق فقط */
private fun nearestKnownCity(lat: Double, lon: Double): Pair<String, Int>? {
    if (CityCoords.map.isEmpty()) return null
    val best = CityCoords.map.minByOrNull { (_, c) ->
        val r = 6371.0
        val dLat = Math.toRadians(c.first - lat)
        val dLon = Math.toRadians(c.second - lon)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat)) * cos(Math.toRadians(c.first)) * sin(dLon / 2).pow(2.0)
        2 * r * atan2(sqrt(a), sqrt(1 - a))
    } ?: return null
    return Cities.nameOf(best.key) to best.key
}

private fun locateNearestCity(context: Context,
                              onFound: (Int, String) -> Unit,
                              onFail: (String) -> Unit) {
    val known = nearestKnownCity(0.0, 0.0)
    if (known == null) {
        onFail("التحديد التلقائي بانتظار جدول الإحداثيات الموثوق - اختر المدينة يدويًا")
        return
    }
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val handler = Handler(Looper.getMainLooper())
    val done = booleanArrayOf(false)

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (done[0]) return
            done[0] = true
            try { lm.removeUpdates(this) } catch (_: Exception) {}
            handler.removeCallbacksAndMessages(null)
            val near = nearestKnownCity(location.latitude, location.longitude)
            if (near != null) onFound(near.second, near.first)
            else onFail("تعذّر مطابقة الموقع مع المدن المعروفة")
        }
    }

    fun request(provider: String): Boolean = try {
        lm.requestSingleUpdate(provider, listener, Looper.getMainLooper()); true
    } catch (e: Exception) { false }

    val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val netOn = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    if (!gpsOn && !netOn) {
        onFail("خدمة الموقع (GPS) متوقفة - فعّلها ثم أعد المحاولة")
        return
    }
    var requested = false
    if (netOn) requested = request(LocationManager.NETWORK_PROVIDER) || requested
    if (gpsOn) requested = request(LocationManager.GPS_PROVIDER) || requested
    if (!requested) { onFail("تعذّر طلب الموقع"); return }

    handler.postDelayed({
        if (done[0]) return@postDelayed
        done[0] = true
        try { lm.removeUpdates(listener) } catch (_: Exception) {}
        @Suppress("MissingPermission")
        val last = lm.getProviders(true)
            .mapNotNull { try { lm.getLastKnownLocation(it) } catch (e: SecurityException) { null } }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 15 * 60_000L) {
            val near = nearestKnownCity(last.latitude, last.longitude)
            if (near != null) onFound(near.second, near.first)
            else onFail("تعذّر مطابقة الموقع مع المدن المعروفة")
        } else onFail("تعذّر تحديد الموقع - اختر المدينة يدويًا")
    }, 12000)
}

@Composable
private fun AutoLocateRow(onLocated: (Int, String) -> Unit) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            busy = true
            status = "جاري تحديد موقعك…"
            locateNearestCity(context,
                onFound = { id, name -> busy = false; status = ""; onLocated(id, name) },
                onFail = { busy = false; status = it })
        } else status = "تم رفض إذن الموقع - الاختيار اليدوي متاح"
    }
    OutlinedButton(
        onClick = { if (!busy) launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(Icons.Default.LocationOn, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (busy) "جاري التحديد…" else "تحديد تلقائي لأقرب مدينة")
    }
    if (status.isNotEmpty()) {
        Text(status, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    }
}

// ── الصلاحيات ────────────────────────────────────────────────

@Composable
private fun PermissionsCard() {
    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    OnResumeEffect { tick++ }
    val refresh = tick

    SectionCard(Icons.Default.Notifications, "الصلاحيات") {
        if (refresh >= 0) {
            val notifGranted = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            PermissionRow("الإشعارات", notifGranted) {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                })
            }
            val exactGranted = Build.VERSION.SDK_INT < 31 ||
                    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                        .canScheduleExactAlarms()
            PermissionRow("المنبه الدقيق (تنبيهات في وقتها)", exactGranted) {
                if (Build.VERSION.SDK_INT >= 31)
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
            if (Build.VERSION.SDK_INT >= 34) {
                PermissionRow("إشعارات ملء الشاشة (منبه الفجر)",
                    NotificationHelper.canFullScreen(context)) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
                }
            }
            val dndGranted = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .isNotificationPolicyAccessGranted
            PermissionRow("عدم الإزعاج (الصامت أثناء الصلاة)", dndGranted) {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            PermissionRow("البطارية: إيقاف التحسين التلقائي (مهم لأجهزة OPPO وسامسونغ)",
                pm.isIgnoringBatteryOptimizations(context.packageName)) {
                // بدون صلاحية خاصة (متوافق مع Play): نفتح صفحة إعدادات النظام
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(8.dp).background(
                if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(if (granted) "مفعّل" else "تفعيل",
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold)
    }
}

// ── تنبيهات الصلاة ───────────────────────────────────────────

@Composable
private fun AlertsCard() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf(settings.alertsEnabled) }
    SectionCard(Icons.Default.NotificationsActive, "تنبيهات الصلاة") {
        SwitchRow("تنبيه قبل الصلاة (الفجر قبل 5 دقائق، وباقي الصلوات قبل 10)", alerts) {
            alerts = it; settings.alertsEnabled = it; reschedule(context, scope)
        }
    }
}

// ── الأذان ───────────────────────────────────────────────────

@Composable
private fun AdhanCard() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var voice by remember { mutableStateOf(settings.adhanVoice) }
    var fajrVoice by remember { mutableStateOf(settings.fajrAdhanVoice) }
    var vol by remember { mutableStateOf(settings.adhanVolume) }
    var showVoices by remember { mutableStateOf(false) }
    var showFajrVoices by remember { mutableStateOf(false) }
    val voices = remember { RawSounds.regularVoices(context) }
    val fajrVoices = remember { RawSounds.fajrVoices(context) }

    SectionCard(Icons.Default.WbSunny, "الأذان") {
        DayTimes.PRAYER_KEYS.forEachIndexed { i, key ->
            var on by remember { mutableStateOf(settings.adhanEnabled(key)) }
            SwitchRow("أذان ${DayTimes.PRAYER_NAMES[i]}", on) { v ->
                on = v; settings.setAdhanEnabled(key, v); reschedule(context, scope)
            }
        }
        Spacer(Modifier.height(4.dp))
        PickerRow("صوت أذان الفجر",
            fajrVoices.firstOrNull { it.id == fajrVoice }?.display ?: "الافتراضي") {
            showFajrVoices = true
        }
        PickerRow("صوت باقي الصلوات",
            voices.firstOrNull { it.id == voice }?.display ?: "الافتراضي") {
            showVoices = true
        }
        Spacer(Modifier.height(8.dp))
        Text("مستوى الصوت: $vol%",
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Slider(
            value = vol / 100f,
            onValueChange = { vol = (it * 100).toInt(); settings.adhanVolume = vol },
            valueRange = 0f..1f
        )
    }

    if (showVoices) {
        VoiceDialog(title = "صوت أذان الظهر والعصر والمغرب والعشاء",
            voices = voices, current = voice,
            onDismiss = { showVoices = false }) { v ->
            voice = v; settings.adhanVoice = v
            showVoices = false; reschedule(context, scope)
        }
    }
    if (showFajrVoices) {
        VoiceDialog(title = "صوت أذان الفجر",
            voices = fajrVoices, current = fajrVoice,
            onDismiss = { showFajrVoices = false }) { v ->
            fajrVoice = v; settings.fajrAdhanVoice = v
            showFajrVoices = false; reschedule(context, scope)
        }
    }
}

@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun VoiceDialog(title: String, voices: List<RawSounds.Voice>, current: String,
                        onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                items(voices) { v ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(v.id) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = v.id == current, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(v.display, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

// ── منبه الفجر ───────────────────────────────────────────────

@Composable
private fun FajrAlarmCard() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(settings.fajrAlarmEnabled) }
    var minutes by remember { mutableStateOf(settings.fajrAlarmMinutes) }
    var sound by remember { mutableStateOf(settings.fajrAlarmSound) }
    var repeat by remember { mutableStateOf(settings.fajrAlarmRepeat) }
    var showSounds by remember { mutableStateOf(false) }

    SectionCard(Icons.Default.Alarm, "منبه الفجر") {
        SwitchRow("تفعيل منبه الفجر", enabled) {
            enabled = it; settings.fajrAlarmEnabled = it; reschedule(context, scope)
        }
        if (enabled) {
            Text("قبل الأذان بـ $minutes دقيقة", modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = minutes.toFloat(),
                onValueChange = { minutes = it.toInt(); settings.fajrAlarmMinutes = minutes },
                valueRange = 5f..60f, steps = 10,
                onValueChangeFinished = { reschedule(context, scope) }
            )
            Text("عدد التكرار: $repeat")
            Slider(
                value = repeat.toFloat(),
                onValueChange = { repeat = it.toInt(); settings.fajrAlarmRepeat = repeat },
                valueRange = 1f..5f, steps = 3,
                onValueChangeFinished = { reschedule(context, scope) }
            )
            val display = if (RawSounds.exists(context, sound)) RawSounds.alarmDisplay(sound)
            else "نغمة النظام"
            PickerRow("نغمة المنبه", display) { showSounds = true }
            Text("بدون غفوة - مدة الرنين = مدة الملف × عدد التكرار",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showSounds) {
        AlarmSoundDialog(
            current = sound,
            onDismiss = { showSounds = false },
            onSelect = { name ->
                sound = name; settings.fajrAlarmSound = name
                showSounds = false; reschedule(context, scope)
            })
    }
}

@Composable
private fun AlarmSoundDialog(current: String, onDismiss: () -> Unit,
                             onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val alarms = remember { RawSounds.alarmNames(context) }
    var playing by remember { mutableStateOf("") }
    DisposableEffect(Unit) { onDispose { AudioPlayer.stop() } }

    AlertDialog(
        onDismissRequest = { AudioPlayer.stop(); onDismiss() },
        title = { Text("نغمة المنبه - ▶ للمعاينة") },
        text = {
            if (alarms.isEmpty()) {
                Text("لم تُضف ملفات منبهات بعد - ضع alarm_01.ogg … في app/src/main/res/raw",
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(alarms) { name ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { AudioPlayer.stop(); playing = ""; onSelect(name) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = name == current, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(RawSounds.alarmDisplay(name), Modifier.weight(1f))
                            IconButton(onClick = {
                                if (playing == name) { AudioPlayer.stop(); playing = "" }
                                else { AudioPlayer.play(context, name, alarmStream = false); playing = name }
                            }) {
                                Icon(
                                    if (playing == name) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = "معاينة")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { AudioPlayer.stop(); onDismiss() }) { Text("إغلاق") } }
    )
}

// ── الصامت أثناء الصلاة ──────────────────────────────────────

@Composable
private fun DndCard() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var dnd by remember { mutableStateOf(settings.dndEnabled) }

    SectionCard(Icons.Default.VolumeOff, "الصامت تلقائيًا أثناء الصلاة") {
        SwitchRow("تفعيل الصامت أثناء الصلاة (وضع \"المنبهات فقط\")", dnd) { v ->
            dnd = v; settings.dndEnabled = v
            if (!v) DndController.onSettingDisabled(context)
            reschedule(context, scope)
        }
        if (dnd) {
            Spacer(Modifier.height(4.dp))
            Text("مدة بقاء الجهاز صامتًا بعد كل صلاة:",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            DayTimes.PRAYER_KEYS.forEachIndexed { i, key ->
                var v by remember { mutableStateOf(settings.dndMinutes(key)) }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بعد ${DayTimes.PRAYER_NAMES[i]}", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge)
                    DropdownSelector(value = v, options = (15..60 step 5).toList()) { nv ->
                        v = nv; settings.setDndMinutes(key, nv); reschedule(context, scope)
                    }
                }
            }
            var friday by remember { mutableStateOf(settings.dndFridayMinutes) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("صلاة الجمعة (الظهر)", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge)
                DropdownSelector(value = friday,
                    options = listOf(30, 40, 50, 60, 90, 120)) { nv ->
                    friday = nv; settings.dndFridayMinutes = nv; reschedule(context, scope)
                }
            }
        }
    }
}

// ── التذكير بالصلاة على النبي ﷺ ──────────────────────────────

@Composable
private fun TasbihCard() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    var enabled by remember { mutableStateOf(settings.tasbihEnabled) }
    var count by remember { mutableStateOf(settings.tasbihCount) }
    var sound by remember { mutableStateOf(settings.tasbihSound) }
    var showTexts by remember { mutableStateOf(false) }

    SectionCard(Icons.Default.Star, "التذكير بالصلاة على النبي ﷺ") {
        SwitchRow("تفعيل التذكيرات (موزعة بين الفجر والعشاء)", enabled) {
            enabled = it; settings.tasbihEnabled = it
        }
        if (enabled) {
            Text("عدد التذكيرات اليومية: $count (بحد أقصى 10)",
                modifier = Modifier.padding(vertical = 4.dp))
            Slider(
                value = count.toFloat(),
                onValueChange = { count = it.toInt(); settings.tasbihCount = count },
                valueRange = 2f..10f, steps = 7
            )
            SwitchRow("تشغيل صوت مع التذكير", sound) { sound = it; settings.tasbihSound = it }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { showTexts = !showTexts }) {
                Text(if (showTexts) "إخفاء الصيغ ▲" else "الصيغ المتاحة ▼")
            }
            if (showTexts) {
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
    }
}
// END OF FILE