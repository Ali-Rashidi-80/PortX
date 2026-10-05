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
        assertEquals("2001:db8::1", sanitizeHost("2001:db8::1"))
        assertEquals("2001:db8::1", sanitizeHost("[2001:db8::1]"))
        assertEquals("::1", sanitizeHost("[::1]"))
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

    @Test
    fun testDatabaseAdaptersPipeEscapingAndResilience() {
        val originalMap = mapOf(
            80 to "Apache/2.4.41 | OpenSSL/1.1.1f",
            443 to "nginx/1.18.0 | custom:proxy",
            8080 to "plain-banner"
        )
        val encoded = com.mrcoder20.portx.data.local.mapIntStringAdapter.encode(originalMap)
        val decoded = com.mrcoder20.portx.data.local.mapIntStringAdapter.decode(encoded)

        assertEquals(originalMap.size, decoded.size)
        assertEquals("Apache/2.4.41 | OpenSSL/1.1.1f", decoded[80])
        assertEquals("nginx/1.18.0 | custom:proxy", decoded[443])
        assertEquals("plain-banner", decoded[8080])

        // Resilient decode with malformed entry
        val malformedString = "80:valid|corrupted_entry|443:also:valid"
        val decodedMalformed = com.mrcoder20.portx.data.local.mapIntStringAdapter.decode(malformedString)
        assertEquals("valid", decodedMalformed[80])
        assertEquals("also:valid", decodedMalformed[443])

        // List of Int adapter resilience
        val validList = listOf(80, 443, 8080)
        val encodedList = com.mrcoder20.portx.data.local.listOfIntAdapter.encode(validList)
        val decodedList = com.mrcoder20.portx.data.local.listOfIntAdapter.decode(encodedList)
        assertEquals(validList, decodedList)

        val malformedList = "80, invalid, 443, , 8080"
        val recoveredList = com.mrcoder20.portx.data.local.listOfIntAdapter.decode(malformedList)
        assertEquals(listOf(80, 443, 8080), recoveredList)
    }

    @Test
    fun testAnomalyDetectionHighRiskVectors() {
        val useCase = com.mrcoder20.portx.domain.usecase.AnomalyDetectionUseCase()
        val dangerousScan = ScanResult(
            target = "10.0.0.1",
            openPorts = listOf(23, 445, 5555, 6379, 27017),
            portBanners = emptyMap(),
            portServices = emptyMap(),
            timestamp = 1700000000000L,
            securityScore = 20
        )
        val anomalies = useCase(dangerousScan)
        assertTrue(anomalies.any { it.contains("Port 23") && it.contains("Telnet") })
        assertTrue(anomalies.any { it.contains("Port 445") && it.contains("SMB") })
        assertTrue(anomalies.any { it.contains("Port 5555") && it.contains("ADB") })
        assertTrue(anomalies.any { it.contains("Port 6379") && it.contains("Redis") })
        assertTrue(anomalies.any { it.contains("Port 27017") && it.contains("MongoDB") })

        val standardWebScan = ScanResult(
            target = "10.0.0.2",
            openPorts = listOf(80, 443, 8080, 8443),
            portBanners = emptyMap(),
            portServices = emptyMap(),
            timestamp = 1700000000000L,
            securityScore = 95
        )
        val webAnomalies = useCase(standardWebScan)
        assertTrue(webAnomalies.isEmpty(), "Standard web ports should not trigger threat anomalies")
    }

    @Test
    fun testSecurityScoreUseCaseCalculation() {
        val useCase = com.mrcoder20.portx.domain.usecase.SecurityScoreUseCase()
        
        val cleanScan = ScanResult(
            target = "127.0.0.1",
            openPorts = emptyList(),
            portBanners = emptyMap(),
            portServices = emptyMap(),
            timestamp = 1700000000000L,
            securityScore = 0
        )
        assertEquals(100, useCase(cleanScan), "Zero open ports should yield perfect score of 100")

        val smbScan = ScanResult(
            target = "192.168.1.50",
            openPorts = listOf(445), // Port 445: 3 base + 20 critical deduction = -23
            portBanners = emptyMap(),
            portServices = emptyMap(),
            timestamp = 1700000000000L,
            securityScore = 0
        )
        assertEquals(77, useCase(smbScan), "Port 445 should result in heavy security penalty")

        val massiveCompromise = ScanResult(
            target = "192.168.1.100",
            openPorts = (1..100).toList(), // 100 open ports with multiple critical ones
            portBanners = emptyMap(),
            portServices = emptyMap(),
            timestamp = 1700000000000L,
            securityScore = 0
        )
        assertEquals(0, useCase(massiveCompromise), "Severe compromise must clamp to 0")
    }

    @Test
    fun testHtmlTitleRegexExtraction() {
        val multilineHtml = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<html><head><title>\n   PortX Administration Portal   \n</title></head></html>"
        val regex = Regex("<title>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val title = regex.find(multilineHtml)?.groupValues?.get(1)?.trim()?.replace("\n", " ")?.replace("\r", "") ?: ""
        assertEquals("PortX Administration Portal", title)
    }
}
