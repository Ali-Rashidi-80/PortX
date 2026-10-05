package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.domain.model.ScanResult

class AnomalyDetectionUseCase {
    operator fun invoke(scanResult: ScanResult): List<String> {
        val anomalies = mutableListOf<String>()
        val openPorts = scanResult.openPorts.toSet()

        // Known high-risk and unauthenticated vectors
        val threats = mapOf(
            21 to "FTP (Cleartext credential transmission risk)",
            23 to "Telnet (Unencrypted remote shell exposure)",
            135 to "RPC Endpoint Mapper (Remote attack surface)",
            139 to "NetBIOS Session Service (Legacy Windows vector)",
            445 to "SMB (WannaCry / EternalBlue ransomware vector)",
            3389 to "RDP (Remote Desktop exposed)",
            5555 to "ADB (Android Debug Bridge unauthenticated access)",
            5900 to "VNC (Remote control service exposed)",
            6379 to "Redis (Unauthenticated key-value store risk)",
            27017 to "MongoDB (Unauthenticated database risk)"
        )

        threats.forEach { (port, desc) ->
            if (openPorts.contains(port)) {
                anomalies.add("High Risk [Port $port]: $desc")
            }
        }

        // Uncommon ports for consumer devices (excluding common web/proxy services)
        val knownServicePorts = setOf(
            80, 443, 8080, 8443, 8000, 3000, 5000, 5173, 8888, 9090, 53, 22, 123
        )
        val uncommonPorts = openPorts.filter { it in 1025..65535 && !knownServicePorts.contains(it) && !threats.containsKey(it) }
        if (uncommonPorts.size > 8) {
            anomalies.add("Warning: Elevated number of unusual open ports (${uncommonPorts.size} ports: ${uncommonPorts.take(5).joinToString(", ")}...)")
        }

        return anomalies
    }
}
