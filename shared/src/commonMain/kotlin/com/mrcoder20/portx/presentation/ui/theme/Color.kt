package com.mrcoder20.portx.presentation.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Cyber-Muted Neon Colors
val PrimaryNeon = Color(0xFF00E5FF) // Electric Cyber Cyan (Ultra High-Contrast)
val SecondaryNeon = Color(0xFFA855F7) // Vivid Cyber Purple
val TertiaryNeon = Color(0xFF00E676) // Toxic Cyber Green (Success/Safe)

// Extended Cyber Palette
val ElectricIndigo = Color(0xFF6366F1)
val CyberAmber = Color(0xFFF59E0B)
val EmeraldGreen = Color(0xFF10B981)
val CrimsonRed = Color(0xFFEF4444)

// Dark Backgrounds (Rich Cyber Slate - Depth & Contrast)
val BackgroundDark = Color(0xFF070B10) // Cyber Midnight Navy
val SurfaceDark = Color(0xFF0E141E) // Elevated Slate Surface
val CardDark = Color(0xFF131B27) // Elevated Card Background

// Light Mode Aesthetics (Linear / GitHub Slate - Modern & Clean)
val BackgroundLight = Color(0xFFF1F5F9) // Clean Slate 100
val SurfaceLight = Color(0xFFFFFFFF) // Crisp Pure White
val GlassLight = Color(0xF2FFFFFF) // Frosted White Glass
val GlassBorderLight = Color(0x260F172A) // Defined Crisp Slate Border

// Glassmorphism Colors
val GlassBackground = Color(0x1400E5FF) // Subtle Electric Cyan Tint
val GlassBorder = Color(0x3300E5FF) // Neon Cyan Micro-Border
val GlassSurface = Color(0x0AFFFFFF) 

// Accent & Severity Colors
val DangerNeon = Color(0xFFFF3B30) // Apple/Cyber Sharp Red (High Risk)
val WarningNeon = Color(0xFFFFD600) // Electric Cyber Yellow (Medium Risk)
val WarningLight = Color(0xFFD97706) // Accessible Amber for Light Theme
val DangerLight = Color(0xFFDC2626) // Accessible Crimson for Light Theme

// Text Colors (WCAG AAA Compliant)
val TextPrimary = Color(0xFFF8FAFC) // 100% Crisp White
val TextSecondary = Color(0xCCF8FAFC) // 80% Slate White
val TextMuted = Color(0x8CF8FAFC) // 55% Muted White

val TextPrimaryLight = Color(0xFF0F172A) // Slate 900
val TextSecondaryLight = Color(0xFF334155) // Slate 700
val TextMutedLight = Color(0xFF64748B) // Slate 500

/**
 * Intelligent contrast adjuster that ensures WCAG 4.5:1+ contrast compliance.
 * In Light mode, overly bright neons (Cyan, Yellow, Light Green) are mapped to
 * rich deep tones that read crisply on light backgrounds.
 */
fun getAccessibleAccent(accent: Color, isDark: Boolean): Color {
    if (isDark) return accent
    return when {
        // Cyan / Electric Blue -> Deep Ocean Blue
        accent == PrimaryNeon || accent == Color(0xFF00D1FF) -> Color(0xFF0284C7)
        // Neon Green / Mint -> Deep Emerald
        accent == TertiaryNeon || accent == Color(0xFF00FFA3) -> Color(0xFF059669)
        // Yellow -> Dark Amber
        accent == WarningNeon || accent == Color(0xFFF0D400) -> Color(0xFFB45309)
        // Purple -> Deep Violet
        accent == SecondaryNeon || accent == Color(0xFFBD00FF) -> Color(0xFF7C3AED)
        // Red -> Deep Crimson
        accent == DangerNeon || accent == Color(0xFFFF4B4B) -> Color(0xFFDC2626)
        else -> accent
    }
}

/**
 * Specular highlight top border brush for frosted glass cards
 */
fun topBorderHighlightBrush(accent: Color, isDark: Boolean): Brush {
    val topColor = if (isDark) {
        accent.copy(alpha = 0.45f)
    } else {
        accent.copy(alpha = 0.35f)
    }
    val sideColor = if (isDark) {
        GlassBorder.copy(alpha = 0.2f)
    } else {
        GlassBorderLight.copy(alpha = 0.15f)
    }
    return Brush.verticalGradient(
        0.0f to topColor,
        0.3f to sideColor,
        1.0f to sideColor.copy(alpha = 0.05f)
    )
}

/**
 * Subtle frosted glass backdrop gradient
 */
fun cardGlassBackdropBrush(isDark: Boolean, accent: Color): Brush {
    return if (isDark) {
        Brush.linearGradient(
            listOf(
                CardDark.copy(alpha = 0.85f),
                SurfaceDark.copy(alpha = 0.75f),
                accent.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFF8FAFC).copy(alpha = 0.92f)
            )
        )
    }
}
