package com.oman.prayertimes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oman.prayertimes.ui.theme.LocalBrand

/** بطاقة بيضاء بحدّ رمادي رفيع — أساس كل الأقسام */
@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
        content = content
    )
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        Modifier.padding(start = 64.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline
    )
}

/** شارة صغيرة ملوّنة (التالية / الآن / مفعّل …) */
@Composable
fun AppBadge(text: String, container: Color, content: Color) {
    Text(
        text,
        Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content
    )
}

/**
 * صف صلاة واحد داخل بطاقة المواقيت.
 * - التالية: خلفية خضراء فاتحة + شريط أحمر جانبي + شارة حمراء
 * - الحالية: نقطة خضراء و«الآن»
 * - الشروق: رمادي لأنه ليس صلاة
 */
@Composable
fun PrayerListRow(
    label: String,
    time: String,
    icon: ImageVector,
    isNext: Boolean,
    isCurrent: Boolean,
    muted: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val accent = LocalBrand.current.accent
    val nameColor = when {
        isNext -> cs.primary
        muted -> cs.onSurfaceVariant
        else -> cs.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(if (isNext) cs.primaryContainer else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // الشريط الأحمر الجانبي للصلاة التالية
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (isNext) accent else Color.Transparent)
        )
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isNext) cs.primary else cs.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = when {
                    isNext -> cs.onPrimary
                    muted -> cs.onSurfaceVariant
                    else -> cs.primary
                },
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                color = nameColor
            )
            if (isNext) AppBadge("التالية", accent, Color.White)
            else if (isCurrent) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(cs.primary))
                Text("الآن", style = MaterialTheme.typography.labelMedium, color = cs.primary)
            }
        }
        Text(
            time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isNext) cs.primary else if (muted) cs.onSurfaceVariant else cs.onSurface
        )
        Spacer(Modifier.width(16.dp))
    }
}
