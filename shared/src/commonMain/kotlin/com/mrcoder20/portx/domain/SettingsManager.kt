package com.mrcoder20.portx.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mrcoder20.portx.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppSettings(
    val language: String = "en",
    val theme: String = "DARK",
    val accentColor: Color = Color(0xFF00D1FF)
)

class SettingsManager(private val database: AppDatabase) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val queries = database.appDatabaseQueries

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            try {
                val dbSettings = queries.getSettings().executeAsOneOrNull()
                if (dbSettings != null) {
                    val rawArgb = dbSettings.accentColor.toInt()
                    val restoredColor = if ((rawArgb ushr 24) == 0) Color(0xFF00D1FF) else Color(rawArgb)
                    _settings.update {
                        it.copy(
                            language = dbSettings.language,
                            theme = dbSettings.theme,
                            accentColor = restoredColor
                        )
                    }
                }
            } catch (_: Throwable) {
                // Defensive fallback to default AppSettings if database is locked or unreadable
            }
        }
    }

    fun updateLanguage(lang: String) {
        _settings.update { it.copy(language = lang) }
        saveToDb()
    }

    fun updateTheme(theme: String) {
        _settings.update { it.copy(theme = theme) }
        saveToDb()
    }

    fun updateAccentColor(color: Color) {
        _settings.update { it.copy(accentColor = color) }
        saveToDb()
    }

    private fun saveToDb() {
        scope.launch {
            try {
                val current = _settings.value
                queries.upsertSettings(
                    language = current.language,
                    theme = current.theme,
                    accentColor = current.accentColor.toArgb().toLong()
                )
            } catch (_: Throwable) {
                // Defensive catch to prevent unhandled SQLite write errors from crashing the coroutine scope
            }
        }
    }
}
