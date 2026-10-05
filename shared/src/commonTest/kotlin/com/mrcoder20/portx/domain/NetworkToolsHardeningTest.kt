package com.mrcoder20.portx.domain

import com.mrcoder20.portx.data.network.PortScanner
import com.mrcoder20.portx.data.network.ScanConfig
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.usecase.ExportReportUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkToolsHardeningTest {

    @Test
    fun testSanitizeHostVariations() {
        assertEquals("google.com", sanitizeHost("google.com"))
        assertEquals("google.com", sanitizeHost("  google.com  "))
        assertEquals("google.com", sanitizeHost("https://google.com"))
        assertEquals("google.com", sanitizeHost("http://google.com"))
        assertEquals("google.com", sanitizeHost("https://google.com/path/to/resource?q=test"))
        assertEquals("google.com", sanitizeHost("http://google.com:8080/"))
        assertEquals("192.168.1.1", sanitizeHost("192.168.1.1:8080"))
        assertEquals("8.8.8.8", sanitizeHost("https://8.8.8.8:443"))
        assertEquals("2001:db8::1", sanitizeHost("2001:db8::1"))
        assertEquals("2001:db8::1", sanitizeHost("[2001:db8::1]:8080"))
    }

    @Test
    fun testPingOutputParsers() {
        val winOutput = "Reply from 8.8.8.8: bytes=32 time=127ms TTL=113"
        assertEquals(127L, parseTimeFromPingOutput(winOutput))
        assertEquals(113, parseTtlFromPingOutput(winOutput))

        val linuxOutput = "64 bytes from 8.8.8.8: icmp_seq=1 ttl=64 time=28.4 ms"
        assertEquals(28L, parseTimeFromPingOutput(linuxOutput))
        assertEquals(64, parseTtlFromPingOutput(linuxOutput))

        val subMsOutput = "Reply from 127.0.0.1: bytes=32 time<1ms TTL=128"
        assertEquals(1L, parseTimeFromPingOutput(subMsOutput))
        assertEquals(128, parseTtlFromPingOutput(subMsOutput))
    }

    @Test
    fun testSanitizedDnsResolution() = runTest {
        val tools = getNetworkTools()
        val ips = tools.dnsLookup("https://google.com/path")
        assertTrue(ips.isNotEmpty(), "Sanitized host should resolve to valid IP addresses")
        assertTrue(ips.all { it.isNotBlank() }, "Resolved IP list should not contain blank entries")
    }

    @Test
    fun testExportReportSanitization() {
        val useCase = ExportReportUseCase()
        val scanResult = ScanResult(
            target = "192.168.1.1",
            openPorts = listOf(80, 443),
            portBanners = mapOf(
                80 to "Apache\nServer|v2.4,test",
                443 to "nginx|1.18\r\nsecure"
            ),
            portServices = mapOf(
                80 to "http",
                443 to "https"
            ),
            timestamp = 1700000000000L,
            securityScore = 90,
            scanType = "UDP"
        )

        val csv = useCase(scanResult, "CSV")
        assertTrue(csv.contains("80,UDP,http,Apache Server\\|v2.4;test,open") || csv.contains("80,UDP,http,Apache Server"), "CSV should contain protocol and sanitized banner")
        assertFalse(csv.contains("Apache\nServer"), "CSV should not contain raw unescaped newlines in banner")

        val md = useCase(scanResult, "MD")
        assertTrue(md.contains("\\|"), "Markdown table should escape pipe characters in banner")
        assertFalse(md.contains("Apache\nServer"), "Markdown table rows should not break on newlines in banner")
    }

    @Test
    fun testPortScannerZeroOperationsGuard() = runTest {
        val scanner = PortScanner()
        val config = ScanConfig(
            target = "127.0.0.1",
            startPort = 500,
            endPort = 100 // Invalid inverted range resulting in 0 operations
        )
        val summary = scanner.scan(config)
        assertEquals(0, summary.totalPorts)
        assertEquals(0, summary.openPorts)
        assertTrue(summary.results.isEmpty())
    }
}
