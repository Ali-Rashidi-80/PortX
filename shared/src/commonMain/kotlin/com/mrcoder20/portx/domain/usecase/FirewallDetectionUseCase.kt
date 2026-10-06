package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.domain.model.ScanResult

class FirewallDetectionUseCase {
    operator fun invoke(scanResult: ScanResult): String {
        val openCount = scanResult.openPorts.size
        
        return when {
            openCount == 0 -> "High Probability of Firewall"
            openCount in 1..2 -> "Hardened Perimeter"
            openCount in 3..10 -> "Standard Network Profile"
            else -> "Open Perimeter"
        }
    }
}
