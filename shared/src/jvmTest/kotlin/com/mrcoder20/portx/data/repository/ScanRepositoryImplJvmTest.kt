package com.mrcoder20.portx.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mrcoder20.portx.data.local.AppDatabase
import com.mrcoder20.portx.data.local.ScanEntity
import com.mrcoder20.portx.data.local.listOfIntAdapter
import com.mrcoder20.portx.data.local.mapIntStringAdapter
import com.mrcoder20.portx.data.network.PortScanner
import com.mrcoder20.portx.domain.model.ScanResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanRepositoryImplJvmTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var repository: ScanRepositoryImpl

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
        repository = ScanRepositoryImpl(PortScanner(), database)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testSaveAndRetrieveScans() = runTest {
        val scan = ScanResult(
            target = "192.168.1.100",
            openPorts = listOf(80, 443),
            portBanners = mapOf(80 to "Apache 2.4", 443 to "OpenSSL 3.0"),
            portServices = mapOf(80 to "http", 443 to "https"),
            timestamp = 1000L,
            securityScore = 85,
            deviceName = "Core Router",
            osFingerprint = "Linux 6.8",
            scanType = "TCP",
            bannerGrabbing = true,
            concurrentScans = 100,
            timeout = 500
        )

        repository.saveScan(scan)

        val allScans = repository.getAllScans().first()
        assertEquals(1, allScans.size)
        val retrieved = allScans[0]
        assertEquals("192.168.1.100", retrieved.target)
        assertEquals(listOf(80, 443), retrieved.openPorts)
        assertEquals("Apache 2.4", retrieved.portBanners[80])
        assertEquals("http", retrieved.portServices[80])
        assertEquals("Core Router", retrieved.deviceName)
        assertEquals("Linux 6.8", retrieved.osFingerprint)
    }

    @Test
    fun testGetScansByTarget() = runTest {
        val scan1 = ScanResult(target = "10.0.0.1", openPorts = listOf(22), timestamp = 1000L, securityScore = 90)
        val scan2 = ScanResult(target = "10.0.0.2", openPorts = listOf(80), timestamp = 2000L, securityScore = 90)
        repository.saveScan(scan1)
        repository.saveScan(scan2)

        val targetScans = repository.getScansByTarget("10.0.0.1").first()
        assertEquals(1, targetScans.size)
        assertEquals("10.0.0.1", targetScans[0].target)
    }

    @Test
    fun testGetLatestScan() = runTest {
        val scan1 = ScanResult(target = "10.0.0.1", openPorts = listOf(22), timestamp = 1000L, securityScore = 90)
        val scan2 = ScanResult(target = "10.0.0.2", openPorts = listOf(80), timestamp = 2000L, securityScore = 90)
        repository.saveScan(scan1)
        repository.saveScan(scan2)

        val latest = repository.getLatestScan().first()
        assertNotNull(latest)
        assertEquals("10.0.0.2", latest.target)
    }

    @Test
    fun testDeleteScanAndClearAll() = runTest {
        val scan = ScanResult(target = "10.0.0.1", openPorts = listOf(80), timestamp = 1000L, securityScore = 90)
        repository.saveScan(scan)
        val inserted = repository.getAllScans().first().first()
        val id = checkNotNull(inserted.id)
        repository.deleteScan(id)
        val emptyAfterDelete = repository.getAllScans().first()
        assertTrue(emptyAfterDelete.isEmpty())

        repository.saveScan(scan)
        repository.deleteAllScans()
        val emptyAfterClearAll = repository.getAllScans().first()
        assertTrue(emptyAfterClearAll.isEmpty())
    }
}
