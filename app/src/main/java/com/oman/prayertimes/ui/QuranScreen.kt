package com.oman.prayertimes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

private fun arabicDigits(n: Int): String =
    n.toString().map { "٠١٢٣٤٥٦٧٨٩"[it - '0'] }.joinToString("")

@Composable
fun QuranScreen() {
    val context = LocalContext.current
    val surahs = remember { QuranData.surahs(context) }
    val marks = remember { QuranMarks(context) }
    var selected by remember { mutableStateOf<Surah?>(null) }

    val current = selected
    if (current == null) {
        SurahIndex(surahs) { selected = it }
    } else {
        BackHandler { selected = null }
        SurahReader(
            surah = current,
            verses = remember(current.id) { QuranData.versesOf(context, current.id) },
            basmala = remember { QuranData.basmala(context) },
            marks = marks,
            onBack = { selected = null }
        )
    }
}

@Composable
private fun QuranHeader(title: String, subtitle: String?, onBack: (() -> Unit)? = null) {
    val brand = LocalBrand.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(brand.heroBrush)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(brand.heroChip).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع",
                        tint = brand.onHero, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = brand.onHero)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.labelLarge,
                        color = brand.onHeroMuted)
                }
            }
        }
    }
}

@Composable
private fun SurahIndex(surahs: List<Surah>, onSelect: (Surah) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) surahs
        else surahs.filter { it.name.contains(query) || it.tr.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        QuranHeader("القرآن الكريم", "المصحف — رواية حفص")

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("ابحث عن سورة…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary, unfocusedBorderColor = cs.outline
            )
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(filtered, key = { _, s -> s.id }) { _, s ->
                AppCard(Modifier.clickable { onSelect(s) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(cs.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(arabicDigits(s.id),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = cs.primary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سورة ${s.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text("${s.typeArabic} • ${arabicDigits(s.verses)} آية",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahReader(
    surah: Surah,
    verses: List<String>,
    basmala: String,
    marks: QuranMarks,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current
    // حالة علامات هذه السورة لدفع إعادة الرسم عند اللمس
    val markState = remember(surah.id) {
        mutableStateMapOf<Int, Int>().apply {
            for (a in 1..verses.size) {
                val m = marks.get(surah.id, a)
                if (m != QuranMarks.NONE) put(a, m)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        QuranHeader("سورة ${surah.name}",
            "${surah.typeArabic} • ${arabicDigits(surah.verses)} آية", onBack = onBack)

        // مفتاح العلامات
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(brand.markReview, "مراجعة")
            LegendDot(brand.markMemorized, "حفظ")
            Spacer(Modifier.weight(1f))
            Text("المس الآية لوضع العلامة",
                style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)
        ) {
            if (surah.bism) {
                item {
                    Text(
                        basmala,
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Amiri),
                        textAlign = TextAlign.Center,
                        color = cs.primary
                    )
                }
            }
            itemsIndexed(verses) { i, text ->
                val ayah = i + 1
                val mark = markState[ayah] ?: QuranMarks.NONE
                AyahRow(text, ayah, mark) {
                    val next = marks.cycle(surah.id, ayah)
                    if (next == QuranMarks.NONE) markState.remove(ayah) else markState[ayah] = next
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AyahRow(text: String, ayah: Int, mark: Int, onTap: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current
    val barColor = when (mark) {
        QuranMarks.REVIEW -> brand.markReview
        QuranMarks.MEMORIZED -> brand.markMemorized
        else -> Color.Transparent
    }
    val bg = when (mark) {
        QuranMarks.REVIEW -> brand.markReview.copy(alpha = 0.12f)
        QuranMarks.MEMORIZED -> brand.markMemorized.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onTap() }
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(barColor))
        Text(
            "$text ﴿${arabicDigits(ayah)}﴾",
            Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Amiri, fontSize = 25.sp, lineHeight = 46.sp
            ),
            textAlign = TextAlign.Justify,
            color = cs.onSurface
        )
    }
}
