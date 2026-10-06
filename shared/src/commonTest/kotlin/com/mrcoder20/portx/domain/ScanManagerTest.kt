package com.mrcoder20.portx.domain

import com.mrcoder20.portx.domain.model.ScanResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScanManagerTest {

    @BeforeTest
    fun setup() {
        ScanManager.setScanning(false)
        ScanManager.updateProgress(0)
        ScanManager.setError(null)
    }

    @Test
    fun testInitialOrResetState() {
        assertFalse(ScanManager.isScanning.value)
        assertEquals(0, ScanManager.progress.value)
        assertNull(ScanManager.error.value)
    }

    @Test
    fun testSetScanningResetsState() {
        ScanManager.updateProgress(50)
        ScanManager.setError("Previous error")

        ScanManager.setScanning(true)

        assertTrue(ScanManager.isScanning.value)
        assertEquals(0, ScanManager.progress.value, "Starting a scan must reset progress to 0")
        assertNull(ScanManager.error.value, "Starting a scan must clear previous error state")
        assertNull(ScanManager.currentResult.value, "Starting a scan must clear previous result")
    }

    @Test
    fun testProgressUpdate() {
        ScanManager.updateProgress(75)
        assertEquals(75, ScanManager.progress.value)

        ScanManager.updateProgress(100)
        assertEquals(100, ScanManager.progress.value)
    }

    @Test
    fun testSetResult() {
        val result = ScanResult(
            target = "127.0.0.1",
            openPorts = listOf(80, 443),
            timestamp = 1000L,
            securityScore = 95
        )
        ScanManager.setResult(result)
        assertEquals(result, ScanManager.currentResult.value)
    }

    @Test
    fun testSetErrorStopsScanning() {
        ScanManager.setScanning(true)
        assertTrue(ScanManager.isScanning.value)

        ScanManager.setError("Network route unreachable")

        assertEquals("Network route unreachable", ScanManager.error.value)
        assertFalse(ScanManager.isScanning.value, "Encountering an error must automatically stop scanning state")
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testUiEventEmission() = runTest(kotlinx.coroutines.test.UnconfinedTestDispatcher()) {
        var received: ScanUIEvent? = null
        backgroundScope.launch {
            ScanManager.uiEvents.collect {
                received = it
            }
        }
        ScanManager.triggerUiEvent(ScanUIEvent.RequestNotificationPermission)
        assertEquals(ScanUIEvent.RequestNotificationPermission, received)
    }
}
