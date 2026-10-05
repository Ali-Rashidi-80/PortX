package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.domain.model.ScanResult

class SecurityScoreUseCase {
    operator fun invoke(scanResult: ScanResult): Int {
        var score = 100
        val openPorts = scanResult.openPorts.toSet()

        // Base deduction for open port surface
        score -= (openPorts.size * 3)

        // Critical vulnerability & unauthenticated vector deductions
        val criticalDeductions = mapOf(
            21 to 10,  // FTP cleartext
            23 to 15,  // Telnet unencrypted
            135 to 10, // RPC endpoint mapper
            139 to 10, // NetBIOS
            445 to 20, // SMB / EternalBlue
            3389 to 15,// RDP exposed
            5555 to 15,// ADB remote debugging
            5900 to 10,// VNC remote access
            6379 to 15,// Redis unauthenticated
            27017 to 15// MongoDB unauthenticated
        )

        criticalDeductions.forEach { (port, deduction) ->
            if (openPorts.contains(port)) {
                score -= deduction
            }
        }

        return score.coerceIn(0, 100)
    }
}
