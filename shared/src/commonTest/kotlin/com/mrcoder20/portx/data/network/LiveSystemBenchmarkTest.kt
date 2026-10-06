package com.mrcoder20.portx.data.network

import kotlinx.coroutines.test.runTest
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
        val concurrencyLevels = listOf(50, 100, 250, 500, 1000)
        val portCount = 500

        println("\n=======================================================")
        println(" PORTX BENCHMARK 1: CONCURRENCY SCALING (500 Ports/step)")
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
        val totalPorts = 3000
        val concurrency = 500

        System.gc()
        Thread.sleep(100)
        val memBefore = getUsedMemoryMb()

        println("\n=======================================================")
        println(" PORTX BENCHMARK 2: SUSTAINED SWEEP ($totalPorts Ports)")
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
        val rate = (result.totalPorts * 1000L) / elapsed

        println("-------------------------------------------------------")
        println("Execution Duration : ${elapsed} ms (${elapsed / 1000.0} seconds)")
        println("Total Scanned Ports: ${result.totalPorts}")
        println("Throughput Rate    : $rate ports/sec")
        println("Initial Heap Memory: $memBefore MB")
        println("Final Heap Memory  : $memAfter MB (Delta: $memDelta MB)")
        println("Closed/Filtered    : ${result.closedPorts} closed, ${result.filtered} filtered")
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
}
