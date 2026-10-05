package com.mrcoder20.portx.presentation

import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.usecase.ExportReportUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresentationAdversarialTest {

    private val ipRegex = Regex("""^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""")
    private val hostnameRegex = Regex("""^([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)*[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?$""")

    private fun isValidTarget(raw: String): Boolean {
        val target = raw.trim()
        if (target.isBlank()) return false
        val isAllNumericDotted = Regex("""^[0-9.]+$""").matches(target)
        return if (isAllNumericDotted) ipRegex.matches(target) else hostnameRegex.matches(target)
    }

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
}
