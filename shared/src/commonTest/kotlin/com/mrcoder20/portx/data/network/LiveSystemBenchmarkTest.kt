package com.mrcoder20.portx.data.network

import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue
import com.mrcoder20.portx.domain.model.ScanResult
import com.mrcoder20.portx.domain.usecase.AnomalyDetectionUseCase
import com.mrcoder20.portx.domain.usecase.ExportReportUseCase
import com.mrcoder20.portx.domain.usecase.SecurityScoreUseCase

class LiveSystemBenchmarkTest {

    private fun getUsedMemoryMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    @Test
    fun benchmark01_concurrencyScalingSweep(): Unit = runTest {
        val scanner = PortScanner()
        val concurrencyLevels = listOf(25, 50, 100, 250, 500, 1000, 1500, 2000, 2500)
        val portCount = 500

        println("\n=======================================================")
        println(" PORTX BENCHMARK 1: CONCURRENCY SATURATION SWEEP (500 Ports/step)")
        println("=======================================================")
        println("Concurrency | Elapsed (ms) | Throughput (Ports/sec)")
        println("-------------------------------------------------------")

        var peakRate = 0L
        var optimalConcurrency = 0

        for (c in concurrencyLevels) {
            val config = ScanConfig(
                target = "127.0.0.1",
                startPort = 30000,
                endPort = 30000 + portCount - 1,
                concurrency = c,
                timeoutMs = 120,
                serviceDetect = false,
                randomizePorts = false
            )

            val start = System.currentTimeMillis()
            val result = scanner.scan(config)
            val elapsed = maxOf(1L, System.currentTimeMillis() - start)
            val rate = (result.totalPorts * 1000L) / elapsed

            if (rate > peakRate) {
                peakRate = rate
                optimalConcurrency = c
            }

            println("    ${c.toString().padEnd(7)} | ${elapsed.toString().padEnd(12)} | $rate ports/sec")
            assertTrue(result.totalPorts == portCount)
        }

        println("-------------------------------------------------------")
        println(">> Peak Throughput: $peakRate ports/sec @ Concurrency = $optimalConcurrency")
        println("=======================================================\n")
    }

    @Test
    fun benchmark02_sustainedLargeSweepWithMemoryTelemetry(): Unit = runTest {
        val scanner = PortScanner()
        val totalPorts = 10000
        val concurrency = 1000

        System.gc()
        Thread.sleep(100)
        val memBefore = getUsedMemoryMb()
        val activeThreadsBefore = Thread.activeCount()

        println("\n=======================================================")
        println(" PORTX BENCHMARK 2: SUSTAINED HIGH-VOLUME SWEEP ($totalPorts Ports)")
        println("=======================================================")
        println("Configuration: Concurrency=$concurrency, Target=127.0.0.1, Range=20000..${20000 + totalPorts - 1}")

        val config = ScanConfig(
            target = "127.0.0.1",
            startPort = 20000,
            endPort = 20000 + totalPorts - 1,
            concurrency = concurrency,
            timeoutMs = 150,
            serviceDetect = false,
            randomizePorts = true
        )

        val start = System.currentTimeMillis()
        val result = scanner.scan(config)
        val elapsed = maxOf(1L, System.currentTimeMillis() - start)
        val memAfter = getUsedMemoryMb()
        val memDelta = memAfter - memBefore
        val activeThreadsAfter = Thread.activeCount()
        val rate = (result.totalPorts * 1000L) / elapsed

        println("-------------------------------------------------------")
        println("Execution Duration : ${elapsed} ms (${elapsed / 1000.0} seconds)")
        println("Total Scanned Ports: ${result.totalPorts}")
        println("Throughput Rate    : $rate ports/sec")
        println("Initial Heap Memory: $memBefore MB")
        println("Final Heap Memory  : $memAfter MB (Delta: $memDelta MB)")
        println("Active Threads     : Start=$activeThreadsBefore, End=$activeThreadsAfter")
        println("Port Accounting    : ${result.closedPorts} closed, ${result.filtered} filtered, ${result.openPorts} open")
        println("=======================================================\n")

        assertTrue(result.totalPorts == totalPorts)
        assertTrue(result.closedPorts + result.filtered + result.openPorts == totalPorts)
    }

    @Test
    fun benchmark03_adaptiveTimingConvergence(): Unit = runTest {
        println("\n=======================================================")
        println(" PORTX BENCHMARK 3: ADAPTIVE TIMING Q-LEARNING CONVERGENCE")
        println("=======================================================")

        val timing = AdaptiveTiming(200, 20000)
        val initialRate = timing.getRate()
        println("Initial Rate: $initialRate ports/sec")

        // 1. Simulate optimal LAN conditions (high success rate, sub-5ms latency)
        repeat(20) {
            timing.adapt(successRate = 98.0, latencyMs = 2L)
        }
        val highPerformanceRate = timing.getRate()
        val highPerfTimeout = timing.getAdaptiveTimeout(100)
        println("Post-Optimization (Clean Network) Rate: $highPerformanceRate ports/sec (Timeout: ${highPerfTimeout}ms)")

        // 2. Simulate network degradation / packet loss (congested, high latency)
        repeat(30) {
            timing.adapt(successRate = 35.0, latencyMs = 650L)
        }
        val backoffRate = timing.getRate()
        val backoffTimeout = timing.getAdaptiveTimeout(100)
        println("Post-Congestion (Degraded Network) Rate: $backoffRate ports/sec (Timeout: ${backoffTimeout}ms)")

        println("-------------------------------------------------------")
        println("Dynamic Rate Swing: from $highPerformanceRate down to $backoffRate ports/sec")
        println("Dynamic Timeout Backoff: from ${highPerfTimeout}ms up to ${backoffTimeout}ms")
        println("=======================================================\n")

        assertTrue(backoffRate < highPerformanceRate, "Engine must throttle rate under congestion")
        assertTrue(backoffTimeout >= highPerfTimeout, "Engine must expand timeout under latency")
    }

    @Test
    fun benchmark04_ipv4VsIpv6StackComparison(): Unit = runTest {
        val scanner = PortScanner()
        val count = 500

        println("\n=======================================================")
        println(" PORTX BENCHMARK 4: IPV4 VS IPV6 STACK COMPARISON ($count Ports)")
        println("=======================================================")

        // IPv4 Loopback
        val v4Config = ScanConfig(
            target = "127.0.0.1",
            startPort = 35000,
            endPort = 35000 + count - 1,
            concurrency = 250,
            timeoutMs = 120,
            serviceDetect = false,
            randomizePorts = false
        )
        val v4Start = System.currentTimeMillis()
        val v4Result = scanner.scan(v4Config)
        val v4Elapsed = maxOf(1L, System.currentTimeMillis() - v4Start)
        val v4Rate = (v4Result.totalPorts * 1000L) / v4Elapsed

        // IPv6 Loopback
        val v6Config = ScanConfig(
            target = "::1",
            startPort = 35000,
            endPort = 35000 + count - 1,
            concurrency = 250,
            timeoutMs = 120,
            serviceDetect = false,
            randomizePorts = false
        )
        val v6Start = System.currentTimeMillis()
        val v6Result = scanner.scan(v6Config)
        val v6Elapsed = maxOf(1L, System.currentTimeMillis() - v6Start)
        val v6Rate = (v6Result.totalPorts * 1000L) / v6Elapsed

        println("IPv4 (127.0.0.1) : $v4Elapsed ms -> $v4Rate ports/sec")
        println("IPv6 (::1)       : $v6Elapsed ms -> $v6Rate ports/sec")
        println("Stack Ratio      : IPv6/IPv4 Efficiency = ${"%.2f".format(v6Rate.toDouble() / v4Rate.toDouble())}x")
        println("=======================================================\n")

        assertTrue(v4Result.totalPorts == count)
        assertTrue(v6Result.totalPorts == count)
    }

    @Test
    fun benchmark05_serviceDetectionAndBannerGrabbingOverhead(): Unit = runTest {
        println("\n=======================================================")
        println(" PORTX BENCHMARK 5: BANNER GRABBING & SERVICE DETECTION OVERHEAD")
        println("=======================================================")

        // Spin up a lightweight local mock server on an ephemeral port
        val server = ServerSocket(0)
        val openPort = server.localPort
        val stopFlag = java.util.concurrent.atomic.AtomicBoolean(false)

        val serverThread = thread(isDaemon = true) {
            while (!stopFlag.get()) {
                try {
                    val client = server.accept()
                    client.getOutputStream().write("HTTP/1.1 200 OK\r\nServer: PortX-Mock/1.0\r\n\r\n<html><head><title>Telemetry Node</title></head></html>\r\n".toByteArray(Charsets.UTF_8))
                    client.getOutputStream().flush()
                    client.close()
                } catch (_: Exception) { break }
            }
        }

        try {
            val scanner = PortScanner()

            // 1. Raw Port Connect Scan (No service detect)
            val rawConfig = ScanConfig(
                target = "127.0.0.1",
                startPort = openPort,
                endPort = openPort,
                concurrency = 1,
                timeoutMs = 300,
                serviceDetect = false
            )
            val rawStart = System.nanoTime()
            val rawResult = scanner.scan(rawConfig)
            val rawDurationUs = (System.nanoTime() - rawStart) / 1000

            // 2. Deep Banner Grabbing Scan (Service detect enabled)
            val deepConfig = ScanConfig(
                target = "127.0.0.1",
                startPort = openPort,
                endPort = openPort,
                concurrency = 1,
                timeoutMs = 300,
                serviceDetect = true
            )
            val deepStart = System.nanoTime()
            val deepResult = scanner.scan(deepConfig)
            val deepDurationUs = (System.nanoTime() - deepStart) / 1000

            val detectedService = deepResult.results.firstOrNull { it.port == openPort }

            println("Open Port Scanned    : $openPort")
            println("Raw Connect Latency  : $rawDurationUs microseconds (${rawDurationUs / 1000.0} ms)")
            println("Deep Banner Latency  : $deepDurationUs microseconds (${deepDurationUs / 1000.0} ms)")
            println("Detected Service     : ${detectedService?.service}")
            println("Extracted HTTP Title : ${detectedService?.httpInfo?.title}")
            println("Server Signature     : ${detectedService?.httpInfo?.server}")
            println("Overhead Ratio       : Deep/Raw = ${"%.2f".format(deepDurationUs.toDouble() / maxOf(1L, rawDurationUs).toDouble())}x")
            println("=======================================================\n")

            assertTrue(rawResult.openPorts == 1)
            assertTrue(deepResult.openPorts == 1)
            assertTrue(detectedService?.httpInfo?.title == "Telemetry Node")
        } finally {
            stopFlag.set(true)
            try { server.close() } catch (_: Exception) {}
            serverThread.interrupt()
        }
    }

    @Test
    fun benchmark06_securityAndAnomalyEngineThroughput(): Unit = runTest {
        println("\n=======================================================")
        println(" PORTX BENCHMARK 6: SECURITY POSTURE & ANOMALY HEURISTICS")
        println("=======================================================")

        val anomalyUseCase = AnomalyDetectionUseCase()
        val scoreUseCase = SecurityScoreUseCase()

        // Construct mock ScanResult with 25 mixed standard, cloud, and ICS/SCADA vectors
        val testPorts = listOf(
            21, 22, 23, 80, 102, 135, 139, 161, 389, 443, 445, 502,
            1883, 1900, 2049, 2181, 2375, 2379, 3389, 4840, 5555, 5683,
            5900, 6379, 8080, 9200, 10250, 11211, 27017, 47808
        )
        val mockResult = ScanResult(
            target = "10.0.0.1",
            openPorts = testPorts,
            timestamp = System.currentTimeMillis(),
            securityScore = 0
        )

        val iterations = 10000
        val startNano = System.nanoTime()
        var totalAnomaliesCount = 0
        var totalScoreSum = 0

        for (i in 0 until iterations) {
            val anomalies = anomalyUseCase(mockResult)
            val score = scoreUseCase(mockResult)
            totalAnomaliesCount += anomalies.size
            totalScoreSum += score
        }

        val elapsedNs = System.nanoTime() - startNano
        val elapsedMs = elapsedNs / 1_000_000.0
        val opsPerSec = (iterations * 1000.0) / elapsedMs
        val latencyUsPerOp = (elapsedNs / 1000.0) / iterations

        println("Evaluated Iterations   : $iterations")
        println("Total Evaluation Time  : ${"%.2f".format(elapsedMs)} ms")
        println("Engine Throughput Rate : ${opsPerSec.toLong()} evaluations/sec")
        println("Single Decision Latency: ${"%.3f".format(latencyUsPerOp)} microseconds")
        println("Identified Anomalies   : ${totalAnomaliesCount / iterations} distinct vectors detected")
        println("Computed Posture Score : ${totalScoreSum / iterations}%")
        println("=======================================================\n")

        assertTrue(totalAnomaliesCount > 0)
        assertTrue(totalScoreSum >= 0)
    }

    @Test
    fun benchmark07_reportSerializationThroughput(): Unit = runTest {
        println("\n=======================================================")
        println(" PORTX BENCHMARK 7: REPORT SERIALIZATION ENGINE (1,000 Ports)")
        println("=======================================================")

        val exportUseCase = ExportReportUseCase()
        val ports = (1000..1999).toList()
        val services = ports.associateWith { "service-$it" }
        val banners = ports.associateWith { "Server: PortX-Daemon/$it OpenSSL/3.0.2" }

        val largeResult = ScanResult(
            target = "192.168.1.100",
            openPorts = ports,
            portServices = services,
            portBanners = banners,
            timestamp = System.currentTimeMillis(),
            securityScore = 45,
            deviceName = "Core-Edge-Gateway",
            osFingerprint = "Linux 6.8 Enterprise Kernel"
        )

        // 1. CSV Format
        val csvStart = System.nanoTime()
        val csvOutput = exportUseCase(largeResult, "CSV")
        val csvDurationMs = (System.nanoTime() - csvStart) / 1_000_000.0

        // 2. Markdown Format
        val mdStart = System.nanoTime()
        val mdOutput = exportUseCase(largeResult, "MD")
        val mdDurationMs = (System.nanoTime() - mdStart) / 1_000_000.0

        // 3. JSON Format
        val jsonStart = System.nanoTime()
        val jsonOutput = exportUseCase(largeResult, "JSON")
        val jsonDurationMs = (System.nanoTime() - jsonStart) / 1_000_000.0

        println("CSV Generation   : ${"%.2f".format(csvDurationMs)} ms (${csvOutput.length} bytes, ${"%.2f".format((csvOutput.length / 1024.0) / (csvDurationMs / 1000.0))} KB/sec)")
        println("Markdown Report  : ${"%.2f".format(mdDurationMs)} ms (${mdOutput.length} bytes, ${"%.2f".format((mdOutput.length / 1024.0) / (mdDurationMs / 1000.0))} KB/sec)")
        println("JSON Export      : ${"%.2f".format(jsonDurationMs)} ms (${jsonOutput.length} bytes, ${"%.2f".format((jsonOutput.length / 1024.0) / (jsonDurationMs / 1000.0))} KB/sec)")
        println("=======================================================\n")

        assertTrue(csvOutput.isNotEmpty())
        assertTrue(mdOutput.isNotEmpty())
        assertTrue(jsonOutput.isNotEmpty())
    }

    @Test
    fun benchmark08_udpProbeEngineThroughput(): Unit = runTest {
        println("\n=======================================================")
        println(" PORTX BENCHMARK 8: UDP PROBE SYNTHESIS & SCAN DISPATCH")
        println("=======================================================")

        val scanner = PortScanner()

        // 1. Measure byte-payload synthesis speed across RFC specs
        val probePorts = listOf(53, 123, 161, 1900, 5683, 9999)
        val synthesisIterations = 50000
        val synStart = System.nanoTime()
        var totalPayloadBytes = 0

        for (i in 0 until synthesisIterations) {
            val p = probePorts[i % probePorts.size]
            val payload = scanner.getUdpProbePayload(p)
            totalPayloadBytes += payload.size
        }
        val synElapsedMs = (System.nanoTime() - synStart) / 1_000_000.0
        val synRate = (synthesisIterations * 1000.0) / synElapsedMs

        println("Payload Synthesis Speed : ${synRate.toLong()} probes/sec (${"%.2f".format(synElapsedMs)} ms for $synthesisIterations probes)")

        // 2. Live UDP socket dispatch on loopback
        val udpConfig = ScanConfig(
            target = "127.0.0.1",
            startPort = 45000,
            endPort = 45019,
            concurrency = 20,
            timeoutMs = 150,
            scanType = "UDP",
            serviceDetect = false
        )

        val netStart = System.currentTimeMillis()
        val result = scanner.scan(udpConfig)
        val netElapsed = maxOf(1L, System.currentTimeMillis() - netStart)
        val udpRate = (result.totalPorts * 1000L) / netElapsed

        println("UDP Loopback 20-Port Dispatch: ${netElapsed} ms -> $udpRate ports/sec")
        println("UDP Accounting        : ${result.closedPorts} closed, ${result.filtered} filtered, ${result.openPorts} open")
        println("=======================================================\n")

        assertTrue(result.totalPorts == 20)
    }
}
