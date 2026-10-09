package com.mrcoder20.portx.domain

import com.mrcoder20.portx.data.network.ScanConfig
import com.mrcoder20.portx.domain.usecase.ScanPortUseCase
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class JvmScannerController : ScannerController, KoinComponent {
    private val scanPortUseCase: ScanPortUseCase by inject()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var scanJob: Job? = null

    override fun startScan(config: ScanConfig) {
        scanJob?.cancel()
        scanJob = scope.launch {
            ScanManager.setScanning(true)
            try {
                val result = scanPortUseCase(config) { progress ->
                    ScanManager.updateProgress(progress)
                }
                
                ScanManager.setResult(result)
                try {
                    if (java.awt.SystemTray.isSupported()) {
                        val tray = java.awt.SystemTray.getSystemTray()
                        val trayIcons = tray.trayIcons
                        val openPortsText = if (result.openPorts.isNotEmpty()) {
                            "${result.openPorts.size} open (${result.openPorts.take(6).joinToString(", ")})"
                        } else "No open ports"
                        val grade = when {
                            result.securityScore >= 90 -> "A+"
                            result.securityScore >= 75 -> "A"
                            result.securityScore >= 50 -> "B"
                            result.securityScore >= 25 -> "C"
                            else -> "F"
                        }
                        if (trayIcons.isNotEmpty()) {
                            trayIcons[0].displayMessage(
                                "PortX: Scan Complete",
                                "Target: ${result.target} | $openPortsText | Score: ${result.securityScore}% (Grade $grade)",
                                java.awt.TrayIcon.MessageType.INFO
                            )
                        }
                    }
                } catch (_: Throwable) {}
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ScanManager.setError(e.message ?: "Scan failed")
            } finally {
                ScanManager.setScanning(false)
            }
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
        ScanManager.setScanning(false)
    }
}

actual fun getScannerController(): ScannerController = JvmScannerController()
