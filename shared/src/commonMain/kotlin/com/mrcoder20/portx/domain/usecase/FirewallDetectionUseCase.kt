package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.domain.isSuspectedTarpitOrWaf
import com.mrcoder20.portx.domain.model.ScanResult

class FirewallDetectionUseCase {
    operator fun invoke(scanResult: ScanResult): String {
        val openCount = scanResult.openPorts.size

        val isWafOrTarpit = isSuspectedTarpitOrWaf(
            target = scanResult.target,
            openPorts = scanResult.openPorts,
            banners = scanResult.portBanners,
            osFingerprint = scanResult.osFingerprint
        )

        if (isWafOrTarpit) {
            return "Stateful Firewall / SYN-Proxy Active"
        }

        return when {
            openCount == 0 -> "High Probability of Firewall"
            openCount in 1..2 -> "Hardened Perimeter"
            openCount in 3..10 -> "Standard Network Profile"
            else -> "Open Perimeter"
        }
    }
}

