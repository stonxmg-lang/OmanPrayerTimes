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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.oman.prayertimes.data.Cities
import com.oman.prayertimes.data.CityCoords
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.notif.DndController
import com.oman.prayertimes.notif.NotificationHelper
import com.oman.prayertimes.ui.theme.LocalBrand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsHeader()
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CitySection()
            AlertsSection()
            DndSection()
            TasbihSection()
            PermissionsSection()
            VersionFooter()
        }
    }
}

// ── أدوات مشتركة ─────────────────────────────────────────────

@Composable
private fun SettingsHeader() {
    val brand = LocalBrand.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(brand.heroBrush)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp)
    ) {
        Text("الإعدادات", style = MaterialTheme.typography.headlineSmall, color = brand.onHero)
        Spacer(Modifier.height(2.dp))
        Text("المدينة والتنبيهات والصامت", style = MaterialTheme.typography.bodyMedium,
            color = brand.onHeroMuted)
        Spacer(Modifier.height(10.dp))
        // لمسة حمراء صغيرة تحت العنوان
        Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(50)).background(brand.accent))
    }
}

/** عنوان القسم (رمادي خارج البطاقة) ثم بطاقة بيضاء بداخلها الصفوف */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(title, Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    AppCard(content = content)
}

@Composable
private fun RowIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary,
                    container: Color = MaterialTheme.colorScheme.primaryContainer) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

/** صف إعداد موحّد: أيقونة + عنوان + وصف اختياري + عنصر جانبي */
@Composable
private fun SettingRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            RowIcon(icon)
            Spacer(Modifier.width(14.dp))
        } else {
            Spacer(Modifier.width(50.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun appSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)

@Composable
private fun SwitchRow(
    icon: ImageVector?, title: String, subtitle: String? = null,
    checked: Boolean, onChange: (Boolean) -> Unit
) {
    SettingRow(
        icon, title, subtitle,
        Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
    ) {
        Switch(checked = checked, onCheckedChange = null, colors = appSwitchColors())
    }
}

@Composable
private fun ChevronValue(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ChevronLeft, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MinutesDropdown(value: Int, options: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(NotificationHelper.minutesText(value), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ExpandMore, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = {
                        Text(NotificationHelper.minutesText(o),
                            fontWeight = if (o == value) FontWeight.Bold else FontWeight.Normal,
                            color = if (o == value) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                    },
                    onClick = { onSelect(o); expanded = false })
            }
        }
    }
}

private fun reschedule(context: Context, scope: CoroutineScope) {
    scope.launch(Dispatchers.IO) { AlarmScheduler.rescheduleToday(context) }
}

// ── المدينة ──────────────────────────────────────────────────

@Composable
private fun CitySection() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var cityName by remember { mutableStateOf(Cities.nameOf(settings.cityId)) }
    var syncMsg by remember { mutableStateOf("") }
    var syncOk by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }

    OnResumeEffect { cityName = Cities.nameOf(settings.cityId) }

    fun applyCity(name: String) {
        syncOk = true
        syncMsg = "جاري تحديث مواقيت $name…"
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { PrayerRepository(context).applyCityChange() } catch (e: Exception) { false }
            }
            syncOk = ok
            syncMsg = if (ok) "تم تحديث المواقيت والتنبيهات لمدينة $name"
            else "تعذّر تحميل مواقيت $name - تحقق من الإنترنت"
        }
    }

    Section("الموقع") {
        SettingRow(Icons.Default.LocationOn, "المدينة", cityName,
            Modifier.clickable { showPicker = true }) {
            ChevronValue("تغيير")
        }
        // التحديد التلقائي يظهر فقط بعد إضافة جدول إحداثيات موثوق في CityCoords
        if (CityCoords.map.isNotEmpty()) {
            RowDivider()
            AutoLocateRow(onLocated = { id, name ->
                settings.cityId = id
                cityName = name
                applyCity(name)
            })
        }
        if (syncMsg.isNotEmpty()) {
            RowDivider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(50.dp))
                Box(Modifier.size(6.dp).clip(CircleShape).background(
                    if (syncOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary))
                Spacer(Modifier.width(8.dp))
                Text(syncMsg, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("اختر المدينة", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث عن مدينة…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(8.dp))
                val q = query.trim()
                val filtered = Cities.list.filter { q.isEmpty() || it.first.contains(q) }
                LazyColumn(Modifier.fillMaxWidth().height(340.dp)) {
                    items(filtered) { (name, id) ->
                        val selected = id == currentId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent)
                                .clickable { onSelect(id, name) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            if (selected) Icon(Icons.Default.Check, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
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
    if (CityCoords.map.isEmpty()) {
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

    @Suppress("DEPRECATION", "MissingPermission")
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
    var status by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            busy = true
            status = "جاري تحديد موقعك…"
            locateNearestCity(context,
                onFound = { id, name -> busy = false; status = null; onLocated(id, name) },
                onFail = { busy = false; status = it })
        } else status = "تم رفض إذن الموقع - الاختيار اليدوي متاح"
    }
    SettingRow(Icons.Default.MyLocation, "تحديد تلقائي لأقرب مدينة", status,
        Modifier.clickable(enabled = !busy) {
            launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        else Icon(Icons.Default.ChevronLeft, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── التنبيه قبل الصلاة ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlertsSection() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(settings.alertsEnabled) }
    var leadFajr by remember { mutableIntStateOf(settings.alertLeadFajr) }
    var leadOther by remember { mutableIntStateOf(settings.alertLeadOther) }

    Section("التنبيه قبل الصلاة") {
        SwitchRow(Icons.Default.NotificationsActive, "تنبيه قبل الأذان",
            "إشعار مع صوت تنبيه قصير", enabled) {
            enabled = it; settings.alertsEnabled = it; reschedule(context, scope)
        }
        if (enabled) {
            RowDivider()
            SettingRow(Icons.Default.Timer, "قبل أذان الفجر بـ") {
                MinutesDropdown(leadFajr, SettingsStore.ALERT_LEAD_OPTIONS) {
                    leadFajr = it; settings.alertLeadFajr = it; reschedule(context, scope)
                }
            }
            RowDivider()
            SettingRow(Icons.Default.Schedule, "قبل باقي الصلوات بـ") {
                MinutesDropdown(leadOther, SettingsStore.ALERT_LEAD_OPTIONS) {
                    leadOther = it; settings.alertLeadOther = it; reschedule(context, scope)
                }
            }
            RowDivider()
            // اختيار الصلوات التي يصلها تنبيه
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text("الصلوات المُنبَّه لها", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayTimes.PRAYER_KEYS.forEachIndexed { i, key ->
                        var on by remember { mutableStateOf(settings.alertEnabled(key)) }
                        FilterChip(
                            selected = on,
                            onClick = {
                                on = !on; settings.setAlertEnabled(key, on); reschedule(context, scope)
                            },
                            label = { Text(DayTimes.PRAYER_NAMES[i]) },
                            leadingIcon = if (on) {
                                { Icon(Icons.Default.Check, contentDescription = null,
                                    modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
            RowDivider()
            SettingRow(Icons.Default.MusicNote, "صوت التنبيه", "تغيير النغمة أو مستواها من إعدادات النظام",
                Modifier.clickable {
                    context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CH_PREALERT)
                    })
                }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RowDivider()
            Box(Modifier.fillMaxWidth().padding(14.dp)) {
                OutlinedButton(
                    onClick = {
                        NotificationHelper.showPreAlert(context, "الظهر", leadOther, "12:00")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تجربة التنبيه الآن")
                }
            }
        }
    }
}

// ── الصامت أثناء الصلاة ──────────────────────────────────────

@Composable
private fun DndSection() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var dnd by remember { mutableStateOf(settings.dndEnabled) }

    Section("الصامت أثناء الصلاة") {
        SwitchRow(Icons.Default.DoNotDisturbOn, "صامت تلقائي عند الأذان",
            "يتحول الجهاز لوضع «المنبهات فقط» ثم يعود", dnd) { v ->
            dnd = v; settings.dndEnabled = v
            if (!v) DndController.onSettingDisabled(context)
            reschedule(context, scope)
        }
        if (dnd) {
            DayTimes.PRAYER_KEYS.forEachIndexed { i, key ->
                var v by remember { mutableIntStateOf(settings.dndMinutes(key)) }
                RowDivider()
                SettingRow(null, "بعد ${DayTimes.PRAYER_NAMES[i]}") {
                    MinutesDropdown(v, (15..60 step 5).toList()) { nv ->
                        v = nv; settings.setDndMinutes(key, nv); reschedule(context, scope)
                    }
                }
            }
            var friday by remember { mutableIntStateOf(settings.dndFridayMinutes) }
            RowDivider()
            SettingRow(null, "صلاة الجمعة") {
                MinutesDropdown(friday, listOf(30, 40, 50, 60, 90, 120)) { nv ->
                    friday = nv; settings.dndFridayMinutes = nv; reschedule(context, scope)
                }
            }
        }
    }
}

// ── التذكير بالصلاة على النبي ﷺ ──────────────────────────────

@Composable
private fun TasbihSection() {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(settings.tasbihEnabled) }
    var count by remember { mutableIntStateOf(settings.tasbihCount) }
    var sound by remember { mutableStateOf(settings.tasbihSound) }
    var showTexts by remember { mutableStateOf(false) }

    Section("التذكير بالذكر") {
        SwitchRow(Icons.Default.Favorite, "التذكير بالصلاة على النبي ﷺ",
            "موزعة بين الفجر والعشاء", enabled) {
            enabled = it; settings.tasbihEnabled = it; reschedule(context, scope)
        }
        if (enabled) {
            RowDivider()
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("عدد التذكيرات يوميًا", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge)
                    Text("$count", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = count.toFloat(),
                    onValueChange = { count = it.toInt(); settings.tasbihCount = count },
                    onValueChangeFinished = { reschedule(context, scope) },
                    valueRange = 2f..10f, steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            RowDivider()
            SwitchRow(null, "تشغيل صوت مع التذكير", checked = sound) {
                sound = it; settings.tasbihSound = it
            }
            RowDivider()
            SettingRow(null, "الصيغ المستخدمة", modifier = Modifier.clickable { showTexts = !showTexts }) {
                Icon(if (showTexts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showTexts) {
                TasbihTexts.list.forEachIndexed { i, text ->
                    var itemEnabled by remember { mutableStateOf(settings.tasbihTextEnabled(i)) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(itemEnabled, role = Role.Checkbox) {
                                itemEnabled = it; settings.setTasbihTextEnabled(i, it)
                                reschedule(context, scope)
                            }
                            .padding(start = 50.dp, end = 14.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = itemEnabled, onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(Modifier.width(8.dp))
                        Text(text, Modifier.weight(1f).padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ── الصلاحيات ────────────────────────────────────────────────

@Composable
private fun PermissionsSection() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    OnResumeEffect { tick++ }

    // قراءة tick تعيد رسم القسم عند العودة من إعدادات النظام فتتحدث الحالات
    if (tick < 0) return
    val notifGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    val exactGranted = Build.VERSION.SDK_INT < 31 ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    val dndGranted = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .isNotificationPolicyAccessGranted
    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    val batteryOk = pm.isIgnoringBatteryOptimizations(context.packageName)

    Section("الصلاحيات") {
        PermissionRow(Icons.Default.Notifications, "الإشعارات", "ضرورية لوصول التنبيه",
            notifGranted) {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            })
        }
        RowDivider()
        PermissionRow(Icons.Default.Schedule, "المنبه الدقيق", "لوصول التنبيه في دقيقته", exactGranted) {
            if (Build.VERSION.SDK_INT >= 31)
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
        RowDivider()
        PermissionRow(Icons.Default.DoNotDisturbOn, "عدم الإزعاج", "للصامت التلقائي أثناء الصلاة",
            dndGranted) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        RowDivider()
        PermissionRow(Icons.Default.BatteryAlert, "تحسين البطارية",
            "أوقفه للتطبيق (مهم لأجهزة OPPO وسامسونج)", batteryOk) {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, name: String, hint: String,
                          granted: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    SettingRow(icon, name, hint, Modifier.clickable(onClick = onClick)) {
        if (granted) AppBadge("مفعّل", cs.primaryContainer, cs.primary)
        else AppBadge("تفعيل", cs.tertiary, cs.onTertiary)
    }
}

@Composable
private fun VersionFooter() {
    val context = LocalContext.current
    val version = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
        catch (e: Exception) { "" }
    }
    Text(
        "مواقيت الصلاة - عُمان  •  الإصدار $version",
        Modifier.fillMaxWidth().padding(vertical = 20.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}
