package com.mrcoder20.portx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.usecase.*
import com.mrcoder20.portx.domain.ScanManager
import com.mrcoder20.portx.domain.ScannerController
import com.mrcoder20.portx.domain.getScannerController
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUIState(
    val ip: String = "",
    val startPort: String = "1",
    val endPort: String = "1024",
    val scanType: String = "TCP",
    val bannerGrabbing: Boolean = false,
    val concurrentScans: Int = 100,
    val timeout: Int = 1000,
    val allPorts: Boolean = false,
    val allProtocols: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val result: ScanResult? = null,
    val firewallStatus: String? = null,
    val anomalies: List<String> = emptyList(),
    val logs: List<String> = emptyList(),
    val error: String? = null
)

class ScanViewModel(
    private val scanPortUseCase: ScanPortUseCase,
    private val securityScoreUseCase: SecurityScoreUseCase,
    private val firewallDetectionUseCase: FirewallDetectionUseCase,
    private val anomalyDetectionUseCase: AnomalyDetectionUseCase,
    private val exportReportUseCase: ExportReportUseCase,
    private val scanRepository: com.mrcoder20.portx.domain.repository.ScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUIState())
    val uiState: StateFlow<ScanUIState> = _uiState.asStateFlow()

    private val scannerController: ScannerController = getScannerController()

    init {
        viewModelScope.launch {
            scanRepository.getLatestScan().collect { latest ->
                _uiState.update { 
                    it.copy(
                        result = latest, 
                        ip = latest?.target ?: it.ip,
                        firewallStatus = latest?.let { res -> firewallDetectionUseCase(res) },
                        anomalies = latest?.let { res -> anomalyDetectionUseCase(res) } ?: emptyList()
                    ) 
                }
            }
        }

        viewModelScope.launch {
            ScanManager.isScanning.collect { isScanning ->
                _uiState.update { it.copy(isLoading = isScanning) }
            }
        }

        viewModelScope.launch {
            ScanManager.progress.collect { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }

        viewModelScope.launch {
            ScanManager.currentResult.collect { result ->
                if (result != null) {
                    val fw = firewallDetectionUseCase(result)
                    val anom = anomalyDetectionUseCase(result)
                    _uiState.update { 
                        it.copy(
                            result = result,
                            firewallStatus = fw,
                            anomalies = anom
                        ) 
                    }
                }
            }
        }

        viewModelScope.launch {
            ScanManager.error.collect { error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error) }
                }
            }
        }
    }

    private var scanLauncherJob: Job? = null

    fun onIpChange(newIp: String) {
        _uiState.update { it.copy(ip = newIp, error = null) }
    }

    fun onStartPortChange(newPort: String) {
        _uiState.update { it.copy(startPort = newPort, error = null) }
    }

    fun onEndPortChange(newPort: String) {
        _uiState.update { it.copy(endPort = newPort, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onScanTypeChange(type: String) {
        _uiState.update { it.copy(scanType = type) }
    }

    fun toggleBannerGrabbing(enabled: Boolean) {
        _uiState.update { it.copy(bannerGrabbing = enabled) }
    }

    fun onConcurrentScansChange(count: Int) {
        _uiState.update { it.copy(concurrentScans = count) }
    }

    fun onTimeoutChange(newTimeout: Int) {
        _uiState.update { it.copy(timeout = newTimeout) }
    }

    fun toggleAllPorts(enabled: Boolean) {
        _uiState.update { it.copy(allPorts = enabled) }
    }

    fun toggleAllProtocols(enabled: Boolean) {
        _uiState.update { it.copy(allProtocols = enabled) }
    }

    fun startScan() {
        val state = _uiState.value
        if (state.isLoading) return
        
        // 1. STRICT Target Validation & Sanitization
        val rawInput = state.ip.trim()
        if (rawInput.isBlank()) {
            _uiState.update { it.copy(error = "Please enter an IP or Hostname") }
            return
        }
        val target = com.mrcoder20.portx.domain.sanitizeHost(rawInput)

        // Support IPv4, IPv6, Domain names, and Local hostnames (e.g. localhost, router, server-01)
        val ipRegex = Regex("""^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""")
        val ipv6Regex = Regex("""^[0-9a-fA-F:]+$""")
        val hostnameRegex = Regex("""^([a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?\.)*[a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?$""")
        
        val isAllNumericDotted = Regex("""^[0-9.]+$""").matches(target)
        val isValid = when {
            isAllNumericDotted -> ipRegex.matches(target)
            target.contains(":") -> ipv6Regex.matches(target) && target.count { it == ':' } >= 2
            else -> hostnameRegex.matches(target)
        }
        val isLocal = target.lowercase() == "localhost" || target == "127.0.0.1" || target == "::1" || target.startsWith("192.168.") || target.startsWith("10.") || target.startsWith("172.")
        
        if (!isValid) {
            _uiState.update { it.copy(error = "Invalid target format (e.g. 8.8.8.8, router, or example.com)") }
            return
        }

        val range = if (state.allPorts) {
            1..65535
        } else {
            try {
                val start = state.startPort.toInt()
                val end = state.endPort.toInt()
                if (start < 1 || end > 65535 || start > end) {
                    _uiState.update { it.copy(error = "Range must be 1-65535") }
                    return
                }
                start..end
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Invalid numeric port range") }
                return
            }
        }

        _uiState.update { 
            it.copy(
                isLoading = true, 
                progress = 0, 
                error = null, 
                result = null, 
                firewallStatus = null, 
                anomalies = emptyList(),
                logs = listOf("Target validated: $target")
            ) 
        }
        
        val config = com.mrcoder20.portx.data.network.ScanConfig(
            target = target,
            startPort = range.first,
            endPort = range.last,
            scanType = if (state.allProtocols) "TCP/UDP" else state.scanType,
            timeoutMs = state.timeout,
            concurrency = if (state.allPorts) (state.concurrentScans * 2).coerceAtMost(2000) else state.concurrentScans,
            rate = 10000,
            serviceDetect = state.bannerGrabbing,
            randomizePorts = true
        )

        scanLauncherJob?.cancel()
        scanLauncherJob = viewModelScope.launch {
            // Network interface check (resilient to offline / air-gapped / firewalled LANs)
            try {
                addLog("Probing network interface...")
                if (!isLocal) {
                    try {
                        val selector = SelectorManager(Dispatchers.Default)
                        try {
                            withTimeout(2500) {
                                val socket = aSocket(selector).tcp().connect(InetSocketAddress("1.1.1.1", 53)) {
                                    socketTimeout = 2000
                                }
                                try {
                                    socket.close()
                                } catch (_: Exception) {}
                                addLog("External connectivity confirmed.")
                            }
                        } finally {
                            selector.close()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Non-fatal: in isolated LANs or firewalled environments, external probe may fail.
                        addLog("Proceeding with direct target route...")
                    }
                } else {
                    addLog("Local interface active.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore unexpected probe exceptions
            }

            addLog("Engine v5.1.0 initializing...")
            delay(400)
            
            if (state.allPorts) addLog("Full port scan mode [1-65535] active")
            addLog("Operationalizing ${config.scanType} scan...")

            scannerController.startScan(config)
        }
    }

    private fun addLog(message: String) {
        _uiState.update { it.copy(logs = (it.logs + message).takeLast(50)) }
    }

    fun stopScan() {
        scanLauncherJob?.cancel()
        scannerController.stopScan()
        _uiState.update { it.copy(isLoading = false) }
    }

    fun exportReport(): String? {
        val result = _uiState.value.result ?: return null
        return exportReportUseCase(result)
    }
}
