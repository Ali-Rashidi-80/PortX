package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.domain.model.ScanResult

class FirewallDetectionUseCase {
    operator fun invoke(scanResult: ScanResult): String {
        val openCount = scanResult.openPorts.size
        
        return when {
            openCount == 0 -> "High Probability of Firewall (Target host down or all ports filtered)"
            openCount in 1..2 -> "Hardened Perimeter (Minimal open service surface)"
            openCount in 3..10 -> "Standard Network Profile (Active service exposure)"
            else -> "Open Perimeter (Permissive firewall rules / High exposure)"
        }
    }
}
