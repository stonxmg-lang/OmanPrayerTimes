package com.oman.prayertimes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oman.prayertimes.data.QuranData
import com.oman.prayertimes.data.QuranMarks
import com.oman.prayertimes.data.Surah
import com.oman.prayertimes.ui.theme.Amiri
import com.oman.prayertimes.ui.theme.LocalBrand
import kotlinx.coroutines.launch

private fun arabicDigits(n: Int): String =
    n.toString().map { "٠١٢٣٤٥٦٧٨٩"[it - '0'] }.joinToString("")

// بدايات الأجزاء الثلاثين بأرقام صفحات مصحف المدينة (1-based)
private val JUZ_START = intArrayOf(
    1, 22, 42, 62, 82, 102, 121, 142, 162, 182, 201, 222, 242, 262, 282,
    302, 322, 342, 362, 382, 402, 422, 442, 462, 482, 502, 522, 542, 562, 582
)
private val JUZ_NAMES = listOf(
    "الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع", "الثامن",
    "التاسع", "العاشر", "الحادي عشر", "الثاني عشر", "الثالث عشر", "الرابع عشر", "الخامس عشر",
    "السادس عشر", "السابع عشر", "الثامن عشر", "التاسع عشر", "العشرون", "الحادي والعشرون",
    "الثاني والعشرون", "الثالث والعشرون", "الرابع والعشرون", "الخامس والعشرون", "السادس والعشرون",
    "السابع والعشرون", "الثامن والعشرون", "التاسع والعشرون", "الثلاثون"
)

private fun juzName(pageNum: Int): String {
    var j = 0
    for (i in JUZ_START.indices) if (JUZ_START[i] <= pageNum) j = i
    return "الجزء ${JUZ_NAMES[j]}"
}

// شكل الشريط (علامة الصفحة) بحافة سفلية مشقوقة
private val RibbonShape = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height)
    lineTo(size.width / 2f, size.height * 0.68f); lineTo(0f, size.height); close()
}

@Composable
fun QuranScreen() {
    val context = LocalContext.current
    val surahs = remember { QuranData.surahs(context) }
    val marks = remember { QuranMarks(context) }
    var startPage by remember { mutableStateOf<Int?>(null) }

    val page = startPage
    if (page == null) {
        QuranIndex(surahs) { startPage = it }
    } else {
        BackHandler { startPage = null }
        MushafReader(surahs, marks, page) { startPage = null }
    }
}

@Composable
private fun QuranIndex(surahs: List<Surah>, onOpenPage: (Int) -> Unit) {
    val brand = LocalBrand.current
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(brand.heroBrush).statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text("القرآن الكريم", style = MaterialTheme.typography.headlineSmall, color = brand.onHero)
            Spacer(Modifier.height(2.dp))
            Text("المصحف — رواية حفص", style = MaterialTheme.typography.labelLarge,
                color = brand.onHeroMuted)
        }
        SegmentedTabs(listOf("السور", "الأجزاء", "الصفحات"), tab) { tab = it }
        when (tab) {
            0 -> SurahList(surahs, onOpenPage)
            1 -> JuzList(onOpenPage)
            else -> PagesList(onOpenPage)
        }
    }
}

@Composable
private fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                    .background(if (active) cs.primary else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) cs.onPrimary else cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SurahList(surahs: List<Surah>, onOpenPage: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) surahs
        else surahs.filter { it.name.contains(query) || it.tr.contains(query, ignoreCase = true) }
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
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
                val pageNum = remember(s.id) { QuranData.firstPageOf(context, s.id) + 1 }
                AppCard(Modifier.clickable { onOpenPage(pageNum - 1) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                            Text(arabicDigits(s.id), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = cs.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        RevelationIcon(s.type == "meccan")
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سورة ${s.name}", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text("${s.typeArabic} • ${arabicDigits(s.verses)} آية",
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Text("صفحة ${arabicDigits(pageNum)}",
                            style = MaterialTheme.typography.labelMedium, color = cs.primary)
                    }
                }
            }
        }
    }
}

/** أيقونة بسيطة: الكعبة للمكية، قبة مسجد للمدنية. */
@Composable
private fun RevelationIcon(meccan: Boolean) {
    if (meccan) {
        // كعبة مبسّطة: مربع داكن بحزام ذهبي
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2B2B2B)),
            contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFFD4AF37)))
        }
    } else {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Mosque, "مدنية", tint = Color(0xFF2E7D32),
                modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun JuzList(onOpenPage: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(30) { i ->
            val startPageNum = JUZ_START[i]
            AppCard(Modifier.clickable { onOpenPage(startPageNum - 1) }) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(arabicDigits(i + 1), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = cs.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("الجزء ${JUZ_NAMES[i]}", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                    Text("صفحة ${arabicDigits(startPageNum)}",
                        style = MaterialTheme.typography.labelMedium, color = cs.primary)
                }
            }
        }
    }
}

@Composable
private fun PagesList(onOpenPage: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(151) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until 4) {
                    val pageNum = row * 4 + c + 1
                    if (pageNum <= 604) {
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(cs.surfaceVariant)
                                .clickable { onOpenPage(pageNum - 1) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(arabicDigits(pageNum), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = cs.onSurface)
                        }
                    } else Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MushafReader(
    surahs: List<Surah>, marks: QuranMarks, startPage: Int, onBack: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current
    val scope = rememberCoroutineScope()
    val pages = remember { QuranData.pages(context) }
    val basmala = remember { QuranData.basmala(context) }
    val byId = remember { surahs.associateBy { it.id } }
    val markState = remember { mutableStateMapOf<Int, Int>() }
    val pagerState = rememberPagerState(initialPage = startPage) { pages.size }
    val pageNum by remember { derivedStateOf { pagerState.currentPage + 1 } }
    val currentSurahName by remember {
        derivedStateOf {
            pages.getOrNull(pagerState.currentPage)?.firstOrNull()?.get(0)
                ?.let { byId[it]?.name } ?: ""
        }
    }
    var showMarkDialog by remember { mutableStateOf(false) }
    var showGotoDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // الشريط العلوي: جزء | صفحة | سورة + أزرار العلامة
        Column(
            Modifier.fillMaxWidth().background(brand.heroBrush).statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", brand) { onBack() }
                Spacer(Modifier.width(8.dp))
                Text("سورة $currentSurahName", Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, color = brand.onHero)
                IconChip(Icons.Default.Bookmark, "وضع علامة", brand) { showMarkDialog = true }
                Spacer(Modifier.width(8.dp))
                IconChip(Icons.Default.Bookmarks, "الانتقال للعلامة", brand) { showGotoDialog = true }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(juzName(pageNum), style = MaterialTheme.typography.labelMedium,
                    color = brand.onHeroMuted)
                Text("صفحة ${arabicDigits(pageNum)}", style = MaterialTheme.typography.labelMedium,
                    color = brand.onHeroMuted)
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { p ->
            val mark = markState[p + 1] ?: marks.get(p + 1)
            MushafPage(pages[p], byId, basmala, mark)
        }
    }

    // حوار وضع العلامة
    if (showMarkDialog) {
        val cur = markState[pageNum] ?: marks.get(pageNum)
        AlertDialog(
            onDismissRequest = { showMarkDialog = false },
            title = { Text("وضع علامة عند صفحة ${arabicDigits(pageNum)}") },
            text = {
                Column {
                    MarkChoice("مراجعة", brand.markReview, cur == QuranMarks.REVIEW) {
                        marks.set(pageNum, QuranMarks.REVIEW); markState[pageNum] = QuranMarks.REVIEW
                        showMarkDialog = false
                    }
                    MarkChoice("حفظ", brand.markMemorized, cur == QuranMarks.MEMORIZED) {
                        marks.set(pageNum, QuranMarks.MEMORIZED); markState[pageNum] = QuranMarks.MEMORIZED
                        showMarkDialog = false
                    }
                    if (cur != QuranMarks.NONE) {
                        MarkChoice("إزالة العلامة", cs.onSurfaceVariant, false) {
                            marks.set(pageNum, QuranMarks.NONE); markState[pageNum] = QuranMarks.NONE
                            showMarkDialog = false
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showMarkDialog = false }) { Text("إغلاق") } }
        )
    }

    // حوار الانتقال إلى العلامات
    if (showGotoDialog) {
        val list = remember(showGotoDialog) { marks.bookmarks() }
        AlertDialog(
            onDismissRequest = { showGotoDialog = false },
            title = { Text("علاماتي") },
            text = {
                if (list.isEmpty()) Text("لا توجد علامات محفوظة بعد.")
                else Column {
                    list.forEach { (pg, st) ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                scope.launch { pagerState.scrollToPage(pg - 1) }
                                showGotoDialog = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(
                                if (st == QuranMarks.MEMORIZED) brand.markMemorized else brand.markReview))
                            Spacer(Modifier.width(10.dp))
                            Text("صفحة ${arabicDigits(pg)}  —  ${if (st == QuranMarks.MEMORIZED) "حفظ" else "مراجعة"}",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showGotoDialog = false }) { Text("إغلاق") } }
        )
    }
}

@Composable
private fun MarkChoice(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }.padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(18.dp).clip(RibbonShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        if (selected) Text("✓", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String,
                     brand: com.oman.prayertimes.ui.theme.BrandColors, onClick: () -> Unit) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(brand.heroChip).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = brand.onHero, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MushafPage(
    entries: List<List<Int>>, byId: Map<Int, Surah>, basmala: String, mark: Int
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current

    data class Run(val surah: Int, val ayat: List<Int>)
    val runs = remember(entries) {
        val out = ArrayList<Run>()
        var cur = ArrayList<Int>(); var curS = -1
        for (e in entries) {
            if (e[0] != curS) { if (cur.isNotEmpty()) out.add(Run(curS, cur)); cur = ArrayList(); curS = e[0] }
            cur.add(e[1])
        }
        if (cur.isNotEmpty()) out.add(Run(curS, cur))
        out
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            for (run in runs) {
                val surah = byId[run.surah]
                if (run.ayat.firstOrNull() == 1 && surah != null) {
                    SurahFrame(surah)
                    if (surah.bism) {
                        Text(basmala, Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Amiri),
                            textAlign = TextAlign.Center, color = cs.onSurface)
                    }
                }
                val pageText = remember(run) {
                    buildString {
                        for (a in run.ayat) {
                            append(QuranData.verseText(context, run.surah, a))
                            append(" ﴿${arabicDigits(a)}﴾ ")
                        }
                    }
                }
                Text(
                    pageText, Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Amiri, fontSize = 24.sp, lineHeight = 50.sp),
                    textAlign = TextAlign.Justify, color = cs.onSurface
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
        // شريط العلامة على الحافة العلوية
        if (mark != QuranMarks.NONE) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(end = 18.dp)
                    .size(width = 22.dp, height = 52.dp).clip(RibbonShape)
                    .background(if (mark == QuranMarks.MEMORIZED) brand.markMemorized else brand.markReview)
            )
        }
    }
}

/** إطار اسم السورة المزخرف (اسم السورة + ترتيبها + عدد آياتها). */
@Composable
private fun SurahFrame(surah: Surah) {
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current
    Box(
        Modifier.fillMaxWidth().padding(vertical = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant)
            .border(2.dp, brand.accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            FrameBadge("ترتيبها", arabicDigits(surah.id))
            Text("سورة ${surah.name}",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = Amiri),
                fontWeight = FontWeight.Bold, color = cs.onSurface)
            FrameBadge("آياتها", arabicDigits(surah.verses))
        }
    }
}

@Composable
private fun FrameBadge(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(46.dp).clip(CircleShape).background(cs.surface)
        .border(1.dp, cs.outline, CircleShape), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                fontSize = 8.sp)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = cs.primary)
        }
    }
}
