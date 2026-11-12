package com.oman.prayertimes.ui

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oman.prayertimes.data.TasbihTexts
import com.oman.prayertimes.ui.theme.LocalBrand

/**
 * شاشة الأذكار والتسبيح: اختر صيغة من الأعلى، ثم المس الدائرة الكبيرة للعدّ.
 * عدّاد بسيط في الذاكرة لكل جلسة، مع زر تصفير.
 */
@Composable
fun AthkarScreen() {
    val phrases = TasbihTexts.list
    var selected by remember { mutableStateOf(0) }
    val counts = remember { mutableStateListOf(*Array(phrases.size) { 0 }) }
    val cs = MaterialTheme.colorScheme
    val brand = LocalBrand.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // الرأس الأخضر
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(brand.heroBrush)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp)
        ) {
            Text("الأذكار والتسبيح",
                style = MaterialTheme.typography.headlineSmall, color = brand.onHero)
            Spacer(Modifier.height(2.dp))
            Text("اختر صيغة ثم المس الدائرة للعدّ",
                style = MaterialTheme.typography.labelLarge, color = brand.onHeroMuted)
        }

        Spacer(Modifier.height(16.dp))

        // اختيار الصيغة
        LazyRow(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(phrases) { i, _ ->
                val active = i == selected
                Text(
                    "${i + 1}",
                    Modifier
                        .clip(CircleShape)
                        .background(if (active) cs.primary else cs.surfaceVariant)
                        .clickable { selected = i }
                        .size(40.dp)
                        .padding(top = 9.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (active) cs.onPrimary else cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // نص الصيغة الحالية
        AppCard(Modifier.padding(horizontal = 16.dp)) {
            Text(
                phrases[selected],
                Modifier.fillMaxWidth().padding(20.dp).animateContentSize(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = cs.onSurface
            )
        }

        Spacer(Modifier.height(28.dp))

        // العدّاد الكبير
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer)
                    .clickable { counts[selected] = counts[selected] + 1 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${counts[selected]}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.primary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // تصفير
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(cs.surfaceVariant)
                    .clickable { counts[selected] = 0 }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null,
                    tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("تصفير", style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
