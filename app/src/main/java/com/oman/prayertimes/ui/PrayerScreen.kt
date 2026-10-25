package com.oman.prayertimes.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.oman.prayertimes.data.Cities
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.ui.theme.LocalBrand
import com.oman.prayertimes.utils.PrayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class LoadState { LOADING, OK, FAILED }

/** حالة البيانات أسفل الشاشة: نص + هل هي سليمة (أخضر) أم تحتاج انتباهًا (أحمر) */
private data class DataStatus(val text: String, val ok: Boolean)

private val RowIcons: List<ImageVector> = listOf(
    Icons.Default.DarkMode, Icons.Default.WbTwilight, Icons.Default.WbSunny,
    Icons.Default.LightMode, Icons.Default.WbTwilight, Icons.Default.NightsStay
)

private val ArabicLatinDigits: Locale = Locale.forLanguageTag("ar-u-nu-latn")

@Composable
internal fun OnResumeEffect(onResume: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) onResume()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    val repo = remember { PrayerRepository(context) }
    val settings = remember { SettingsStore(context) }

    var times by remember { mutableStateOf<DayTimes?>(null) }
    var tomorrow by remember { mutableStateOf<DayTimes?>(null) }
    var status by remember { mutableStateOf<DataStatus?>(null) }
    var state by remember { mutableStateOf(LoadState.LOADING) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var cityName by remember { mutableStateOf(Cities.nameOf(settings.cityId)) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    OnResumeEffect { cityName = Cities.nameOf(settings.cityId) }

    // ساعة واحدة للشاشة كلها: تحدّث العد التنازلي، وتعيد التحميل عند تغيّر اليوم
    LaunchedEffect(Unit) {
        while (true) {
            val n = LocalDateTime.now()
            if (n.toLocalDate() != now.toLocalDate()) reloadKey++
            now = n
            delay(1000)
        }
    }

    LaunchedEffect(reloadKey) {
        val today = LocalDate.now()
        val cached = withContext(Dispatchers.IO) {
            repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
        }
        if (cached != null) { times = cached; state = LoadState.OK }
        else { times = null; state = LoadState.LOADING }

        val refreshed = withContext(Dispatchers.IO) {
            try { repo.refreshIfNeeded() } catch (e: Exception) { false }
        }
        val cache = withContext(Dispatchers.IO) { repo.loadCache() }
        val fresh = repo.getDay(cache, today.year, today.monthValue, today.dayOfMonth)
        val t2 = today.plusDays(1)
        tomorrow = repo.getDay(cache, t2.year, t2.monthValue, t2.dayOfMonth)

        if (fresh == null) {
            status = DataStatus("تعذّر تحميل المواقيت - تحقق من الإنترنت", ok = false)
            if (state != LoadState.OK) state = LoadState.FAILED
            return@LaunchedEffect
        }
        times = fresh
        state = LoadState.OK

        val age = repo.cacheAgeDays(cache, today.year) ?: 0
        status = when {
            refreshed -> DataStatus("تم التحديث من وزارة الأوقاف والشؤون الدينية", true)
            age == 0 -> DataStatus("البيانات محدّثة اليوم", true)
            age < 25 -> DataStatus("آخر تحديث منذ $age يوم", true)
            else -> DataStatus("البيانات قديمة - تعذّر الاتصال بالوزارة", false)
        }

        withContext(Dispatchers.IO) { AlarmScheduler.scheduleForDay(context, fresh, today) }
    }

    val t = times
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            t != null -> PrayerContent(t, tomorrow, status, cityName, now)
            state == LoadState.FAILED -> LoadFailed { state = LoadState.LOADING; reloadKey++ }
            else -> Loading()
        }
    }
}

@Composable
private fun Loading() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
        Spacer(Modifier.height(16.dp))
        Text("جاري تحميل المواقيت…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("تعذّر تحميل المواقيت",
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("تأكد من الاتصال بالإنترنت ثم أعد المحاولة",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("إعادة المحاولة")
        }
    }
}

@Composable
private fun PrayerContent(
    times: DayTimes, tomorrow: DayTimes?, status: DataStatus?,
    cityName: String, now: LocalDateTime
) {
    val nowMin = now.hour * 60 + now.minute
    val nextIdx = PrayerUtils.nextPrayerIndex(times, nowMin)
    val currentIdx = PrayerUtils.currentPrayerIndex(times, nowMin)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PrayerHero(cityName, times, tomorrow, now, nextIdx)

        Spacer(Modifier.height(16.dp))
        Text(
            "مواقيت اليوم",
            Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        AppCard(Modifier.padding(horizontal = 16.dp)) {
            DayTimes.KEYS.forEachIndexed { i, key ->
                if (i > 0) RowDivider()
                PrayerListRow(
                    label = DayTimes.NAMES[i],
                    time = PrayerUtils.format12(DayTimes.get(times, key)),
                    icon = RowIcons[i],
                    isNext = i == nextIdx,
                    isCurrent = i == currentIdx,
                    muted = key == "sunrise"
                )
            }
        }

        if (status != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(
                    if (status.ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary))
                Spacer(Modifier.width(8.dp))
                Text(status.text, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** الرأس الأخضر: المدينة والتاريخ، ثم الصلاة القادمة والعدّ التنازلي وشريط تقدّم أحمر */
@Composable
private fun PrayerHero(
    cityName: String, times: DayTimes, tomorrow: DayTimes?,
    now: LocalDateTime, nextIdx: Int
) {
    val brand = LocalBrand.current
    val today = now.toLocalDate()
    val nowMs = System.currentTimeMillis()

    // الهدف = الصلاة القادمة اليوم، أو فجر الغد بعد العشاء
    val nextKey = DayTimes.KEYS[nextIdx]
    val todayNext = PrayerUtils.calendarForToday(DayTimes.get(times, nextKey), today)
    val target: Long
    val prev: Long
    val targetTimeStr: String
    if (todayNext > nowMs) {
        target = todayNext
        targetTimeStr = DayTimes.get(times, nextKey)
        val prevIdx = if (nextIdx == 0) DayTimes.KEYS.lastIndex else nextIdx - 1
        var p = PrayerUtils.calendarForToday(DayTimes.get(times, DayTimes.KEYS[prevIdx]), today)
        if (p >= target) p -= 24 * 3_600_000L          // العشاء أمس قبل فجر اليوم
        prev = p
    } else {
        targetTimeStr = DayTimes.get(tomorrow ?: times, "fajr")
        target = PrayerUtils.calendarForToday(targetTimeStr, today.plusDays(1))
        prev = PrayerUtils.calendarForToday(DayTimes.get(times, "isha"), today)
    }
    val remainingSec = ((target - nowMs) / 1000).coerceAtLeast(0)
    val progress = ((nowMs - prev).toFloat() / (target - prev).coerceAtLeast(1)).coerceIn(0f, 1f)

    val isSunrise = nextKey == "sunrise"
    val nextName = DayTimes.NAMES[nextIdx]

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(brand.heroBrush)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
    ) {
        // المدينة + التاريخ
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(brand.heroChip)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    tint = brand.onHero, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(cityName, style = MaterialTheme.typography.labelLarge, color = brand.onHero)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM", ArabicLatinDigits)),
                    style = MaterialTheme.typography.labelLarge, color = brand.onHero
                )
                Text(PrayerUtils.hijriString(today),
                    style = MaterialTheme.typography.labelSmall, color = brand.onHeroMuted)
            }
        }

        Spacer(Modifier.height(28.dp))

        // الصلاة القادمة والعدّ التنازلي
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isSunrise) "الشروق بعد" else "الصلاة القادمة",
                style = MaterialTheme.typography.labelLarge, color = brand.onHeroMuted)
            Spacer(Modifier.height(2.dp))
            Text(nextName, style = MaterialTheme.typography.headlineMedium, color = brand.onHero)
            Spacer(Modifier.height(4.dp))
            Text(
                PrayerUtils.formatCountdown(remainingSec),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = "tnum",
                    textDirection = TextDirection.Ltr
                ),
                color = brand.onHero
            )
            Spacer(Modifier.height(10.dp))
            Text(
                (if (isSunrise) "الشروق " else "الأذان ") + PrayerUtils.format12(targetTimeStr),
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(brand.heroChip)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = brand.onHero
            )
        }

        Spacer(Modifier.height(22.dp))

        // شريط التقدّم بين الوقت السابق والقادم (أحمر على أبيض شفاف)
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(brand.accent)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            val prevIdx = if (nextIdx == 0) DayTimes.KEYS.lastIndex else nextIdx - 1
            Text(DayTimes.NAMES[prevIdx], style = MaterialTheme.typography.labelSmall,
                color = brand.onHeroMuted)
            Spacer(Modifier.weight(1f))
            Text(nextName, style = MaterialTheme.typography.labelSmall, color = brand.onHeroMuted)
        }
    }
}
