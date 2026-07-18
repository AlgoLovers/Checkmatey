package com.checkmatey.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.checkmatey.R

// Pretendard (OFL) — a Korean + Latin typeface with high finish, so Hangul UI copy stops looking
// like the default system font. Only the glyphs Checkmatey actually renders are subset into the
// bundled ttf (~0.23MB, tools/fonts/subset.py); it's a variable font, so one file covers every
// weight. Missing glyphs fall back to the system font automatically.
@OptIn(ExperimentalTextApi::class)
private fun pretendard(weight: Int) = Font(
    resId = R.font.pretendard,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Pretendard = FontFamily(
    pretendard(400),
    pretendard(500),
    pretendard(600),
    pretendard(700),
)

// Material 3 defaults with Pretendard swapped in, plus a touch more size/weight on the styles we
// lean on (headlines, titles, body) so screens read comfortably.
private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Pretendard),
    displayMedium = base.displayMedium.copy(fontFamily = Pretendard),
    displaySmall = base.displaySmall.copy(fontFamily = Pretendard),
    headlineLarge = base.headlineLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = base.headlineSmall.copy(fontFamily = Pretendard, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = base.titleMedium.copy(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Pretendard),
    bodyLarge = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = base.bodySmall.copy(fontFamily = Pretendard),
    labelLarge = base.labelLarge.copy(fontFamily = Pretendard, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Pretendard),
    labelSmall = base.labelSmall.copy(fontFamily = Pretendard),
)
