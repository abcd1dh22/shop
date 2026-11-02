package com.hawatraders.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Font family used everywhere.
 *
 * To bundle Noto Sans Bengali: copy NotoSansBengali-Regular.ttf, -Medium.ttf and -Bold.ttf into
 * app/src/main/res/font/ as noto_sans_bengali_regular.ttf / _medium.ttf / _bold.ttf, then replace
 * the line below with:
 *
 *   val AppFontFamily = FontFamily(
 *       Font(R.font.noto_sans_bengali_regular, FontWeight.Normal),
 *       Font(R.font.noto_sans_bengali_medium, FontWeight.Medium),
 *       Font(R.font.noto_sans_bengali_bold, FontWeight.Bold),
 *   )
 *
 * (and import com.hawatraders.manager.R and androidx.compose.ui.text.font.Font).
 * Until then the system font is used; Android ships a Bengali-capable font on every supported API level.
 */
val AppFontFamily: FontFamily = FontFamily.Default

private fun TextStyle.f(): TextStyle = copy(fontFamily = AppFontFamily)

private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.f(),
    displayMedium = base.displayMedium.f(),
    displaySmall = base.displaySmall.f(),
    headlineLarge = base.headlineLarge.f(),
    headlineMedium = base.headlineMedium.f(),
    headlineSmall = base.headlineSmall.f(),
    titleLarge = base.titleLarge.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, lineHeight = 32.sp),
    titleMedium = base.titleMedium.copy(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
    titleSmall = base.titleSmall.copy(fontFamily = AppFontFamily, lineHeight = 22.sp),
    bodyLarge = base.bodyLarge.copy(fontFamily = AppFontFamily, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = AppFontFamily, lineHeight = 22.sp),
    bodySmall = base.bodySmall.copy(fontFamily = AppFontFamily, lineHeight = 18.sp),
    labelLarge = base.labelLarge.copy(fontFamily = AppFontFamily, lineHeight = 22.sp),
    labelMedium = base.labelMedium.copy(fontFamily = AppFontFamily, lineHeight = 18.sp),
    labelSmall = base.labelSmall.copy(fontFamily = AppFontFamily, lineHeight = 16.sp),
)

/** Price on list cards: large and bold. */
val PriceTextStyle = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 34.sp,
)

/** Price in the detail sheet, meant to be read by a customer across the counter. */
val PriceHugeTextStyle = TextStyle(
    fontFamily = AppFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 56.sp,
    lineHeight = 72.sp,
)
