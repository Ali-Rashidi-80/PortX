package com.mrcoder20.portx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcoder20.portx.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToolsUIState(
    val pingResults: List<PingResult> = emptyList(),
    val pingMode: String = "ICMP", // "ICMP" or "TCP"
    val pingPort: Int = 80,
    val dnsResult: DnsResolutionResult? = null,
    val dnsFilterType: String = "ALL", // "ALL", "A", "AAAA", "MX", "TXT", "NS", "CNAME", "SOA", "CAA", "PTR"
    val dnsViewMode: String = "CARDS", // "CARDS" or "RAW"
    val whoisResult: String? = null,
    val localIp: LocalIpInfo? = null,
    val subnetInfo: SubnetInfo? = null,
    val publicIp: String? = null,
    val isLoading: Boolean = false,
    val activeTool: String = "LOCAL",
    val target: String = "",
    val error: String? = null,
    val snackbarMessage: String? = null
)

class ToolsViewModel : ViewModel() {
    private val networkTools = getNetworkTools()
    private val clipboardManager = getClipboardManager()
    
    private val _uiState = MutableStateFlow(ToolsUIState())
    val uiState: StateFlow<ToolsUIState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    init {
        refreshLocalInfo()
    }

    fun onTargetChange(newTarget: String) {
        _uiState.update { it.copy(target = newTarget, error = null) }
    }

    fun setDnsFilterType(type: String) {
        _uiState.update { it.copy(dnsFilterType = type) }
    }

    fun setDnsViewMode(mode: String) {
        _uiState.update { it.copy(dnsViewMode = mode) }
    }

    fun setPingMode(mode: String) {
        _uiState.update { it.copy(pingMode = mode) }
    }

    fun setPingPort(port: Int) {
        _uiState.update { it.copy(pingPort = port.coerceIn(1, 65535)) }
    }

    fun selectTool(tool: String) {
        stopActiveTool()
        _uiState.update { it.copy(activeTool = tool, error = null, pingResults = emptyList(), dnsResult = null, whoisResult = null) }
        if (tool == "LOCAL") {
            if (_uiState.value.localIp == null || _uiState.value.publicIp == null) {
                refreshLocalInfo()
            }
        }
    }

    fun refreshLocalInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val info = networkTools.getLocalIpInfo()
                val subnet = calculateSubnetInfo(info.ipAddress)
                _uiState.update { it.copy(localIp = info, subnetInfo = subnet) }
                
                val pubIp = networkTools.getPublicIp()
                _uiState.update { it.copy(publicIp = pubIp) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep existing info if refresh fails
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun runPing() {
        val rawInput = _uiState.value.target.trim()
        if (rawInput.isBlank()) {
            _uiState.update { it.copy(error = "err_enter_ip") }
            return
        }
        val target = sanitizeHost(rawInput)
        if (!isValidTarget(target)) {
            _uiState.update { it.copy(error = "err_invalid_target") }
            return
        }
        
        stopActiveTool()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pingResults = emptyList(), error = null) }
            try {
                val isTcp = _uiState.value.pingMode == "TCP"
                val port = _uiState.value.pingPort
                val pingFlow = if (isTcp) {
                    networkTools.pingTcp(target, port)
                } else {
                    networkTools.ping(target)
                }
                pingFlow.collect { res ->
                    _uiState.update { it.copy(pingResults = it.pingResults + res) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "err_engine_failure") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun runDnsLookup() {
        val rawInput = _uiState.value.target.trim()
        if (rawInput.isBlank()) {
            _uiState.update { it.copy(error = "err_enter_dns") }
            return
        }
        val target = sanitizeHost(rawInput)
        if (!isValidTarget(target)) {
            _uiState.update { it.copy(error = "err_invalid_target") }
            return
        }

        stopActiveTool()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dnsResult = null, error = null) }
            try {
                val result = networkTools.dnsResolve(target)
                if (result.records.isEmpty()) {
                    _uiState.update { it.copy(error = "err_dns_unreachable", dnsResult = result) }
                } else {
                    _uiState.update { it.copy(dnsResult = result) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "err_engine_failure") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun runWhois() {
        val rawInput = _uiState.value.target.trim()
        if (rawInput.isBlank()) {
            _uiState.update { it.copy(error = "err_enter_whois") }
            return
        }
        val target = sanitizeHost(rawInput)
        if (!isValidTarget(target)) {
            _uiState.update { it.copy(error = "err_invalid_target") }
            return
        }

        stopActiveTool()
        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, whoisResult = null, error = null) }
            try {
                val result = networkTools.whois(target)
                _uiState.update { it.copy(whoisResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "err_whois_unreachable") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun stopActiveTool() {
        activeJob?.cancel()
        activeJob = null
        _uiState.update { it.copy(isLoading = false) }
    }

    fun clearResults() {
        stopActiveTool()
        _uiState.update { it.copy(pingResults = emptyList(), dnsResult = null, whoisResult = null, error = null) }
    }

    fun copyResultsToClipboard() {
        val state = _uiState.value
        val text = when (state.activeTool) {
            "PING" -> state.pingResults.joinToString("\n") { it.message }
            "DNS" -> state.dnsResult?.rawDigOutput ?: state.dnsResult?.records?.joinToString("\n") { "${it.type}\t${it.name}\t${it.value}" } ?: ""
            "WHOIS" -> state.whoisResult ?: ""
            "LOCAL" -> state.localIp?.let { "Internal IP: ${it.ipAddress}\nInterface: ${it.interfaceName}\nSubnet: ${state.subnetInfo?.networkAddress}/${state.subnetInfo?.cidr}\nPublic IP: ${state.publicIp ?: "Not available"}" } ?: (state.publicIp?.let { "Public IP: $it" } ?: "")
            else -> ""
        }
        if (text.isNotBlank()) {
            clipboardManager.copyToClipboard(text)
            showSnackbar("report_copied_clipboard")
        } else {
            showSnackbar("no_output_to_copy")
        }
    }

    fun copyIndividualResult(text: String) {
        clipboardManager.copyToClipboard(text)
        showSnackbar("copied:$text")
    }

    private var snackbarJob: Job? = null

    private fun showSnackbar(message: String) {
        snackbarJob?.cancel()
        snackbarJob = viewModelScope.launch {
            _uiState.update { it.copy(snackbarMessage = message) }
            kotlinx.coroutines.delay(2500)
            _uiState.update { it.copy(snackbarMessage = null) }
        }
    }
}
