package com.oman.prayertimes.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/** لوحة الترويسة المتدرجة حسب وقت اليوم */
private data class HeaderPalette(
    val brush: Brush,
    val content: Color,      // لون النصوص الأساسية فوق التدرج
    val accent: Color,       // لون العدّاد واللمسات الذهبية
    val lightStatusIcons: Boolean // أيقونات شريط الحالة داكنة (للخلفيات الفاتحة)
)

@Composable
private fun headerPalette(nextKey: String, dark: Boolean): HeaderPalette = when (nextKey) {
    // الفجر: أزرق فاتح مشرق (ضوء ما قبل الشروق)
    "fajr" -> if (!dark) HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF8FBCE8), Color(0xFFC2DDF5), Color(0xFFEAF3FC))),
        Color(0xFF16324F), Color(0xFF8A6D1A), true)
    else HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF16273F), Color(0xFF1E3A5C), Color(0xFF12202F))),
        Color.White, OmanGold, false)
    // الشروق/الصباح: أزرق سماوي صافٍ مشرق
    "sunrise" -> if (!dark) HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF6FB1E8), Color(0xFFA8D0F2), Color(0xFFE3F0FA))),
        Color(0xFF12304E), Color(0xFF8A6D1A), true)
    else HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF14304E), Color(0xFF1C4568), Color(0xFF122336))),
        Color.White, OmanGold, false)
    // الظهر: الأخضر العُماني
    "dhuhr" -> if (!dark) HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF0B3D22), Color(0xFF046A38), Color(0xFF0E7A45))),
        Color.White, OmanGold, false)
    else HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF0A2016), Color(0xFF0E3524), Color(0xFF12402C))),
        Color.White, OmanGold, false)
    // العصر: كهرماني دافئ
    "asr" -> if (!dark) HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFFE8B04B), Color(0xFFF2C878), Color(0xFFF9E4BC))),
        Color(0xFF4A2E04), Color(0xFF7A4E00), true)
    else HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF3A2C12), Color(0xFF4E3A18), Color(0xFF2E2410))),
        Color(0xFFF5E3C0), OmanGold, false)
    // المغرب: أصفر لامع (ساعة الغروب)
    "maghrib" -> if (!dark) HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF8F00))),
        Color(0xFF3A2600), Color(0xFF6B4200), true)
    else HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF4A3408), Color(0xFF6B4A0F), Color(0xFF33240A))),
        Color(0xFFFFE9B8), OmanGold, false)
    // العشاء/الليل: أزرق ليلي داكن
    else -> if (!dark) HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF16294D), Color(0xFF1F3A66), Color(0xFF0E1B33))),
        Color.White, OmanGold, false)
    else HeaderPalette(
        Brush.verticalGradient(listOf(Color(0xFF070F1E), Color(0xFF0D1D38), Color(0xFF050B16))),
        Color.White, OmanGold, false)
}

private val RowIcons: List<ImageVector> = listOf(
    Icons.Default.DarkMode, Icons.Default.WbTwilight, Icons.Default.WbSunny,
    Icons.Default.LightMode, Icons.Default.WbTwilight, Icons.Default.NightsStay
)
private val RowTints: List<Color> = listOf(
    Color(0xFF7986CB), Color(0xFFFFB74D), Color(0xFFFDD835),
    Color(0xFFFFB300), Color(0xFFFF8A65), Color(0xFF5C6BC0)
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

        // 1) اعرض من الكاش فورًا بدون انتظار الشبكة
        val cached = withContext(Dispatchers.IO) {
            repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
        }
        if (cached != null) { times = cached; state = LoadState.OK }
        else { times = null; state = LoadState.LOADING }

        // 2) حدّث في الخلفية عند اللزوم
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
            status = "تعذّر تحميل مواقيت $cityName - تحقق من الإنترنت ثم أعد المحاولة"
            if (state != LoadState.OK) state = LoadState.FAILED
            return@LaunchedEffect
        }
        times = fresh
        state = LoadState.OK

        val age = withContext(Dispatchers.IO) { repo.cacheAgeDays(repo.loadCache(), today.year) } ?: 0
        status = when {
            refreshed -> "تم تحديث المواقيت من وزارة الأوقاف والشؤون الدينية"
            age == 0 -> "البيانات محدّثة اليوم"
            age < 25 -> "آخر تحديث منذ $age يوم"
            else -> "البيانات قديمة (منذ $age يوم) - تعذّر الاتصال بالوزارة"
        }

        // 3) جدولة تنبيهات اليوم
        withContext(Dispatchers.IO) { AlarmScheduler.scheduleForDay(context, fresh, today) }
    }

    val t = times
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            t == null && state == LoadState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("جاري تحميل مواقيت $cityName…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            t == null && state == LoadState.FAILED ->
                LoadFailed(cityName) { state = LoadState.LOADING; reloadKey++ }
            t != null -> PrayerContent(t, tomorrow, status, cityName)
        }
    }
}

@Composable
private fun LoadFailed(cityName: String, onRetry: () -> Unit) {
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
        androidx.compose.material3.Button(onClick = onRetry) { Text("إعادة المحاولة") }
    }
}

@Composable
private fun PrayerContent(times: DayTimes, tomorrow: DayTimes?, status: String, cityName: String) {
    // تحديث دقّة الدقائق فقط لتجنّب إعادة تركيب القائمة كل ثانية
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
        NextPrayerHeader(times, tomorrow, cityName)
        PrayerList(times, nextIdx, status)
    }
}

private fun currentMinute(): Int {
    val n = LocalDateTime.now()
    return n.hour * 60 + n.minute
}

@Composable
private fun NextPrayerHeader(times: DayTimes, tomorrow: DayTimes?, cityName: String) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }

    val dark = isSystemInDarkTheme()
    val today = now.toLocalDate()
    val nowMin = now.hour * 60 + now.minute
    val nextIdx = nextPrayerIndex(times, nowMin)
    val nextKey = DayTimes.KEYS[nextIdx]
    val nextStr = DayTimes.get(times, nextKey)
    val palette = headerPalette(nextKey, dark)

    // مطابقة شريط حالة النظام مع لون الترويسة (عند تغيّر الفترة فقط، لا كل ثانية)
    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    LaunchedEffect(nextKey, dark) {
        val w = activity?.window ?: return@LaunchedEffect
        w.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(w, false)
        WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = palette.lightStatusIcons
    }

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
    val frac = (remainingSec / intervalSec).coerceIn(0f, 1f)

    Box(
        Modifier
            .fillMaxWidth()
            .background(palette.brush)
            .statusBarsPadding()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    tint = palette.content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(cityName, color = palette.content, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar-u-nu-latn"))),
                color = palette.content.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            Text(PrayerUtils.hijriString(today), color = palette.accent,
                style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(14.dp))

            Box(Modifier.size(196.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = frac,
                    modifier = Modifier.size(196.dp),
                    color = palette.content.copy(alpha = 0.28f),
                    strokeWidth = 7.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("باقي على صلاة", color = palette.content.copy(alpha = 0.75f), fontSize = 12.sp)
                    Text(DayTimes.NAMES[nextIdx], color = palette.content,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("\u200E" + formatCountdown(remainingSec),
                        color = palette.accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(PrayerUtils.format12(nextStr), color = palette.content.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PrayerList(times: DayTimes, nextIdx: Int, status: String) {
    val currentIdx = if (nextIdx == 0) 5 else nextIdx - 1
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(DayTimes.KEYS) { i, key ->
            val isNext = i == nextIdx
            val isCurrent = i == currentIdx
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = if (isNext) BorderStroke(1.5.dp, OmanGold) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .background(
                                if (isNext) OmanGold.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(RowIcons[i], contentDescription = null,
                            tint = RowTints[i], modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(DayTimes.NAMES[i], style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
                        if (isCurrent) Text("الصلاة الحالية", color = OmanGold, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(PrayerUtils.format12(DayTimes.get(times, key)),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isNext) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface)
                        if (isNext) Text("التالية", color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp)
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
            Spacer(Modifier.height(20.dp))
        }
    }
}
// END OF FILE