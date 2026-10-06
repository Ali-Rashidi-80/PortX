package com.mrcoder20.portx.presentation

import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.usecase.ExportReportUseCase
import com.mrcoder20.portx.presentation.ui.getServiceTitle
import com.mrcoder20.portx.presentation.ui.getServiceDescription
import com.mrcoder20.portx.presentation.ui.getPortColor
import com.mrcoder20.portx.presentation.ui.DisplayPort
import com.mrcoder20.portx.presentation.ui.theme.DangerNeon
import com.mrcoder20.portx.presentation.ui.theme.SecondaryNeon
import com.mrcoder20.portx.presentation.ui.theme.TertiaryNeon
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import com.mrcoder20.portx.domain.isValidTarget

class PresentationAdversarialTest {

    private fun validatePortRange(startStr: String, endStr: String, allPorts: Boolean): Pair<Boolean, String?> {
        if (allPorts) return true to null
        return try {
            val start = startStr.toInt()
            val end = endStr.toInt()
            if (start < 1 || end > 65535 || start > end) {
                false to "Range must be 1-65535"
            } else {
                true to null
            }
        } catch (e: Exception) {
            false to "Invalid numeric port range"
        }
    }

    private fun sanitizeFilename(target: String, timestamp: Long, extension: String): String {
        val sanitizedTarget = target.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "PortX_Report_${sanitizedTarget}_$timestamp.$extension"
    }

    @Test
    fun testTargetValidationAdversarialCases() {
        // Valid edge cases
        assertTrue(isValidTarget("127.0.0.1"))
        assertTrue(isValidTarget("192.168.1.1"))
        assertTrue(isValidTarget("10.0.0.1"))
        assertTrue(isValidTarget("172.16.0.1"))
        assertTrue(isValidTarget("8.8.8.8"))
        assertTrue(isValidTarget("localhost"))
        assertTrue(isValidTarget("router"))
        assertTrue(isValidTarget("nas-server"))
        assertTrue(isValidTarget("t.co"))
        assertTrue(isValidTarget("sub.domain.example.com"))

        // Adversarial / Invalid edge cases
        assertFalse(isValidTarget(""))
        assertFalse(isValidTarget("   "))
        assertFalse(isValidTarget("http://example.com"))
        assertFalse(isValidTarget("192.168.1.1:8080"))
        assertFalse(isValidTarget("999.999.999.999"))
        assertFalse(isValidTarget("example..com"))
        assertFalse(isValidTarget("target with space"))
        assertFalse(isValidTarget("; rm -rf /"))
    }

    @Test
    fun testPortRangeValidationAdversarialCases() {
        // Valid cases
        val (v1, e1) = validatePortRange("1", "1024", false)
        assertTrue(v1)
        assertEquals(null, e1)

        val (v2, e2) = validatePortRange("80", "80", false)
        assertTrue(v2)
        assertEquals(null, e2)

        val (v3, e3) = validatePortRange("1", "65535", true)
        assertTrue(v3)
        assertEquals(null, e3)

        // Inverted range
        val (v4, e4) = validatePortRange("1000", "500", false)
        assertFalse(v4)
        assertEquals("Range must be 1-65535", e4)

        // Zero / negative port
        val (v5, e5) = validatePortRange("0", "100", false)
        assertFalse(v5)
        assertEquals("Range must be 1-65535", e5)

        // Beyond 65535
        val (v6, e6) = validatePortRange("1", "65536", false)
        assertFalse(v6)
        assertEquals("Range must be 1-65535", e6)

        // Non numeric input
        val (v7, e7) = validatePortRange("abc", "100", false)
        assertFalse(v7)
        assertEquals("Invalid numeric port range", e7)

        val (v8, e8) = validatePortRange("80", "", false)
        assertFalse(v8)
        assertEquals("Invalid numeric port range", e8)
    }

    @Test
    fun testExportFilenameWindowsSanitization() {
        // Targets with Windows illegal characters: \ / : * ? " < > |
        val fn1 = sanitizeFilename("192.168.1.1:8080", 1700000000L, "json")
        assertEquals("PortX_Report_192.168.1.1_8080_1700000000.json", fn1)

        val fn2 = sanitizeFilename("http://example.com/api?query=*&test=1", 1700000000L, "csv")
        assertFalse(fn2.contains(":"))
        assertFalse(fn2.contains("/"))
        assertFalse(fn2.contains("?"))
        assertFalse(fn2.contains("*"))
        assertFalse(fn2.contains("&"))
        assertTrue(fn2.startsWith("PortX_Report_http___example.com_api_query___test_1_1700000000.csv"))
    }

    @Test
    fun testExportReportFormatting() {
        val exportReportUseCase = ExportReportUseCase()
        val mockScan = ScanResult(
            target = "192.168.1.1",
            openPorts = listOf(80, 443),
            portServices = mapOf(80 to "http", 443 to "https"),
            portBanners = mapOf(80 to "nginx/1.24.0, gzip", 443 to "cloudflare"),
            securityScore = 95,
            timestamp = 1700000000000L
        )

        // 1. CSV formatting: commas in banner replaced with semicolon
        val csv = exportReportUseCase(mockScan, "CSV")
        assertTrue(csv.contains("80,TCP,http,nginx/1.24.0; gzip,open"))
        assertTrue(csv.contains("443,TCP,https,cloudflare,open"))

        // 2. MD formatting
        val md = exportReportUseCase(mockScan, "MD")
        assertTrue(md.contains("# PortX Scan Report"))
        assertTrue(md.contains("- **Target:** `192.168.1.1`"))
        assertTrue(md.contains("- **Security Score:** 95%"))
        assertTrue(md.contains("| `80` | http | nginx/1.24.0, gzip |"))
    }

    @Test
    fun testDashboardProtocolRegistryAndColors() {
        val testAccent = Color(0xFF00D1FF)

        // 1. Critical & Industrial Ports -> DangerNeon
        val dangerPorts = listOf(21, 23, 102, 135, 139, 445, 502, 1883, 2375, 47808, 5555, 10250, 11211)
        dangerPorts.forEach { port ->
            assertEquals(DangerNeon, getPortColor(port, testAccent), "Port $port must be colored DangerNeon")
        }

        // 2. High-Privilege & Database Ports -> SecondaryNeon
        val adminPorts = listOf(22, 1433, 1521, 3306, 3389, 4840, 5432, 5900, 6379, 6443, 8200, 8500, 9200, 27017)
        adminPorts.forEach { port ->
            assertEquals(SecondaryNeon, getPortColor(port, testAccent), "Port $port must be colored SecondaryNeon")
        }

        // 3. Web & Proxy Ports -> testAccent
        val webPorts = listOf(80, 443, 8080, 8443, 8000, 3000, 5000)
        webPorts.forEach { port ->
            assertEquals(testAccent, getPortColor(port, testAccent), "Port $port must be colored with accent")
        }

        // 4. Low risk / standard -> TertiaryNeon
        assertEquals(TertiaryNeon, getPortColor(53, testAccent))
        assertEquals(TertiaryNeon, getPortColor(123, testAccent))
        assertEquals(TertiaryNeon, getPortColor(49152, testAccent))

        // 5. Service Titles
        assertEquals("Modbus Industrial ICS", getServiceTitle(502))
        assertEquals("BACnet Building Automation", getServiceTitle(47808))
        assertEquals("Siemens S7comm PLC", getServiceTitle(102))
        assertEquals("MQTT IoT Broker", getServiceTitle(1883))
        assertEquals("Android ADB Debugger", getServiceTitle(5555))
        assertEquals("PostgreSQL Database", getServiceTitle(5432))
        assertEquals("MariaDB Database", getServiceTitle(3306))
        assertEquals("Redis In-Memory DB", getServiceTitle(6379))
        assertEquals("SSH Secure Shell", getServiceTitle(22))
        assertEquals("HTTP Web Server", getServiceTitle(80))
        assertEquals("OpenVPN Server", getServiceTitle(1194))
        assertEquals("WireGuard VPN Tunnel", getServiceTitle(51820))
        assertEquals("CoAP IoT Node", getServiceTitle(5683))
        assertEquals("etcd Datastore", getServiceTitle(2379))
        assertEquals("Cassandra Database", getServiceTitle(9042))
        assertEquals("ClickHouse Analytical DB", getServiceTitle(8123))
        assertEquals("Custom-api Service", getServiceTitle(9999, "custom-api"))
        assertEquals("Service on Port 8899", getServiceTitle(8899, null))

        // 6. Service Descriptions
        assertTrue(getServiceDescription(502).contains("Modbus TCP"))
        assertTrue(getServiceDescription(47808).contains("BACnet/IP"))
        assertTrue(getServiceDescription(102).contains("Siemens Step7"))
        assertTrue(getServiceDescription(1883).contains("MQTT"))
        assertTrue(getServiceDescription(5555).contains("Android Debug Bridge"))
        assertTrue(getServiceDescription(445).contains("Microsoft SMB"))
        assertTrue(getServiceDescription(51820).contains("WireGuard"))
        assertTrue(getServiceDescription(1194).contains("OpenVPN"))
        assertTrue(getServiceDescription(5683).contains("Constrained Application Protocol"))
        assertEquals("Active custom-proxy service", getServiceDescription(8099, "custom-proxy"))
        assertEquals("Active Network Service", getServiceDescription(60000, null))

        // 7. Verify banned technologies are absent from all registered ports
        val registeredPorts = listOf(21, 22, 23, 25, 53, 80, 443, 1194, 1433, 1521, 1883, 2049, 2375, 2379, 3000, 3306, 3389, 4222, 4840, 5000, 5432, 5555, 5672, 5683, 5900, 6379, 6443, 8000, 8080, 8123, 8200, 8443, 8500, 9000, 9042, 9092, 9200, 10250, 11211, 27017, 47808, 50051, 51820)
        registeredPorts.forEach { port ->
            val title = getServiceTitle(port)
            val desc = getServiceDescription(port)
            assertFalse(title.contains("MySQL", ignoreCase = true), "Port $port title contains MySQL: $title")
            assertFalse(desc.contains("MySQL", ignoreCase = true), "Port $port desc contains MySQL: $desc")
            assertFalse(title.contains("Node.js", ignoreCase = true), "Port $port title contains Node.js: $title")
            assertFalse(desc.contains("Node.js", ignoreCase = true), "Port $port desc contains Node.js: $desc")
            assertFalse(title.contains("PHP", ignoreCase = true), "Port $port title contains PHP: $title")
            assertFalse(desc.contains("PHP", ignoreCase = true), "Port $port desc contains PHP: $desc")
            assertFalse(title.contains("Flask", ignoreCase = true), "Port $port title contains Flask: $title")
            assertFalse(desc.contains("Flask", ignoreCase = true), "Port $port desc contains Flask: $desc")
        }

        // 8. DisplayPort data class contract
        val dp = DisplayPort(
            number = 502,
            title = getServiceTitle(502),
            description = getServiceDescription(502),
            color = getPortColor(502, testAccent)
        )
        assertEquals(502, dp.number)
        assertEquals("Modbus Industrial ICS", dp.title)
        assertEquals(DangerNeon, dp.color)
    }

    @Test
    fun testTimeoutConfigurationAndLocalization() {
        // 1. Verify Localization of socket_timeout and ms in EN, FA, RU, ZH
        assertEquals("Socket Timeout", com.mrcoder20.portx.domain.LocalizedStrings.get("socket_timeout", "en"))
        assertEquals("مهلت زمانی اتصال", com.mrcoder20.portx.domain.LocalizedStrings.get("socket_timeout", "fa"))
        assertEquals("Тайм-аут сокета", com.mrcoder20.portx.domain.LocalizedStrings.get("socket_timeout", "ru"))
        assertEquals("套接字超时", com.mrcoder20.portx.domain.LocalizedStrings.get("socket_timeout", "zh"))

        assertEquals("ms", com.mrcoder20.portx.domain.LocalizedStrings.get("ms", "en"))
        assertEquals("میلی‌ثانیه", com.mrcoder20.portx.domain.LocalizedStrings.get("ms", "fa"))
        assertEquals("мс", com.mrcoder20.portx.domain.LocalizedStrings.get("ms", "ru"))
        assertEquals("毫秒", com.mrcoder20.portx.domain.LocalizedStrings.get("ms", "zh"))

        // 2. Fallback to English for unmapped language & verified French translation
        assertEquals("Socket Timeout", com.mrcoder20.portx.domain.LocalizedStrings.get("socket_timeout", "xx"))
        assertEquals("Délai d'Attente", com.mrcoder20.portx.domain.LocalizedStrings.get("socket_timeout", "fr"))

        // 3. ScanUIState timeout default and bounds
        val defaultState = com.mrcoder20.portx.presentation.viewmodel.ScanUIState()
        assertEquals(1000, defaultState.timeout)
        val modifiedState = defaultState.copy(timeout = 2500)
        assertEquals(2500, modifiedState.timeout)
    }
}

