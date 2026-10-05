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
            102 to "Siemens S7comm (Industrial PLC communication exposure)",
            135 to "RPC Endpoint Mapper (Remote attack surface)",
            139 to "NetBIOS Session Service (Legacy Windows vector)",
            161 to "SNMP (Unauthenticated UDP community string / information disclosure risk)",
            445 to "SMB (WannaCry / EternalBlue ransomware vector)",
            502 to "Modbus/TCP (Unauthenticated industrial PLC control protocol exposure)",
            1883 to "MQTT (Unencrypted IoT broker / telemetry command exposure)",
            1900 to "SSDP / UPnP (Unauthenticated discovery & reflection amplification risk)",
            2375 to "Docker Daemon (Unauthenticated remote container escape risk)",
            3389 to "RDP (Remote Desktop exposed)",
            4840 to "OPC UA (Industrial automation server discovery risk)",
            5555 to "ADB (Android Debug Bridge unauthenticated access)",
            5900 to "VNC (Remote control service exposed)",
            6379 to "Redis (Unauthenticated key-value store risk)",
            9200 to "Elasticsearch (Unauthenticated cluster API risk)",
            10250 to "Kubernetes Kubelet (Remote execution risk)",
            11211 to "Memcached (Unauthenticated cache & DDoS amplification risk)",
            27017 to "MongoDB (Unauthenticated database risk)",
            47808 to "BACnet (Building automation & HVAC controller protocol exposure)"
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
