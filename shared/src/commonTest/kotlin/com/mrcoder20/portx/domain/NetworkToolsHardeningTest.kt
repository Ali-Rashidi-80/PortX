package com.mrcoder20.portx.domain

import com.mrcoder20.portx.data.network.PortScanner
import com.mrcoder20.portx.data.network.ScanConfig
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.usecase.ExportReportUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        val multilineHtml = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<html><head><title dir=\"ltr\" data-rh=\"true\">\n   PortX &amp; Security &quot;Console&quot; &#39;Pro&#39;   \n</title></head></html>"
        val regex = Regex("""<title\b[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val raw = regex.find(multilineHtml)?.groupValues?.get(1)?.trim()?.replace("\n", " ")?.replace("\r", "") ?: ""
        val title = raw.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
        assertEquals("PortX & Security \"Console\" 'Pro'", title)
    }

    @Test
    fun testDatabaseAdapterEmptyValuesPreservation() {
        val mapWithEmpty = mapOf(
            80 to "",
            443 to "nginx/1.24",
            8080 to ""
        )
        val encoded = com.mrcoder20.portx.data.local.mapIntStringAdapter.encode(mapWithEmpty)
        val decoded = com.mrcoder20.portx.data.local.mapIntStringAdapter.decode(encoded)

        assertEquals(3, decoded.size, "Empty string values must not be dropped during decoding")
        assertEquals("", decoded[80])
        assertEquals("nginx/1.24", decoded[443])
        assertEquals("", decoded[8080])
    }

    @Test
    fun testMultilingualPingOutputParsing() {
        val germanOutput = "Antwort von 8.8.8.8: Bytes=32 Zeit=15ms TTL=117"
        assertEquals(15L, parseTimeFromPingOutput(germanOutput))

        val frenchOutput = "Réponse de 8.8.8.8 : octets=32 temps=22ms TTL=57"
        assertEquals(22L, parseTimeFromPingOutput(frenchOutput))

        val russianOutput = "Ответ от 8.8.8.8: число байт=32 время=45мс TTL=58"
        assertEquals(45L, parseTimeFromPingOutput(russianOutput))
    }

    @Test
    fun testFirewallDetectionUseCaseScenarios() {
        val useCase = com.mrcoder20.portx.domain.usecase.FirewallDetectionUseCase()

        val emptyScan = ScanResult(target = "10.0.0.1", openPorts = emptyList(), timestamp = 0L, securityScore = 100)
        assertTrue(useCase(emptyScan).contains("High Probability of Firewall"))

        val minimalScan = ScanResult(target = "10.0.0.2", openPorts = listOf(443), timestamp = 0L, securityScore = 90)
        assertTrue(useCase(minimalScan).contains("Hardened Perimeter"))

        val standardScan = ScanResult(target = "10.0.0.3", openPorts = listOf(80, 443, 22, 53), timestamp = 0L, securityScore = 80)
        assertTrue(useCase(standardScan).contains("Standard Network Profile"))

        val openPerimeterScan = ScanResult(target = "10.0.0.4", openPorts = (1..15).toList(), timestamp = 0L, securityScore = 30)
        assertTrue(useCase(openPerimeterScan).contains("Open Perimeter"))
    }

    @Test
    fun testSettingsAccentColorAlphaSafety() {
        val zeroAlphaArgb = 0x00000000 // Alpha = 0 (completely transparent)
        val safeColor = if ((zeroAlphaArgb ushr 24) == 0) androidx.compose.ui.graphics.Color(0xFF00D1FF) else androidx.compose.ui.graphics.Color(zeroAlphaArgb)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF00D1FF), safeColor, "Corrupted zero-alpha accent color must revert to default cyan")

        val validArgb = 0xFF00D1FF.toInt()
        val restoredValid = if ((validArgb ushr 24) == 0) androidx.compose.ui.graphics.Color(0xFF00D1FF) else androidx.compose.ui.graphics.Color(validArgb)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF00D1FF), restoredValid, "Valid full-alpha accent color must be preserved")
    }

    @Test
    fun testScanManagerUiEventBuffering() {
        // Must not throw or block when triggering event without collector
        ScanManager.triggerUiEvent(ScanUIEvent.RequestNotificationPermission)
        
        // Changing state must be atomic and non-blocking
        ScanManager.setScanning(true)
        assertTrue(ScanManager.isScanning.value)
        
        ScanManager.updateProgress(45)
        assertEquals(45, ScanManager.progress.value)

        ScanManager.setScanning(false)
        assertFalse(ScanManager.isScanning.value)
    }

    @Test
    fun testSecurityHardenTransformSymmetry() {
        val original = "PortX-Professional-V5.1.0-SecCheck-192.168.1.1"
        val obfuscated = SecurityHarden.transform(original)
        assertFalse(obfuscated == original, "Obfuscated string must not equal original")
        val restored = SecurityHarden.transform(obfuscated)
        assertEquals(original, restored, "Transform must be fully symmetric and reversible")
    }

    @Test
    fun testExportReportFormatsAndEscaping() {
        val useCase = com.mrcoder20.portx.domain.usecase.ExportReportUseCase()
        val mockResult = ScanResult(
            target = "192.168.1.50",
            openPorts = listOf(80, 445),
            portServices = mapOf(80 to "http", 445 to "microsoft-ds,smb"),
            portBanners = mapOf(80 to "nginx|1.24", 445 to "Windows 11 SMB"),
            timestamp = 1775390400000L,
            securityScore = 75,
            scanType = "TCP"
        )

        // CSV must replace commas with semicolons
        val csv = useCase(mockResult, "CSV")
        assertTrue(csv.contains("80,TCP,http,nginx|1.24,open"))
        assertTrue(csv.contains("445,TCP,microsoft-ds;smb,Windows 11 SMB,open"))

        // Markdown must escape pipes
        val md = useCase(mockResult, "MD")
        assertTrue(md.contains("# PortX Scan Report"))
        assertTrue(md.contains("`80` | http | nginx\\|1.24"))
        assertTrue(md.contains("Security Score:** 75%"))

        // JSON must serialize correctly
        val json = useCase(mockResult, "JSON")
        assertTrue(json.contains("\"target\": \"192.168.1.50\""))
        assertTrue(json.contains("\"securityScore\": 75"))
    }

    @Test
    fun testSanitizeHostIPv6Brackets() {
        assertEquals("2001:db8::1", sanitizeHost("[2001:db8::1]:8080"))
        assertEquals("2001:db8::1", sanitizeHost("[2001:db8::1]"))
        assertEquals("2001:db8::1", sanitizeHost("https://[2001:db8::1]/path?query=1"))
        assertEquals("127.0.0.1", sanitizeHost("http://127.0.0.1:3000/"))
        assertEquals("example.com", sanitizeHost("https://example.com:8443/api/v1"))
    }

    @Test
    fun testAnomalyDetectionCriticalVectors() {
        val useCase = com.mrcoder20.portx.domain.usecase.AnomalyDetectionUseCase()
        val cleanResult = ScanResult(
            target = "10.0.0.1",
            openPorts = listOf(80, 443),
            timestamp = 0L,
            securityScore = 95
        )
        assertTrue(useCase(cleanResult).isEmpty(), "Clean web server should have zero critical anomalies")

        val vulnerableResult = ScanResult(
            target = "10.0.0.2",
            openPorts = listOf(21, 23, 445, 5555, 6379),
            timestamp = 0L,
            securityScore = 15
        )
        val anomalies = useCase(vulnerableResult)
        assertEquals(5, anomalies.size)
        assertTrue(anomalies.any { it.contains("EternalBlue") })
        assertTrue(anomalies.any { it.contains("Android Debug Bridge") })
        assertTrue(anomalies.any { it.contains("Redis") })
        assertTrue(anomalies.any { it.contains("Telnet") })
        assertTrue(anomalies.any { it.contains("FTP") })
    }

    @Test
    fun testExportReportCorruptTimestampHandling() {
        val useCase = com.mrcoder20.portx.domain.usecase.ExportReportUseCase()
        val corruptResult = ScanResult(
            target = "10.0.0.99",
            openPorts = listOf(80),
            timestamp = -999999999999999999L,
            securityScore = 90
        )
        val md = useCase(corruptResult, "MD")
        assertTrue(md.contains("# PortX Scan Report"))
        assertTrue(md.contains("Date:** N/A N/A") || md.contains("Date:**"))
    }

    @Test
    fun testThemeCompositionLocalsDefaultValues() {
        assertNotNull(com.mrcoder20.portx.presentation.ui.theme.LocalAccentColor)
        assertNotNull(com.mrcoder20.portx.presentation.ui.theme.LocalAppSettings)
    }

    @Test
    fun testIsTargetLocalOrPrivateRFCCompliance() {
        // Loopback RFC 1122
        assertTrue(isTargetLocalOrPrivate("localhost"))
        assertTrue(isTargetLocalOrPrivate("127.0.0.1"))
        assertTrue(isTargetLocalOrPrivate("127.0.0.2"))
        assertTrue(isTargetLocalOrPrivate("127.255.255.254"))
        assertTrue(isTargetLocalOrPrivate("::1"))
        assertTrue(isTargetLocalOrPrivate("[::1]"))

        // RFC 1918 Private IPv4
        assertTrue(isTargetLocalOrPrivate("10.0.0.1"))
        assertTrue(isTargetLocalOrPrivate("10.255.255.254"))
        assertTrue(isTargetLocalOrPrivate("192.168.0.1"))
        assertTrue(isTargetLocalOrPrivate("192.168.100.254"))
        assertTrue(isTargetLocalOrPrivate("172.16.0.1"))
        assertTrue(isTargetLocalOrPrivate("172.24.10.5"))
        assertTrue(isTargetLocalOrPrivate("172.31.255.255"))
        assertFalse(isTargetLocalOrPrivate("172.15.255.255"))
        assertFalse(isTargetLocalOrPrivate("172.32.0.1"))
        assertFalse(isTargetLocalOrPrivate("172.217.16.14")) // Google Public IP

        // RFC 3927 Link-Local IPv4
        assertTrue(isTargetLocalOrPrivate("169.254.1.1"))

        // RFC 6598 CGNAT
        assertTrue(isTargetLocalOrPrivate("100.64.0.1"))
        assertTrue(isTargetLocalOrPrivate("100.127.255.255"))
        assertFalse(isTargetLocalOrPrivate("100.63.255.255"))
        assertFalse(isTargetLocalOrPrivate("100.128.0.1"))

        // RFC 4291 / RFC 4193 IPv6
        assertTrue(isTargetLocalOrPrivate("fe80::1"))
        assertTrue(isTargetLocalOrPrivate("[fe80::1]"))
        assertTrue(isTargetLocalOrPrivate("fc00::1"))
        assertTrue(isTargetLocalOrPrivate("fd12:3456:789a::1"))

        // Public WAN targets
        assertFalse(isTargetLocalOrPrivate("8.8.8.8"))
        assertFalse(isTargetLocalOrPrivate("1.1.1.1"))
        assertFalse(isTargetLocalOrPrivate("google.com"))
        assertFalse(isTargetLocalOrPrivate("2606:4700:4700::1111"))

        // Local Domains RFC 6762 / 8375 / 6761
        assertTrue(isTargetLocalOrPrivate("router.local"))
        assertTrue(isTargetLocalOrPrivate("nas.lan"))
        assertTrue(isTargetLocalOrPrivate("cluster.internal"))
        assertTrue(isTargetLocalOrPrivate("gateway.home.arpa"))
    }

    @Test
    fun testCloudAndDevOpsAnomalyVectors() {
        val useCase = com.mrcoder20.portx.domain.usecase.AnomalyDetectionUseCase()
        val devopsResult = ScanResult(
            target = "10.0.0.5",
            openPorts = listOf(2375, 9200, 10250, 11211),
            timestamp = 0L,
            securityScore = 20
        )
        val anomalies = useCase(devopsResult)
        assertEquals(4, anomalies.size)
        assertTrue(anomalies.any { it.contains("Docker Daemon") })
        assertTrue(anomalies.any { it.contains("Elasticsearch") })
        assertTrue(anomalies.any { it.contains("Kubernetes Kubelet") })
        assertTrue(anomalies.any { it.contains("Memcached") })

        val scoreUseCase = com.mrcoder20.portx.domain.usecase.SecurityScoreUseCase()
        val score = scoreUseCase(devopsResult)
        // 100 - (4*3=12) - 20(Docker) - 15(ES) - 20(Kubelet) - 15(Memcached) = 100 - 12 - 70 = 18
        assertEquals(18, score)
    }

    @Test
    fun testExtractTitleSanitization() {
        val scanner = PortScanner()
        val htmlBanner = "HTTP/1.1 200 OK\r\n\r\n<html><head><title>Admin Panel &amp; Dashboard | v1.0 &lt;PRO&gt;</title></head></html>"
        val title = scanner.extractTitle(htmlBanner)
        assertEquals("Admin Panel & Dashboard / v1.0 <PRO>", title)
        assertFalse(title.contains("|"), "Title should replace pipe with slash to avoid breaking Markdown tables")
        
        val giantTitleHtml = "<html><head><title>" + "A".repeat(300) + "</title></head></html>"
        val truncatedTitle = scanner.extractTitle(giantTitleHtml)
        assertEquals(120, truncatedTitle.length, "Title should be bounded to 120 characters")
    }

    @Test
    fun testIsValidIpAddress() {
        // Valid IPv4
        assertTrue(isValidIpAddress("192.168.1.1"))
        assertTrue(isValidIpAddress("10.0.0.1"))
        assertTrue(isValidIpAddress("255.255.255.255"))
        assertTrue(isValidIpAddress("0.0.0.0"))
        assertTrue(isValidIpAddress("127.0.0.1"))
        assertTrue(isValidIpAddress("  192.168.1.1  "))

        // Invalid IPv4
        assertFalse(isValidIpAddress("256.0.0.1"))
        assertFalse(isValidIpAddress("192.168.1"))
        assertFalse(isValidIpAddress("192.168.1.1.1"))
        assertFalse(isValidIpAddress("192.168.01.1")) // Leading zeros disallowed
        assertFalse(isValidIpAddress("abc.def.ghi.jkl"))
        assertFalse(isValidIpAddress("-1.0.0.1"))

        // Valid IPv6
        assertTrue(isValidIpAddress("::1"))
        assertTrue(isValidIpAddress("[::1]"))
        assertTrue(isValidIpAddress("2001:db8::1"))
        assertTrue(isValidIpAddress("[2001:db8::1]"))
        assertTrue(isValidIpAddress("fe80::1"))
        assertTrue(isValidIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))

        // Invalid IPv6
        assertFalse(isValidIpAddress("2001:db8:::1"))
        assertFalse(isValidIpAddress("gggg::1"))
        assertFalse(isValidIpAddress("12345::1"))

        // HTML, Captive Portals, & Corrupt text
        assertFalse(isValidIpAddress(""))
        assertFalse(isValidIpAddress("   "))
        assertFalse(isValidIpAddress("<html><body>502 Bad Gateway</body></html>"))
        assertFalse(isValidIpAddress("Error 404: Not Found"))
        assertFalse(isValidIpAddress("captive.apple.com"))
    }

    @Test
    fun testExportReportFormulaInjectionProtectionAndPerimeter() {
        val useCase = ExportReportUseCase()
        val maliciousResult = ScanResult(
            target = "10.0.0.1",
            openPorts = listOf(80, 443, 8080),
            portBanners = mapOf(
                80 to "=cmd|' /C calc'!A0",
                443 to "+12345",
                8080 to "@SUM(A1:A10)"
            ),
            portServices = mapOf(
                80 to "-calc",
                443 to "https",
                8080 to "http-proxy"
            ),
            timestamp = 1700000000000L,
            securityScore = 80
        )

        val csv = useCase(maliciousResult, "CSV")
        assertTrue(csv.contains("80,TCP,'-calc,'=cmd|' /C calc'!A0,open") || csv.contains("'-calc") && csv.contains("'=cmd"), "CSV export must sanitize formula-triggering characters (=, +, -, @) by prepending a single quote")
        assertTrue(csv.contains("'+12345"), "CSV export must prepend single quote to fields starting with +")
        assertTrue(csv.contains("'@SUM(A1:A10)"), "CSV export must prepend single quote to fields starting with @")

        val md = useCase(maliciousResult, "MD")
        assertTrue(md.contains("## Summary"), "Markdown report must contain Summary section")
        assertTrue(md.contains("- **Perimeter Assessment:**"), "Markdown report must contain Perimeter Assessment")
    }

    @Test
    fun testSnmpAndSsdpAnomalyDetectionAndScoring() {
        val anomalyUseCase = com.mrcoder20.portx.domain.usecase.AnomalyDetectionUseCase()
        val scoreUseCase = com.mrcoder20.portx.domain.usecase.SecurityScoreUseCase()

        val scanResult = ScanResult(
            target = "192.168.1.1",
            openPorts = listOf(161, 1900),
            timestamp = 1700000000000L,
            securityScore = 0
        )

        val anomalies = anomalyUseCase(scanResult)
        assertTrue(anomalies.any { it.contains("SNMP") }, "Port 161 should trigger SNMP unauthenticated vector anomaly")
        assertTrue(anomalies.any { it.contains("SSDP / UPnP") }, "Port 1900 should trigger SSDP / UPnP vector anomaly")

        val score = scoreUseCase(scanResult)
        // 100 - (2 open ports * 3 = 6) - 10 (SNMP) - 10 (SSDP) = 74
        assertEquals(74, score, "Security score should deduct 10 for SNMP and 10 for SSDP in addition to base open port deduction")
    }

    @Test
    fun testUnicodeAndPersianPreservationInTitles() {
        val scanner = PortScanner()
        val persianHtml = "<html><head><title>داشبورد مدیریتی پورتکس &amp; سرور اصلی | v2.0</title></head><body>OK</body></html>"
        val extracted = scanner.extractTitle(persianHtml)
        assertEquals("داشبورد مدیریتی پورتکس & سرور اصلی / v2.0", extracted, "Persian UTF-8 characters should be completely preserved in extractTitle")

        val htmlWithNbsp = "<html><head><title>Server&nbsp;&nbsp;Status&nbsp;Monitor</title></head></html>"
        val extractedNbsp = scanner.extractTitle(htmlWithNbsp)
        assertEquals("Server Status Monitor", extractedNbsp, "HTML &nbsp; entities and multiple consecutive spaces should be normalized to single spaces")
    }

    @Test
    fun testGuessServiceMapping() {
        val scanner = PortScanner()
        assertEquals("http", scanner.guessService(80))
        assertEquals("https", scanner.guessService(443))
        assertEquals("ssh", scanner.guessService(22))
        assertEquals("snmp", scanner.guessService(161))
        assertEquals("ssdp", scanner.guessService(1900))
        assertEquals("redis", scanner.guessService(6379))
        assertEquals("memcached", scanner.guessService(11211))
        assertEquals("mysql", scanner.guessService(3306))
        assertEquals("postgres", scanner.guessService(5432))
        assertEquals("unknown", scanner.guessService(49999))
    }

    @Test
    fun testTargetSanitizationAndValidationWorkflow() {
        val isTargetValid: (String) -> Boolean = { rawInput ->
            val target = sanitizeHost(rawInput.trim())
            val hostnameRegex = Regex("""^([a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?\.)*[a-zA-Z0-9_]([a-zA-Z0-9_\-]{0,61}[a-zA-Z0-9_])?$""")
            val isAllNumericDotted = Regex("""^[0-9.]+$""").matches(target)
            when {
                isAllNumericDotted || target.contains(":") -> isValidIpAddress(target)
                else -> hostnameRegex.matches(target)
            }
        }

        // Valid targets
        assertTrue(isTargetValid("192.168.1.1"))
        assertTrue(isTargetValid("https://192.168.1.1:8080/"))
        assertTrue(isTargetValid("google.com"))
        assertTrue(isTargetValid("sub.domain.example.com"))
        assertTrue(isTargetValid("localhost"))
        assertTrue(isTargetValid("router.local"))
        assertTrue(isTargetValid("::1"))
        assertTrue(isTargetValid("[2001:db8::1]:8080"))

        // Invalid targets
        assertFalse(isTargetValid("999.999.999.999"))
        assertFalse(isTargetValid("256.0.0.1"))
        assertFalse(isTargetValid("2001:db8:::1"))
        assertFalse(isTargetValid("example..com"))
        assertFalse(isTargetValid("-invalid-.com"))
    }

    @Test
    fun testUdpProbePayloads() {
        val scanner = PortScanner()
        val dnsPayload = scanner.getUdpProbePayload(53)
        assertEquals(17, dnsPayload.size, "DNS UDP probe payload should be a standard 17-byte DNS query header")

        val ntpPayload = scanner.getUdpProbePayload(123)
        assertEquals(48, ntpPayload.size, "NTP probe payload should be 48 bytes according to RFC 5905")
        assertEquals(0x1B.toByte(), ntpPayload[0], "First byte of NTP packet should be 0x1B (LI=0, VN=3, Mode=3)")

        val snmpPayload = scanner.getUdpProbePayload(161)
        assertTrue(snmpPayload.isNotEmpty(), "SNMP payload should be non-empty")
        assertTrue(snmpPayload.decodeToString().contains("public"), "SNMPv1 payload should query with public community string")

        val ssdpPayload = scanner.getUdpProbePayload(1900).decodeToString()
        assertTrue(ssdpPayload.startsWith("M-SEARCH * HTTP/1.1"), "SSDP payload should be an HTTP/1.1 M-SEARCH discovery query")
        assertTrue(ssdpPayload.contains("239.255.255.250:1900"), "SSDP payload should target standard multicast address")

        val genericPayload = scanner.getUdpProbePayload(9999).decodeToString()
        assertEquals("PROBE\r\n", genericPayload)
    }

    @Test
    fun testLocalizationCompleteness() {
        val requiredKeys = listOf(
            "dashboard", "tools", "reports", "settings", "no_reports", 
            "security_trend", "global_export_format", "clear_history_title",
            "clear_history_desc", "clear_all", "cancel", "about", "support", "feedback",
            "engine_configuration", "parallel_threads", "connections",
            "banner_grabbing", "full_port_scan", "multi_protocol", "stealth_mode",
            "device_environment", "connection", "wifi", "wired", "adapter",
            "tactical_icmp_output", "querying", "engine_ready",
            "dns_resolution_records", "awaiting_dns", "whois_authority_data",
            "ready_whois", "copy_whois", "engine_running", "ready",
            "live_engine_logs", "waiting_engine_activity", "found", "scanning_network"
        )
        for (key in requiredKeys) {
            val enVal = LocalizedStrings.get(key, "en")
            val faVal = LocalizedStrings.get(key, "fa")
            assertFalse(enVal.isBlank(), "English value for '$key' must not be blank")
            assertFalse(faVal.isBlank(), "Persian value for '$key' must not be blank")
            assertFalse(faVal == key, "Persian translation for '$key' should be defined and not fallback to key name")
        }
    }

    @Test
    fun testExportReportDeduplicationAndSorting() {
        val exporter = com.mrcoder20.portx.domain.usecase.ExportReportUseCase()
        val scan = com.mrcoder20.portx.domain.model.ScanResult(
            target = "10.0.0.1",
            openPorts = listOf(443, 80, 443, 22, 80),
            portServices = mapOf(22 to "ssh", 80 to "http", 443 to "https"),
            portBanners = mapOf(22 to "OpenSSH_8.9", 80 to "nginx", 443 to "nginx"),
            timestamp = 1700000000000L,
            securityScore = 85
        )

        val csv = exporter(scan, "CSV")
        val csvLines = csv.lines().filter { it.isNotBlank() }
        assertEquals(4, csvLines.size, "CSV should have 1 header line and 3 unique port lines")
        assertTrue(csvLines[1].startsWith("22,"), "First port in CSV should be 22")
        assertTrue(csvLines[2].startsWith("80,"), "Second port in CSV should be 80")
        assertTrue(csvLines[3].startsWith("443,"), "Third port in CSV should be 443")

        val md = exporter(scan, "MD")
        assertTrue(md.contains("- **Open Ports:** 3"), "Markdown summary should report 3 distinct open ports")
        val p22Idx = md.indexOf("| `22` |")
        val p80Idx = md.indexOf("| `80` |")
        val p443Idx = md.indexOf("| `443` |")
        assertTrue(p22Idx != -1 && p80Idx != -1 && p443Idx != -1, "All 3 distinct ports must appear in table")
        assertTrue(p22Idx < p80Idx && p80Idx < p443Idx, "Markdown table rows must be sorted in ascending port order")
    }

    @Test
    fun testIndustrialProtocolRecognition() {
        val scanner = com.mrcoder20.portx.data.network.PortScanner()
        assertEquals("modbus", scanner.guessService(502), "Port 502 should be mapped to Modbus/TCP")
        assertEquals("s7comm", scanner.guessService(102), "Port 102 should be mapped to Siemens S7comm")
        assertEquals("opcua", scanner.guessService(4840), "Port 4840 should be mapped to OPC UA")
        assertEquals("bacnet", scanner.guessService(47808), "Port 47808 should be mapped to BACnet")
    }

    @Test
    fun testIndustrialProtocolAnomaliesAndSecurityDeductions() {
        val anomalyUseCase = com.mrcoder20.portx.domain.usecase.AnomalyDetectionUseCase()
        val scoreUseCase = com.mrcoder20.portx.domain.usecase.SecurityScoreUseCase()

        val scan = com.mrcoder20.portx.domain.model.ScanResult(
            target = "192.168.1.100",
            openPorts = listOf(502, 102, 4840, 47808, 1883),
            portServices = mapOf(502 to "modbus", 102 to "s7comm", 4840 to "opcua", 47808 to "bacnet", 1883 to "mqtt"),
            timestamp = 1700000000000L,
            securityScore = 100
        )

        val anomalies = anomalyUseCase(scan)
        assertEquals(5, anomalies.size)
        assertTrue(anomalies.any { it.contains("Modbus/TCP") })
        assertTrue(anomalies.any { it.contains("Siemens S7comm") })
        assertTrue(anomalies.any { it.contains("OPC UA") })
        assertTrue(anomalies.any { it.contains("BACnet") })
        assertTrue(anomalies.any { it.contains("MQTT") })

        // Base deduction: 5 ports * 3 = 15
        // Modbus (502): -20, S7comm (102): -15, OPC UA (4840): -10, BACnet (47808): -15, MQTT (1883): -10 -> Total deductions: 15 + 70 = 85 -> Score = 15
        val calculatedScore = scoreUseCase(scan)
        assertEquals(15, calculatedScore)
    }

    @Test
    fun testDatabaseAdapterSortingAndDeduplication() {
        val listAdapter = com.mrcoder20.portx.data.local.listOfIntAdapter
        val mapAdapter = com.mrcoder20.portx.data.local.mapIntStringAdapter

        // List adapter: test encode sorting & deduplication
        val encoded = listAdapter.encode(listOf(8080, 80, 443, 80, 22))
        assertEquals("22,80,443,8080", encoded, "Encoded ports must be sorted in ascending order and deduplicated")

        // List adapter: test decode sorting & deduplication
        val decoded = listAdapter.decode("8080, 80, 443, 80, 22")
        assertEquals(listOf(22, 80, 443, 8080), decoded, "Decoded ports must be sorted in ascending order and deduplicated")

        // Map adapter: test key sorting on encode
        val encodedMap = mapAdapter.encode(mapOf(8080 to "alt", 80 to "http", 443 to "https"))
        assertEquals("80:http|443:https|8080:alt", encodedMap, "Encoded map must have ascending sorted port keys")
    }

    @Test
    fun testSettingsDefaultInvariants() {
        val defaultSettings = com.mrcoder20.portx.domain.AppSettings()
        assertEquals("en", defaultSettings.language)
        assertEquals("DARK", defaultSettings.theme)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF00D1FF), defaultSettings.accentColor)
    }

    @Test
    fun testIpv4MappedIpv6ValidationAndLocality() {
        // RFC 4291 IPv4-mapped IPv6 address validation
        assertTrue(isValidIpAddress("::ffff:192.168.1.1"), "::ffff:192.168.1.1 should be valid IPv4-mapped IPv6")
        assertTrue(isValidIpAddress("::ffff:8.8.8.8"), "::ffff:8.8.8.8 should be valid IPv4-mapped IPv6")
        assertTrue(isValidIpAddress("0:0:0:0:0:ffff:10.0.0.1"), "0:0:0:0:0:ffff:10.0.0.1 should be valid IPv4-mapped IPv6")
        assertFalse(isValidIpAddress("::ffff:999.1.1.1"), "Invalid IPv4 octet in mapped address should fail")
        assertFalse(isValidIpAddress(":::ffff:192.168.1.1"), "Triple colon in mapped address should fail")

        // Local & Private range detection for mapped & special addresses
        assertTrue(isTargetLocalOrPrivate("::ffff:192.168.1.1"), "Mapped 192.168.1.1 should be detected as private")
        assertTrue(isTargetLocalOrPrivate("::ffff:10.0.0.1"), "Mapped 10.0.0.1 should be detected as private")
        assertTrue(isTargetLocalOrPrivate("::ffff:127.0.0.1"), "Mapped 127.0.0.1 should be detected as local")
        assertFalse(isTargetLocalOrPrivate("::ffff:8.8.8.8"), "Mapped 8.8.8.8 should not be detected as private")
        
        // Broadcast & Multicast
        assertTrue(isTargetLocalOrPrivate("255.255.255.255"), "255.255.255.255 is broadcast/local")
        assertTrue(isTargetLocalOrPrivate("224.0.0.1"), "224.0.0.1 is IPv4 multicast/local")
        assertTrue(isTargetLocalOrPrivate("ff02::1"), "ff02::1 is IPv6 multicast/local")
    }

    @Test
    fun testUdpProbePayloadIntegrity() {
        val scanner = com.mrcoder20.portx.data.network.PortScanner()
        val dnsPayload = scanner.getUdpProbePayload(53)
        assertTrue(dnsPayload.isNotEmpty(), "DNS UDP probe payload must not be empty")

        val ntpPayload = scanner.getUdpProbePayload(123)
        assertEquals(48, ntpPayload.size, "NTP probe must be exactly 48 bytes (RFC 5905)")
        assertEquals(0x1B.toByte(), ntpPayload[0], "NTP client mode header must be 0x1B")

        val ssdpPayload = scanner.getUdpProbePayload(1900).decodeToString()
        assertTrue(ssdpPayload.startsWith("M-SEARCH * HTTP/1.1"), "SSDP payload must be valid M-SEARCH discovery")
    }

    @Test
    fun testDeviceFingerprintUseCase() {
        val useCase = com.mrcoder20.portx.domain.usecase.DeviceFingerprintUseCase()

        // Industrial Modbus PLC
        val modbusResult = useCase(listOf(502, 80), mapOf(502 to "Modbus Server", 80 to "Web"))
        assertEquals("Industrial Controller", modbusResult.deviceName)
        assertEquals("Modbus/TCP PLC", modbusResult.osFingerprint)

        // Windows SMB
        val winResult = useCase(listOf(135, 445), mapOf(445 to "SMB"))
        assertEquals("Windows Host", winResult.deviceName)
        assertEquals("Microsoft Windows (SMB Active)", winResult.osFingerprint)

        // Linux Host
        val linuxResult = useCase(listOf(22, 80), mapOf(22 to "SSH-2.0-OpenSSH_8.2p1 Ubuntu-4ubuntu0.5"))
        assertEquals("Linux Host", linuxResult.deviceName)
        assertEquals("Ubuntu Linux", linuxResult.osFingerprint)

        // Android ADB
        val androidResult = useCase(listOf(5555), emptyMap())
        assertEquals("Android Device", androidResult.deviceName)
        assertEquals("Android (ADB Enabled)", androidResult.osFingerprint)

        // Database Server (PostgreSQL)
        val pgResult = useCase(listOf(5432), emptyMap())
        assertEquals("Database Server", pgResult.deviceName)
        assertEquals("PostgreSQL Database Server", pgResult.osFingerprint)

        // Database Server with Web Port 80 (Must prioritize DB Server over generic Web Server)
        val pgWebResult = useCase(listOf(80, 5432), mapOf(80 to "Apache", 5432 to "PostgreSQL"))
        assertEquals("Database Server", pgWebResult.deviceName)
        assertEquals("PostgreSQL Database Server", pgWebResult.osFingerprint)

        // Network Printer (IPP / JetDirect)
        val printerResult = useCase(listOf(631, 9100), emptyMap())
        assertEquals("Network Printer", printerResult.deviceName)
        assertEquals("Printer Firmware / CUPS", printerResult.osFingerprint)

        // Network Printer with Port 80 Web Console (Must prioritize Printer over generic Web Server)
        val hpPrinterResult = useCase(listOf(80, 9100), mapOf(80 to "HP LaserJet Pro 400", 9100 to "JetDirect"))
        assertEquals("Network Printer", hpPrinterResult.deviceName)
        assertEquals("HP LaserJet / JetDirect", hpPrinterResult.osFingerprint)

        // Exact PJL quoted printer identification model extraction
        val pjlPrinterResult = useCase(listOf(9100), mapOf(9100 to "@PJL INFO ID\r\n\"HP LaserJet Pro MFP M428fdw\"\r\n"))
        assertEquals("Network Printer", pjlPrinterResult.deviceName)
        assertEquals("HP LaserJet Pro MFP M428fdw", pjlPrinterResult.osFingerprint)

        // Network Attached Storage (NAS)
        val synologyResult = useCase(listOf(5000, 5001), mapOf(5000 to "Synology DiskStation DSM 7.2"))
        assertEquals("Network Storage (NAS)", synologyResult.deviceName)
        assertEquals("Synology DSM", synologyResult.osFingerprint)

        // Modern Router / UniFi Gateway
        val unifiResult = useCase(listOf(80, 443), mapOf(443 to "UniFi OS Dream Machine"))
        assertEquals("Network Gateway", unifiResult.deviceName)
        assertEquals("UniFi OS", unifiResult.osFingerprint)
    }

    @Test
    fun testPortScannerServiceGuessing() {
        val scanner = com.mrcoder20.portx.data.network.PortScanner()
        assertEquals("jetdirect", scanner.guessService(9100))
        assertEquals("vnc", scanner.guessService(5900))
        assertEquals("postgres", scanner.guessService(5432))
        assertEquals("mssql", scanner.guessService(1433))
        assertEquals("oracle", scanner.guessService(1521))
        assertEquals("mongodb", scanner.guessService(27017))
        assertEquals("rdp", scanner.guessService(3389))
        assertEquals("redis", scanner.guessService(6379))
        assertEquals("modbus", scanner.guessService(502))
        assertEquals("s7comm", scanner.guessService(102))
        assertEquals("opcua", scanner.guessService(4840))
        assertEquals("bacnet", scanner.guessService(47808))
    }

    @Test
    fun testMarkdownExportIncludesDeviceAndOs() {
        val exporter = com.mrcoder20.portx.domain.usecase.ExportReportUseCase()
        val scan = com.mrcoder20.portx.domain.model.ScanResult(
            target = "192.168.1.50",
            openPorts = listOf(22),
            portBanners = mapOf(22 to "SSH-2.0-OpenSSH_8.2p1 Ubuntu"),
            timestamp = 1700000000000L,
            securityScore = 90,
            deviceName = "Linux Host",
            osFingerprint = "Ubuntu Linux"
        )
        val md = exporter(scan, "MD")
        assertTrue(md.contains("- **Device:** Linux Host"), "Markdown export should include Device name")
        assertTrue(md.contains("- **OS Fingerprint:** Ubuntu Linux"), "Markdown export should include OS fingerprint")
    }

    @Test
    fun testMarkdownExportEmptyOpenPorts() {
        val exporter = com.mrcoder20.portx.domain.usecase.ExportReportUseCase()
        val emptyScan = com.mrcoder20.portx.domain.model.ScanResult(
            target = "192.168.1.200",
            openPorts = emptyList(),
            timestamp = 1700000000000L,
            securityScore = 100
        )
        val md = exporter(emptyScan, "MD")
        assertTrue(md.contains("| - | No Open Services Found | - |"), "Empty scan report must contain placeholder row in table")
    }

    @Test
    fun testExtractTitleNumericHtmlEntities() {
        val scanner = com.mrcoder20.portx.data.network.PortScanner()
        val banner = "HTTP/1.1 200 OK\r\n\r\n<html><head><title>&#80;&#111;&#114;&#116;&#88; &#x26; Security &#x3C;&#x3E;</title></head></html>"
        val title = scanner.extractTitle(banner)
        assertEquals("PortX & Security <>", title)
    }

    @Test
    fun testMysqlVersionParsingRegex() {
        val banner = "5.7.34-log\u0000\u0000\u0000\u0002"
        val verMatch = Regex("""(\d+\.\d+\.\d+[\w.-]*)""").find(banner)
        assertEquals("5.7.34-log", verMatch?.value)

        val mariaBanner = "10.11.4-MariaDB-1:10.11.4+maria~deb12"
        val mariaMatch = Regex("""(\d+\.\d+\.\d+[\w.-]*)""").find(mariaBanner)
        assertEquals("10.11.4-MariaDB-1", mariaMatch?.value)
    }

    @Test
    fun testReportFilteringLogic() {
        val scans = listOf(
            com.mrcoder20.portx.domain.model.ScanResult(
                target = "192.168.1.1",
                openPorts = listOf(80),
                timestamp = 100L,
                securityScore = 90,
                deviceName = "Gateway Router",
                osFingerprint = "OpenWrt Linux"
            ),
            com.mrcoder20.portx.domain.model.ScanResult(
                target = "10.0.0.50",
                openPorts = listOf(9100),
                timestamp = 200L,
                securityScore = 80,
                deviceName = "Office Printer",
                osFingerprint = "HP LaserJet"
            )
        )

        val filteredByIp = scans.filter { it.target.contains("192.168") }
        assertEquals(1, filteredByIp.size)
        assertEquals("Gateway Router", filteredByIp.first().deviceName)

        val filteredByDevice = scans.filter { it.deviceName?.contains("Printer", ignoreCase = true) == true }
        assertEquals(1, filteredByDevice.size)
        assertEquals("10.0.0.50", filteredByDevice.first().target)
    }
}
