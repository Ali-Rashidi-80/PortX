package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.data.network.ScanConfig
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanPortUseCaseTest {

    private class FakeScanRepository : ScanRepository {
        var scanCalled = false
        var saveCalled = false
        var savedScan: ScanResult? = null

        override suspend fun scanPorts(config: ScanConfig, onProgress: (Int) -> Unit): ScanResult {
            scanCalled = true
            onProgress(100)
            return ScanResult(
                target = config.target,
                openPorts = listOf(80, 443),
                timestamp = 123456L,
                securityScore = 90
            )
        }

        override suspend fun saveScan(scan: ScanResult) {
            saveCalled = true
            savedScan = scan
        }

        override fun getAllScans(): Flow<List<ScanResult>> = emptyFlow()
        override fun getScansByTarget(target: String): Flow<List<ScanResult>> = emptyFlow()
        override fun getLatestScan(): Flow<ScanResult?> = emptyFlow()
        override suspend fun deleteScan(id: Long) {}
        override suspend fun deleteAllScans() {}
    }

    @Test
    fun testScanPortUseCaseOrchestration() = runTest {
        val fakeRepo = FakeScanRepository()
        val useCase = ScanPortUseCase(fakeRepo)

        val config = ScanConfig(target = "10.0.0.1", startPort = 80, endPort = 443)
        var reportedProgress = 0

        val result = useCase(config, onProgress = { reportedProgress = it })

        assertTrue(fakeRepo.scanCalled, "UseCase must trigger repository scanPorts")
        assertTrue(fakeRepo.saveCalled, "UseCase must automatically persist scan result")
        assertEquals(100, reportedProgress, "Progress callback must be forwarded")
        assertEquals(result, fakeRepo.savedScan, "Persisted scan must match returned result")
        assertEquals("10.0.0.1", result.target)
    }
}
