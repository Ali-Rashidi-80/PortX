package com.mrcoder20.portx.domain.usecase

data class FingerprintResult(
    val deviceName: String?,
    val osFingerprint: String?
)

class DeviceFingerprintUseCase {
    operator fun invoke(openPorts: Collection<Int>, banners: Map<Int, String>): FingerprintResult {
        val ports = openPorts.toSet()
        val allBannersLower = banners.values.joinToString(" ").lowercase()

        // 1. Industrial ICS / SCADA
        if (ports.contains(502)) return FingerprintResult("Industrial Controller", "Modbus/TCP PLC")
        if (ports.contains(102)) return FingerprintResult("Siemens Simatic S7", "Siemens Industrial PLC")
        if (ports.contains(47808)) return FingerprintResult("Building Controller", "BACnet/IP Automation Unit")
        if (ports.contains(4840)) return FingerprintResult("Industrial Gateway", "OPC UA Server")

        // 2. Mobile / Android
        if (ports.contains(5555)) return FingerprintResult("Android Device", "Android (ADB Enabled)")

        // 3. Network Printer (Prioritized before Web Server since virtually all network printers expose Port 80)
        if (ports.contains(9100) || ports.contains(515) || (ports.contains(631) && !ports.contains(22)) ||
            allBannersLower.contains("jetdirect") || allBannersLower.contains("laserjet") || allBannersLower.contains("cups") ||
            allBannersLower.contains("epson") || allBannersLower.contains("brother") || allBannersLower.contains("canon") ||
            allBannersLower.contains("xerox") || allBannersLower.contains("kyocera") || allBannersLower.contains("ricoh")
        ) {
            val printerModel = when {
                allBannersLower.contains("laserjet") || allBannersLower.contains("jetdirect") -> "HP LaserJet / JetDirect"
                allBannersLower.contains("canon") -> "Canon Network Print Device"
                allBannersLower.contains("xerox") -> "Xerox Network Print Device"
                allBannersLower.contains("brother") -> "Brother Network Print Device"
                allBannersLower.contains("epson") -> "Epson Network Print Device"
                allBannersLower.contains("kyocera") -> "Kyocera Network Print Device"
                allBannersLower.contains("ricoh") -> "Ricoh Network Print Device"
                else -> "Printer Firmware / CUPS"
            }
            return FingerprintResult("Network Printer", printerModel)
        }

        // 4. Network Gateway / Router / Firewall
        if (allBannersLower.contains("openwrt") || allBannersLower.contains("dd-wrt") || allBannersLower.contains("routeros") ||
            allBannersLower.contains("mikrotik") || allBannersLower.contains("pfsense") || allBannersLower.contains("opnsense") ||
            allBannersLower.contains("cisco") || allBannersLower.contains("ubiquiti") || allBannersLower.contains("unifi") ||
            allBannersLower.contains("tp-link") || allBannersLower.contains("tplink") || allBannersLower.contains("netgear") ||
            allBannersLower.contains("d-link") || allBannersLower.contains("dlink") || allBannersLower.contains("asuswrt") ||
            allBannersLower.contains("fritz!box") || allBannersLower.contains("avm") || allBannersLower.contains("fortigate")
        ) {
            val os = when {
                allBannersLower.contains("openwrt") -> "OpenWrt Linux"
                allBannersLower.contains("routeros") || allBannersLower.contains("mikrotik") -> "MikroTik RouterOS"
                allBannersLower.contains("pfsense") -> "pfSense FreeBSD"
                allBannersLower.contains("opnsense") -> "OPNsense FreeBSD"
                allBannersLower.contains("dd-wrt") -> "DD-WRT Linux"
                allBannersLower.contains("cisco") -> "Cisco IOS"
                allBannersLower.contains("ubiquiti") || allBannersLower.contains("unifi") -> "UniFi OS"
                allBannersLower.contains("fritz!box") || allBannersLower.contains("avm") -> "FRITZ!OS"
                allBannersLower.contains("fortigate") -> "FortiOS"
                else -> "Embedded Network Router OS"
            }
            return FingerprintResult("Network Gateway", os)
        }

        // 5. Network Attached Storage (NAS)
        if (allBannersLower.contains("synology") || allBannersLower.contains("diskstation") || allBannersLower.contains("dsm") ||
            allBannersLower.contains("qnap") || allBannersLower.contains("qts") || allBannersLower.contains("truenas") ||
            allBannersLower.contains("freenas") || allBannersLower.contains("asustor")
        ) {
            val nasOs = when {
                allBannersLower.contains("synology") || allBannersLower.contains("diskstation") || allBannersLower.contains("dsm") -> "Synology DSM"
                allBannersLower.contains("qnap") || allBannersLower.contains("qts") -> "QNAP QTS"
                allBannersLower.contains("truenas") -> "TrueNAS"
                allBannersLower.contains("freenas") -> "FreeNAS"
                else -> "NAS Storage OS"
            }
            return FingerprintResult("Network Storage (NAS)", nasOs)
        }

        // 6. Windows Systems (Ports 135, 139, 445, 3389 or banner keywords)
        if (ports.contains(445) || (ports.contains(135) && ports.contains(139)) || ports.contains(3389) ||
            allBannersLower.contains("microsoft") || allBannersLower.contains("iis") || allBannersLower.contains("ms-wbt-server")
        ) {
            val os = when {
                allBannersLower.contains("windows server") -> "Microsoft Windows Server"
                ports.contains(3389) -> "Microsoft Windows (RDP Active)"
                ports.contains(445) -> "Microsoft Windows (SMB Active)"
                else -> "Microsoft Windows"
            }
            return FingerprintResult("Windows Host", os)
        }

        // 7. Apple / macOS
        if (ports.contains(548) || (ports.contains(5000) && allBannersLower.contains("airplay")) ||
            allBannersLower.contains("darwin") || allBannersLower.contains("macos")
        ) {
            return FingerprintResult("Apple Device", "macOS / iOS (Darwin)")
        }

        // 8. Dedicated Database Server (Prioritized before generic Web Server)
        if (ports.contains(3306) || ports.contains(5432) || ports.contains(27017) || ports.contains(6379) || ports.contains(1433) || ports.contains(1521)) {
            val dbType = when {
                ports.contains(5432) -> "PostgreSQL Database Server"
                ports.contains(3306) -> "MySQL / MariaDB Server"
                ports.contains(1433) -> "Microsoft SQL Server"
                ports.contains(1521) -> "Oracle Database Server"
                ports.contains(27017) -> "MongoDB NoSQL Server"
                ports.contains(6379) -> "Redis Cache/Datastore"
                else -> "Database Server"
            }
            return FingerprintResult("Database Server", dbType)
        }

        // 9. Linux / Unix
        if (ports.contains(22) || allBannersLower.contains("ubuntu") || allBannersLower.contains("debian") ||
            allBannersLower.contains("centos") || allBannersLower.contains("alpine") || allBannersLower.contains("linux")
        ) {
            val os = when {
                allBannersLower.contains("ubuntu") -> "Ubuntu Linux"
                allBannersLower.contains("debian") -> "Debian Linux"
                allBannersLower.contains("centos") || allBannersLower.contains("red hat") ||
                    allBannersLower.contains("el7") || allBannersLower.contains("el8") || allBannersLower.contains("el9") -> "RHEL / CentOS Linux"
                allBannersLower.contains("alpine") -> "Alpine Linux"
                allBannersLower.contains("freebsd") -> "FreeBSD"
                else -> "Linux / Unix Generic"
            }
            return FingerprintResult("Linux Host", os)
        }

        // 10. General Web Server
        if (ports.contains(80) || ports.contains(443) || ports.contains(8080) || ports.contains(8443)) {
            val server = when {
                allBannersLower.contains("nginx") -> "Nginx Web Server"
                allBannersLower.contains("apache") -> "Apache HTTP Server"
                allBannersLower.contains("cloudflare") -> "Cloudflare Edge"
                allBannersLower.contains("caddy") -> "Caddy Web Server"
                allBannersLower.contains("litespeed") -> "LiteSpeed Web Server"
                else -> "Web Server"
            }
            return FingerprintResult(server, "HTTP Service Platform")
        }

        return FingerprintResult(null, null)
    }
}
