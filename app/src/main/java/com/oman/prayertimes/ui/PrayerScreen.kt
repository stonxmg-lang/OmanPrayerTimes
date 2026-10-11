package com.oman.prayertimes.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.oman.prayertimes.data.Cities
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.ui.theme.OmanGold
import com.oman.prayertimes.utils.PrayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class LoadState { LOADING, OK, FAILED }

/** هالة لونية خفيفة حول العدّاد حسب فترة اليوم (لمسة فقط، لا تدرج صاخب) */
private fun periodTint(nextKey: String, dark: Boolean): Color = when (nextKey) {
    "fajr"    -> if (dark) Color(0xFF35507A) else Color(0xFF7FA8D9)
    "sunrise" -> if (dark) Color(0xFF2E6690) else Color(0xFF5EA4DC)
    "dhuhr"   -> if (dark) Color(0xFF2C6E54) else Color(0xFF4CAF7D)
    "asr"     -> if (dark) Color(0xFF7A5A22) else Color(0xFFD9A441)
    "maghrib" -> if (dark) Color(0xFF7C4E1D) else Color(0xFFE8912D)
    else      -> if (dark) Color(0xFF2C3A66) else Color(0xFF6D7FB8)
}

private val RowIcons: List<ImageVector> = listOf(
    Icons.Default.DarkMode, Icons.Default.WbTwilight, Icons.Default.WbSunny,
    Icons.Default.LightMode, Icons.Default.WbTwilight, Icons.Default.NightsStay
)
private val RowTints: List<Color> = listOf(
    Color(0xFF5C6BC0), Color(0xFFE0A13E), Color(0xFFE8C53A),
    Color(0xFFD9932A), Color(0xFFE07B4F), Color(0xFF3F51B5)
)

@Composable
private fun OnResumeEffect(onResume: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) onResume()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

private fun nextPrayerIndex(times: DayTimes, nowMin: Int): Int {
    for (i in DayTimes.KEYS.indices) {
        if (PrayerUtils.toMinutes(DayTimes.get(times, DayTimes.KEYS[i])) > nowMin) return i
    }
    return 0 // فجر الغد
}

private fun formatCountdown(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    val repo = remember { PrayerRepository(context) }
    val settings = remember { SettingsStore(context) }

    var times by remember { mutableStateOf<DayTimes?>(null) }
    var tomorrow by remember { mutableStateOf<DayTimes?>(null) }
    var status by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(LoadState.LOADING) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var reloadKey by remember { mutableStateOf(0) }
    var cityName by remember { mutableStateOf(Cities.nameOf(settings.cityId)) }

    OnResumeEffect { cityName = Cities.nameOf(settings.cityId) }

    // شريط الحالة بنفس لون الخلفية (مرة واحدة)
    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        val w = activity?.window ?: return@LaunchedEffect
        w.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(w, false)
        WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
    }

    // إعادة التحميل عند تغيّر اليوم (بعد منتصف الليل)
    LaunchedEffect(Unit) {
        while (true) {
            val n = LocalDateTime.now()
            if (n.toLocalDate() != day) { day = n.toLocalDate(); reloadKey++ }
            delay(1000)
        }
    }

    LaunchedEffect(reloadKey) {
        val today = day
        val cached = withContext(Dispatchers.IO) {
            repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
        }
        if (cached != null) { times = cached; state = LoadState.OK }
        else { times = null; state = LoadState.LOADING }

        val refreshed = withContext(Dispatchers.IO) {
            try { repo.refreshIfNeeded() } catch (e: Exception) { false }
        }
        val fresh = withContext(Dispatchers.IO) {
            repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
        }
        val tmr = withContext(Dispatchers.IO) {
            val t2 = today.plusDays(1)
            repo.getDay(repo.loadCache(), t2.year, t2.monthValue, t2.dayOfMonth)
        }
        tomorrow = tmr

        if (fresh == null) {
            status = "تعذّر تحميل المواقيت - تحقق من الإنترنت"
            if (state != LoadState.OK) state = LoadState.FAILED
            return@LaunchedEffect
        }
        times = fresh
        state = LoadState.OK

        val age = withContext(Dispatchers.IO) { repo.cacheAgeDays(repo.loadCache(), today.year) } ?: 0
        status = when {
            refreshed -> "تم التحديث من وزارة الأوقاف والشؤون الدينية"
            age == 0 -> "البيانات محدّثة اليوم"
            age < 25 -> "آخر تحديث منذ $age يوم"
            else -> "البيانات قديمة - تعذّر الاتصال بالوزارة"
        }

        withContext(Dispatchers.IO) { AlarmScheduler.scheduleForDay(context, fresh, today) }
    }

    val t = times
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            t == null && state == LoadState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("جاري تحميل المواقيت…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            t == null && state == LoadState.FAILED ->
                LoadFailed { state = LoadState.LOADING; reloadKey++ }
            t != null -> PrayerContent(t, tomorrow, status, cityName)
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("تعذّر تحميل المواقيت",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("تأكد من الاتصال بالإنترنت ثم أعد المحاولة",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("إعادة المحاولة") }
    }
}

@Composable
private fun PrayerContent(times: DayTimes, tomorrow: DayTimes?, status: String, cityName: String) {
    var nowMin by remember { mutableStateOf(currentMinute()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val m = currentMinute()
            if (m != nowMin) nowMin = m
        }
    }
    val nextIdx = nextPrayerIndex(times, nowMin)
    Column(Modifier.fillMaxSize()) {
        NextPrayerHero(times, tomorrow, cityName)
        PrayerList(times, nextIdx, status)
    }
}

private fun currentMinute(): Int {
    val n = LocalDateTime.now()
    return n.hour * 60 + n.minute
}

@Composable
private fun NextPrayerHero(times: DayTimes, tomorrow: DayTimes?, cityName: String) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }

    val dark = isSystemInDarkTheme()
    val today = now.toLocalDate()
    val nowMin = now.hour * 60 + now.minute
    val nextIdx = nextPrayerIndex(times, nowMin)
    val nextKey = DayTimes.KEYS[nextIdx]
    val nextStr = DayTimes.get(times, nextKey)
    val tint = periodTint(nextKey, dark)

    val target: Long
    val prev: Long
    val nextAt = LocalDateTime.of(today, LocalTime.parse(nextStr))
    if (nextAt.isAfter(now)) {
        target = PrayerUtils.calendarForToday(nextStr, today)
        val prevIdx = if (nextIdx == 0) 5 else nextIdx - 1
        var p = PrayerUtils.calendarForToday(DayTimes.get(times, DayTimes.KEYS[prevIdx]), today)
        if (p >= target) p = target - 6 * 3600_000L
        prev = p
    } else {
        target = PrayerUtils.calendarForToday(
            DayTimes.get(tomorrow ?: times, "fajr"), today.plusDays(1))
        prev = PrayerUtils.calendarForToday(DayTimes.get(times, "isha"), today)
    }

    val zone = java.time.ZoneId.systemDefault()
    val targetLdt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(target), zone)
    val remainingSec = java.time.Duration.between(now, targetLdt).seconds.coerceAtLeast(0)
    val intervalSec = ((target - prev) / 1000f).coerceAtLeast(1f)
    // الحلقة تمتلئ منذ بداية الفترة حتى الآن (تتحرك بوضوح)
    val elapsed = (1f - remainingSec / intervalSec).coerceIn(0f, 1f)

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(4.dp))
        // المدينة + التاريخان
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(cityName, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar-u-nu-latn"))),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(PrayerUtils.hijriString(today),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary)

        Spacer(Modifier.height(18.dp))

        // العدّاد البطل: هالة لونية خفيفة + حلقة واضحة
        Box(Modifier.size(238.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(tint.copy(alpha = 0.20f), Color.Transparent),
                            radius = 260f
                        ), CircleShape
                    )
            )
            CircularProgressIndicator(
                progress = elapsed,
                modifier = Modifier.size(206.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الصلاة القادمة", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(DayTimes.NAMES[nextIdx],
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text("‎" + formatCountdown(remainingSec),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "الأذان " + PrayerUtils.format12(nextStr),
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun PrayerList(times: DayTimes, nextIdx: Int, status: String) {
    val currentIdx = if (nextIdx == 0) 5 else nextIdx - 1
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(DayTimes.KEYS) { i, key ->
            val isNext = i == nextIdx
            val isCurrent = i == currentIdx
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isNext) 3.dp else 1.dp
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(RowIcons[i], contentDescription = null,
                            tint = RowTints[i], modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(DayTimes.NAMES[i], style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium)
                        if (isCurrent) {
                            Text("الآن", color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(PrayerUtils.format12(DayTimes.get(times, key)),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isNext) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                        if (isNext) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("التالية", Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                status,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
// END OF FILE