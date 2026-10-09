package com.mrcoder20.portx.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getPortXTypography(lang: String): Typography {
    val isPersian = lang == "fa"
    // Persian script requires larger line-height for diacritics, dots, and ligatures to prevent baseline clipping
    val bodyLineHeightMultiplier = if (isPersian) 1.65f else 1.45f
    val titleLineHeightMultiplier = if (isPersian) 1.5f else 1.35f

    return Typography(
        headlineLarge = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            lineHeight = (28 * titleLineHeightMultiplier).sp,
            letterSpacing = if (isPersian) 0.sp else (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = (22 * titleLineHeightMultiplier).sp,
            letterSpacing = if (isPersian) 0.sp else (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = (18 * titleLineHeightMultiplier).sp
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = (16 * titleLineHeightMultiplier).sp
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = (14 * titleLineHeightMultiplier).sp
        ),
        titleSmall = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = (13 * titleLineHeightMultiplier).sp
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = (15 * bodyLineHeightMultiplier).sp
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = (13 * bodyLineHeightMultiplier).sp
        ),
        bodySmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = (12 * bodyLineHeightMultiplier).sp
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = (13 * bodyLineHeightMultiplier).sp
        ),
        labelMedium = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = (11 * bodyLineHeightMultiplier).sp
        ),
        labelSmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = (10 * bodyLineHeightMultiplier).sp
        )
    )
}
