package com.oman.prayertimes.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.oman.prayertimes.R

/**
 * خط القاهرة (Cairo) — خط عربي حديث مرخّص OFL، مدمج داخل التطبيق.
 * الأوزان مسجّلة بأوزانها الصحيحة فيختار Compose المناسب تلقائيًا
 * حسب FontWeight في كل نمط نصّي.
 */
val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold)
)

/** خط أميري (Amiri) — خط نسخ كلاسيكي لعرض نص القرآن العثماني، مرخّص OFL. */
val Amiri = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)
