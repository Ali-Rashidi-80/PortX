package com.mrcoder20.portx.data.network

import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

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
}
