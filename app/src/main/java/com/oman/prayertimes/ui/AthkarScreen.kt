package com.oman.prayertimes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oman.prayertimes.data.AthkarCategory
import com.oman.prayertimes.data.AthkarData
import com.oman.prayertimes.data.Zekr
import com.oman.prayertimes.ui.theme.LocalBrand

@Composable
fun AthkarScreen() {
    val context = LocalContext.current
    val categories = remember { AthkarData.categories(context) }
    var selected by remember { mutableStateOf<AthkarCategory?>(null) }

    val current = selected
    if (current == null) {
        CategoriesList(categories) { selected = it }
    } else {
        BackHandler { selected = null }
        CategoryDetail(
            category = current,
            athkar = remember(current.id) { AthkarData.athkarOf(context, current.id) },
            onBack = { selected = null }
        )
    }
}

@Composable
private fun HeroHeader(title: String, subtitle: String?, onBack: (() -> Unit)? = null) {
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
private fun CategoriesList(
    categories: List<AthkarCategory>,
    onSelect: (AthkarCategory) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) categories
        else categories.filter { it.name.contains(query) || it.nameShort.contains(query) }
    }

    Column(Modifier.fillMaxSize()) {
        HeroHeader("الأذكار", "حصن المسلم — ${categories.size} بابًا")

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("ابحث عن باب…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = cs.outline
            )
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { cat ->
                AppCard(Modifier.clickable { onSelect(cat) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cat.nameShort,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDetail(
    category: AthkarCategory,
    athkar: List<Zekr>,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        HeroHeader(category.nameShort, "${athkar.size} ذِكر", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(athkar, key = { it.id }) { ZekrCard(it) }
        }
    }
}

@Composable
private fun ZekrCard(zekr: Zekr) {
    val cs = MaterialTheme.colorScheme
    var remaining by remember(zekr.id) { mutableStateOf(zekr.count) }
    val done = remaining == 0

    AppCard {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                zekr.text.ifBlank { zekr.short },
                style = MaterialTheme.typography.titleMedium.copy(lineHeight =
                    MaterialTheme.typography.titleMedium.lineHeight * 1.35f),
                color = cs.onSurface
            )
            if (zekr.ref.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    zekr.ref,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (zekr.count > 1) {
                    Text("التكرار: ${zekr.count}", style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                // عدّاد: المس لتنقص حتى الانتهاء
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (done) cs.primary else cs.primaryContainer)
                        .clickable(enabled = !done) { if (remaining > 0) remaining-- }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (done) {
                        Icon(Icons.Default.Check, null, tint = cs.onPrimary,
                            modifier = Modifier.size(18.dp))
                        Text("تمّ", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = cs.onPrimary)
                    } else {
                        Text("$remaining", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = cs.primary)
                    }
                }
            }
        }
    }
}
