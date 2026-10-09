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

@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    val repo = remember { PrayerRepository(context) }
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()

    var times by remember { mutableStateOf<DayTimes?>(null) }
    var online by remember { mutableStateOf(false) }
    var noData by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var isLoading by remember { mutableStateOf(true) }

    // ساعة مُحدِّثة كل ثانية للعد التنازلي
    LaunchedEffect(Unit) {
        while (true) { now = LocalDateTime.now(); delay(1000) }
    }

    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        var cache = repo.loadCache()
        val net = repo.hasInternet()
        var t: DayTimes? = null
        if (net) {
            t = repo.fetchDay(today.year, today.monthValue, today.dayOfMonth, settings.cityId)
        }
        if (t != null) {
            online = true
        } else {
            t = repo.getDay(cache, today.year, today.monthValue, today.dayOfMonth)
        }
        times = t
        isLoading = false
        if (t == null && !cache.containsKey(today.year.toString())) {
            noData = true // سنة جديدة بدون كاش
        }

        // تحديث الخلفية: السنة الحالية كل 25 يوم + السنة الجديدة في آخر 10 أيام من نوفمبر
        scope.launch(Dispatchers.IO) {
            var changed = false
            val years = mutableListOf<Int>()
            if (repo.shouldRefreshYear(cache, today.year)) years.add(today.year)
            if (today.monthValue == 11 && today.dayOfMonth >= 21 &&
                repo.shouldRefreshYear(cache, today.year + 1)) {
                years.add(today.year + 1)
            }
            for (y in years) {
                val days = repo.fetchYear(y, settings.cityId)
                if (days.isNotEmpty()) {
                    cache[y.toString()] = PrayerRepository.YearCache(LocalDateTime.now().toString(), days)
                    changed = true
                }
            }
            if (repo.cleanupOldYears(cache)) changed = true
            if (changed) repo.saveCache(cache)

            val todayTimes = t ?: repo.getDay(cache, today.year, today.monthValue, today.dayOfMonth)
            if (todayTimes != null) {
                AlarmScheduler.scheduleForDay(context, todayTimes, today)
            }
        }
    }

    val t = times
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            noData || t == null -> NewYearMessage()
            else -> PrayerContent(t, now, online)
        }
    }
}

@Composable
private fun NewYearMessage() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("بدأت سنة جديدة - لا توجد بيانات محفوظة",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("يرجى الاتصال بالإنترنت لتحديث مواقيت الصلاة",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrayerContent(times: DayTimes, now: LocalDateTime, online: Boolean) {
    val today = LocalDate.now()
    val nowMin = now.hour * 60 + now.minute
    val nextIdx = PrayerUtils.nextPrayerIndex(times, nowMin)
    val nextTimeStr = DayTimes.get(times, DayTimes.KEYS[nextIdx])
    val nextAt = LocalDateTime.of(today, LocalTime.parse(nextTimeStr))
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

    // بطاقة العد التنازلي (تظهر قبل الصلاة بساعة)
    if (secondsLeft in 0..3600) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏳ المتبقي على صلاة ${DayTimes.NAMES[nextIdx]}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    PrayerUtils.timeUntilString(secondsLeft),
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
                        DayTimes.get(times, key),
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
                if (online) "🟢 متصل - بيانات مباشرة من mara.gov.om"
                else "🟠 غير متصل - بيانات من الذاكرة",
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

