package com.mrcoder20.portx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.repository.ScanRepository
import com.mrcoder20.portx.domain.getFileSharer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsUIState(
    val scans: List<ScanResult> = emptyList(),
    val exportFormat: String = "JSON",
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)

class ReportsViewModel(
    private val scanRepository: ScanRepository,
    private val exportReportUseCase: com.mrcoder20.portx.domain.usecase.ExportReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUIState())
    val uiState: StateFlow<ReportsUIState> = _uiState.asStateFlow()

    private val fileSharer = getFileSharer()

    fun onFormatChange(format: String) {
        _uiState.update { it.copy(exportFormat = format) }
    }

    fun getExportContent(scan: ScanResult, format: String = _uiState.value.exportFormat): String {
        return exportReportUseCase(scan, format)
    }

    fun shareScan(scan: ScanResult, format: String = _uiState.value.exportFormat) {
        try {
            val (_, content, fileName, mimeType) = prepareExport(scan, format)
            fileSharer.shareFile(content, fileName, mimeType)
        } catch (e: Exception) {
            showSnackbar("Share failed: ${e.message ?: "Unknown error"}")
        }
    }

    fun downloadScan(scan: ScanResult, format: String = _uiState.value.exportFormat) {
        try {
            val (_, content, fileName, mimeType) = prepareExport(scan, format)
            val path = fileSharer.downloadFile(content, fileName, mimeType)
            if (path != null) {
                showSnackbar("Saved to: $path")
            } else {
                showSnackbar("Download failed")
            }
        } catch (e: Exception) {
            showSnackbar("Download failed: ${e.message ?: "Unknown error"}")
        }
    }

    fun saveScanAs(scan: ScanResult, format: String = _uiState.value.exportFormat) {
        shareScan(scan, format)
    }

    fun quickSaveScan(scan: ScanResult, format: String = _uiState.value.exportFormat) {
        downloadScan(scan, format)
    }

    var snackbarJob: Job? = null
    private var loadScansJob: Job? = null

    fun showSnackbar(message: String) {
        snackbarJob?.cancel()
        snackbarJob = viewModelScope.launch {
            _uiState.update { it.copy(snackbarMessage = message) }
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun prepareExport(scan: ScanResult, targetFormat: String = _uiState.value.exportFormat): ExportData {
        val format = targetFormat.uppercase()
        val content = exportReportUseCase(scan, format)
        val extension = if (format == "MD") "md" else format.lowercase()
        val sanitizedTarget = scan.target.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "PortX_Report_${sanitizedTarget}_${scan.timestamp}.$extension"
        val mimeType = when (format) {
            "CSV" -> "text/csv"
            "MD" -> "text/markdown"
            else -> "application/json"
        }
        return ExportData(format, content, fileName, mimeType)
    }

    private data class ExportData(val format: String, val content: String, val fileName: String, val mimeType: String)

    init {
        loadScans()
    }

    fun loadScans() {
        loadScansJob?.cancel()
        loadScansJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                scanRepository.getAllScans().collect { scans ->
                    _uiState.update { it.copy(scans = scans, isLoading = false) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                showSnackbar("Failed to load records: ${e.message}")
            }
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            try {
                scanRepository.deleteScan(id)
                showSnackbar("Scan report deleted")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                showSnackbar("Failed to delete record: ${e.message}")
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                scanRepository.deleteAllScans()
                showSnackbar("All scan reports cleared")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                showSnackbar("Failed to clear history: ${e.message}")
            }
        }
    }
}
