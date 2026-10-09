package com.mrcoder20.portx.domain

import androidx.compose.ui.graphics.Color
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mrcoder20.portx.data.local.AppDatabase
import com.mrcoder20.portx.data.local.ScanEntity
import com.mrcoder20.portx.data.local.listOfIntAdapter
import com.mrcoder20.portx.data.local.mapIntStringAdapter
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsManagerJvmTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var manager: SettingsManager

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(
            driver = driver,
            ScanEntityAdapter = ScanEntity.Adapter(
                openPortsAdapter = listOfIntAdapter,
                portBannersAdapter = mapIntStringAdapter,
                portServicesAdapter = mapIntStringAdapter
            )
        )
        manager = SettingsManager(database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun <T> waitForNonNull(timeoutMs: Long = 1500L, block: () -> T?): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val res = block()
            if (res != null) return res
            Thread.sleep(25)
        }
        return block() ?: throw AssertionError("Condition not met within ${timeoutMs}ms")
    }

    @Test
    fun testDefaultSettings() {
        val initial = manager.settings.value
        assertEquals("en", initial.language)
        assertEquals("DARK", initial.theme)
        assertEquals(Color(0xFF00D1FF), initial.accentColor)
    }

    @Test
    fun testUpdateLanguage() {
        manager.updateLanguage("fa")
        assertEquals("fa", manager.settings.value.language)

        val dbRow = waitForNonNull {
            database.appDatabaseQueries.getSettings().executeAsOneOrNull()?.takeIf { it.language == "fa" }
        }
        assertEquals("fa", dbRow.language)

        val restoredManager = SettingsManager(database)
        val restored = waitForNonNull {
            restoredManager.settings.value.takeIf { it.language == "fa" }
        }
        assertEquals("fa", restored.language)
    }

    @Test
    fun testUpdateTheme() {
        manager.updateTheme("LIGHT")
        assertEquals("LIGHT", manager.settings.value.theme)

        val dbRow = waitForNonNull {
            database.appDatabaseQueries.getSettings().executeAsOneOrNull()?.takeIf { it.theme == "LIGHT" }
        }
        assertEquals("LIGHT", dbRow.theme)

        val restoredManager = SettingsManager(database)
        val restored = waitForNonNull {
            restoredManager.settings.value.takeIf { it.theme == "LIGHT" }
        }
        assertEquals("LIGHT", restored.theme)
    }

    @Test
    fun testUpdateAccentColor() {
        val customColor = Color(0xFFFF5722)
        manager.updateAccentColor(customColor)
        assertEquals(customColor, manager.settings.value.accentColor)

        val dbRow = waitForNonNull {
            database.appDatabaseQueries.getSettings().executeAsOneOrNull()
        }
        val restoredManager = SettingsManager(database)
        val restored = waitForNonNull {
            restoredManager.settings.value.takeIf { it.accentColor == customColor }
        }
        assertEquals(customColor, restored.accentColor)
    }
}
