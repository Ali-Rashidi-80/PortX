package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.domain.isSuspectedTarpitOrWaf

data class FingerprintResult(
    val deviceName: String?,
    val osFingerprint: String?
)

class DeviceFingerprintUseCase {
    operator fun invoke(
        openPorts: Collection<Int>,
        banners: Map<Int, String>,
        target: String = ""
    ): FingerprintResult {
        val ports = openPorts.toSet()
        val allBannersLower = banners.values.joinToString(" ").lowercase()
        val totalPorts = ports.size

        // Heuristic: Check for WAF / SYN-Proxy / Tarpit Reflexive Port Flooding
        val isTarpitOrWaf = isSuspectedTarpitOrWaf(
            target = target,
            openPorts = ports,
            banners = banners
        )

        // If WAF / Proxy is suspected, we MUST strictly rely on verified banners or standard web ports
        if (isTarpitOrWaf) {
            if (allBannersLower.contains("cloudflare")) {
                return FingerprintResult("Web Application Host", "Cloudflare Protected Edge (WAF Active)")
            }
            if (allBannersLower.contains("nginx") || allBannersLower.contains("apache") || allBannersLower.contains("litespeed") || allBannersLower.contains("caddy") || allBannersLower.contains("iis")) {
                val srv = when {
                    allBannersLower.contains("nginx") -> "Nginx Web Server"
                    allBannersLower.contains("apache") -> "Apache HTTP Server"
                    allBannersLower.contains("litespeed") -> "LiteSpeed Web Server"
                    allBannersLower.contains("iis") -> "Microsoft IIS Server"
                    else -> "Web Server"
                }
                return FingerprintResult("Web Application Host", "$srv (Perimeter WAF Active)")
            }
            if (ports.contains(80) || ports.contains(443)) {
                return FingerprintResult("Web Application Host", "Web Edge Platform (Stateful Firewall / SYN-Proxy Active)")
            }
            return FingerprintResult("Protected Host", "Stateful Firewall / SYN-Proxy Active")
        }

        // 1. Explicit Verified Banner Signatures (Highest Confidence)
        if (allBannersLower.contains("openwrt") || allBannersLower.contains("routeros") || allBannersLower.contains("mikrotik") ||
            allBannersLower.contains("pfsense") || allBannersLower.contains("opnsense") || allBannersLower.contains("cisco") ||
            allBannersLower.contains("ubiquiti") || allBannersLower.contains("unifi") || allBannersLower.contains("fortigate")
        ) {
            val os = when {
                allBannersLower.contains("openwrt") -> "OpenWrt Linux"
                allBannersLower.contains("routeros") || allBannersLower.contains("mikrotik") -> "MikroTik RouterOS"
                allBannersLower.contains("pfsense") -> "pfSense FreeBSD"
                allBannersLower.contains("opnsense") -> "OPNsense FreeBSD"
                allBannersLower.contains("cisco") -> "Cisco IOS"
                allBannersLower.contains("ubiquiti") || allBannersLower.contains("unifi") -> "UniFi OS"
                allBannersLower.contains("fortigate") -> "FortiOS"
                else -> "Embedded Network Router OS"
            }
            return FingerprintResult("Network Gateway", os)
        }

        // Network Printer Banners
        if (allBannersLower.contains("jetdirect") || allBannersLower.contains("laserjet") || allBannersLower.contains("cups") ||
            allBannersLower.contains("epson") || allBannersLower.contains("brother") || allBannersLower.contains("canon") ||
            allBannersLower.contains("xerox") || allBannersLower.contains("kyocera") || allBannersLower.contains("ricoh")
        ) {
            val quotedModel = Regex(""""([^"]{3,60})"""").find(banners.values.joinToString("\n"))?.groupValues?.get(1)?.trim()
            val printerModel = when {
                !quotedModel.isNullOrBlank() && (quotedModel.contains("HP", true) || quotedModel.contains("LaserJet", true) || quotedModel.contains("Brother", true) || quotedModel.contains("Canon", true) || quotedModel.contains("Epson", true) || quotedModel.contains("Xerox", true)) -> quotedModel
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

        // NAS Banners
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

        // 2. Industrial ICS / SCADA (Requires sparse, dedicated port profile or verified ICS keywords)
        val isIndustrialProfile = totalPorts <= 25 && !ports.contains(445) && !ports.contains(135)
        if (allBannersLower.contains("modbus") || (ports.contains(502) && isIndustrialProfile)) {
            return FingerprintResult("Industrial Controller", "Modbus/TCP PLC")
        }
        if (allBannersLower.contains("simatic") || allBannersLower.contains("s7comm") || (ports.contains(102) && isIndustrialProfile)) {
            return FingerprintResult("Siemens Simatic S7", "Siemens Industrial PLC")
        }
        if (allBannersLower.contains("bacnet") || (ports.contains(47808) && isIndustrialProfile)) {
            return FingerprintResult("Building Controller", "BACnet/IP Automation Unit")
        }
        if (allBannersLower.contains("opc ua") || allBannersLower.contains("opcua") || (ports.contains(4840) && isIndustrialProfile)) {
            return FingerprintResult("Industrial Gateway", "OPC UA Server")
        }

        // 3. Mobile / Android
        if (ports.contains(5555)) return FingerprintResult("Android Device", "Android (ADB Enabled)")

        // 4. Network Printer (Ports 9100, 515, 631)
        if (ports.contains(9100) || ports.contains(515) || (ports.contains(631) && !ports.contains(22))) {
            return FingerprintResult("Network Printer", "Printer Firmware / CUPS")
        }

        // 5. Network Gateway / Router / VPN
        if (ports.contains(51820) || ports.contains(1194) ||
            allBannersLower.contains("dd-wrt") || allBannersLower.contains("asuswrt") || allBannersLower.contains("fritz!box") ||
            allBannersLower.contains("avm") || allBannersLower.contains("tp-link") || allBannersLower.contains("tplink") ||
            allBannersLower.contains("netgear") || allBannersLower.contains("d-link") || allBannersLower.contains("dlink")
        ) {
            val os = when {
                ports.contains(51820) -> "WireGuard VPN Gateway"
                ports.contains(1194) -> "OpenVPN Server"
                allBannersLower.contains("dd-wrt") -> "DD-WRT Linux"
                allBannersLower.contains("fritz!box") || allBannersLower.contains("avm") -> "FRITZ!OS"
                else -> "Embedded Network Router OS"
            }
            return FingerprintResult("Network Gateway", os)
        }

        // 6. Cloud & Container Infrastructure
        if (ports.contains(2375) || ports.contains(2376) || ports.contains(6443) || ports.contains(10250) ||
            ports.contains(2379) || ports.contains(2380) || ports.contains(9092) || ports.contains(8200) || ports.contains(8500) ||
            ports.contains(2181) || ports.contains(5672) || allBannersLower.contains("rabbitmq") || allBannersLower.contains("zookeeper")
        ) {
            val (name, os) = when {
                ports.contains(6443) || ports.contains(10250) -> "Kubernetes Node" to "Kubernetes Cluster Node"
                ports.contains(2375) || ports.contains(2376) -> "Container Host" to "Docker Engine Daemon"
                ports.contains(2379) || ports.contains(2380) -> "Key-Value Store" to "etcd Distributed Datastore"
                ports.contains(2181) || allBannersLower.contains("zookeeper") -> "Coordination Service" to "Apache ZooKeeper Cluster Node"
                ports.contains(5672) || allBannersLower.contains("rabbitmq") -> "Message Broker" to "RabbitMQ Message Broker"
                ports.contains(9092) -> "Message Streaming" to "Apache Kafka Broker"
                ports.contains(8200) -> "Secrets Vault" to "HashiCorp Vault Server"
                ports.contains(8500) -> "Service Mesh" to "HashiCorp Consul Node"
                else -> "Infrastructure Host" to "Cloud Native Platform"
            }
            return FingerprintResult(name, os)
        }

        // 7. IoT & Sensor Nodes
        if (ports.contains(5683) || ports.contains(5684) || ports.contains(1883) || ports.contains(8883) || ports.contains(1884)) {
            val (name, os) = when {
                ports.contains(5683) || ports.contains(5684) -> "IoT Constrained Node" to "CoAP Sensor Node (RFC 7252)"
                allBannersLower.contains("mosquitto") -> "IoT Message Broker" to "Eclipse Mosquitto MQTT Broker"
                allBannersLower.contains("emqx") -> "IoT Message Broker" to "EMQX Distributed Broker"
                allBannersLower.contains("hivemq") -> "IoT Message Broker" to "HiveMQ Enterprise Broker"
                else -> "IoT Message Broker" to "MQTT Broker"
            }
            return FingerprintResult(name, os)
        }

        // 8. Windows Systems
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

        // 9. Apple / macOS
        if (ports.contains(548) || (ports.contains(5000) && allBannersLower.contains("airplay")) ||
            allBannersLower.contains("darwin") || allBannersLower.contains("macos")
        ) {
            return FingerprintResult("Apple Device", "macOS / iOS (Darwin)")
        }

        // 10. Dedicated Database Server
        if (ports.contains(3306) || ports.contains(5432) || ports.contains(27017) || ports.contains(6379) || ports.contains(1433) || ports.contains(1521) || ports.contains(9042) || ports.contains(8123)) {
            val dbType = when {
                ports.contains(5432) -> "PostgreSQL Database Server"
                ports.contains(3306) -> "MariaDB / Relational SQL Server"
                ports.contains(1433) -> "Microsoft SQL Server"
                ports.contains(1521) -> "Oracle Database Server"
                ports.contains(27017) -> "MongoDB NoSQL Server"
                ports.contains(6379) -> "Redis Cache/Datastore"
                ports.contains(9042) -> "Apache Cassandra Server"
                ports.contains(8123) -> "ClickHouse Analytical Database"
                else -> "Database Server"
            }
            return FingerprintResult("Database Server", dbType)
        }

        // 11. Linux / Unix
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

        // 12. General Web Server
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
