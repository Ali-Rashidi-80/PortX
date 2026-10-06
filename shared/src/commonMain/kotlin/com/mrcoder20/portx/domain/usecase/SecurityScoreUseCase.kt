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
            102 to 15, // Siemens S7comm industrial control vector
            135 to 10, // RPC endpoint mapper
            139 to 10, // NetBIOS
            161 to 10, // SNMP unauthenticated community string
            389 to 10, // LDAP cleartext directory exposure
            445 to 20, // SMB / EternalBlue
            502 to 20, // Modbus/TCP unauthenticated ICS/SCADA vector
            1883 to 10,// MQTT unencrypted IoT broker
            1900 to 10,// SSDP / UPnP amplification
            2049 to 15,// NFS unauthenticated share exposure
            2375 to 20,// Docker daemon unauthenticated API
            2379 to 20,// etcd unauthenticated cluster datastore
            3389 to 15,// RDP exposed
            4840 to 10,// OPC UA industrial server exposure
            5555 to 15,// ADB remote debugging
            5683 to 10,// CoAP unencrypted IoT vector
            5900 to 10,// VNC remote access
            6379 to 15,// Redis unauthenticated
            9200 to 15,// Elasticsearch unauthenticated cluster API
            10250 to 20,// Kubernetes Kubelet unauthenticated API
            11211 to 15,// Memcached unauthenticated cache & DDoS amplification
            27017 to 15,// MongoDB unauthenticated
            47808 to 15// BACnet building automation system vector
        )

        criticalDeductions.forEach { (port, deduction) ->
            if (openPorts.contains(port)) {
                score -= deduction
            }
        }

        return score.coerceIn(0, 100)
    }
}
