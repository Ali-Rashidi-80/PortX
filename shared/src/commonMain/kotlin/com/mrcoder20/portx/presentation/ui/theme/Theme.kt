package com.mrcoder20.portx.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.mrcoder20.portx.domain.SettingsManager
import org.koin.compose.koinInject

@Composable
fun PortXTheme(
    settingsManager: SettingsManager = koinInject(),
    content: @Composable () -> Unit
) {
    val settingsState by settingsManager.settings.collectAsState()
    val rawAccent = settingsState.accentColor
    val isDark = settingsState.theme == "DARK"
    val accessibleAccent = getAccessibleAccent(rawAccent, isDark)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accessibleAccent,
            secondary = accessibleAccent.copy(alpha = 0.75f),
            background = BackgroundDark,
            surface = SurfaceDark,
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = accessibleAccent,
            secondary = accessibleAccent.copy(alpha = 0.8f),
            background = BackgroundLight,
            surface = SurfaceLight,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A)
        )
    }

    val layoutDirection = if (settingsState.language == "fa" || settingsState.language == "ar") {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    } else {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    }

    // Provide accessible accent, raw accent, app settings and directional typography
    CompositionLocalProvider(
        LocalAccentColor provides accessibleAccent,
        LocalRawAccentColor provides rawAccent,
        LocalAppSettings provides settingsState,
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

val LocalAccentColor = compositionLocalOf { Color(0xFF00D1FF) }
val LocalRawAccentColor = compositionLocalOf { Color(0xFF00D1FF) }
val LocalAppSettings = compositionLocalOf { com.mrcoder20.portx.domain.AppSettings() }
