package com.mrcoder20.portx.domain

import com.mrcoder20.portx.domain.model.CustomServiceSignature
import kotlin.test.*

class DeclarativeSignatureTest {

    @BeforeTest
    fun setup() {
        DeclarativeSignatureRegistry.clearCustomSignatures()
    }

    @Test
    fun testBuiltInSignaturesResolution() {
        val signatures = DeclarativeSignatureRegistry.getAllSignatures()
        assertTrue(signatures.isNotEmpty(), "Built-in signatures must not be empty")

        // Test OpenSSH matching
        val sshMatch = DeclarativeSignatureRegistry.findMatch(22, "SSH-2.0-OpenSSH_9.6p1 Ubuntu-3ubuntu13")
        assertNotNull(sshMatch)
        assertEquals("ssh-openssh", sshMatch.id)
        val sshVersion = sshMatch.extractVersion("SSH-2.0-OpenSSH_9.6p1 Ubuntu-3ubuntu13")
        assertEquals("9.6p1", sshVersion)

        // Test Nginx matching
        val nginxMatch = DeclarativeSignatureRegistry.findMatch(80, "HTTP/1.1 200 OK\r\nServer: nginx/1.24.0\r\n")
        assertNotNull(nginxMatch)
        assertEquals("web-nginx", nginxMatch.id)
        assertEquals("1.24.0", nginxMatch.extractVersion("Server: nginx/1.24.0"))

        // Test Docker Daemon matching
        val dockerMatch = DeclarativeSignatureRegistry.findMatch(2375, "{\"message\":\"page not found\",\"docker-engine\":\"24.0.5\"}")
        assertNotNull(dockerMatch)
        assertEquals("cloud-docker", dockerMatch.id)
        assertEquals("CRITICAL", dockerMatch.riskSeverity)

        // Test Modbus industrial matching
        val modbusMatch = DeclarativeSignatureRegistry.findMatch(502, "Schneider Electric Modbus/TCP Gateway 0x01")
        assertNotNull(modbusMatch)
        assertEquals("scada-modbus", modbusMatch.id)
        assertEquals("INDUSTRIAL", modbusMatch.category)
    }

    @Test
    fun testCustomSignatureRegistrationAndMatching() {
        val customSig = CustomServiceSignature(
            id = "custom-internal-api",
            name = "Internal Auth Microservice",
            defaultPorts = listOf(8088),
            matchSubstrings = listOf("PortX-Auth-Daemon"),
            versionExtractionRegex = "PortX-Auth-Daemon/v([0-9.]+)",
            category = "CLOUD",
            riskSeverity = "MEDIUM"
        )

        DeclarativeSignatureRegistry.register(customSig)

        val match = DeclarativeSignatureRegistry.findMatch(8088, "HTTP/1.1 200 OK\r\nServer: PortX-Auth-Daemon/v2.1.0\r\n")
        assertNotNull(match)
        assertEquals("custom-internal-api", match.id)
        assertEquals("2.1.0", match.extractVersion("Server: PortX-Auth-Daemon/v2.1.0"))
    }

    @Test
    fun testEmptyOrNonMatchingBannerReturnsNull() {
        assertNull(DeclarativeSignatureRegistry.findMatch(80, ""))
        assertNull(DeclarativeSignatureRegistry.findMatch(9999, "Random Non-matching Gibberish 123456"))
    }
}
