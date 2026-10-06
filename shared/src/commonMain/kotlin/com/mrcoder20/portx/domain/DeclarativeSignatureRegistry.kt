package com.mrcoder20.portx.domain

import com.mrcoder20.portx.domain.model.CustomServiceSignature

/**
 * Thread-safe registry for declarative service signatures.
 * Enables zero-code extensions and custom template ingestion.
 */
object DeclarativeSignatureRegistry {

    private val customSignatures = mutableListOf<CustomServiceSignature>()

    private val builtInSignatures = listOf(
        CustomServiceSignature(
            id = "ssh-openssh",
            name = "OpenSSH Server",
            defaultPorts = listOf(22, 2222),
            matchSubstrings = listOf("SSH-", "OpenSSH"),
            versionExtractionRegex = "OpenSSH[_-]([0-9a-zA-Z.]+)",
            category = "REMOTE_ACCESS",
            riskSeverity = "LOW"
        ),
        CustomServiceSignature(
            id = "web-nginx",
            name = "nginx Web Server",
            defaultPorts = listOf(80, 443, 8080, 8443),
            matchSubstrings = listOf("nginx"),
            versionExtractionRegex = "nginx/([0-9.]+)",
            category = "GENERAL",
            riskSeverity = "INFO"
        ),
        CustomServiceSignature(
            id = "db-mysql",
            name = "MySQL / MariaDB Database",
            defaultPorts = listOf(3306),
            matchSubstrings = listOf("mysql", "mariadb"),
            versionExtractionRegex = "([0-9]+\\.[0-9]+\\.[0-9]+-[0-9a-zA-Z]+)",
            category = "DATABASE",
            riskSeverity = "HIGH"
        ),
        CustomServiceSignature(
            id = "db-redis",
            name = "Redis In-Memory Datastore",
            defaultPorts = listOf(6379),
            matchSubstrings = listOf("+PONG", "NOAUTH", "redis"),
            category = "DATABASE",
            riskSeverity = "HIGH"
        ),
        CustomServiceSignature(
            id = "cloud-docker",
            name = "Docker Daemon REST API",
            defaultPorts = listOf(2375, 2376),
            matchSubstrings = listOf("Docker", "docker-engine", "containers/json"),
            category = "CLOUD",
            riskSeverity = "CRITICAL"
        ),
        CustomServiceSignature(
            id = "cloud-kubelet",
            name = "Kubernetes Kubelet API",
            defaultPorts = listOf(10250),
            matchSubstrings = listOf("kubelet", "Unauthorized"),
            category = "CLOUD",
            riskSeverity = "CRITICAL"
        ),
        CustomServiceSignature(
            id = "scada-modbus",
            name = "Modbus/TCP Industrial Controller",
            defaultPorts = listOf(502),
            matchSubstrings = listOf("modbus", "schneider", "siemens"),
            category = "INDUSTRIAL",
            riskSeverity = "HIGH"
        ),
        CustomServiceSignature(
            id = "scada-s7comm",
            name = "Siemens S7comm PLC Node",
            defaultPorts = listOf(102),
            matchSubstrings = listOf("s7comm", "siemens", "step7"),
            category = "INDUSTRIAL",
            riskSeverity = "HIGH"
        ),
        CustomServiceSignature(
            id = "scada-bacnet",
            name = "BACnet Building Automation",
            defaultPorts = listOf(47808),
            matchSubstrings = listOf("bacnet", "building automation"),
            category = "INDUSTRIAL",
            riskSeverity = "HIGH"
        )
    )

    fun getAllSignatures(): List<CustomServiceSignature> {
        return builtInSignatures + customSignatures
    }

    fun register(signature: CustomServiceSignature) {
        if (customSignatures.none { it.id == signature.id } && builtInSignatures.none { it.id == signature.id }) {
            customSignatures.add(signature)
        }
    }

    fun clearCustomSignatures() {
        customSignatures.clear()
    }

    /**
     * Resolves the best matching signature for a port and raw response banner.
     */
    fun findMatch(port: Int, banner: String): CustomServiceSignature? {
        if (banner.isEmpty()) return null
        val all = getAllSignatures()

        // 1. Prioritize signatures matching both default port AND banner content
        val portMatched = all.filter { it.defaultPorts.contains(port) }
        val exactMatch = portMatched.firstOrNull { it.matches(banner) }
        if (exactMatch != null) return exactMatch

        // 2. Fall back to any signature matching banner content
        return all.firstOrNull { it.matches(banner) }
    }
}
