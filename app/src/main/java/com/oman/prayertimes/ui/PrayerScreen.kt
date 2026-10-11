package com.oman.prayertimes.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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

private fun periodTint(nextKey: String, dark: Boolean): Color = when (nextKey) {
    "fajr" -> if (dark) Color(0xFF35507A) else Color(0xFF7FA8D9)
    "sunrise" -> if (dark) Color(0xFF2E6690) else Color(0xFF5EA4DC)
    "dhuhr" -> if (dark) Color(0xFF2C6E54) else Color(0xFF4CAF7D)
    "asr" -> if (dark) Color(0xFF7A5A22) else Color(0xFFD9A441)
    "maghrib" -> if (dark) Color(0xFF7C4E1D) else Color(0xFFE8912D)
    else -> if (dark) Color(0xFF2C3A66) else Color(0xFF6D7FB8)
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
    return 0
}

private fun formatCountdown(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private fun currentMinute(): Int {
    val n = LocalDateTime.now()
    return n.hour * 60 + n.minute
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
    var nowMin by remember { mutableStateOf(currentMinute()) }

    OnResumeEffect { cityName = Cities.nameOf(settings.cityId) }

    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    LaunchedEffect(Unit) {
        val w = activity?.window ?: return@LaunchedEffect
        w.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(w, false)
        WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
    }

    LaunchedEffect(Unit) {
        while (true) {
            val n = LocalDateTime.now()
            if (n.toLocalDate() != day) { day = n.toLocalDate(); reloadKey++ }
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val m = currentMinute()
            if (m != nowMin) nowMin = m
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
                    Spacer(Modifier.height(16.dp))
                    Text("جاري تحميل المواقيت…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            t == null && state == LoadState.FAILED -> LoadFailed { state = LoadState.LOADING; reloadKey++ }
            t != null -> PrayerContent(t, tomorrow, status, cityName, nowMin)
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
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("تأكد من الاتصال بالإنترنت ثم أعد المحاولة",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) { Text("إعادة المحاولة") }
    }
}

@Composable
private fun PrayerContent(times: DayTimes, tomorrow: DayTimes?, status: String, cityName: String, nowMin: Int) {
    Column(Modifier.fillMaxSize()) {
        PrayerHeader(cityName, times, tomorrow, nowMin)
        PrayerTimerSection(times, tomorrow, nowMin, Modifier.weight(1f))
        PrayerListCompact(times, nowMin)
    }
}

@Composable
private fun PrayerHeader(cityName: String, times: DayTimes, tomorrow: DayTimes?, nowMin: Int) {
    val today = LocalDate.now()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(cityName, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar-u-nu-latn"))),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(PrayerUtils.hijriString(today),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp)
    }
}

@Composable
private fun PrayerTimerSection(times: DayTimes, tomorrow: DayTimes?, nowMin: Int, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }

    val dark = isSystemInDarkTheme()
    val today = now.toLocalDate()
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
    val elapsed = (1f - remainingSec / intervalSec).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(tint.copy(alpha = 0.18f), Color.Transparent),
                                radius = 180f
                            ), CircleShape
                        )
                )
                CircularProgressIndicator(
                    progress = elapsed,
                    modifier = Modifier.size(140.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    strokeWidth = 7.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الصلاة القادمة", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(DayTimes.NAMES[nextIdx],
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(formatCountdown(remainingSec),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "الأذان " + PrayerUtils.format12(nextStr),
                    Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PrayerListCompact(times: DayTimes, nowMin: Int) {
    val currentIdx = if (nowMin == 0) 5 else nextPrayerIndex(times, nowMin - 1) - 1
    val nextIdx = nextPrayerIndex(times, nowMin)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in DayTimes.KEYS.indices) {
            val key = DayTimes.KEYS[i]
            val isNext = i == nextIdx
            val isCurrent = i == currentIdx
            PrayerListRow(
                label = DayTimes.NAMES[i],
                time = DayTimes.get(times, key),
                icon = RowIcons[i],
                tint = RowTints[i],
                isNext = isNext,
                isCurrent = isCurrent
            )
        }
    }
}
