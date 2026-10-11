package com.oman.prayertimes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oman.prayertimes.data.Cities
import com.oman.prayertimes.data.PrayerRepository
import com.oman.prayertimes.data.SettingsStore
import com.oman.prayertimes.model.DayTimes
import com.oman.prayertimes.notif.AlarmScheduler
import com.oman.prayertimes.utils.PrayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class LoadState { LOADING, OK, FAILED }

@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    val repo = remember { PrayerRepository(context) }
    val settings = remember { SettingsStore(context) }

    var times by remember { mutableStateOf<DayTimes?>(null) }
    var status by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(LoadState.LOADING) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var reloadKey by remember { mutableStateOf(0) }

    // ساعة مُحدِّثة كل ثانية للعد التنازلي، وإعادة التحميل عند تغيّر اليوم (بعد منتصف الليل)
    LaunchedEffect(Unit) {
        while (true) {
            val n = LocalDateTime.now()
            now = n
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

        // 2) حدّث في الخلفية عند اللزوم (لا يعطّل الواجهة)
        val refreshed = withContext(Dispatchers.IO) {
            try { repo.refreshIfNeeded() } catch (e: Exception) { false }
        }
        val fresh = withContext(Dispatchers.IO) {
            repo.getDay(repo.loadCache(), today.year, today.monthValue, today.dayOfMonth)
        }
        if (fresh == null) { state = LoadState.FAILED; return@LaunchedEffect }
        times = fresh
        state = LoadState.OK

        val age = withContext(Dispatchers.IO) { repo.cacheAgeDays(repo.loadCache(), today.year) } ?: 0
        status = when {
            refreshed -> "🟢 تم تحديث المواقيت من mara.gov.om"
            age == 0 -> "🟢 البيانات محدّثة اليوم"
            age < 25 -> "🟢 بيانات محفوظة (آخر تحديث منذ $age يوم)"
            else -> "🟠 بيانات قديمة (منذ $age يوم) - تعذّر التحديث"
        }

        // 3) جدولة تنبيهات اليوم
        withContext(Dispatchers.IO) { AlarmScheduler.scheduleForDay(context, fresh, today) }
    }

    val t = times
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            state == LoadState.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("جاري تحميل مواقيت ${Cities.nameOf(settings.cityId)}…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state == LoadState.FAILED || t == null ->
                LoadFailed(Cities.nameOf(settings.cityId)) { state = LoadState.LOADING; reloadKey++ }
            else -> PrayerContent(t, now, status)
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
        Text("⚠️", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("تعذّر تحميل مواقيت $cityName",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("تأكد من الاتصال بالإنترنت ثم أعد المحاولة",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("إعادة المحاولة") }
    }
}

@Composable
private fun PrayerContent(times: DayTimes, now: LocalDateTime, status: String) {
    val today = LocalDate.now()
    val nowMin = now.hour * 60 + now.minute
    val nextIdx = PrayerUtils.nextPrayerIndex(times, nowMin)
    val nextTimeStr = DayTimes.get(times, DayTimes.KEYS[nextIdx])
    var nextAt = LocalDateTime.of(today, LocalTime.parse(nextTimeStr))
    if (!nextAt.isAfter(now)) nextAt = nextAt.plusDays(1) // بعد العشاء: فجر الغد
    val secondsLeft = java.time.Duration.between(now, nextAt).seconds

    // بطاقة التاريخ
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(Cities.nameOf(SettingsStore(LocalContext.current).cityId),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium)
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar-u-nu-latn"))),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(PrayerUtils.hijriString(today),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall)
        }
    }

    Spacer(Modifier.height(12.dp))

    // بطاقة العد التنازلي
    if (secondsLeft > 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏳ باقي على صلاة ${DayTimes.NAMES[nextIdx]}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "\u200E" + PrayerUtils.formatCountdown(secondsLeft),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // قائمة الصلوات
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(DayTimes.KEYS) { i, key ->
            val isNext = i == nextIdx
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(DayTimes.ICONS[i], fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        DayTimes.NAMES[i],
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        PrayerUtils.format12(DayTimes.get(times, key)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                        color = if (isNext) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            Text(
                status,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


