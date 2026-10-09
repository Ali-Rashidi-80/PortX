package com.mrcoder20.portx.presentation

import com.mrcoder20.portx.data.network.ScanConfig
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.repository.ScanRepository
import com.mrcoder20.portx.domain.usecase.ExportReportUseCase
import com.mrcoder20.portx.presentation.viewmodel.ReportsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private class MockScanRepository : ScanRepository {
        val scansFlow = MutableStateFlow<List<ScanResult>>(emptyList())
        val deletedIds = mutableListOf<Long>()
        var clearedAll = false

        override fun getAllScans(): Flow<List<ScanResult>> = scansFlow
        override fun getScansByTarget(target: String): Flow<List<ScanResult>> = emptyFlow()
        override fun getLatestScan(): Flow<ScanResult?> = emptyFlow()

        override suspend fun scanPorts(config: ScanConfig, onProgress: (Int) -> Unit): ScanResult {
            throw UnsupportedOperationException()
        }

        override suspend fun saveScan(scan: ScanResult) {
            scansFlow.value = scansFlow.value + scan
        }

        override suspend fun deleteScan(id: Long) {
            deletedIds.add(id)
            scansFlow.value = scansFlow.value.filter { it.id != id }
        }

        override suspend fun deleteAllScans() {
            clearedAll = true
            scansFlow.value = emptyList()
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: MockScanRepository
    private lateinit var viewModel: ReportsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = MockScanRepository()
        viewModel = ReportsViewModel(mockRepo, ExportReportUseCase())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialFormatAndLoadScans() = runTest(testDispatcher) {
        val scan1 = ScanResult(id = 1L, target = "192.168.1.1", openPorts = listOf(80), timestamp = 1000L, securityScore = 90)
        val scan2 = ScanResult(id = 2L, target = "192.168.1.2", openPorts = listOf(443), timestamp = 2000L, securityScore = 85)
        mockRepo.scansFlow.value = listOf(scan1, scan2)

        advanceUntilIdle()

        assertEquals("JSON", viewModel.uiState.value.exportFormat)
        assertEquals(2, viewModel.uiState.value.scans.size)
        assertEquals(scan1, viewModel.uiState.value.scans[0])
    }

    @Test
    fun testOnFormatChange() {
        viewModel.onFormatChange("CSV")
        assertEquals("CSV", viewModel.uiState.value.exportFormat)

        viewModel.onFormatChange("MD")
        assertEquals("MD", viewModel.uiState.value.exportFormat)
    }

    @Test
    fun testDeleteScan() = runTest(testDispatcher) {
        val scan = ScanResult(id = 42L, target = "10.0.0.1", openPorts = listOf(22), timestamp = 5000L, securityScore = 95)
        mockRepo.scansFlow.value = listOf(scan)
        advanceUntilIdle()

        viewModel.deleteScan(42L)
        advanceUntilIdle()

        assertTrue(mockRepo.deletedIds.contains(42L))
        assertEquals(0, viewModel.uiState.value.scans.size)
    }

    @Test
    fun testClearAll() = runTest(testDispatcher) {
        val scan = ScanResult(id = 1L, target = "10.0.0.1", openPorts = listOf(80), timestamp = 5000L, securityScore = 95)
        mockRepo.scansFlow.value = listOf(scan)
        advanceUntilIdle()

        viewModel.clearAll()
        advanceUntilIdle()

        assertTrue(mockRepo.clearedAll)
        assertEquals(0, viewModel.uiState.value.scans.size)
    }
}
