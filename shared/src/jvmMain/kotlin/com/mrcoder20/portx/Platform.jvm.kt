package com.mrcoder20.portx

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mrcoder20.portx.data.local.AppDatabase
import java.io.File

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun createDatabaseDriver(passphrase: String?): SqlDriver {
    val userHome = System.getProperty("user.home")
    val appDir = File(userHome, ".portx")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    val databaseFile = File(appDir, "portx.db")
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    try {
        driver.execute(null, "PRAGMA journal_mode = WAL;", 0)
        driver.execute(null, "PRAGMA busy_timeout = 5000;", 0)
        driver.execute(null, "PRAGMA synchronous = NORMAL;", 0)

        val currentVersion = driver.executeQuery(null, "PRAGMA user_version;", { cursor ->
            app.cash.sqldelight.db.QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L)
        }, 0).value

        if (currentVersion == 0L) {
            AppDatabase.Schema.create(driver)
            driver.execute(null, "PRAGMA user_version = ${AppDatabase.Schema.version};", 0)
        } else if (currentVersion < AppDatabase.Schema.version) {
            AppDatabase.Schema.migrate(driver, currentVersion, AppDatabase.Schema.version)
            driver.execute(null, "PRAGMA user_version = ${AppDatabase.Schema.version};", 0)
        }
    } catch (e: Exception) {
        try {
            AppDatabase.Schema.create(driver)
        } catch (_: Exception) {}
    }
    return driver
}
