package com.oman.prayertimes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oman.prayertimes.data.QuranData
import com.oman.prayertimes.data.QuranMarks
import com.oman.prayertimes.data.Surah
import com.oman.prayertimes.ui.theme.Amiri
import com.oman.prayertimes.ui.theme.LocalBrand

private fun arabicDigits(n: Int): String =
    n.toString().map { "٠١٢٣٤٥٦٧٨٩"[it - '0'] }.joinToString("")

@Composable
fun QuranScreen() {
    val context = LocalContext.current
    val surahs = remember { QuranData.surahs(context) }
    val marks = remember { QuranMarks(context) }
    var startPage by remember { mutableStateOf<Int?>(null) }

    val page = startPage
    if (page == null) {
        SurahIndex(surahs) { startPage = QuranData.firstPageOf(context, it.id) }
    } else {
        BackHandler { startPage = null }
        MushafReader(surahs, marks, page) { startPage = null }
    }
}

@Composable
private fun SurahIndex(surahs: List<Surah>, onSelect: (Surah) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) surahs
        else surahs.filter { it.name.contains(query) || it.tr.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(brand.heroBrush).statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
        ) {
            Text("القرآن الكريم", style = MaterialTheme.typography.headlineSmall, color = brand.onHero)
            Spacer(Modifier.height(2.dp))
            Text("المصحف — رواية حفص", style = MaterialTheme.typography.labelLarge,
                color = brand.onHeroMuted)
        }

        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("ابحث عن سورة…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true, shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary, unfocusedBorderColor = cs.outline)
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(filtered, key = { _, s -> s.id }) { _, s ->
                AppCard(Modifier.clickable { onSelect(s) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                            Text(arabicDigits(s.id), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = cs.primary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سورة ${s.name}", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text("${s.typeArabic} • ${arabicDigits(s.verses)} آية",
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MushafReader(
    surahs: List<Surah>, marks: QuranMarks, startPage: Int, onBack: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current
    val pages = remember { QuranData.pages(context) }
    val basmala = remember { QuranData.basmala(context) }
    val byId = remember { surahs.associateBy { it.id } }
    // علامات الآيات: المفتاح "سورة:آية" → حالة. يدفع إعادة الرسم عند اللمس.
    val markState = remember { mutableStateMapOf<String, Int>() }
    val pagerState = rememberPagerState(initialPage = startPage) { pages.size }
    val currentSurahName by remember {
        derivedStateOf {
            pages.getOrNull(pagerState.currentPage)?.firstOrNull()?.get(0)
                ?.let { byId[it]?.name } ?: ""
        }
    }

    Column(Modifier.fillMaxSize()) {
        // شريط علوي
        Row(
            Modifier.fillMaxWidth()
                .background(brand.heroBrush).statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(brand.heroChip)
                .clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = brand.onHero,
                    modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("سورة $currentSurahName", Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, color = brand.onHero)
            Text("صفحة ${arabicDigits(pagerState.currentPage + 1)}",
                style = MaterialTheme.typography.labelLarge, color = brand.onHeroMuted)
        }
        // مفتاح العلامات
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            LegendDot(brand.markReview, "مراجعة")
            LegendDot(brand.markMemorized, "حفظ")
            Spacer(Modifier.weight(1f))
            Text("المس الآية لوضع العلامة", style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant)
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { p ->
            MushafPage(pages[p], byId, basmala, marks, markState)
        }
    }
}

@Composable
private fun MushafPage(
    entries: List<List<Int>>,
    byId: Map<Int, Surah>,
    basmala: String,
    marks: QuranMarks,
    markState: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current

    // تقسيم الصفحة إلى مقاطع متتالية بحسب السورة
    data class Run(val surah: Int, val ayat: List<Int>)
    val runs = remember(entries) {
        val out = ArrayList<Run>()
        var cur = ArrayList<Int>(); var curS = -1
        for (e in entries) {
            val s = e[0]; val a = e[1]
            if (s != curS) { if (cur.isNotEmpty()) out.add(Run(curS, cur)); cur = ArrayList(); curS = s }
            cur.add(a)
        }
        if (cur.isNotEmpty()) out.add(Run(curS, cur))
        out
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        for (run in runs) {
            val surah = byId[run.surah]
            if (run.ayat.firstOrNull() == 1 && surah != null) {
                SurahHeaderBox(surah)
                if (surah.bism) {
                    Text(basmala, Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Amiri),
                        textAlign = TextAlign.Center, color = cs.primary)
                }
            }
            // نص متصل مرصوص لكل آيات المقطع، مع إمكان اللمس لكل آية
            val annotated = buildAnnotatedString {
                for (a in run.ayat) {
                    val key = "${run.surah}:$a"
                    val mark = markState[key] ?: marks.get(run.surah, a)
                    val bg = when (mark) {
                        QuranMarks.REVIEW -> brand.markReview.copy(alpha = 0.16f)
                        QuranMarks.MEMORIZED -> brand.markMemorized.copy(alpha = 0.16f)
                        else -> null
                    }
                    pushStringAnnotation("ayah", key)
                    if (bg != null) withStyle(SpanStyle(background = bg)) {
                        append(QuranData.verseText(context, run.surah, a))
                        append(" ﴿${arabicDigits(a)}﴾ ")
                    } else {
                        append(QuranData.verseText(context, run.surah, a))
                        append(" ﴿${arabicDigits(a)}﴾ ")
                    }
                    pop()
                }
            }
            ClickableText(
                text = annotated,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                style = TextStyle(
                    fontFamily = Amiri, fontSize = 24.sp, lineHeight = 50.sp,
                    textAlign = TextAlign.Justify, color = cs.onSurface
                )
            ) { offset ->
                annotated.getStringAnnotations("ayah", offset, offset).firstOrNull()?.let { ann ->
                    val (s, a) = ann.item.split(":").map { it.toInt() }
                    markState[ann.item] = marks.cycle(s, a)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SurahHeaderBox(surah: Surah) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().padding(vertical = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.primaryContainer)
            .border(1.dp, cs.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("سورة ${surah.name}", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer)
            Text("${surah.typeArabic} • ${arabicDigits(surah.verses)} آية",
                style = MaterialTheme.typography.labelMedium, color = cs.primary)
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
