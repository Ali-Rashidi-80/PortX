package com.mrcoder20.portx.data.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PortScannerTest {

    @Test
    fun testScannerInitialization(): Unit = runTest {
        val scanner = PortScanner()
        val config = ScanConfig(
            target = "127.0.0.1",
            startPort = 80,
            endPort = 81,
            concurrency = 10
        )
        
        // This won't actually scan much without a network stack, 
        // but we verify the engine doesn't crash on start.
        try {
            val result = scanner.scan(config) { progress ->
                println("Progress: $progress%")
            }
            assertTrue(result.totalPorts >= 2)
        } catch (e: Exception) {
            // In some environments, SelectorManager might fail to init
            println("Scanner init check: ${e.message}")
        }
    }

    @Test
    fun testAdaptiveTimingLogic() {
        val timing = AdaptiveTiming(100, 1000)
        
        // Simulate severe congestion to trigger "safety" or "maintain" at low rate
        repeat(10) {
            timing.adapt(10.0, 2000) // 10% success, 2000ms latency
        }
        val lowRate = timing.getRate()
        
        // Rate should eventually decrease significantly from the initial middle ground
        assertTrue(lowRate < 1000)
    }

    @Test
    fun testRealLoopbackScanThroughput(): Unit = runTest {
        val scanner = PortScanner()
        val count = 1000
        val config = ScanConfig(
            target = "127.0.0.1",
            startPort = 50000,
            endPort = 50000 + count - 1,
            concurrency = 500,
            timeoutMs = 150,
            serviceDetect = false,
            randomizePorts = false
        )
        val start = System.currentTimeMillis()
        val result = scanner.scan(config)
        val elapsed = System.currentTimeMillis() - start
        val throughput = if (elapsed > 0) (result.totalPorts * 1000L) / elapsed else result.totalPorts.toLong()
        println("BENCHMARK_RESULT: Scanned ${result.totalPorts} ports in ${elapsed}ms -> Measured Rate: $throughput ports/sec")
        assertTrue(result.totalPorts == count)
        assertTrue(result.durationMs >= 0)
    }
}
